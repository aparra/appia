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
// Class: PeerInfo: Fifo ordering support class grouping            //
// information for a certain peer                                   //
//                                                                  //
// Author: Hugo Miranda, Nuno Carvalho 11/2001                      //
//                                                                  //
// Change Log:                                                      //
//////////////////////////////////////////////////////////////////////


import java.util.LinkedList;

import net.sf.appia.core.*;
import net.sf.appia.core.events.*;

/**
 * PeerInfo contains next expected sequence number, next sequence
 * number to send and out of order messages for a single peer of a
 * fifo session. It is a implementation support class of the FIFO protocol.
 * 
 * Problems may arise with queuing of messages when crossing from the
 * highest sequence number possible ((2^31)-1) to 0. Enqueue of 
 * out-of-order messages is not available for messages between
 * 0 and queueSize. Reliability or ordering is not compromised with this
 * feature. Only efficiency might be slightly compromised.
 * @author Hugo Miranda, Nuno Carvalho
 */
public class PeerInfo {

    protected static final int DEFAULT_QUEUE_SIZE = 11;
	public int nextOutgoing, firstUnconfirmed, nextIncoming, lastAckSent;
	public int peerSyn;
	private boolean isMySynSent, isHisSynSent;
	private PeerWaitingMessage[] waitQueue;
	private int queueSize = DEFAULT_QUEUE_SIZE;

	/* Used to now if FIFO has give up sending messages to this endpoint. */
	public boolean failed = false;
	/* used to discard this peer if there are no message exchange for a long time */
	public long lastUsed;

	/* keeps references to headers of peers that not acked this message */
	public LinkedList<Header> headers;

	/* keep a list of channels used by this peer */
	private LinkedList<Channel> channels;

	/* Address of this peer */
	public Object peer;

	public PeerInfo(Object peer, Channel c) {
		final long now = c.getTimeProvider().currentTimeMillis();
		headers = new LinkedList<Header>();
		channels = new LinkedList<Channel>();
		channels.add(c);
		this.peer = peer;
        //FIXME: why this number?
		nextOutgoing = (int) (now % 0x7FFFFFFF);
		nextIncoming = 0;
		firstUnconfirmed = nextOutgoing;
		lastAckSent = -1;
		waitQueue = new PeerWaitingMessage[queueSize];
		isMySynSent = false;
		isHisSynSent = false;
//		isMySynAck = false;
		lastUsed = now;
	}

	/* methods to deal with channels list */

	public void addChannel(Channel c) {
		if (!channels.contains(c))
			channels.add(c);
	}

	public void removeChannel(Channel c) {
		channels.remove(c);
	}

	public Channel getChannel() {
		return (Channel) channels.getLast();
	}

	/* this peer has been used in the specified time */
	public void usedOn(long millis) {
		lastUsed = millis;
	}

	public boolean isOld(long now) {
		return ((now - lastUsed) >= FifoConfig.PEER_INACTIVITY_TIME);
	}

	/* methods that deal with sequence numbers */

	public void incOutgoing() {
		if (((long) nextOutgoing) + 1 == 0x80000000L)
			nextOutgoing = 0;
		else
			nextOutgoing++;
	}

	public void incIncoming() {
		if (((long) nextIncoming) + 1 == 0x80000000L)
			nextIncoming = 0;
		else
			nextIncoming++;
	}

	public boolean isNext(int check) {
		return isHisSynSent && nextIncoming == check;
	}

	public boolean isDuplicated(int check) {
		return nextIncoming > check
			|| (nextIncoming < 0x40000000 && check > 0x40000000);
	}

	public boolean isDuplicatedSyn(int check) {
		return !isHisSynSent || peerSyn == check;
	}

	public void ackSentNow() {
		lastAckSent = nextIncoming;
	}

	public boolean mustSendAck(int check) {
		return lastAckSent < check || nextIncoming < lastAckSent;
	}

	public boolean mustSendAck() {
		return nextIncoming < lastAckSent;
	}

	public SendableEvent dequeueNextIncoming() {

		/* Sequence number near 0 problem */
		if (nextIncoming < queueSize)
			return null;

		final int queueIndex = nextIncoming % queueSize;

		if (waitQueue[queueIndex] == null)
			return null;

		final SendableEvent ret = waitQueue[queueIndex].e;
		waitQueue[queueIndex] = null;

		return ret;
	}

	public void enqueueIncoming(SendableEvent e, int seqNumber) {

		/* Messages too far from next expected or near 
		   sequence number 0 will be thrown away */
		if (seqNumber - nextIncoming > queueSize || seqNumber < queueSize) {
			return;
		}
		final int queueIndex = seqNumber % queueSize;
		if (waitQueue[queueIndex] == null) {
			waitQueue[queueIndex] = new PeerWaitingMessage(e, seqNumber);
		}
	}

	public void confirmedUntil(int seq) {
		/* The if clause prevents out of order acks and performs a 
		   sanity check: acknowledgments of unsent messages */
		if (firstUnconfirmed < seq && seq <= nextOutgoing)
			firstUnconfirmed = seq;
		else
			/* Sequence numbers are turning from the biggest to 0 */
			if (firstUnconfirmed > nextOutgoing
				&& (firstUnconfirmed < seq || seq <= nextOutgoing))
				firstUnconfirmed = seq;
	}

	/* Since this layer uses cumulative acks, checking the first 
	   unacknowledged suffices */
	boolean isAcknowledged(int seq) {
		return firstUnconfirmed > seq ||
		/* seq number may turnaround */
		 (firstUnconfirmed < 0x40000000 && seq > 0x40000000);
	}

	/* Messages waiting to be acknowledged */
	public int getPendingMessages() {
		return nextOutgoing - firstUnconfirmed > 0
			? nextOutgoing - firstUnconfirmed
			: 0x7FFFFFFF - firstUnconfirmed + 1 + nextOutgoing;
	}

	/* Determining in which phase we are of the three-ways handshake */

	public boolean sendSynAck() {
		return isHisSynSent();
	}

	public boolean sendMySyn() {
		if (isMySynSent)
			return false;
		isMySynSent = true;
		return true;
	}

	public boolean isHisSynSent() {
		return isHisSynSent;
	}

	public void synReceived(int nextIncoming) {
		this.nextIncoming = nextIncoming;
		this.lastAckSent = nextIncoming - 1;
		this.peerSyn = nextIncoming;
		isHisSynSent = true;
	}

	public void forceAck() {
		lastAckSent = lastAckSent == 0 ? 0x7fffffff : lastAckSent - 1;
	}

	public void windowChange(int newWindow) {
		final PeerWaitingMessage[] newQueue = new PeerWaitingMessage[newWindow];
		int i = nextIncoming;
        final int j = Math.min(newWindow, queueSize) + nextIncoming;
		for (; i < j; i++)
			newQueue[i % newWindow] = waitQueue[i % queueSize];
		waitQueue = newQueue;
		queueSize = newWindow;
	}
    

    public String toString(){
        return "[Fifo peer: "+peer+" hasFailed="+failed+"]";
    }
}
