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
 package net.sf.appia.protocols.fifo;

//////////////////////////////////////////////////////////////////////
//                                                                  //
// Appia: protocol development and composition framework            //
//                                                                  //
// Version: 1.0/J                                                   //
//                                                                  //
// Copyright, 2000, Universidade de Lisboa                          //
// All rights reserved                                              //
// See license.txt for further information                          //
//                                                                  //
// Class: FifoSession: Fifo reliable ordering for multicast         //
//                     or unicast messages                          //
//                                                                  //
// Author: Nuno Carvalho, 11/2001                                   //
//                                                                  //
// Change Log:
// 13/11/2002 - Nuno Carvalho - Removed bug on discarding of acked messages.
//////////////////////////////////////////////////////////////////////

import java.util.*;
import java.io.PrintStream;
import java.net.InetSocketAddress;

import net.sf.appia.core.*;
import net.sf.appia.core.events.*;
import net.sf.appia.core.events.channel.*;
import net.sf.appia.core.message.*;
import net.sf.appia.protocols.common.FIFOUndeliveredEvent;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.common.SendableNotDeliveredEvent;
import net.sf.appia.protocols.frag.MaxPDUSizeEvent;




/**
 * Class that implements reliable FIFO order for point to point
 * and multicast messages.
 * @author Nuno Carvalho
 * @see Session
 */
public class FifoSession extends Session {

    /*
	 * Keeps all addresses. each address is a PeerInfo
	 * that has a reference to all sequence numbers
	 * of Waiting messages on a HashMap.
	 */
	private HashMap<Object,PeerInfo> addresses;

	/*
	 * List of pending messages.
	 * This is a list of WaitingMessage classes 
	 * that holds the event and the timeStamp.
	 */
	private LinkedList<WaitingMessage> messages;
	private LinkedList<Channel> channels;
	private Channel timerChannel;

	private long timerPeriod;
	private int timersToResend, currentTTR, nResends;
	private TimeProvider timeProvider = null;

	private Object myAddr = null;
	private boolean changeTimer = false;

	private PrintStream debugOutput = System.out;

	/**
	 * Constructor of this session.
	 * @param l corresponding layer
	 */
	public FifoSession(Layer l) {
		super(l);

		addresses = new HashMap<Object,PeerInfo>();
		messages = new LinkedList<WaitingMessage>();
		channels = new LinkedList<Channel>();
		timerPeriod = FifoConfig.TIMER_PERIOD;
		currentTTR = timersToResend = FifoConfig.TIMERS_TO_RESEND;
		this.nResends = FifoConfig.NUM_RESENDS;
	}

	/**
	 * Method called when <i>Appia</i> has a event to deliver to this Session.
	 * The protocol accepts the following events:
	 * <ul>
	 * <li>SendableEvent: Events ordered by the protocol.
	 * <li>ChannelInit
	 * <li>ChannelClose
	 * <li>FifoTimer
	 * <li>MaxPDUSizeEvent: Decrements the pdu size by eight bytes
	 * <li>RegisterSocketEvent: Used to learn wich port (point to point) 
	 * was opened for communication
	 * <li>Debug: The protocol follows the usual procedures for 
	 * handling Debug information
	 * <li>FIFOConfigEvent: Event for configuration of several 
	 * parameters of the protocol.
	 * <li>MulticastInitEvent: Used to learn wich multicast 
	 * address will be used for communication
     * <li>AckEvent
     * <li>SendableNotDelivered
	 * </ul>
	 *
	 * @param e event to be delivered to this session
	 */
	public void handle(Event e) {
		//System.err.println("FIFO received event "+e);
		if (e instanceof ChannelInit)
			handleInit((ChannelInit) e);
		else if (e instanceof ChannelClose)
			handleChannelClose((ChannelClose) e);
		else if (e instanceof AckEvent)
			handleAck((AckEvent) e);
		else if (e instanceof SendableEvent)
			handleSendable((SendableEvent) e);
		else if (e instanceof FifoTimer)
			handleTimer((FifoTimer) e);
		else if (e instanceof FIFOConfigEvent)
			handleConfigEvent((FIFOConfigEvent) e);
		else if (e instanceof Debug)
			handleDebug((Debug) e);
		else if (e instanceof MaxPDUSizeEvent)
			handlePDUSize((MaxPDUSizeEvent) e);
		else if (e instanceof RegisterSocketEvent)
			handleRegisterSocket((RegisterSocketEvent) e);
		else if (e instanceof SendableNotDeliveredEvent)
			handleSendableNotDelivered((SendableNotDeliveredEvent) e);
		else {
			/* Unexpected event arrived */
			try {
				e.go();
			} catch (AppiaEventException ex) {
			}
		}
	}

