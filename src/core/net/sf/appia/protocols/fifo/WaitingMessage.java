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
// Class: WaitingMessage                                            //
//                                                                  //
// Author: Nuno Carvalho, 11/2001                                   //
//                                                                  //
// Change Log:                                                      //
//////////////////////////////////////////////////////////////////////

import java.util.LinkedList;

import net.sf.appia.core.events.AppiaMulticast;
import net.sf.appia.core.events.SendableEvent;

/**
 * this class keeps information about the Pending message
 * and number of endPoints that didn't ack
 * <br> It is also a Node of the global list of messages
 * @see FifoSession
 * @author Nuno Carvalho
 */
public class WaitingMessage {

    /* pending event */
    public SendableEvent event;
    /* number of receivers of this messge */
    protected int endPoints, nResends;
    protected long timeStamp;

    /* header of each user that this message was sent to */
    private LinkedList<Header> headers;

    /* constructors */
    public WaitingMessage(SendableEvent e, int nResends) {
	super();
	init(e, nResends);
	this.timeStamp = e.getChannel().getTimeProvider().currentTimeMillis();
    }
    
    public WaitingMessage(SendableEvent e, long ts, int nResends) {
	super();
	init(e, nResends);
	this.timeStamp = ts;
    }

    /**
     * add a header into the LinkedList of headers
     */
    public void addHeader(Header h) {
	headers.addLast(h);
    }

    /**
     * remove a header from the LinkedList of headers
     */
    public void removeHeader(Header h) {
	headers.remove((Object)h);
    }

    /**
     * gets a array of headers of Peers that this message was sent to.
     * @return a array of headers
     * @see Header
     * @deprecated
     */
    public Object[] toHeaderArray() {
	return headers.toArray();
    }
    
    public LinkedList<Header> getHeaders(){
        return headers;
    }
    
    public boolean equals(Object o) {
	if (o instanceof WaitingMessage) {
	    final WaitingMessage we = (WaitingMessage)o;
	    return event.equals(we.event) &&
		(endPoints == we.endPoints) &&
		(timeStamp == we.timeStamp);
	}
	else return false;
    }
    
    /* initializes this WaitingMessage */
    private void init(SendableEvent e, int nResends) {
	this.event = e;
	if (e.dest instanceof AppiaMulticast)
	    endPoints = ((AppiaMulticast)e.dest).getDestinations().length;
	else
	    endPoints = 1;
	this.headers = new LinkedList<Header>();
	this.nResends = nResends;
    }
}
