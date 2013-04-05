/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Initial developer(s): Alexandre Pinto and Hugo Miranda.
 * Contributor(s): See Appia web page for a list of contributors.
 */

/**
 * Title:        Apia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Alexandre Pinto & Hugo Miranda & Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto & Hugo Miranda & Luis Rodrigues
 * @version 1.0
 */
package net.sf.appia.protocols.group.inter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.ListIterator;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.AppiaMulticast;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.AppiaGroupException;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.heal.ConcurrentViewEvent;
import net.sf.appia.protocols.group.intra.PreView;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.intra.ViewChange;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import org.apache.log4j.Logger;



public class InterSession extends Session implements InitializableSession {
    private static Logger log = Logger.getLogger(InterSession.class);

    // TODO new values
    public static final long DEFAULT_TERMINATION_TIME=15000; // 15 seconds
    public static final long DEFAULT_WAITING_TIME=1500; // 1,5 seconds

    public InterSession(Layer layer) {
        super(layer);
    }

    /**
     * Initializes the session using the parameters given in the XML configuration.
     * Possible parameters:
     * <ul>
     * <li><b>termination</b> maximum time duration of a view merge. (in milliseconds)
     * <li><b>waiting</b> time to wait for additional concurrent views, to allow the merging of several views in a single step. (in milliseconds) 
     * </ul>
     * 
     * @param params The parameters given in the XML configuration.
     */
    public void init(SessionProperties params) {
        if (params.containsKey("termination"))
            termination_time=params.getLong("termination");
        if (params.containsKey("waiting"))
            waiting_time=params.getLong("waiting");
    }