	private void handleInit(ChannelInit e) {
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println(
				"FIFO: channel init from channel "
					+ e.getChannel().getChannelID());

		timeProvider = e.getChannel().getTimeProvider();
		try {
			e.go();
		} catch (AppiaEventException ex) {
			System.err.println(
				"(FIFO:handleInit) Unexpected event exception in FifoSession");
		}

		if (channels.size() == 0) {
			requestPeriodicTimer(e.getChannel());
		}
		channels.add(e.getChannel());
	}

	private void handleChannelClose(ChannelClose e) {
		/* Checks if the timer must be requested by other of 
		 * the available channels */
		channels.remove(e.getChannel());
		if ((e.getChannel() == timerChannel) && (channels.size() > 0)) {
			timerChannel = (Channel) channels.getFirst();
			requestPeriodicTimer(timerChannel);
		}

		/* removes the channel from the List of
		 * channels of each Peer */		
		for(PeerInfo p : addresses.values()){
		    p.removeChannel(e.getChannel());
		}

		try {
			e.go();
		} catch (AppiaEventException ex) {
		    ex.printStackTrace();
		}
	}

	/* Changes configuration */
	private void handleConfigEvent(FIFOConfigEvent e) {
		if (FifoConfig.DEBUG_ON) {
			System.out.println(
				"FifoSession: handleConfigEvent: received config event. Definitions to change:"
					+ "Period:"
					+ ((e.isPeriodDef())
						? "Yes (new value is " + e.getPeriod() + ") "
						: "No ")
					+ "Window:"
					+ ((e.isWindowDef())
						? "Yes (new value is " + e.getWindow() + ") "
						: "No ")
					+ "Timers to Resend:"
					+ ((e.isTimersToResendDef())
						? "Yes (new value is " + e.getTimersToResend() + ") "
						: "No ")
					+ "Number of resends: "
					+ ((e.isNumResendsDef())
						? "Yes (new value is " + e.getNumResends() + ") "
						: "No"));
		}

		if (e.isPeriodDef()) {
			timerPeriod = e.getPeriod();
			changeTimer = true;
		}
		if (e.isWindowDef()) {
			final int newWindow = e.getWindow();
			for(PeerInfo p : addresses.values())
			    p.windowChange(newWindow);
		}
		if (e.isTimersToResendDef())
			timersToResend = e.getTimersToResend();
		if (e.isNumResendsDef())
			nResends = e.getNumResends();

	}

	private void handlePDUSize(MaxPDUSizeEvent e) {
		try {
			/* Subtract to the actual PDUSize the biggest possible header */
			e.pduSize -= Header.HEADER_SIZE;
			e.go();
		} catch (AppiaEventException ex) {
			System.err.println(
				"Unexpected event exception when forwarding "
					+ "MaxPDUSize event in FIFO");
		}
	}

