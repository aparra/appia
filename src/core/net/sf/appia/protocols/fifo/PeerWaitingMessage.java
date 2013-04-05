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
// Class: WaitEvent : Keeps events and their sequence numbers to be //
// hold in a Queue.                                                 //
//                                                                  //
// Author: Hugo Miranda, 05/2000                                    //
//                                                                  //
// Change Log:                                                      //
//                                                                  //
//////////////////////////////////////////////////////////////////////


/**
 * WaitEvent simply keeps a pair with the SendableEvent and the
 * sequence number. This structure is kept in peer info vectors
 * to keep the holding messages wether they go down (unacknowledged
 * messages) or up (out of order)
 */
class PeerWaitingMessage {
    
    public net.sf.appia.core.events.SendableEvent e;
    public int seqNumber;
    
    public PeerWaitingMessage(net.sf.appia.core.events.SendableEvent e,int seqNumber) {
	this.e = e;
	this.seqNumber = seqNumber;
    }
}
