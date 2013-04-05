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
package net.sf.appia.protocols.group.stable;

import java.util.Arrays;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.PeriodicTimer;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.events.Send;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.suspect.Fail;
import net.sf.appia.protocols.group.suspect.Suspect;
import net.sf.appia.protocols.group.suspect.SuspectedMemberEvent;

import org.apache.log4j.Logger;

public class StableSession extends Session {
    private static Logger log = Logger.getLogger(StableSession.class);

    /**
     * Default number of messages between gossip dissemination.
     */
    public static final long DEFAULT_GOSSIP_INTERVAL=32;
    public static final long DEFAULT_GOSSIP_TIME=5000;

    private long gossip_interval=DEFAULT_GOSSIP_INTERVAL;
    private long gossip_time=DEFAULT_GOSSIP_TIME;

    public StableSession(Layer layer) {
        super(layer);
    }

    public void handle(Event event) {

        // StableGossip
        if (event instanceof StableGossip) {
            handleGroupSendableEvent((StableGossip)event); return;
            // Retransmit
        } else if (event instanceof Retransmit) {
            handleRetransmit((Retransmit)event); return;
            // Retransmission
        } else if (event instanceof Retransmission) {
            handleRetransmission((Retransmission)event); return;
            // Fail
        } else if (event instanceof Fail) {
            handleFail((Fail)event); return;
            // View
        } else if (event instanceof View) {
            handleView((View)event); return;
            // GroupSendableEvent
        } else if (event instanceof GroupSendableEvent) {
            handleGroupSendableEvent((GroupSendableEvent)event); return;
            // PeriodicTimer
        } else if (event instanceof PeriodicTimer) {
            handlePeriodicTimer((PeriodicTimer)event); return;
            // Debug
        } 

        log.warn("Unwanted event (\""+event.getClass().getName()+"\") received. Continued...");
        try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private ViewState vs;
    private LocalState ls;
    private long[][] table;
    private StableStorage storage=new StableStorage();
    private long last_gossip;
    private long last_timer=0;
    private long last_timer_gossip_value=-1;
    private boolean stabilizing=false;

    private void handleView(View ev) {
        vs=ev.vs;
        ls=ev.ls;

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        // initiate variables
        table=new long[vs.view.length][vs.view.length];
        for (int i=0 ; i < table.length ; i++)
            for (int j=0 ; j < table[i].length ; j++)
                table[i][j]=0; //((long)2147483647)+2147483647+2147483647-20;
        storage.reset(vs.view.length);

        last_gossip=ls.my_rank; // avoids gossip message synchronization among members
        last_timer_gossip_value=-1;
        stabilizing=false;
    }

    private void handlePeriodicTimer(PeriodicTimer ev) {

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        if ((vs != null) && (ls != null) && (ev.getQualifierMode() == EventQualifier.NOTIFY)) {
            if (vs.view.length < 2)
                return;

            long now=ev.getChannel().getTimeProvider().currentTimeMillis();

            if (now-last_timer >= gossip_time) {
                if (debugFull)
                    log.debug("Timeout");
                if ((table[ls.my_rank][ls.my_rank] > last_gossip) && (last_gossip == last_timer_gossip_value)) {
                    last_gossip=-gossip_interval;
                    sendStableGossip(ev.getChannel());
                }
                last_timer=now;
                last_timer_gossip_value=last_gossip;
            }
        }
    }

    private void handleFail(Fail ev) {
        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
        stabilize(ev.getChannel());
        stabilizing=true;
    }

    private void handleGroupSendableEvent(GroupSendableEvent ev) {
        if (ev instanceof Send) {
            try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
            return;
        }
        
        // to avoid NPEs of bad programming in other layers...
        if (ev instanceof Suspect){
            log.debug("Received unexpected Suspect event. Ignoring it");
            return;
        }

        if (ev.getDir() == Direction.DOWN) {

            long seq;
            if (!(ev instanceof StableGossip))
                seq=++table[ls.my_rank][ls.my_rank];
            else
                seq=table[ls.my_rank][ls.my_rank];

            if (seq-last_gossip > gossip_interval) {
                int col;
                for (col=table[ls.my_rank].length-1 ; col >= 0 ; col--)
                    pushSeq(ev.getMessage(),table[ls.my_rank][col]);
                ev.getMessage().pushBoolean(true);
                last_gossip=seq;
                if (debugFull)
                    log.debug("Placing gossip information in "+(ev instanceof StableGossip ? "StableGossip" : "message")+" with number "+last_gossip);
            } else {
                pushSeq(ev.getMessage(), seq);
                ev.getMessage().pushBoolean(false);
            }

            try {
                ev.go();
            } catch (AppiaEventException ex) {
                table[ls.my_rank][ls.my_rank]--;
                ex.printStackTrace();
                System.err.println("appia:group:StableSession: down event discarded");
            }
        } else { // UP

            long seqno;
            boolean doClean=false;

            if (ev.getMessage().popBoolean()) {
                int col;
                for (col=0 ; col < table[ev.orig].length ; col++) {
                    long seq=popSeq(ev.getMessage(),table[ev.orig][col]);
                    if (seq > table[ev.orig][col]) {
                        table[ev.orig][col]=seq;
                        doClean=true;
                    }
                }
                seqno=table[ev.orig][ev.orig];
                if (debugFull)
                    log.debug("Received gossip information from "+ev.orig+" (doClean="+doClean+")");
            } else {
                seqno=popSeq(ev.getMessage(), table[ls.my_rank][ev.orig]);
            }

            if (debugFull)
                log.debug("Received "+seqno+" from "+ev.orig);

            if (!(ev instanceof StableGossip)) {
                if (seqno != table[ls.my_rank][ev.orig]+1) {
                    suspect(ev,seqno);
                    return;
                }

                table[ls.my_rank][ev.orig]++;

                try {
                    Message omsg=(Message)ev.getMessage().clone();
                    storage.put(ev.orig,new StableInfo(seqno,ev.getClass().getName(),omsg));
                } catch (CloneNotSupportedException ex) {
                    table[ls.my_rank][ev.orig]--;
                    ex.printStackTrace();
                    log.warn("Event ("+ev.getClass().getName()+") discarded because it was impossible to store");
                }

                try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace();  }
            }

            if (doClean) {
                clean();
                if (stabilizing)
                    stabilize(ev.getChannel());
            }
        }
    }