	private void handleSendableNotDelivered(SendableNotDeliveredEvent e) {
        // If the source of the event is null, this protocol cannot do nothing.
        if(e.getEvent().dest == null)
            return;
	    final PeerInfo p = addresses.get(e.getEvent().dest);
        if(p == null){
            if(FifoConfig.DEBUG_ON)
                System.err.println("Sendable not delivered to a peer that was not found in the peers hash map. Ignoring it.");
            return;
        }
            
	    if (FifoConfig.DEBUG_ON)
	        System.out.println("FifoSession: going to giveup sending a message to peer: "+p);
	    giveup(p, e.getEvent());
	}

	private void handleDebug(Debug e) {

		final int q = e.getQualifierMode();

		if (q == EventQualifier.ON) {
			debugOutput = new PrintStream(e.getOutput());
			debugOutput.println("FIFO: Debugging started");
		} else if (q == EventQualifier.OFF) {
			debugOutput = null;
		} else if (q == EventQualifier.NOTIFY) {
			printState(new PrintStream(debugOutput));
		}

		try {
			e.go();
		} catch (AppiaEventException ex) {
		    ex.printStackTrace();
		}
	}

	private void printState(PrintStream out) {
		out.println("FIFO Session state dumping:");
		out.println("Period : " + timerPeriod + "ms");
		out.println("Current number of peers: " + addresses.size());
		out.println("Buffer of messages size is " + messages.size());

		int count = 0;
		for(PeerInfo p : addresses.values()){
			out.println(
				"Host "
					+ count
					+ ": "
					+ (p.peer instanceof InetSocketAddress ? ((InetSocketAddress) p.peer).getAddress().getHostAddress()
						: "")
					+ " Port:"
					+ (p.peer instanceof InetSocketAddress	? "" + ((InetSocketAddress) p.peer).getPort()
						: ""));
			out.println("  Next sequence number to be sent: " + p.nextOutgoing);
			out.println("  Next sequence number expected: " + p.nextIncoming);
			out.println(
				"  First message still waiting for "
					+ "acknowledgment: "
					+ p.firstUnconfirmed);
			out.println(
				"  There are "
					+ p.getPendingMessages()
					+ " messages waiting to be acknowledged.");
			count++;

		}
		out.println(
			"Debug output is currently "
				+ (debugOutput == null ? "off" : "on"));
	}

	/* gets local address */
	private void handleRegisterSocket(RegisterSocketEvent rse) {
		if (rse.getDir() == Direction.UP && !rse.error) {
            myAddr = new InetSocketAddress(rse.localHost,rse.port);
		}

		try {
			rse.go();
		} catch (AppiaEventException ex) {
		    ex.printStackTrace();
		}
	}

	/*
	 * Acknowledgment event received. Will be used to delete all
	 * pending messages waiting for confirmation.
	 */
	private void handleAck(AckEvent e) {
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println("FIFO: Ack event received");
		/* Sanity checks */
		if (e.getDir() != Direction.UP) {
			/* Oops, this is not our acknowledge */
			handleSendable((SendableEvent) e);
			return;
		}
		final PeerInfo p = addresses.get(e.source);
		/* False is only expected when the peer has failed. Ignored */
		if (p == null)
			return;
		MsgBuffer msgBuf = new MsgBuffer();
		msgBuf.len = Header.INT_SIZE;
		e.getMessage().pop(msgBuf);
		/* false is not expected */
		if (hasSynActive(msgBuf)) {
			final int confirmation = byteToSeq(msgBuf);
			confirmedUntil(p, confirmation);
			if (FifoConfig.DEBUG_ON && debugOutput != null)
				debugOutput.println(
					"FIFO: Ack received until sequence "
						+ "number "
						+ confirmation);
		}
	}

	/**
	 * takes care of an incoming SendableEvent
	 */
	private void handleSendable(SendableEvent e) {
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println(
				"(FIFO:handleSendable) SendableEvent received! "+e);
			
			//System.err.println("--> S "+e.source+" D "+e.dest+" SS "+e.getSource()+" going "+(e.getDir()));

		switch (e.getDir()) {
			case Direction.UP :
				processIncoming(e);
				break;
			case Direction.DOWN :
				processOutgoing(e);
		}
	}

