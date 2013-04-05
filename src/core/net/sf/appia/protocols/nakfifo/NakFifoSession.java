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
package net.sf.appia.protocols.nakfifo;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ListIterator;

import net.sf.appia.core.AppiaError;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.AppiaMulticast;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.FIFOUndeliveredEvent;
import net.sf.appia.protocols.common.SendableNotDeliveredEvent;
import net.sf.appia.protocols.frag.MaxPDUSizeEvent;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import org.apache.log4j.Logger;


/** Session of protocols that provides reliable point-to-point communication.
 * <br>
 * It offers <i>AppiaMulticast</i> support by sending a different message to each
 * destination.
 * @author Alexandre Pinto
 */
public class NakFifoSession extends Session implements InitializableSession {
    private static Logger log = Logger.getLogger(NakFifoSession.class);

    /** The default duration of a round in milliseconds. 
     */  
    public static final long DEFAULT_TIMER_PERIOD=5000; // 5 secs
    /** Default time, in milliseconds, to resend a NAK.
     * <br>
     * Must be a multiple of the round duration (DEFAULT_TIMER_PERIOD). 
     */  
    public static final long DEFAULT_RESEND_TIME=10000;  // 10 secs
    /** Default maximum time without receiving an Application message.
     * When this time is reached the peer is discarded.
     * <br>
     * Must be a multiple of the round duration (DEFAULT_TIMER_PERIOD). 
     */  
    public static final long DEFAULT_MAX_APPL_TIME=180000;    // 3 mins
    /** Default maximum time, in milliseconds, to recieve a message from a peer.
     * If this time is reached the peer is considered failed.
     * <br>
     * Must be a multiple of the round duration (DEFAULT_TIMER_PERIOD). 
     */  
    public static final long DEFAULT_MAX_RECV_TIME=60000;    // 60 secs
    /** Default maximum time to send a message to a peer.
     * If this time is reached a Ping message is sent.
     * <br>
     * Must be a multiple of the round duration (DEFAULT_TIMER_PERIOD). 
     */  
    public static final long DEFAULT_MAX_SENT_TIME=45000;     // 45 secs

    private long param_TIMER_PERIOD=DEFAULT_TIMER_PERIOD;
    private long param_RESEND_NACK_ROUNDS=DEFAULT_RESEND_TIME/param_TIMER_PERIOD;
    private long param_MAX_APPL_ROUNDS=DEFAULT_MAX_APPL_TIME/param_TIMER_PERIOD;
    private long param_MAX_RECV_ROUNDS=DEFAULT_MAX_RECV_TIME/param_TIMER_PERIOD;
    private long param_MAX_SENT_ROUNDS=DEFAULT_MAX_SENT_TIME/param_TIMER_PERIOD;

    /** Creates a new instance of NakFifoSession */
    public NakFifoSession(Layer layer) {
        super(layer);
    }

    /**
     * Initializes the session using the parameters given in the XML configuration.
     * Possible parameters:
     * <ul>
     * <li><b>timer_period</b> the period of the internal timer. (in milliseconds)
     * <li><b>resend_nack_time</b> the time to resend a negative ack. (in milliseconds)
     * <li><b>max_appl_time</b> maximum time without receiving an Application message, and discarding the peer. (in milliseconds)
     * <li><b>max_recv_time</b> maximum time for message reception, before suspecting the peer. (in milliseconds)
     * <li><b>max_sent_time</b> maximum time between sent messages. (in milliseconds)
     * </ul>
     * 
     * @param params The parameters given in the XML configuration.
     */
    public void init(SessionProperties params) {
        if (params.containsKey("timer_period"))
            param_TIMER_PERIOD=params.getLong("timer_period");
        if (params.containsKey("resend_nack_time"))
            param_RESEND_NACK_ROUNDS=params.getLong("resend_nack_time")/param_TIMER_PERIOD;
        if (params.containsKey("max_appl_time"))
            param_MAX_APPL_ROUNDS=params.getLong("max_appl_time")/param_TIMER_PERIOD;
        if (params.containsKey("max_recv_time"))
            param_MAX_RECV_ROUNDS=params.getLong("max_recv_time")/param_TIMER_PERIOD;
        if (params.containsKey("max_sent_time"))
            param_MAX_SENT_ROUNDS=params.getLong("max_sent_time")/param_TIMER_PERIOD;
    }

