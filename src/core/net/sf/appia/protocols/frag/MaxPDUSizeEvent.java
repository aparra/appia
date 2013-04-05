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
 package net.sf.appia.protocols.frag;

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
// Class: MaxPDUSizeEvent: Event used for learning the maximum PDU  //
// size of a channel.                                               //
//                                                                  //
// Author: Hugo Miranda, 09/2000                                    //
//                                                                  //
// Change Log:                                                      //
//                                                                  //
//////////////////////////////////////////////////////////////////////

import net.sf.appia.core.*;

/**
 * This is the event that is used by the Frag protocol to learn the maximum
 * Protocol Data Unit (PDU) size available for a channel. The event is initially
 * forwarded below in a EchoEvent and then from the bottom to the top expects
 * that each protocol performs one of two possible actions:
 * <ul>
 * <li> reset the pduSize attrbiute to the maximum value allowed by 
 * that protocol
 * <li> decreases the pduSize attribute by the maximum number of bytes it will
 *    add.
 * </ul>
 * @author Hugo Miranda
 * @see FragSession
 * @see FragLayer
 * @see net.sf.appia.core.events.channel.EchoEvent
 * @see Event
 */
public class MaxPDUSizeEvent extends Event {

    /**
     * Keeps the maximum Protocol Data Unit size available for the session in the
     * current channel.
     */
    public int pduSize;
    
    public MaxPDUSizeEvent(){
    	super();
    }

    public MaxPDUSizeEvent(Channel channel, int dir, Session src) throws AppiaEventException{
    	super(channel,dir,src);
    }

}