	private WaitingMessage prepareMessage(SendableEvent e) {
		final WaitingMessage we = new WaitingMessage(e, nResends);
		messages.addLast(we);
		return we;
	}

	private void sendMessage(WaitingMessage we, Header header) {
		SendableEvent clone = null;
		/* clone event to send a copy */
		try {
			clone = (SendableEvent) we.event.cloneEvent();
		} catch (CloneNotSupportedException ex) {
			System.err.println("(FIFO) could not clone event!");
		}
		header.peer.usedOn(timeProvider.currentTimeMillis());
		/* set destination */
		clone.dest = header.peer.peer;
		/* push message header into the message */
		header.pushHeader(clone, header.peer.nextIncoming);
		/* prepare event and send it */
		try {
			clone.setSourceSession(this);
			clone.init();
			clone.go();
			if (FifoConfig.DEBUG_ON && debugOutput != null)
				debugOutput.println("(FIFO:sendMessage) Event sent to channel");
		} catch (AppiaEventException ex) {
			System.err.println(
				"(Fifo:sendMessage) Unexpected exception in FifoSession: "+ex.getMessage());
		}
	}

	/*
	 * used to simulate multicast when there is no
	 * multicast socket open
	 */
	private void makeMulticast(Object[] dests, WaitingMessage we) {
		PeerInfo peer = null;
		Header header = null;

		/* for each member of the group */
		for (int i = 0; i < dests.length; i++) {
			/* find peer (or create it) */
			peer = addresses.get(dests[i]);
			if (peer == null) {
				if (FifoConfig.DEBUG_ON && debugOutput != null)
					debugOutput.println("(FIFO) Creating a new peer");
				peer = newPeer(dests[i], we.event.getChannel());
			} else
				peer.usedOn(timeProvider.currentTimeMillis());

			/* creates message header */
			header = new Header(peer, we);
			/* add to the peer (for message confirmations) */
			peer.headers.addLast(header);
			/* sends the message */
			sendMessage(we, header);
			/* keep a reference to the header to resend message */
			we.addHeader(header);
			peer.incOutgoing();
		}
	}

	/**
	 * process outgoing messages.
	 * this is just for p2p messages.
	 */
	private void processOutgoing(SendableEvent e) {
		final WaitingMessage we = prepareMessage(e);
		PeerInfo peer = null;
		Header header = null;

		if (e.dest instanceof AppiaMulticast) {
			/* do Multicast */
			if (FifoConfig.DEBUG_ON && debugOutput != null)
				debugOutput.println(
					"(FIFO) Processing outgoing multicast message.");

			final AppiaMulticast am = (AppiaMulticast) e.dest;
			final Object[] dests = am.getDestinations();
			makeMulticast(dests, we);
		} else {
			/* point to point */
			if (FifoConfig.DEBUG_ON && debugOutput != null)
				debugOutput.println("(FIFO) Processing outgoing p2p message.");
			/* gets PeerInfo from HashMap */
			peer = addresses.get(e.dest);
			if (peer == null) {
				if (FifoConfig.DEBUG_ON && debugOutput != null)
					debugOutput.println("(FIFO) Creating a new peer");
				peer = newPeer(e.dest, e.getChannel());
			} else
				peer.usedOn(timeProvider.currentTimeMillis());

			header = new Header(peer, we);
			peer.headers.addLast(header);
			sendMessage(we, header);
			/* keep a reference to the header to resend message */
			we.addHeader(header);
			peer.incOutgoing();
		}
	}