    /** Main Event handler. */  
    public void handle(Event event) {

        if (event instanceof NackEvent) {
            handleNack((NackEvent)event); return;
        } else if (event instanceof IgnoreEvent) {
            handleIgnore((IgnoreEvent)event); return;
        } else if (event instanceof PingEvent) {
            handlePing((PingEvent)event); return;
        } else if (event instanceof NakFifoTimer) {
            handleNakFifoTimer((NakFifoTimer)event); return;
        } else if (event instanceof SendableEvent) {
            handleSendable((SendableEvent)event); return;
        } else if (event instanceof SendableNotDeliveredEvent) {
            handleSendableNotDelivered((SendableNotDeliveredEvent)event); return;
        } else if (event instanceof ChannelInit) {
            handleChannelInit((ChannelInit)event); return;
        } else if (event instanceof ChannelClose) {
            handleChannelClose((ChannelClose)event); return;
        } else if (event instanceof MaxPDUSizeEvent) {
            handleMaxPDUSize((MaxPDUSizeEvent)event); return;
        } else

            log.error("Unwanted event (\""+event.getClass().getName()+"\") received. Continued...");
        try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private HashMap peers=new HashMap();
    private Channel timerChannel=null;
    private MessageUtils utils=new MessageUtils();

    private void handleChannelInit(ChannelInit ev) {
        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        if (timerChannel == null)
            sendTimer(ev.getChannel());

        log.debug("Params:\n\tTIMER_PERIOD="+param_TIMER_PERIOD+
                "\n\tMAX_APPL_ROUNDS="+param_MAX_APPL_ROUNDS+
                "\n\tMAX_RECV_ROUNDS="+param_MAX_RECV_ROUNDS+
                "\n\tMAX_SENT_ROUNDS="+param_MAX_SENT_ROUNDS+
                "\n\tRESEND_NACK_ROUNDS="+param_RESEND_NACK_ROUNDS);
    }

    private void handleChannelClose(ChannelClose ev) {
        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        if (ev.getChannel() == timerChannel) {
            timerChannel=null;
            Iterator iter=peers.values().iterator();
            while (iter.hasNext()) {
                Peer peer=(Peer)iter.next();
                if (peer.last_channel != null) {
                    sendTimer(peer.last_channel);
                    if (timerChannel != null)
                        return;
                }
            }
            log.warn("Unable to send timer. Corret operation is not garanteed");
        }
    }

    private void handleMaxPDUSize(MaxPDUSizeEvent event) {
        if (event.getDir() == Direction.UP) {
            event.pduSize-=9;
        }
        try {
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleSendableNotDelivered(SendableNotDeliveredEvent ev) {
        try {
            final FIFOUndeliveredEvent event=new FIFOUndeliveredEvent(ev.getChannel(),this,ev.getEvent());
            event.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }

    private void handleSendable(SendableEvent ev) {
        if (ev.getDir() == Direction.UP) {      
            receive(ev);
            return;
        }

        if (ev.getDir() == Direction.DOWN) {

            if (ev.dest instanceof AppiaMulticast) {
                Object[] dests=((AppiaMulticast)ev.dest).getDestinations();
                for (int i=0 ; i < dests.length ; i++) {
                    send(ev,dests[i]);
                }
                return;
            }

            if ((ev.dest instanceof InetSocketAddress) && (((InetSocketAddress)ev.dest).getAddress().isMulticastAddress())) {
                log.debug("Destination is a IP Multicast address. Ignored.");
                ev.getMessage().pushByte(MessageUtils.IGNORE_FLAG);
                try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
                return;
            }

            send(ev,ev.dest);
            return;
        }

        log.warn("Direction is wrong. Discarding event "+ev);
    }

    private void handlePing(PingEvent ev) {
        if (ev.getDir() != Direction.UP) {
            log.warn("Discarding Ping event due to wrong diretion.");
            return;
        }
        receive(ev);
    }

    private void handleNack(NackEvent ev) {
        Peer peer=(Peer)peers.get(ev.source);
        if (peer == null) {
            peer=createPeer(ev.source,ev.getChannel());
            return;
        }

        long first;
        long last;

        if ((first=ev.getMessage().popLong()) < 0) {
            log.debug("Ignoring Nack due to wrong first seq number.");
            return;
        }
        if ((last=ev.getMessage().popLong()) < 0) {
            log.debug("Ignoring Nack due to wrong last seq number.");
            return;
        }
        if (first > last) {
            log.debug("Ignoring Nack due to wrong seq numbers (first="+first+",last="+last+",confirmed="+peer.last_msg_confirmed+").");
            return;
        }

        if ((first < peer.first_msg_sent) || (last > peer.last_msg_sent)) {
            // Restart comunication
            log.debug("Received Nack for message not sent. Restarting communication.");
            ignore(peer,ev.getChannel());
            return;
        }

        if (debugFull)
            debugPeer(peer,"handleNack("+first+","+last+")");

        if (first <= peer.last_msg_confirmed) {
            if (last <= peer.last_msg_confirmed) {
                log.debug("Received Nack for messages already confirmed. Discarding.");
                return;
            }
            first=peer.last_msg_confirmed+1;
            log.debug("Received Nack for message already confirmed. Changig first to "+first);
        }

        resend(peer,first,last);
    }

    private void handleNakFifoTimer(NakFifoTimer ev) {
        if (ev.getQualifierMode() != EventQualifier.NOTIFY)
            return;
        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        Iterator peers_iter=peers.values().iterator();
        while (peers_iter.hasNext()) {
            Peer peer=(Peer)peers_iter.next();

            peer.rounds_appl_msg++;
            peer.rounds_msg_recv++;
            peer.rounds_msg_sent++;

            if (debugFull)
                debugPeer(peer,"Timer");

            if (peer.nacked != null) {
                peer.nacked.rounds++;
                if (peer.nacked.rounds > param_RESEND_NACK_ROUNDS) {
                    nack(peer,peer.last_msg_delivered >= peer.nacked.first_msg ? peer.last_msg_delivered+1 : peer.nacked.first_msg, peer.nacked.last_msg, ((SendableEvent)peer.undelivered_msgs.getFirst()).getChannel());
                    peer.nacked.rounds=0;
                }
            } else {
                if (peer.rounds_appl_msg > param_MAX_APPL_ROUNDS) {
                    peers_iter.remove();
                    peer=null;
                }
            }

            if ((peer != null) && (peer.rounds_msg_recv > param_MAX_RECV_ROUNDS)) {
                Iterator msgs=peer.unconfirmed_msgs.iterator();
                while (msgs.hasNext()) {
                    sendFIFOUndelivered((SendableEvent)msgs.next(),peer.addr);
                }
                peers_iter.remove();
                peer=null;
            }

            if ((peer != null) && (peer.rounds_msg_sent > param_MAX_SENT_ROUNDS)) {
                try {
                    PingEvent e=new PingEvent(peer.last_channel,this);
                    e.dest=peer.addr;
                    send(e,peer.addr);
                } catch (AppiaEventException ex) {
                    ex.printStackTrace();
                    log.warn("Impossible to send ping.");
                }
            }
        }
    }

    private void handleIgnore(IgnoreEvent ev) {
        Peer peer=(Peer)peers.get(ev.source);
        if (peer == null)
            peer=createPeer(ev.source,ev.getChannel());

        if (debugFull)
            debugPeer(peer,"handleIgnore");

        peer.last_msg_delivered=ev.getMessage().popLong();
        peer.undelivered_msgs.clear();
        peer.nacked=null;

        peer.rounds_msg_recv=0;
        peer.last_channel=ev.getChannel();

        //if (debugFull)
        log.debug("Received Ignore from "+peer.addr.toString()+" with value "+peer.last_msg_delivered);
    }

    private void send(SendableEvent event, Object addr) {
        Peer peer=(Peer)peers.get(addr);
        if (peer == null)
            peer=createPeer(addr,event.getChannel());      

        try {
            // Must send a clone because original may be shared among several peers 
            // due to AppiaMulticast
            SendableEvent ev=(SendableEvent)event.cloneEvent();
            ev.setSourceSession(this);
            ev.init();

            utils.pushSeq(ev.getMessage(),peer.last_msg_delivered);
            utils.pushSeq(ev.getMessage(),peer.last_msg_sent+1);
            ev.getMessage().pushByte(MessageUtils.NOFLAGS);

            ev.dest=addr;
            ev.go();

            peer.last_msg_sent++;
            storeUnconfirmed(peer,event);

            peer.rounds_msg_sent=0;
            if (!(ev instanceof PingEvent))
                peer.rounds_appl_msg=0;

            peer.last_channel=ev.getChannel();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            log.warn("To mantain coerence, sending undelivered.");
            sendFIFOUndelivered(event,peer.addr);
            return;
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
            log.warn("To mantain coerence, sending undelivered.");
            sendFIFOUndelivered(event,peer.addr);
            return;
        }
    }

    private void receive(SendableEvent ev) {
        byte flags=ev.getMessage().popByte();
        if ((flags & MessageUtils.IGNORE_FLAG) != 0) {
            log.debug("Received msg with ignore flag. Ignoring.");
            try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
            return;
        }

        Peer peer=(Peer)peers.get(ev.source);
        if (peer == null)
            peer=createPeer(ev.source,ev.getChannel());

        long seq;
        if ((seq=utils.popSeq(ev.getMessage(),peer.last_msg_delivered,false)) < 0) {
            log.debug("Problems reading sequence number discarding event "+ev+" from "+ev.dest.toString());
            return;
        }

        long peer_confirmed;
        if ((peer_confirmed=utils.popSeq(ev.getMessage(),peer.last_msg_confirmed,false)) < 0) {
            log.debug("Problems reading last message received by peer, discarding event "+ev+" from "+ev.dest.toString());
            return;
        }

        // Channel
        peer.last_channel=ev.getChannel();

        // Rounds
        peer.rounds_msg_recv=0;
        if (!(ev instanceof PingEvent))
            peer.rounds_appl_msg=0;

        // Confirmed
        if ((peer_confirmed >= peer.first_msg_sent) && (peer_confirmed <= peer.last_msg_sent)) {
            if (peer_confirmed > peer.last_msg_confirmed)
                removeUnconfirmed(peer,peer_confirmed);
        } else {
            if ((peer_confirmed > 0) && (peer_confirmed != peer.last_msg_confirmed)) {
                log.debug("Received wrong peer confirmed number (expected between "+peer.first_msg_sent+" and "+peer.last_msg_sent+", received "+peer_confirmed+". Sending Ignore.");
                ignore(peer,ev.getChannel());
            }
        }

        // Deliver
        if (seq == peer.last_msg_delivered+1) {
            try {
                if (!(ev instanceof PingEvent))
                    ev.go();
            } catch  (AppiaEventException ex) {
                ex.printStackTrace();
                return;
            }

            peer.last_msg_delivered=seq;
            if (peer.undelivered_msgs.size() > 0) {
                long undelivered=deliverUndelivered(peer);

                if (debugFull)
                    debugPeer(peer,"receive1("+seq+","+undelivered+")");

                if (peer.nacked != null) {
                    if (peer.last_msg_delivered >= peer.nacked.last_msg)
                        peer.nacked=null;
                }

                if ((peer.nacked == null) && (undelivered >= 0))
                    nack(peer,peer.last_msg_delivered+1,undelivered-1,ev.getChannel());
            }
        } else { // Wrong seq number
            if (seq <= peer.last_msg_delivered) {
                log.debug("Received old message from "+peer.addr.toString()+". Discarding.");
                return;
            }

            storeUndelivered(peer,ev,seq);

            if (peer.nacked == null)
                nack(peer,peer.last_msg_delivered+1,seq-1,ev.getChannel());
        }
    }

    private void nack(Peer peer, long first, long last, Channel channel) {
        //TODO: erase
        if (first > last) {
            debugPeer(peer,"nack error");
            throw new AppiaError("first("+first+") > last("+last+")");
        }

        try {
            NackEvent nack=new NackEvent(channel,this);
            nack.getMessage().pushLong(last);
            nack.getMessage().pushLong(first);
            nack.dest=peer.addr;
            nack.go();

            peer.nacked=new Nacked(first,last);
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            log.warn("Impossible to send Nack. Maybe next time.");
        }

        if (debugFull)
            debugPeer(peer,"nack");
    }

    private void ignore(Peer peer, Channel channel) {
        try {
            IgnoreEvent ev=new IgnoreEvent(channel,this);
            ev.getMessage().pushLong(peer.last_msg_confirmed);
            ev.dest=peer.addr;
            ev.go();
            peer.rounds_msg_sent=0;
            if (debugFull)
                log.debug("Sent Ignore with "+peer.last_msg_confirmed+" to "+peer.addr);
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            log.warn("Unable to send Ignore later it will be retransmited.");
        }
    }

    private void storeUnconfirmed(Peer peer, SendableEvent ev) {
        peer.unconfirmed_msgs.addLast(ev);
    }

    private void removeUnconfirmed(Peer peer, long last) {
        while (peer.last_msg_confirmed < last) {
            SendableEvent ev = (SendableEvent) peer.unconfirmed_msgs.removeFirst();
            peer.last_msg_confirmed++;
            // FIXME: this cannot be done here
            //ev.getMessage().discardAll();
        }
    }

    private void resend(Peer peer, long first, long last) {
        ListIterator aux=peer.unconfirmed_msgs.listIterator();
        long seq=peer.last_msg_confirmed;
        while (aux.hasNext() && (seq <= last)) {
            SendableEvent evaux=(SendableEvent)aux.next();
            seq++;
            if ((seq >= first) && (seq <= last)) {
                try {
                    // Must send a clone because original may be shared among several peers
                    // due to AppiaMulticast
                    SendableEvent ev=(SendableEvent)evaux.cloneEvent();
                    ev.setSourceSession(this);
                    ev.init();

                    utils.pushSeq(ev.getMessage(),peer.last_msg_delivered);
                    utils.pushSeq(ev.getMessage(),seq);
                    ev.getMessage().pushByte(MessageUtils.NOFLAGS);
                    ev.dest=peer.addr;
                    ev.go();

                    peer.rounds_msg_sent=0;
                } catch (AppiaEventException ex1) {
                    ex1.printStackTrace();
                } catch (CloneNotSupportedException ex2) {
                    ex2.printStackTrace();
                }
            }
        }
    }

    private void storeUndelivered(Peer peer, SendableEvent ev, long seq) {
        utils.pushSeq(ev.getMessage(),seq);
        ListIterator aux=peer.undelivered_msgs.listIterator(peer.undelivered_msgs.size());
        while (aux.hasPrevious()) {
            SendableEvent evaux=(SendableEvent)aux.previous();
            long seqaux=utils.popSeq(evaux.getMessage(),peer.last_msg_delivered,true);
            if (seqaux == seq) {
                log.debug("Received undelivered message already stored. Discarding new copy.");
                return;
            }
            if (seqaux < seq) {
                aux.next();
                aux.add(ev);
                return;
            }
        }
        peer.undelivered_msgs.addFirst(ev);
    }

    private long deliverUndelivered(Peer peer) {
        ListIterator aux=peer.undelivered_msgs.listIterator();
        while (aux.hasNext()) {
            SendableEvent evaux=(SendableEvent)aux.next();
            long seqaux=utils.popSeq(evaux.getMessage(),peer.last_msg_delivered,true);
            if (seqaux == peer.last_msg_delivered+1) {
                try {
                    if (!(evaux instanceof PingEvent)) {
                        evaux.getMessage().discard(MessageUtils.SEQ_SIZE);
                        evaux.go();
                    }
                } catch (AppiaEventException ex) {
                    ex.printStackTrace();
                    log.debug("Discarding event "+evaux+". This may lead to incoherence.");
                }
                peer.last_msg_delivered=seqaux;
                aux.remove();
            } else {
                return seqaux;
            }
        }
        return -1;
    }

    private Peer createPeer(Object addr, Channel channel) {
        Peer peer=new Peer(addr, channel.getTimeProvider());
        peers.put(peer.addr,peer);
        ignore(peer,channel);
        return peer;
    }

    private void sendFIFOUndelivered(SendableEvent ev, Object addr) {
        if (ev instanceof PingEvent)
            return;
        try {
            SendableEvent clone=(SendableEvent)ev.cloneEvent();
            clone.dest=addr;
            FIFOUndeliveredEvent e=new FIFOUndeliveredEvent(ev.getChannel(),this,clone);
            e.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            log.warn("Unable to send Undelivered notification. Continuing but problems may happen.");
        } catch (CloneNotSupportedException ex) {
            ex.printStackTrace();
            log.warn("Unable to send Undelivered notification. Continuing but problems may happen.");
        }     
    }

    private void sendTimer(Channel channel) {
        try {
            NakFifoTimer timer=new NakFifoTimer(param_TIMER_PERIOD,channel,this,EventQualifier.ON);
            timer.go();
            timerChannel=channel;
        } catch (AppiaException ex) {
            //ex.printStackTrace();
            log.error("Unable to send timer. Corrcet operation of session is not guaranteed.");
        }
    }


    /*

  /** Number of bytes of the sequence number of messages.   
  public static final int SEQ_SIZE=4;
  /** Number of bytes for the first sequence number.   
  public static final int INIT_SEQ_SIZE=3;
  protected static long SEQ_MASK;
  protected static long INIT_SEQ_MASK;
  static {
    SEQ_MASK=0;
    for (int i=0 ; i < SEQ_SIZE ; i++)
      SEQ_MASK |= ((long)0xFF) << (i*8);

    INIT_SEQ_MASK=0;
    for (int i=0 ; i < INIT_SEQ_SIZE ; i++)
      INIT_SEQ_MASK |= ((long)0xFF) << (i*8);    
  }

  private void pushSeq(SendableEvent ev, long seq) {
    mbuf.len=SEQ_SIZE;
    ev.getMessage().push(mbuf);

    if (SEQ_SIZE == 4) {
      mbuf.data[mbuf.off+0]=(byte)((seq >>> 24) & 0xFF);
      mbuf.data[mbuf.off+1]=(byte)((seq >>> 16) & 0xFF);
      mbuf.data[mbuf.off+2]=(byte)((seq >>>  8) & 0xFF);
      mbuf.data[mbuf.off+3]=(byte)((seq >>>  0) & 0xFF);
    } else {
      for (int i=0 ; i < SEQ_SIZE ; i++)
        mbuf.data[mbuf.off+i]=(byte)((seq >>> ((SEQ_SIZE-i-1)*8)) & 0xFF);
    }
  }

  private long popSeq(SendableEvent ev, boolean keep) {
    mbuf.len=SEQ_SIZE;
    if (keep)
      ev.getMessage().peek(mbuf);
    else
      ev.getMessage().pop(mbuf);

    long l=0;
    if (SEQ_SIZE == 4) {
      l |= (((long)mbuf.data[mbuf.off+0]) & 0xFF) << 24;
      l |= (((long)mbuf.data[mbuf.off+1]) & 0xFF) << 16;
      l |= (((long)mbuf.data[mbuf.off+2]) & 0xFF) <<  8;
      l |= (((long)mbuf.data[mbuf.off+3]) & 0xFF) <<  0;
    } else {
      for (int i=0 ; i < SEQ_SIZE ; i++)
        l |= (((long)mbuf.data[mbuf.off+i]) & 0xFF) << ((SEQ_SIZE-i-1)*8);
    }

    return l;
  }
     */

    // DEBUG
    /** Full debug information.
     */  
    public static final boolean debugFull=true;
    public static final int debugListLimit=10;

    private void debugPeer(Peer peer, String s) {
        if (debugFull && log.isDebugEnabled()) {
            s="@"+s+" Peer: "+peer.addr.toString()+"\n";
            s+="\t First Msg Sent: "+peer.first_msg_sent+"\n";
            s+="\t Last Msg Sent/Confirmed: "+peer.last_msg_sent+"/"+peer.last_msg_confirmed+"\n";
            s+="\t Last Msg Delivered: "+peer.last_msg_delivered+"\n";
            s+="\t Rounds Appl/Sent/Recv: "+peer.rounds_appl_msg+"/"+peer.rounds_msg_sent+"/"+peer.rounds_msg_recv+"\n";

            int limit=debugListLimit;
            s+="\t Unconfirmed Msgs:"+"\n";
            ListIterator iter=peer.unconfirmed_msgs.listIterator();
            long l=peer.last_msg_confirmed;
            while (iter.hasNext()) {
                SendableEvent ev=(SendableEvent)iter.next();
                l++;
                s+="\t\t "+l+": "+ev+"\n";
                if (--limit <= 0) {
                    s+="\t\t  ..."+"\n";
                    break;
                }
            }

            limit=debugListLimit;
            s+="\t Undelivered Msgs:"+"\n";
            iter=peer.undelivered_msgs.listIterator();
            while (iter.hasNext()) {
                SendableEvent ev=(SendableEvent)iter.next();
                l=utils.popSeq(ev.getMessage(),peer.last_msg_delivered,true);
                s+="\t\t "+l+": "+ev+"\n";
                if (--limit <= 0) {
                    s+="\t\t  ..."+"\n";
                    break;
                }
            }

            s+="\t Nacked First/Last/Rounds: ";
            if (peer.nacked == null)
                s+="null"+"\n";
            else
                s+=""+peer.nacked.first_msg+"/"+peer.nacked.last_msg+"/"+peer.nacked.rounds+"\n";

            s+="\t Channel: "+peer.last_channel+"\n";

            log.debug(s);
        }
    }
    /*  
  public static void main(String[] args) {
    NakFifoSession fs=new NakFifoSession(null);
    SendableEvent ev=new SendableEvent();
    Peer peer=new Peer(new InetWithPort());
    long l;

    System.out.println("SEQ_SIZE="+SEQ_SIZE+" SEQ_MASK="+Long.toHexString(SEQ_MASK)+" INIT_SEQ_SIZE="+INIT_SEQ_SIZE+" INIT_SEQ_MASK="+Long.toHexString(INIT_SEQ_MASK));

//    if (SEQ_SIZE == 4) {
      ev=new SendableEvent();
      fs.storeUndelivered(peer,ev,10);
      ev=new SendableEvent();
      fs.storeUndelivered(peer,ev,15);
      ev=new SendableEvent();
      fs.storeUndelivered(peer,ev,16);
      ev=new SendableEvent();
      fs.storeUndelivered(peer,ev,12);
      ev=new SendableEvent();
      fs.storeUndelivered(peer,ev,9);
      ev=new SendableEvent();
      fs.storeUndelivered(peer,ev,11);
//    }
    fs.debugPeer(peer,"");
  }
     */
}