    private void handleRetransmit(Retransmit ev) {
        int rank=ev.getMessage().popInt();
        long lo=ev.getMessage().popLong();
        long hi=ev.getMessage().popLong();
        StableInfo info;

        while (lo <= hi) {
            if ((info=storage.get(rank,lo)) == null) {
                log.debug("I don't have message "+lo+" from "+rank+" as requested");
                return;
            }

            try {
                Message omsg = (Message)info.omsg.clone();
                omsg.pushString(info.eventName);
                omsg.pushLong(info.seqno);
                omsg.pushInt(rank);
                retransmission(ev.orig,omsg,ev.getChannel());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }

            lo++;
        }
    }

    private void handleRetransmission(Retransmission ev) {
        Message omsg=(Message)ev.getMessage();

        int orig=omsg.popInt();
        long seqno=omsg.popLong();
        String name=omsg.popString();

        if (seqno != table[ls.my_rank][orig]+1) {
            log.debug("Received Retransmission of message "+seqno+" already received (strange but ...). Discarding retrasmission.");
            return;
        }

        if (debugFull) 
            log.debug("Received Retransmission from "+ev.orig+" of message "+seqno+" sent by "+orig+" in event of class "+name);

        GroupSendableEvent event=null;
        try {
            event=(GroupSendableEvent)Class.forName(name).newInstance();

            event.setChannel(ev.getChannel());
            event.setDir(ev.getDir());
            event.setSourceSession(this);

            event.dest=null;
            event.source=vs.view[orig];
            event.orig=orig;

            event.group=ev.group;
            event.view_id=ev.view_id;

            pushSeq(omsg, seqno);
            omsg.pushBoolean(false);

            event.setObjectsMessage((Message)omsg);
            event.init();

            handleGroupSendableEvent(event);
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            log.warn("Impossible to create/send retransmited event");
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace();
            log.warn("Impossible to create/send retransmited event");
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
            log.warn("Impossible to create/send retransmited event");
        } catch (InstantiationException ex) {
            ex.printStackTrace();
            log.warn("Impossible to create/send retransmited event");
        }
    }