	/**
	 * process incoming SendableEvent
	 */
	private void processIncoming(SendableEvent e) {
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println(
				"(FIFO:processIncoming) Processing incoming message.");

        /* get header message */
		MsgBuffer header = new MsgBuffer();
		header.len = Header.INT_SIZE * 2;
		e.getMessage().pop(header);

		final PeerInfo p = checkConnection(e, header);
		/* p==null means "discard message" */
		if (p == null)
			return;
		else
			p.usedOn(timeProvider.currentTimeMillis());

		// This adds the message to an incoming queue and delivers
		// messages already acknowledged
		if (checkOrder(p, e, header))
			dequeue(p);
	}

	/**
	 * remove nodes not needed anymore
	 */
	private void confirmedUntil(PeerInfo peer, int seq) {
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println("(FIFO:confirmedUntil) seqNumber = " + seq);

		peer.confirmedUntil(seq);
		peer.usedOn(timeProvider.currentTimeMillis());
		final ListIterator<Header> it = peer.headers.listIterator();
		boolean done = false;
		Header h = null;
		while (it.hasNext() && !done) {
			h = it.next();
			if (h.sequenceNumber < seq) {
				h.waitingMessage.endPoints--;
				h.waitingMessage.removeHeader(h);
				if (h.waitingMessage.endPoints <= 0)
				    messages.remove(h.waitingMessage);
				it.remove();
			} else
				done = true;
		}
	}

	/* *************************
	 *   methods for the timer
	 * ************************* */

	private void processUnackedMessages() {
		// sends acks
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println("(FIFO:handleTimer) Processing acks.");
		
		for(PeerInfo peer : addresses.values()){
			if (peer.mustSendAck(peer.nextIncoming))
				sendAck(peer);
		}
	}

	private boolean isTimeToResend() {
		currentTTR--;
		if (currentTTR == 0) {
			currentTTR = timersToResend;
			return true;
		} else
			return false;
	}

	private void processResend() {
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println(
				"(FIFO) fifo will verify if it needs to resend messages.");

        final long currentTime = timeProvider.currentTimeMillis();
        long delta = 0;
        boolean stop = false;
        WaitingMessage message = null;
        LinkedList<WaitingMessage> localBuffer = new LinkedList<WaitingMessage>();
        ListIterator<WaitingMessage>it = messages.listIterator();
		while(!stop && it.hasNext()){
		    message = it.next();
            delta = currentTime - message.timeStamp;
            if (delta > timerPeriod){
                it.remove();
                localBuffer.addLast(message);
            }
            else
                stop = true;
		}
		for(WaitingMessage waiting : localBuffer)
            resendMessage(waiting);
	}

	private void resendMessage(WaitingMessage we) {    
		we.nResends--;
		if (we.nResends < 0) {
			if (FifoConfig.DEBUG_ON)
				System.out.println(
					"FifoSession: going to giveup sending some message because exceeded number of resends!");
			for (Header header : we.getHeaders())
				giveup(header.peer, we.event);
		} else {
			if (FifoConfig.DEBUG_ON)
				System.out.println(
					"FifoSession: going to resend a message! Number of retries left: "
						+ we.nResends);
			we.timeStamp = timeProvider.currentTimeMillis();
			for (Header header : we.getHeaders())
				sendMessage(we,header);
			/*TODO: this could be buggy because the method
			    is called inside an iterator.
			    This could be fixed by returning the new we and
			    adding later to the list;
			*/
			messages.addLast(we);
		}
	}

	/**
	 * process the periodic timer event:
	 * - checks all messages for resends
	 * - sends acks (if needed)
	 */
	private void handleTimer(FifoTimer timer) {
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println(
				"(FIFO:handleTimer) Processing periodic timer event.");
    
		if (isTimeToResend()) {
			processResend();
			cleanOldPeers();
		}

		processUnackedMessages();

		if (changeTimer) {
			final Channel channel = timer.getChannel();
			/* if a configEvent was received and period was changed, 
			 * we need to change thid timer.
			 * We cancel this one and start another with new period
			 */
			timer.setQualifierMode(EventQualifier.OFF);
			timer.setDir(Direction.invert(timer.getDir()));
			timer.setSourceSession(this);
			try {
				timer.init();
				timer.go();
			} catch (AppiaEventException ex) {
				System.err.println(
					"(FIFO:handleTimer) Error when trying to send timer with qulifier OFF");
			}

			requestPeriodicTimer(channel);
			changeTimer = false;
		} else
			try {
				timer.go();
			} catch (AppiaEventException ex) {
			}
	}

