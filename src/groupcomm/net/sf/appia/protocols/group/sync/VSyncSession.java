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
package net.sf.appia.protocols.group.sync;

import java.util.Arrays;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.protocols.group.AppiaGroupError;
import net.sf.appia.protocols.group.ArrayOptimized;
import net.sf.appia.protocols.group.EventBuffer;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.events.Send;
import net.sf.appia.protocols.group.events.Uniform;
import net.sf.appia.protocols.group.intra.NewView;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.suspect.Fail;

import org.apache.log4j.Logger;

public class VSyncSession extends Session {
    private static Logger log = Logger.getLogger(VSyncSession.class);

    public VSyncSession(Layer layer) {
        super(layer);
    }

    public void handle(Event event) {

        // Block
        if (event instanceof Block) {
            handleBlock((Block)event);
            return;
        }

        // BlockOk
        if (event instanceof BlockOk) {
            handleBlockOk((BlockOk)event);
            return;
        }

        // Sync
        if (event instanceof Sync) {
            handleSync((Sync)event);
            return;
        }

        // Fail
        if (event instanceof Fail) {
            handleFail((Fail)event);
            return;
        }

        // NewView
        if (event instanceof NewView) {
            handleNewView((NewView)event);
            return;
        }

        // View
        if (event instanceof View) {
            handleView((View)event);
            return;
        }

        // GroupSendableEvent
        if (event instanceof GroupSendableEvent) {
            handleGroupSendableEvent((GroupSendableEvent)event);
            return;
        }

        log.warn("Unwanted event (\""+event.getClass().getName()+"\") received. Continued...");
        try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private NewView newview;
    private boolean buffering;
    private boolean blocking;
    private boolean[] sync;

    private boolean sent_blockok;
    private boolean recv_blockok;

    private int syncCoord;

    private long[] global_casts;
    private long[][] casts_table;
    private long[] my_casts;

    private long[] global_sends;
    private long[][] sends_table;
    private long[] my_sends_sent;
    private long[] my_sends_recv;

    private static final short BLOCK=1;
    private static final short BLOCKED=2;
    private static final short SYNC=3;
    private static final short SYNCHED=4;

    private ViewState vs;
    private LocalState ls;

    private EventBuffer buffer=new EventBuffer(50);

    private void handleBlock(Block ev) {
        short type=ev.getMessage().popShort();

        if (syncCoord == ls.my_rank) {

            if (type != BLOCKED) {
                log.debug("Invalid Block type (received="+type+", expected="+BLOCKED+") from "+ev.orig+" to coordinator");
                return;
            }

            long[] rcasts=ArrayOptimized.popArrayLong(ev.getMessage());
            long[] rsends=ArrayOptimized.popArrayLong(ev.getMessage());
            coord_handleBlock(ev.orig,rcasts,rsends,ev.getChannel());

        } else {

            if (type != BLOCK) {
                log.debug("Invalid Block type (received="+type+", expected="+BLOCK+") from "+ev.orig+" to other");
                return;
            }

            // reset variables
            global_casts=null;
            global_sends=null;

            syncCoord=ev.orig;
            blockok(ev.getChannel());
        }
    }

    private void handleBlockOk(BlockOk ev) {
        recv_blockok=true;
        in_block(ev.getChannel());
    }

    private void handleSync(Sync ev) {
        short type=ev.getMessage().popShort();

        if (syncCoord == ls.my_rank) {

            if (type != SYNCHED) {
                log.debug("Invalid Sync type (received="+type+", expected="+SYNCHED+") from "+ev.orig+" to coordinator");
                return;
            }

            long[] rcasts=ArrayOptimized.popArrayLong(ev.getMessage());
            coord_handleSync(ev.orig,rcasts,ev.getChannel());

        } else {

            if (type != SYNC) {
                log.debug("Invalid Sync type (received="+type+", expected="+SYNC+") from "+ev.orig+" to other");
                return;
            }

            syncCoord=ev.orig;
            global_casts=ArrayOptimized.popArrayLong(ev.getMessage());
            global_sends=ArrayOptimized.popArrayLong(ev.getMessage());

            if (debugFull) {
                log.debug("{"+ls.my_rank+"} Received Sync from "+ev.orig);
                debugLongArray("global_casts=",global_casts);
                debugLongArray("global_sends=",global_sends);
                debugLongArray("my_casts=",my_casts);
                debugLongArray("my_sends_recv=",my_sends_recv);
            }

            empty_buffer(ev.getChannel());
        }
    }

    private void handleGroupSendableEvent(GroupSendableEvent ev) {
        if (recv_blockok && (ev.getDir() == Direction.DOWN))
            throw new AppiaGroupError("VSyncSession: tried to sent event (name="+ev.getClass().getName()+" , direction="+ev.getDir()+" , source="+ev.getSourceSession().getClass().getName()+" , message=\""+(new String(ev.getMessage().toByteArray()))+"\") while blocked");


        // SEND
        if (ev instanceof Send) {
            if (ev.getDir() == Direction.DOWN) {
                int[] dests=(int[])ev.dest;
                for (int i=0 ; i < dests.length ; i++)
                    my_sends_sent[dests[i]]++;
                try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

            } else {
                if ((global_sends != null) && (my_sends_recv[ev.orig] == global_sends[ev.orig])) {
                    log.debug("Send Event from "+ev.orig+" (failed="+ls.failed[ev.orig]+") discarded because it was greater than global");
                    return;
                }

                my_sends_recv[ev.orig]++;
                try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

                if (global_sends != null)
                    in_sync(ev.getChannel());
            }

            return;
        }

        // CAST
        if (ev.getDir() == Direction.DOWN) {
            if(!(ev instanceof Uniform))
                my_casts[ls.my_rank]++;

            try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        } else {

            if ( !(ev instanceof Uniform) && (ev.orig == ls.my_rank)){
                throw new AppiaGroupError("VSyncSession: received UP event from myself!!!!"+ev.getClass().getName());
            }

            if (buffering) {
                buffer.put(ev);
                return;
            }

            if ((global_casts != null) && (my_casts[ev.orig] == global_casts[ev.orig])) {
                log.debug("Cast Event from "+ev.orig+" (failed="+ls.failed[ev.orig]+") discarded because it was greater than global");
            }
            else{
                my_casts[ev.orig]++;
                try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
            }

            if (global_casts != null)
                in_sync(ev.getChannel());
        }
    }

    private void handleFail(Fail ev) {

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        if (newview == null)
            return;

        if (syncCoord == ls.my_rank) {
            if (!blocking)
                begin_blocking(ev.getChannel());
            else
                end_blocking(ev.getChannel());
        }
    }

    private void handleNewView(NewView ev) {
        newview=ev;
        syncCoord=ls.my_rank;
        begin_blocking(ev.getChannel());
    }

    private void handleView(View ev) {
        vs=ev.vs;
        ls=ev.ls;

        buffering=false;
        blocking=false;
        sync=null;

        sent_blockok=false;
        recv_blockok=false;

        syncCoord=-1;

        global_casts=null;
        casts_table=null;
        if ((my_casts == null) || (vs.view.length != my_casts.length))
            my_casts=new long[vs.view.length];
        Arrays.fill(my_casts,0);

        global_sends=null;
        sends_table=null;
        if ((my_sends_sent == null) || (vs.view.length != my_sends_sent.length)) {
            my_sends_sent=new long[vs.view.length];
            my_sends_recv=new long[vs.view.length];
        }
        Arrays.fill(my_sends_sent,0);
        Arrays.fill(my_sends_recv,0);

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private void coord_handleBlock(int orig, long[] casts, long[] sends, Channel channel) {
        casts_table[orig]=casts;

        int row;
        for (row=0 ; row < sends_table.length ; row++) {
            sends_table[row][orig]=sends[row];
        }

        end_blocking(channel);
    }

    private void coord_handleSync(int orig, long[] casts, Channel channel) {
        if (sync == null) {
            log.debug("Received Sync out of time. Ignored.");
            return;
        }

        if (equals(casts,global_casts,null)) {
            sync[orig]=true;
            end_sync(channel);
        }
    }

    private void in_block(Channel channel) {
        buffering=true;

        if (debugFull) {
            log.debug("{"+ls.my_rank+"} Sending my_casts and my_sends_sent to coordinator");
            debugLongArray("my_casts=",my_casts);
            debugLongArray("my_sends_sent=",my_sends_sent);
        }

        if (syncCoord != ls.my_rank) {
            int[] dests={syncCoord};
            block(dests,BLOCKED,channel);
        } else {
            coord_handleBlock(ls.my_rank,my_casts,my_sends_sent,channel);
        }
    }

    private void in_sync(Channel channel) {
        if (debugFull) {
            log.debug("testing in_sync");
            log.debug("in_sync: my_casts==global_casts -> "+equals(my_casts,global_casts,null));
            log.debug("in_sync: my_sends_recv== global_sends -> "+equals(my_sends_recv,global_sends,ls.failed));
        }

        if (equals(my_casts,global_casts,null) && equals(my_sends_recv,global_sends,ls.failed)) {
            if (syncCoord != ls.my_rank) {
                int[] dests={syncCoord};
                sync(dests,SYNCHED,null,channel);
            } else {
                coord_handleSync(ls.my_rank,my_casts,channel);
            }
        }
    }

    private void begin_blocking(Channel channel) {
        blocking=true;

        // init casts
        global_casts=null;
        if (casts_table == null)
            casts_table=new long[vs.view.length][];
        Arrays.fill(casts_table,null);

        // init sends_table
        global_sends=null;
        if (sends_table == null)
            sends_table=new long[vs.view.length][vs.view.length];
        fill2(sends_table,0);

        // init sync
        sync=null;

        // destinations
        int[] all=new int[vs.view.length-1];
        for (int i=0 ; i < all.length ; i++) {
            all[i]=((i < ls.my_rank) ? i : i+1);
        }

        if (debugFull)
            log.debug("Started blocking");

        block(all,BLOCK,channel);
        blockok(channel);
    }

    private void end_blocking(Channel channel) {
        int i;

        for (i=0 ; i < casts_table.length ; i++) {
            if (!ls.failed[i] && (casts_table[i] == null))
                return;
        }

        blocking=false;

        global_casts=new long[vs.view.length];
        Arrays.fill(global_casts,0);
        int row,col;
        for (row=0 ; row < casts_table.length ; row++) {
            if (!ls.failed[row]) {
                for (col=0 ; col < casts_table[row].length ; col++) {
                    if (casts_table[row][col] > global_casts[col])
                        global_casts[col]=casts_table[row][col];
                }
            }
        }

        sync=new boolean[vs.view.length];
        Arrays.fill(sync,false);

        if (debugFull) {
            log.debug("{"+ls.my_rank+"} Ended blocking started synching");
            debugLongArray("global_casts=",global_casts);
            for (int k=0 ; k < sends_table.length ; k++)
                debugLongArray("sends_table["+k+"]=",sends_table[k]);
            debugLongArray("my_casts=",my_casts);
        }

        // sync everyone
        for (i=0 ; i < vs.view.length ; i++) {
            if (i == ls.my_rank) {
                global_sends=sends_table[i];
                empty_buffer(channel);
            } else {
                if (!ls.failed[i]) {
                    int[] dests={i};
                    sync(dests,SYNC,sends_table[i],channel);
                }
            }
        }
    }

    private void end_sync(Channel channel) {
        if (newview == null)
            return;

        int i;
        for (i=0 ; i < sync.length ; i++) {
            if (!ls.failed[i] && !sync[i])
                return;
        }

        try {
            newview.go();
            newview=null;
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            throw new AppiaGroupError("VSyncSession: sanity");
        }
    }

    private void empty_buffer(Channel channel) {
        buffering=false;

        GroupSendableEvent gse;

        if ((gse=buffer.get()) == null) {
            in_sync(channel);
            return;
        }

        while (gse != null) {
            handleGroupSendableEvent(gse);
            gse=buffer.get();
        }
    }

    private void block(int[] dests, short type, Channel channel) {
        try {
            Block ev=new Block(channel,Direction.DOWN,this,vs.group,vs.id);
            switch (type) {
            case BLOCK:
                break;
            case BLOCKED:
                ArrayOptimized.pushArrayLong(my_sends_sent,ev.getMessage());
                ArrayOptimized.pushArrayLong(my_casts,ev.getMessage());
                break;
            default:
                throw new AppiaGroupError("VSyncSession: Invalid Block type ("+type+")");
            }
            ev.getMessage().pushShort(type);
            ev.dest=dests;
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void blockok(Channel channel) {
        if (recv_blockok) {
            in_block(channel);
            return;
        }

        if (sent_blockok)
            return;

        try {
            BlockOk ev=new BlockOk(vs.group,vs.id);
            EchoEvent echo=new EchoEvent(ev,channel,Direction.UP,this);
            echo.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void sync(int[] dests, short type, long[] s, Channel channel) {
        try {
            Sync ev=new Sync(channel,Direction.DOWN,this,vs.group,vs.id);
            switch (type) {
            case SYNC:
                ArrayOptimized.pushArrayLong(s,ev.getMessage());
                ArrayOptimized.pushArrayLong(global_casts,ev.getMessage());
                break;
            case SYNCHED:
                ArrayOptimized.pushArrayLong(my_casts,ev.getMessage());          
                break;
            default:
                throw new AppiaGroupError("VSyncSession: Invalid Block type ("+type+")");
            }
            ev.getMessage().pushShort(type);
            ev.dest=dests;
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private boolean equals(long[] a1, long[] a2, boolean[] b) {
        if (a1.length != a2.length)
            return false;

        int i;
        for (i=0 ; i < a1.length ; i++) {
            if ((b == null) || !b[i]) {
                if (a1[i] != a2[i])
                    return false;
            }
        }
        return true;
    }

    private void fill2(long[][] a, long l) {
        int i,j;
        for (i=0 ; i < a.length ; i++) {
            for (j=0 ; j < a[i].length ; j++)
                a[i][j]=0;
        }
    }

    // DEBUG

    public static final boolean debugFull=true;

    private void debugLongArray(String s, long[] a) {
        if (log.isDebugEnabled()) {
            s+="[";
            for (int i=0 ; i < a.length ; i++)
                s+=",";
            s+="]";
            log.debug(s);
        }
    }
}
