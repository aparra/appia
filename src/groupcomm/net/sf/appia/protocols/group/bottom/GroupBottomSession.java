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
package net.sf.appia.protocols.group.bottom;


import java.net.InetSocketAddress;
import java.net.SocketAddress;

import net.sf.appia.core.*;
import net.sf.appia.core.events.AppiaMulticast;
import net.sf.appia.core.events.AppiaMulticastSupport;
import net.sf.appia.core.message.*;
import net.sf.appia.protocols.group.*;
import net.sf.appia.protocols.group.events.*;
import net.sf.appia.protocols.group.intra.View;

import org.apache.log4j.Logger;

/**
 * The <i>group communication</i> bottommost Session.
 * <br>
 * Converts group <i>cast</i> messages into point to point messages for each
 * of the members of the group, if <i>IP multicast</i> is not being used.
 * <br>
 * It also filters events that don't belong to the current view.
 */
public class GroupBottomSession extends Session {
    private static Logger log = Logger.getLogger(GroupBottomSession.class);

    public static final int BUFFER_SIZE=100;

    public GroupBottomSession(Layer layer) {
        super(layer);
    }

    public void handle(Event event) {

        // GroupSendableEvent
        if (event instanceof GroupSendableEvent) {
            if (event.getDir() == Direction.DOWN)
                handleDownGroupSendableEvent((GroupSendableEvent)event);
            else
                handleUpGroupSendableEvent((GroupSendableEvent)event);
            return;
        }

        // View
        // it must allway be before GroupEvent
        if (event instanceof View) {
            handleView((View)event);
            return;
        }

        // GroupEvent
        if (event instanceof GroupEvent) {
            handleGroupEvent((GroupEvent)event);
            return;
        }

        // GroupInit
        if (event instanceof GroupInit) {
            ip_multicast=(InetSocketAddress) ((GroupInit)event).getIPmulticast();
            try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
            return;
        }

        log.warn("Unwanted event (\""+event.getClass().getName()+"\") received. Continued...");
        try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private ViewState vs;
    private LocalState ls;
    private InetSocketAddress ip_multicast;
    private EventBuffer buffer=new EventBuffer(BUFFER_SIZE);
    private int viewHashCode;
    private int groupHashCode;

    private boolean[] same_view=new boolean[0];
    private boolean send_prevs=false;

    private int[] all=null;
    private AppiaMulticast optAppiaMulticast=null;
    private boolean supportsAppiaMulticast=false;


    private void handleUpGroupSendableEvent(GroupSendableEvent ev) {
        Message omsg=ev.getMessage();

        if (vs == null) {
            buffer.put(ev);
            return;
        }

        try {
            int groupHash=omsg.popInt();

            if (groupHash != groupHashCode) {
                log.warn("discarded event ("+ev+") due to bad \"group\"");
                return;
            }

            int viewHash=omsg.popInt();
            short nprevs=omsg.popShort();

            if (viewHash != viewHashCode) {
                // check if event belongs to the next view
                // if it does, buffer it, if not send other view warning
                boolean isNext=false;
                int prevHash;
                while (nprevs > 0) {
                    prevHash=omsg.popInt();
                    if (!isNext && (prevHash == viewHashCode))
                        isNext=true;
                    nprevs--;
                }
                if (isNext) {
                    if (debugFull)
                        log.debug("buffering event from possible next view (hash="+viewHash+").");
                    omsg.pushShort((short)0);
                    omsg.pushInt(viewHash);
                    omsg.pushInt(groupHash);
                    buffer.put(ev);
                    return;
                }

                // check if event belongs to the previous view
                // if it does, discard it
                for (int i=0 ; i < vs.previous.length ; i++) {
                    if (viewHash == vs.previous[i].hashCode()) {
                        log.debug("discarded event because it belonged to previous view");
                        return;
                    }
                }

                // it doesn't belong to the current, next or previous view
                // therefore send other view notification
                log.debug("received event from same \"group\", but different \"view id\". Sent OtherView. "+ ev.getClass().getName());
                sendOtherViews(OtherViews.NOTIFY,ev.source,ip_multicast,ev.getChannel());
                return;
            } else {
                // view id is equal, previous view isn't necessary
                omsg.discard(nprevs*4);
            }

            ev.orig=omsg.popInt();

            if ((ev.orig < 0) || (ev.orig >= vs.view.length) || (ev.orig == ls.my_rank)) {
                log.debug("Event discarded due to bad origin "+ev.orig);
                return;
            }

            if (send_prevs) {
                same_view[ev.orig]=true;
                int i;
                for (i=0 ; (i < same_view.length) && same_view[i] ; i++);
                send_prevs= i < same_view.length;
            }

            if (ev instanceof Send) {
                bitset.setBitsFromMessage(ev.getMessage(),AppiaBitSet.POP,vs.view.length);
                if (!bitset.get(ls.my_rank)) {
                    log.debug("send event discarded because it wasn't for me ("+ev+")");
                    return;
                }
            }

            ev.dest=null;
            ev.source=vs.view[ev.orig];
            ev.group = vs.group;
            ev.view_id = vs.id;

            if (debugFull)
                log.debug("Received message "+ev+" from "+ev.orig);

            try {
                ev.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
                log.warn("up event discarded");
            }
        } catch (MessageException ex) {
            log.warn("Event (\""+ev.getClass().getName()+"\") discarded because i couldn't read headers.");
            return;
        }
    }

    private AppiaBitSet bitset=new AppiaBitSet(0);

    private void handleDownGroupSendableEvent(GroupSendableEvent ev) {
        if ((vs == null) || (vs.view.length == 1)) {
            log.debug("Trying to send to somenone but i'm the only one. Discarding Event");
            return;
        }

        boolean isSend=ev instanceof Send;
        Message omsg=ev.getMessage();

        if (isSend) {
            int[] dests=(int[])ev.dest;
            bitset.setBitsFromMessage(omsg,AppiaBitSet.PUSH,vs.view.length);
            bitset.zero();
            for (int i=0 ; i < dests.length ; i++) {
                if((dests[i] >= vs.addresses.length) || (dests[i] < 0))
                    log.debug("Invalid destination rank ("+dests[i] +") in Send. Event ("+ev.getClass().getName()+ ") Discarded.");
                else
                    bitset.set(dests[i]);
            }
        }

        omsg.pushInt(ls.my_rank);

        if (send_prevs) {
            for (int i=0 ; i < vs.previous.length ; i++)
                omsg.pushInt(vs.previous[i].hashCode());
            omsg.pushShort((short)vs.previous.length);
        } else {
            omsg.pushShort((short)0);
        }

        omsg.pushInt(viewHashCode);
        omsg.pushInt(groupHashCode);

        if (debugFull)
            log.debug("Sending message (isSend="+isSend+") "+ev);

        ev.source=vs.addresses[ls.my_rank];
        ev.group=vs.group;
        ev.view_id=vs.id;

        if (supportsAppiaMulticast) {
            if (isSend) {
                int[] group_dests=(int[])ev.dest;
                Object[] dests=new Object[group_dests.length];
                for (int i=0 ; i < dests.length ; i++)
                    dests[i]=vs.addresses[group_dests[i]];
                // TODO: what is better ???
                //ev.dest=new AppiaMulticast(null,dests);
                ev.dest=new AppiaMulticast(ip_multicast,dests);
            } else { // Cast
                ev.dest=optAppiaMulticast;
            }

            try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        } else {
            if (isSend) {
                send((int[])ev.dest,ev);
            } else { // Cast
                send(all,ev);
            }
        }    
    }

    private void handleView(View ev) {
        int i;

        vs=ev.vs;
        groupHashCode=vs.group.hashCode();
        viewHashCode=vs.id.hashCode();
        ls=ev.ls;

        if (debugFull)
            log.debug("Received new view: group="+groupHashCode+" view="+viewHashCode);

        // Checks if the channel support AppiaMulticast
        supportsAppiaMulticast=false;
        Layer[] layers = ev.getChannel().getQoS().getLayers();
        for (i=0; (i<layers.length) && !supportsAppiaMulticast ; i++) {
            if (layers[i] instanceof AppiaMulticastSupport) {
                supportsAppiaMulticast=true;
            }
        }

        if (supportsAppiaMulticast) {
            //create addresses array to be used with AppiaMulticast
            SocketAddress[] addrs = new InetSocketAddress[vs.view.length-1];
            for (i=0 ; i < addrs.length ; i++) {
                addrs[i]=vs.addresses[(i < ls.my_rank) ? i : i+1];
            }
            optAppiaMulticast=new AppiaMulticast(ip_multicast,addrs);
            // all isn't necessary
            all=null;
        } else {
            // optAppiaMulticast isn't necessary
            optAppiaMulticast=null;
            // creates array with all ranks except mine
            all=new int[vs.view.length-1];
            for(i=0 ; i < all.length ; i++)
                all[i]= ((i < ls.my_rank) ? i : i+1);
        }

        if (vs.view.length != same_view.length)
            same_view=new boolean[vs.view.length];
        for (i=0 ; i < same_view.length ; i++)
            same_view[i]= (i == ls.my_rank);
        send_prevs=true;

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        // Emptying buffer
        GroupSendableEvent e;
        while ((e=buffer.get()) != null) {
            if (log.isDebugEnabled())
                log.debug("Debuffering event "+e);
            handleUpGroupSendableEvent(e);
        }
    }

    private void handleGroupEvent(GroupEvent ev) {
        if (!vs.group.equals(ev.group) || !vs.id.equals(ev.view_id)) {
            log.debug("event (\""+ev.getClass().getName()+"\") going ("+ev.getDir()+") discarded due to bad group or view_id");
            return;
        }

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private void send(int[] dests, GroupSendableEvent ev) {
        int i;

        for(i=0 ; i < dests.length-1 ; i++) {
            try {
                GroupSendableEvent clone=(GroupSendableEvent)ev.cloneEvent();
                clone.dest=vs.addresses[dests[i]];
                clone.setSourceSession(this);
                clone.init();
                clone.go();
            } catch (CloneNotSupportedException ex) {
                throw new AppiaGroupError("GroupBottomSession: "+ex.getMessage());
            } catch (AppiaEventException ex) {
                throw new AppiaGroupError("GroupBottomSession: "+ex.getMessage()+"(possible violation of \"all or nothing\" property");
            }
        }

        // TODO: erase. for testing porpuses only
        //if (ev instanceof appia.test.TestGSEvent)
        // return;

        if (dests.length > 0) {
            ev.dest=vs.addresses[dests[dests.length-1]];
            try {
                ev.go();
            } catch (AppiaEventException ex) {
                throw new AppiaGroupError("GroupBottomSession: "+ex.getMessage()+"(possible violation of \"all or nothing\" property");
            }
        }
    }

    private void sendOtherViews(int state, Object other_addr, Object multicast_addr, Channel channel) {
        try {
            OtherViews ev=new OtherViews(state,channel,Direction.UP,this,vs.group,vs.id);
            ev.other_addr=other_addr;
            ev.multicast_addr=multicast_addr;
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            log.warn("Impossible to send OtherViews ("+state+")");
        }
    }

    // DEBUG
    public static final boolean debugFull=true;
}