	/* if there are peers that didn't exchange messages for a long time, this info is discarded.
	 * this is done periodically */
	private void cleanOldPeers() {
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println("(FIFO:handleTimer) cleaning old peers...");
		final Iterator<Map.Entry<Object,PeerInfo>> peers = addresses.entrySet().iterator();
		PeerInfo peer = null;
		final long now = timeProvider.currentTimeMillis();
		while (peers.hasNext()) {
		    peer = peers.next().getValue();
			if (peer.isOld(now))
				peers.remove();
		}
	}

	private void requestPeriodicTimer(Channel channel) {
		timerChannel = channel;
		/* starts the timer */
		try {
			new FifoTimer(timerPeriod, channel, this).go();
		} catch (AppiaException ex) {
			System.err.println(
				"(FIFO:handleInit) Unexpected Appia Exception when"
					+ "trying to send FifoTimer");
		}
	}

	/* Removes a PeerInfo from the list of peers. Gives up of
	   sending messages to him
	*/
	private void giveup(PeerInfo p, SendableEvent s) {
		/* If the peer exists in the vector, removes it and
		   notifies sessions above with an event */
		//System.err.println("GIVEUP:\nFIFO source: "+s.source+" Session source "+s.getSource());
		//System.err.println("FIFO dest: "+s.dest+" event: "+s+" DIR "+(s.getDir()==Direction.UP?"UP":"DOWN"));
		if (!p.failed) {
			addresses.remove(p.peer);
			p.failed = true;
		}
		SendableEvent unAckedEvent = null;
		if (s.dest instanceof AppiaMulticast) {
			/* prepares event to notify other layers 
			 * that fifo couldn't deliver it */
			try {
				unAckedEvent = (SendableEvent) s.cloneEvent();
			} catch (CloneNotSupportedException ex) {
				System.err.println("(FIFO:giveup) Could not clone event!");
			}
			// set dest to peer that is being removed
			unAckedEvent.dest = p.peer;
		} else {
			unAckedEvent = s;
			// remove the fifo header before sending the event back
			unAckedEvent.getMessage().pop(new MsgBuffer(new byte[Header.HEADER_SIZE], 0, Header.HEADER_SIZE));
		}
		try {
			final FIFOUndeliveredEvent e =
				new FIFOUndeliveredEvent(s.getChannel(), this, unAckedEvent);
			e.go();
		} catch (AppiaEventException ex) {
			switch (ex.type) {
				case AppiaEventException.UNWANTEDEVENT :
					/* This one is ok. No one cares about it */
					break;
				case AppiaEventException.UNKNOWNSESSION :
					System.err.println(
						"Unknown session exception catched "
							+ "in FifoSession");
					break;
				case AppiaEventException.ATTRIBUTEMISSING :
					System.err.println(
						"Missing attribute exception catched "
							+ "in FifoSession");
					break;
				case AppiaEventException.NOTINITIALIZED :
					System.err.println(
						"Impossible exception event not "
							+ "initialized in FifoSession");
					break;
			}
		}
	}

