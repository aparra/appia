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
// Class: Header                                                    //
//                                                                  //
// Author: Nuno Carvalho, 11/2001                                   //
//                                                                  //
// Change Log:                                                      //
//////////////////////////////////////////////////////////////////////

import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.message.*;


/**
 * Class that implements the header of a message.
 * @see SendableEvent
 * @see FifoSession
 * @author Nuno Carvalho
 */
public class Header {

	protected boolean mySyn, hisSyn;
	protected int sequenceNumber;
	protected WaitingMessage waitingMessage;
	protected PeerInfo peer;
    protected static final int INT_SIZE = 4;
    protected static final int HEADER_SIZE = INT_SIZE * 2;

	public Header(PeerInfo peer, WaitingMessage we) {
		this.mySyn = peer.sendMySyn();
		this.hisSyn = peer.sendSynAck();
		this.sequenceNumber = peer.nextOutgoing;
		this.waitingMessage = we;
		this.peer = peer;
	}

	/**
	 * push a header into the message of the sendable event
	 */
	public void pushHeader(SendableEvent e, int nextIncoming) {
		/*System.out.println(
			"-->> Sending message to "
				+ e.dest
				+ " with seq number = "
				+ sequenceNumber
				+ " and confirmation = "
				+ nextIncoming); */
		/* Append header with sequence number */
		final Message m = e.getMessage();
		/* Asks for 8 bytes and puts header in it*/
		final MsgBuffer msgBuf = new MsgBuffer();

		msgBuf.len = HEADER_SIZE;
		m.push(msgBuf);
		seqToByte(msgBuf, sequenceNumber, mySyn);
		msgBuf.off += INT_SIZE;
		seqToByte(msgBuf, nextIncoming, hisSyn);
	}

//	/**
//	 * push a header with a id into the message of the sendable event
//	 */
//	public void pushHeader(SendableEvent e, int nextIncoming, int id) {
//		/* Append header with sequence number */
//		final Message m = e.getMessage();
//		/* Asks for 8 bytes and puts header in it*/
//		final MsgBuffer msgBuf = new MsgBuffer();
//
//		msgBuf.len = 12;
//		m.push(msgBuf);
//		putInt(id, msgBuf);
//		msgBuf.off += 4;
//		seqToByte(msgBuf, sequenceNumber, mySyn);
//		msgBuf.off += 4;
//		seqToByte(msgBuf, nextIncoming, hisSyn);
//	}

	public boolean equals(Object o) {
		return (o instanceof Header)
			&& (((Header) o).mySyn == mySyn)
			&& (((Header) o).hisSyn == hisSyn)
			&& (((Header) o).sequenceNumber == sequenceNumber)
			&& peer.peer.equals(((Header) o).peer.peer);
	}

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

//	private void putInt(int i, MsgBuffer mbuf) {
//		mbuf.data[mbuf.off + 0] = (byte) ((i >>> 24) & 0xFF);
//		mbuf.data[mbuf.off + 1] = (byte) ((i >>> 16) & 0xFF);
//		mbuf.data[mbuf.off + 2] = (byte) ((i >>> 8) & 0xFF);
//		mbuf.data[mbuf.off + 3] = (byte) ((i >>> 0) & 0xFF);
//	}
}
