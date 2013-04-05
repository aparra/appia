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
 package net.sf.appia.protocols.udpsimple;

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
// Data, alteracao, razao/consequências                             //
//                                                                  //
//////////////////////////////////////////////////////////////////////

import net.sf.appia.core.*;
import net.sf.appia.core.events.SendableEvent;


/**
 * Currently replaced with <b>appia.protocols.common.SendableNotDeliveredEvent</b>
 * Remains for compatibility reasons.
 * @see net.sf.appia.protocols.common.SendableNotDeliveredEvent
 * @author Hugo Miranda
 * @deprecated
 */

public class SendableNotDeliveredEvent extends net.sf.appia.protocols.common.SendableNotDeliveredEvent {

  /**
     * Events created using this constructor do not need to be explicitly initialized.
     *
     * @param channel The channel where the event will flow. This is the same channel
     * of the event attribute.
     * @param source The session creating the event.
     * @param event The event that could not be delivered.
     * @see Channel
     * @see Session
     * @see SendableEvent
     * @deprecated
     */
    public SendableNotDeliveredEvent(Channel channel,Session source,
				     SendableEvent event) throws
	AppiaEventException {

	super(channel,source,event);
    }
}