	/*
	 * sends acks to the specified address (PeerInfo)
	 */
	private void sendAck(PeerInfo p) {
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println(
				"(FIFO:sendAck) Fifo will send a Ack event to: "+p.peer
					+ " to channel "
					+ p.getChannel().getChannelID());
		AckEvent ack = null;

		try {
			ack =
				new AckEvent(
					p.getChannel(),
					this,
					p.peer,
					myAddr);
			final Message m = p.getChannel().getMessageFactory().newMessage();
			MsgBuffer msgBuf = new MsgBuffer();
			msgBuf.len = Header.INT_SIZE;
			m.push(msgBuf);
			/* Always acknowledges the SYN */
			seqToByte(msgBuf, p.nextIncoming, true);
			ack.setMessage(m);
			ack.go();
			p.ackSentNow();
			p.usedOn(timeProvider.currentTimeMillis());

			if (FifoConfig.DEBUG_ON && debugOutput != null)
				debugOutput.println("(FIFO:sendAck) ack sent");
		} catch (AppiaEventException ex) {
			System.err.println(
				"(FIFO:sendAck) Unexpected event exception in FifoSession");
		}
	}

	/* ************************************ *
	 * Other methods!
	 * ************************************ */

	/* Utils */

	/* Most significant bit is used as the SYN flag in TCP: notifies
	   that this is the first packet sent from the source to the
	   destination.
	   So, sequence numbers range between 0 and (2^31)-1. */

	private void seqToByte(MsgBuffer buf, int seq, boolean syn) {

		buf.data[buf.off + 3] =
			(byte) ((byte) (0xff & (seq >> 24)) | ((byte) (syn ? 0x80 : 0x0)));
		buf.data[buf.off + 2] = (byte) (0xff & (seq >> 16));
		buf.data[buf.off + 1] = (byte) (0xff & (seq >> 8));
		buf.data[buf.off] = (byte) (0xff & seq);
	}

	private int byteToSeq(MsgBuffer buf) {
		return (
			((buf.data[buf.off + 3] & 0x7f) << 24)
				| ((buf.data[buf.off + 2] & 0xff) << 16)
				| ((buf.data[buf.off + 1] & 0xff) << 8)
				| (buf.data[buf.off] & 0xff));
	}

	private boolean hasSynActive(MsgBuffer buf) {
		return (buf.data[buf.off + 3] & 128) != 0;
	}

	/*
	 * used to create a peer when fifo receives a message
	 * from a new Addr
	 */
	private PeerInfo newPeer(Object who, Channel c) {
		final PeerInfo newpeer = new PeerInfo(who, c);
		addresses.put(who, newpeer);
		return newpeer;
	}

	@Deprecated
	protected void addMessage(WaitingMessage message) {
		messages.addLast(message);
	}

	@Deprecated
	protected void removeMessage(WaitingMessage message) {
		messages.remove(message);
	}

	@Deprecated
	protected int sizeOfBuffer() {
		return messages.size();
	}

	@Deprecated
	protected Object[] getArrayOfBuffer() {
		return messages.toArray();
	}

