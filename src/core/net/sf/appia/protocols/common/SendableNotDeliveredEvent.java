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
 package net.sf.appia.protocols.common;

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
// Class: SendableNotDeliveredEvent                                 //
//   UdpSimpleSession notification that a message could not be      //
//   delivered                                                      //
//                                                                  //
// Author: Hugo Miranda, 05/2000                                    //
//                                                                  //
// Change Log:                                                      //
//                                                                  //
//////////////////////////////////////////////////////////////////////

import net.sf.appia.core.*;
import net.sf.appia.core.events.SendableEvent;


/**
 * SendableNotDeliveredEvents are used to notify upper layers that
 * UdpSimple protocol was informed by the UDP socket that the datagram
 * could not be delivered to the destination. This event will always go UP
 *
 * @see net.sf.appia.core.Event
 * @see net.sf.appia.protocols.udpsimple.UdpSimpleSession
 * @see net.sf.appia.core.events.SendableEvent
 */

public class SendableNotDeliveredEvent extends NetworkUndeliveredEvent {
	/*
	 * The event received by UdpSimpleSession that could not be delivered.
	 * @see net.sf.appia.core.events.SendableEvent
	 */
    private SendableEvent event;
    
    /**
     * Events created using this constructor do not need to be explicitly initialized.
     *
     * @param channel The channel where the event will flow. This is the same channel
     * of the event attribute. The direction is set to UP.
     * @param source The session creating the event.
     * @param event The event that could not be delivered.
     * @see Channel the appia channel
     * @see Session the source session
     * @see SendableEvent the event not delivered
     */
    public SendableNotDeliveredEvent(Channel channel,Session source,
				     SendableEvent event) throws AppiaEventException {

	super(channel,Direction.UP,source);
	    this.event=event;
        this.setFailedAddress(event.dest);
    }

    /**
     * @return Returns the event.
     */
    public SendableEvent getEvent() {
        return event;
    }

    /**
     * @param event The event to set.
     */
    public void setEvent(SendableEvent event) {
        this.event = event;
        this.setFailedAddress(event.dest);
    }
}