    public void handle(Event event) {

        // MergeEvent
        if (event instanceof MergeEvent) {
            handleMergeEvent((MergeEvent)event); return;
        } else if (event instanceof MergeTimer) {
            handleMergeTimer((MergeTimer)event); return;
            // ConcurrentViewEvent
        } else if (event instanceof ConcurrentViewEvent) {
            handleConcurrent((ConcurrentViewEvent)event); return;
            // PreView
        } else if (event instanceof PreView) {
            handlePreView((PreView)event); return;
            // View
        } else if (event instanceof View) {
            handleView((View)event); return;
        }

        log.warn("Unwanted event (\""+event.getClass().getName()+"\") received. Continued...");
        try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private long termination_time=DEFAULT_TERMINATION_TIME;
    private long waiting_time=DEFAULT_WAITING_TIME;
    private ViewState vs=null;
    private LocalState ls;
    private int round;
    private ArrayList views=new ArrayList();
    private boolean waited;
    private ViewInfo myInfo;
    private boolean sent_viewchange;
    private boolean sent_preview;
    private PreView preview;
    private long timerID=0;

    private void reset() {
        round=0;
        views.clear();
        waited=false;
        myInfo=null;

        if (preview != null) {
            try {
                preview.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
            }
            preview=null;
            sent_preview=true;
        }
    }

    private void handleView(View ev) {
        vs=ev.vs;
        ls=ev.ls;

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        sent_viewchange=false;
        sent_preview=false;
        preview=null;
        reset();
    }

    private void handlePreView(PreView ev) {
        if (views.size() == 0) {
            try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
            sent_preview=true;
            return;
        }

        myInfo.vs=ev.vs;
        preview=ev;

        decide(ev.getChannel());
    }

    private void handleConcurrent(ConcurrentViewEvent ev) {
        if (!ls.am_coord || (ev.id == null)) {
            try {
                ev.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
            return;
        }

        if (sent_preview) {
            log.debug("Concurrent view warning discarded because i already sent PreView"+
            ". Wait for next view.");
            return;
        }

        if ((myInfo != null) && myInfo.decided) {
            log.debug("Concurrent view warning discarded because i have already decided.");
            return;
        }

        if (find(ev.id) >= 0) {
            log.debug("Received duplicate concurrent view warning. Ignoring it.");
            return;
        }

        round++;

        if (views.size() == 0) {
            myInfo=new ViewInfo(vs.id,vs.addresses[ls.my_rank]);
            addSorted(myInfo);
            sendTimers(ev.getChannel(),true);
        }

        addSorted(new ViewInfo(ev.id,(InetSocketAddress)ev.addr));
        if (debugFull)
            debugViews("created new proposal");

        if (!validateProposal()) {
            log.debug("Generated invalid proposal. Aborting merge.");
            sendAbort(ev.getChannel());
            reset();
            return;
        }

        sendPropose(ev.getChannel());
        myInfo.proposed=true;
        decide(ev.getChannel());
    }

    private void handleMergeTimer(MergeTimer ev) {
        if (ev.timerID.equals("WAIT "+timerID+" "+this)) {
            log.debug("Wait timer expired.");
            waited=true;
            decide(ev.getChannel());
        } else if (ev.timerID.equals("TERMINATE "+timerID+" "+this)) {
            log.debug("Terminate timer expired.");
            if (views.size() > 0) {
                sendAbort(ev.getChannel());
                reset();
            }
        }
    }

    private void handleMergeEvent(MergeEvent ev) {
        int r,n,sender;
        ViewID[] vids;
        InetSocketAddress[] addrs;
        boolean same;

        int type=ev.getMessage().popInt();
        switch (type) {
        case MergeEvent.PROPOSE:
            r=ev.getMessage().popInt();
            n=ev.getMessage().popInt();
            vids=new ViewID[n];
            addrs=new InetSocketAddress[n];
            same=readProposal(ev.getMessage(),n,vids,addrs);
            sender=ev.getMessage().popInt();
            if (same && (r == round)) {
                log.debug("Received identical proposal from "+vids[sender]+"("+addrs[sender]+") with address "+ev.source);
                receiveIdenticalProposal(sender, ev.getChannel());
            } else {
                log.debug("Received new proposal from "+vids[sender]+"("+addrs[sender]+") with address "+ev.source);
                if (debugFull)
                    debugProposal("New proposal", r, n, vids, addrs);
                receiveNewProposal(sender, r, vids, addrs, ev.getChannel());
            }
            break;
        case MergeEvent.DECIDE:
            r=ev.getMessage().popInt();
            n=ev.getMessage().popInt();
            vids=new ViewID[n];
            addrs=new InetSocketAddress[n];
            same=readProposal(ev.getMessage(),n,vids,addrs);
            sender=ev.getMessage().popInt();
            ViewState sender_vs=ViewState.pop(ev.getMessage());
            if (same && (r == round)) {
                log.debug("Received identical decide from "+vids[sender]+"("+addrs[sender]+") with address "+ev.source);
                receiveIdenticalDecide(sender,sender_vs);
            } else {
                log.debug("Received new decide from "+vids[sender]+"("+addrs[sender]+") with address "+ev.source);
                if (debugFull)
                    debugProposal("New decide", r, n, vids, addrs);
                receiveNewDecide(sender,r,vids,addrs,sender_vs,ev.getChannel());
            }
            break;
        case MergeEvent.ABORT:
            ViewID sender_id=ViewID.pop(ev.getMessage());
            if (find(sender_id) < 0) {
                log.debug("Received abort from "+sender_id+" with address "+ev.source);
                sendAbort(ev.getChannel());
                reset();
            } else {
                log.debug("Received abort from unknown sender ("+sender_id+"). Ignoring it.");
            }
            break;
        }
    }

    private void receiveIdenticalProposal(int sender, Channel channel) {
        ViewInfo info=(ViewInfo)views.get(sender);
        info.proposed=true;
        // If received at least one Proposal then we can request the view change
        if (!sent_viewchange)
            sendViewChange(channel);
    }

    private boolean receiveNewProposal(int sender, int r, ViewID[] vids, InetSocketAddress[] addrs, Channel channel) {
        if (sent_preview) {
            log.debug("Received new proposal but already sent PreView");
            sendAbort(channel,addrs[sender]);
            return false;
        }

        if (views.size() == 0) {
            int i, myIndex;
            for(i=0 ; i < vids.length ; i++)
                views.add(new ViewInfo(vids[i],addrs[i]));
            round=r;
            if ((myIndex=find(vs.id)) < 0) {
                log.debug("Received new proposal but i don't belong. Ignoring it.");
                reset();
                return false;
            } 
            myInfo=(ViewInfo)views.get(myIndex);
            sendTimers(channel, true);
        } else {
            int i;
            int originalSize=views.size();
            int newElements=0;
            int commonElements=0;

            for (i=0 ; i < vids.length ; i++) {
                if (find(vids[i]) < 0) {
                    addSorted(new ViewInfo(vids[i],addrs[i]));
                    newElements++;
                } else {
                    commonElements++;
                }
            }

            if ((newElements > 0) || (round > r)) {
                for (i=0 ; i < views.size() ; i++) {
                    ViewInfo info=(ViewInfo)views.get(i);
                    info.decided=false;
                }
                if (commonElements == originalSize) {
                    round=r;
                    log.debug("Received new updated proposal.");
                } else {
                    round=(r > round ? r : round)+1;
                    myInfo.proposed=false;
                    log.debug("Created new proposal round.");
                } 
            } else {
                log.debug("Received old proposal. Ignoring it.");
                return false;
            }
        }

        if (debugFull)
            debugViews("new proposal");

        if (!validateProposal()) {
            log.debug("Generated invalid proposal. Aborting merge.");
            sendAbort(channel);
            reset();
            return false;
        }

        if (!myInfo.proposed) {
            sendPropose(channel);
            myInfo.proposed=true;
        }
        receiveIdenticalProposal(sender, channel);
        decide(channel);
        return true;
    }

    private void decide(Channel channel) {
        if (!waited)
            return;
        if (preview == null)
            return;

        myInfo.decided=true;
        myInfo.vs=preview.vs;

        if (debugFull)
            debugViews("decided");

        sendDecide(channel);
        conclude();
    }

    private void receiveIdenticalDecide(int sender, ViewState sender_vs) {
        ViewInfo info=(ViewInfo)views.get(sender);
        info.vs=sender_vs;
        info.decided=true;
        conclude();
    }

    private void receiveNewDecide(int sender, int r, ViewID[] vids, InetSocketAddress[] addrs, ViewState sender_vs, Channel channel) {
        if (receiveNewProposal(sender, r, vids, addrs, channel))
            receiveIdenticalDecide(sender, sender_vs);
    }

    private void conclude() {
        int i;
        for(i=0 ; i < views.size() ; i++) {
            ViewInfo info=(ViewInfo)views.get(i);
            if (!info.decided)
                return;
        }

        ViewState backupvs=preview.vs;
        try {
            preview.vs=mergeViews();
            preview.go();
            preview=null;
            sent_preview=true;
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            preview.vs=backupvs;
        }
    }

    private int find(ViewID id) {
        int i;
        for (i=0 ; i < views.size() ; i++) {
            ViewInfo no=(ViewInfo)views.get(i);
            if (no.id.equals(id))
                return i;
        }
        return -1;
    }

    private void addSorted(ViewInfo info) {
        int i;
        for (i=0 ; i < views.size() ; i++) {
            ViewInfo no=(ViewInfo)views.get(i);
            if (info.id.ltime > no.id.ltime || 
                    ((info.id.ltime == no.id.ltime) && (info.id.coord.id.compareTo(no.id.coord.id) < 0))) {
                views.add(i, info);
                return;
            }
        }
        views.add(info);
    }

    private boolean validateProposal() {
        int i,j,k;
        for (i=0 ; i < views.size() ; i++) {
            ViewInfo info=(ViewInfo)views.get(i);
            for (k=0 ; k < vs.previous.length ; k++) {
                if (info.id.equals(vs.previous[k])) {
                    if (debugFull)
                        log.debug("validateProposal: has previous.");
                    return false;
                }
            }
            for (j=i+1 ; j < views.size() ; j++) {
                ViewInfo aux=(ViewInfo)views.get(j);
                if (info.addr.equals(aux.addr)) {
                    if (debugFull)
                        log.debug("validateProposal: duplicate address.");
                    return false;
                }
            }
        }
        return true;
    }

    private ViewState mergeViews() {
        ArrayList vss=new ArrayList(views.size());
        int i;
        for (i=0 ; i < views.size() ; i++) {
            ViewInfo info=(ViewInfo)views.get(i);
            vss.add(info.vs);
        }

        ViewState newVS=null;
        try {
            newVS=ViewState.merge(vss);
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (AppiaGroupException e) {
            e.printStackTrace();
        }
        return newVS;
    }

    private void sendViewChange(Channel channel) {
        try {
            ViewChange ev=new ViewChange(channel,Direction.DOWN,this,vs.group,vs.id);
            ev.go();
            sent_viewchange=true;
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            log.debug("Impossible to send \"ViewChange\"");
        }
    }

    private void sendTimers(Channel channel, boolean on) {
        ++timerID;
        try {
            MergeTimer ev=new MergeTimer(waiting_time,"WAIT "+timerID+" "+this,channel,Direction.DOWN,this,(on ? EventQualifier.ON : EventQualifier.OFF));
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            waited=true;
        } catch (AppiaException e) {
            e.printStackTrace();
            waited=true;
        }
        try {
            MergeTimer ev=new MergeTimer(termination_time+waiting_time,"TERMINATE "+timerID+" "+this,channel,Direction.DOWN,this,(on ? EventQualifier.ON : EventQualifier.OFF));
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        } catch (AppiaException e) {
            e.printStackTrace();
        }
    }

    private void sendPropose(Channel channel) {
        int j,i;
        int n=views.size();
        int sender=find(vs.id);

        i=0;
        Object[] dests=new Object[n-1];
        for (j=0 ; j < n ; j++) {
            if (j != sender) {
                ViewInfo dest=(ViewInfo)views.get(j);
                dests[i++]=dest.addr;
            }
        }

        try {
            MergeEvent ev=new MergeEvent(channel,Direction.DOWN,this);
            ev.dest=new AppiaMulticast(null,dests);

            ev.getMessage().pushInt(sender);
            writeProposal(ev.getMessage());
            ev.getMessage().pushInt(n);
            ev.getMessage().pushInt(round);
            ev.getMessage().pushInt(MergeEvent.PROPOSE);

            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void sendDecide(Channel channel) {
        int j,i;
        int n=views.size();
        int sender=find(vs.id);

        i=0;
        Object[] dests=new Object[n-1];    
        for (j=0 ; j < n ; j++) {
            if (j != sender) {
                ViewInfo dest=(ViewInfo)views.get(j);
                dests[i++]=dest.addr;
            }
        }

        try {
            MergeEvent ev=new MergeEvent(channel,Direction.DOWN,this);
            ev.dest=new AppiaMulticast(null,dests);

            ViewState.push(preview.vs,ev.getMessage());
            ev.getMessage().pushInt(sender);
            writeProposal(ev.getMessage());
            ev.getMessage().pushInt(n);
            ev.getMessage().pushInt(round);
            ev.getMessage().pushInt(MergeEvent.DECIDE);

            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void sendAbort(Channel channel) {
        int j;
        int n=views.size();

        for (j=0 ; j < n ; j++) {
            ViewInfo dest=(ViewInfo)views.get(j);
            if (!dest.id.equals(vs.id)) {
                sendAbort(channel,(InetSocketAddress)dest.addr);
            }
        }    
    }

    private void sendAbort(Channel channel, InetSocketAddress dest) {
        try {
            MergeEvent ev=new MergeEvent(channel,Direction.DOWN,this);
            ev.dest=dest;

            ViewID.push(vs.id, ev.getMessage());
            ev.getMessage().pushInt(MergeEvent.ABORT);

            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }    
    }

    private void writeProposal(Message msg) {
        int i;
        int n=views.size();

        for (i=n-1 ; i >= 0 ; i--) {
            ViewInfo info=(ViewInfo)views.get(i);
            msg.pushObject(info.addr);
            ViewID.push(info.id, msg);
        }
    }

    private boolean readProposal(Message msg, int n, ViewID[] vids, InetSocketAddress[] addrs) {
        boolean equal=(n == views.size());

        for (int i=0 ; i < n ; i++) {
            vids[i]=ViewID.pop(msg);
            addrs[i]=(InetSocketAddress) msg.popObject();

            if (equal) {
                ViewInfo info=(ViewInfo)views.get(i);
                if (!info.id.equals(vids[i]))
                    equal=false;
            }
        }

        return equal;
    }

    private class ViewInfo {
        public ViewID id;
        public ViewState vs;
        public boolean proposed;
        public boolean decided;
        public SocketAddress addr;

        public ViewInfo(ViewID id, SocketAddress addr) {
            this.id=id;
            vs=null;
            proposed=false;
            decided=false;
            this.addr=addr;
        }
    }

    /*
  public static void main(String[] args) {
    InterSession session=(InterSession)new InterLayer().createSession();
    ViewInfo info;

    try {
      info=session.new ViewInfo(new ViewID(1,new Endpt("g")),null);
      info.vs=new ViewState("1",new Group("G"),info.id,new ViewID[] {new ViewID(5,new Endpt("v1"))}, new Endpt[] {new Endpt("eg")},new InetWithPort[] {new InetWithPort()});
      session.addSorted(info);

      info=session.new ViewInfo(new ViewID(2,new Endpt("c")),null);
      info.vs=new ViewState("1",new Group("G"),info.id,new ViewID[] {new ViewID(5,new Endpt("v2"))}, new Endpt[] {new Endpt("ec")},new InetWithPort[] {new InetWithPort()});
      session.addSorted(info);

      info=session.new ViewInfo(new ViewID(3,new Endpt("b")),null);
      info.vs=new ViewState("1",new Group("G"),info.id,new ViewID[] {new ViewID(5,new Endpt("v3"))}, new Endpt[] {new Endpt("eb")},new InetWithPort[] {new InetWithPort()});
      session.addSorted(info);

      info=session.new ViewInfo(new ViewID(3,new Endpt("d")),null);
      info.vs=new ViewState("2",new Group("G"),info.id,new ViewID[] {new ViewID(5,new Endpt("v4"))}, new Endpt[] {new Endpt("ed")},new InetWithPort[] {new InetWithPort()});
      session.addSorted(info);

      info=session.new ViewInfo(new ViewID(2,new Endpt("f")),null);
      info.vs=new ViewState("1",new Group("G"),info.id,new ViewID[] {new ViewID(5,new Endpt("v5a")), new ViewID(6, new Endpt("v5b"))}, new Endpt[] {new Endpt("ef")},new InetWithPort[] {new InetWithPort()});
      session.addSorted(info);

      info=session.new ViewInfo(new ViewID(4,new Endpt("e")),null);
      info.vs=new ViewState("0.5",new Group("G"),info.id,new ViewID[] {new ViewID(5,new Endpt("v6"))}, new Endpt[] {new Endpt("ee")},new InetWithPort[] {new InetWithPort()});
      session.addSorted(info);

      info=session.new ViewInfo(new ViewID(1,new Endpt("h")),null);
      info.vs=new ViewState("1",new Group("G"),info.id,new ViewID[] {new ViewID(5,new Endpt("v7"))}, new Endpt[] {new Endpt("eh")},new InetWithPort[] {new InetWithPort()});
      session.addSorted(info);

      info=session.new ViewInfo(new ViewID(4,new Endpt("a")),null);
      info.vs=new ViewState("0.2",new Group("G"),info.id,new ViewID[] {new ViewID(5,new Endpt("v8"))}, new Endpt[] {new Endpt("ea")},new InetWithPort[] {new InetWithPort()});
      session.addSorted(info);
    } catch (Exception ex) {
      ex.printStackTrace();
    }

    int i;
    for (i=0 ; i < session.views.size() ; i++) {
      info=(ViewInfo)session.views.get(i);
      System.out.println("views["+i+"]: "+info.id+" "+info.addr);
    }
    System.out.println(session.mergeViews().toString());

  }
     */

    // DEBUG
    public static final boolean debugFull=true;

    private void debugViews(String s) {
        if (log.isDebugEnabled()) {
            ListIterator iter=views.listIterator();
            log.debug("appia:group:InterSession:VIEWS("+round+"): "+s);
            while (iter.hasNext()) {
                ViewInfo info=(ViewInfo)iter.next();
                log.debug("\t-> "+info.id);
                log.debug("\t\tproposed = "+info.proposed);
                log.debug("\t\tdecided = "+info.decided);
                log.debug("\t\tvs = "+(info.vs != null ? "filled" : "null"));
                log.debug("\t\taddress = "+info.addr);
            }
        }
    }

    private void debugProposal(String s, int r, int n, ViewID[] vids, InetSocketAddress[] addrs) {
        if (log.isDebugEnabled()) {
            s+=" round="+r+"("+round+") size="+n+"("+views.size()+") vids={";
            for (int x=0 ; x < vids.length ; x++)
                s+=vids[x]+",";
            s+="} addrs={";
            for (int x=0 ; x < addrs.length ; x++)
                s+=addrs[x]+",";
            s+="}";
            log.debug("appia:group:InterSession: "+s);
        }
    }
}