	/* *******************************************
	 * methods to help processIncoming()
	 * ******************************************* */
	private PeerInfo checkConnection(SendableEvent e, MsgBuffer header) {
		final boolean syn = hasSynActive(header);
		header.off += Header.INT_SIZE;
//		final boolean synAck = hasSynActive(header);
		header.off -= Header.INT_SIZE;

		if(FifoConfig.DEBUG_ON)
			System.out.println("<<-- Received message "+byteToSeq(header)+ " de "+e.source);

		PeerInfo p = addresses.get(e.source);
		if (FifoConfig.DEBUG_ON && debugOutput != null)
			debugOutput.println(
				"(FIFO:checkConnection) PeerInfo is "
					+ ((p == null) ? "NULL" : "NOT NULL"));

		if (syn) {
			/* SYN was active: several indications are possible with this.
			   If this is a duplicated SYN request (SYN flag on and the
			   same initial sequence number, ignore it. It will be handled
			   by the retransmission of the Ack. */

			/* 1: The peer is sending his half of a connection that was
			   already half open by this peer */
			if (p != null && !p.isHisSynSent()) {
				if (FifoConfig.DEBUG_ON && debugOutput != null)
					debugOutput.println(
						"FIFO: Peer completed connection " + "establishment");
				p.synReceived(byteToSeq(header));
			} else {
				/* 2: Peer has failed and resumed quickly. Nothing was
				   noted localy either because there were no pending
				   messages or because none was yet considered
				   undelivered (still retrying).
				   Declare him as failed so messages stop being
				   retransmited and this is informed to user. User will
				   be notified by receiving undelivered message events.
				   A new peer is created to handle the new connection
				   messages. */
				if (p != null && !p.isDuplicatedSyn(byteToSeq(header))) {
					if (FifoConfig.DEBUG_ON && debugOutput != null)
						debugOutput.println(
							"FIFO: Peer quick " + "failure/resume noticed");
					addresses.remove(e.source);
					p.failed = true;
				}
				/* 3: Peer is requesting a new connection to be open.
				   Creates a new peer and reades his initial sequence
				   number*/
				if (p == null || !p.isDuplicatedSyn(byteToSeq(header))) {
					p = newPeer(e.source, e.getChannel());
					p.synReceived(byteToSeq(header));
					if (FifoConfig.DEBUG_ON && debugOutput != null)
						debugOutput.println(
							"Half-connection established " + "with peer");
				}
			}
		}
		/* There is one special case: It is a new peer but the Syn flag
		   is not active. Probably this endpoint has crashed and recovered 
		   quickly. Discard the message (signaled by returning null). Later the
		   other endpoint will give up and learn about this failure.
		*/

		/* synAck: discarded */

		return p;
	}

	/* CheckOrder returns true if the event was the next expected. The
	 * goal is to dequeue only in this case else the event will be
	 * added to existing queue. */

	private boolean checkOrder(PeerInfo p, SendableEvent e, MsgBuffer header) {
		/* Extracting sequence number */
		final int seqNumber = byteToSeq(header);
		header.off += Header.INT_SIZE;
		final int confirmation = byteToSeq(header);
		/* Checks the piggybacked acknowledgment number */
		if (hasSynActive(header)) {
			confirmedUntil(p, confirmation);
			if (FifoConfig.DEBUG_ON && debugOutput != null)
				debugOutput.println(
					"(FIFO:checkOrder) Peer piggybacked confirmation until "
						+ confirmation+" from channel "+e.getChannel().getChannelID());
		}
		/* Check if event is next expected */
		if (p.isNext(seqNumber)) {
			p.incIncoming();
			try {
				e.go();
				if (FifoConfig.DEBUG_ON && debugOutput != null)
					debugOutput.println(
						"(FIFO:checkOrder) received event ("
							+ seqNumber
							+ ") is the next expected. Forwarding it.");
			} catch (AppiaEventException ex) {
				System.err.println(
					"Unexpected event not initialized exception "
						+ "in FifoSession");
			}
			return true;
		} else {
			if (!p.isDuplicated(seqNumber)) {
				p.enqueueIncoming(e, seqNumber);
				if (FifoConfig.DEBUG_ON && debugOutput != null)
					debugOutput.println(
						"(FIFO:checkOrder) received event ("
							+ seqNumber
							+ ") is not the next expected ("
							+ p.nextIncoming
							+ "). enqueing it.");
			} else {
				/* Duplicated message received. Force the sending of an
				   acknowledgment by considering that the lastAckSent is lower than
				   it realy is */
				p.forceAck();
				if (FifoConfig.DEBUG_ON && debugOutput != null)
					debugOutput.println(
						"(FIFO:checkOrder) Duplicated message ("
							+ seqNumber + ") received");
			}
		}
		return false;
	}

	
	private void dequeue(PeerInfo p) {
		SendableEvent e;
		while ((e = p.dequeueNextIncoming()) != null) {
			p.incIncoming();
			try {
				e.go();
			} catch (AppiaEventException ex) {
				System.err.println("Unexpected event in FifoSession");
			}
		}
	}

} // end of class