    private void stabilize(Channel channel) {
        int col,row;

        long[] maxs=new long[table.length];
        int[] ranks=new int[table.length];

        for (row=0 ; row < table.length ; row++) {
            if (!ls.failed[row]) {
                for (col=0 ; col < table[row].length ; col++) {
                    if (table[row][col] > maxs[col]) {
                        maxs[col]=table[row][col];
                        ranks[col]=row;
                    }
                }
            }
        }

        for (col=0 ; col < ls.failed.length ; col++) {
            if (ls.failed[col] && (table[ls.my_rank][col] < maxs[col])) {
                retransmit(ranks[col],col,table[ls.my_rank][col]+1,maxs[col],channel);
            }
        }
    }

    private void clean() {
        int row,col;
        long[] mins=new long[table.length];
        Arrays.fill(mins,Long.MAX_VALUE);

        for (row=0 ; row < table.length ; row++) {
            if (!ls.failed[row]) {
                for (col=0 ; col < table[row].length ; col++) {
                    if (table[row][col] < mins[col])
                        mins[col]=table[row][col];
                }
            }
        }

        for (col=0 ; col < mins.length ; col++) {
            storage.clean(col,mins[col]);
        }
    }

    private void sendStableGossip(Channel channel) {    
        try {
            StableGossip ev=new StableGossip(channel,Direction.DOWN,this,vs.group,vs.id);
            handleGroupSendableEvent(ev);
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            System.err.println("appia:group:StableSession: impossible to send gossip");
        }
    }

    private void suspect(GroupSendableEvent ev, long received) {
        System.err.println("Event ("+ev+" "+ev.getDir()+" "+ev.getSourceSession()+") from "+ev.orig+"("+ls.my_rank+") discarded due to bad seq. number. Received "+received+" expected "+(table[ls.my_rank][ev.orig]+1));
        try {
            SuspectedMemberEvent sme = new SuspectedMemberEvent(ev.getChannel(),Direction.DOWN,this);
            sme.setSuspectedMember(ev.orig);
            sme.setGroup(ev.group);
            sme.setViewID(ev.view_id);
            sme.go();
            log.debug("Suspecting member "+ev.orig+". Notification sent to the failure detector.");
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void retransmit(int dest, int rank, long lo, long hi, Channel channel) {
        if (debugFull)
            log.debug("Requesting retransmisson of messages ["+lo+","+hi+"] from member "+rank+" to alive member "+dest);
        try {
            Retransmit ev=new Retransmit(channel,Direction.DOWN,this,vs.group,vs.id);
            ev.getMessage().pushLong(hi);
            ev.getMessage().pushLong(lo);
            ev.getMessage().pushInt(rank);
            int[] dests={dest};
            ev.dest=dests;
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            System.err.println("appia:group:StableSession: impossible to send retransmit request");
        }
    }

    private void retransmission(int dest, Message omsg, Channel channel) {
        try {
            int[] dests={dest};
            Retransmission ev=new Retransmission((Message)omsg,channel,Direction.DOWN,this,vs.group,vs.id);
            ev.dest=dests;
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            System.err.println("appia:group:StableSession: impossible to send retransmission");
        }
    }

    private static final long MASK=((long)1) << 31;
    private static final long ADD_MASK=((long)1) << 32;
    private static final long CLEAR_MASK=(((long)0xFFFF) << 48) | (((long)0xFFFF) << 32);

    private static void pushSeq(Message emsg, long seq) {
        emsg.pushUnsignedInt(seq);
    }

    private static long popSeq(Message emsg, long base) {
        long l=emsg.popUnsignedInt();
        if (((l & MASK) == 0) && ((base & MASK) != 0))
            l = ((base & CLEAR_MASK) + ADD_MASK) | l;
        else
            l = (base & CLEAR_MASK) | l;
        return l;
    }

    /*
  public static void main(String[] args) {
    System.out.println("MASK="+MASK+" 0x"+Long.toString(MASK,16));
    System.out.println("ADD_MASK="+ADD_MASK+" 0x"+Long.toString(ADD_MASK,16));
    System.out.println("CLEAR_MASK="+CLEAR_MASK+" 0x"+Long.toString(CLEAR_MASK,16));

    long te2=((long)2147483647)+2147483647+2147483647+10000;
    long te1=te2+5+2147483647;
    GroupSendableEvent ev=new GroupSendableEvent();
    pushSeq(ev, te1);
    long te3=popSeq(ev, te2);
    System.out.println("BASE="+te2+" 0x"+Long.toString(te2,16));
    System.out.println("## "+te1+" 0x"+Long.toString(te1,16)+" -> "+te3+" 0x"+Long.toString(te3, 16));

  }
     */

    // DEBUG
    public static final boolean debugFull=true;
}