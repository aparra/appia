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
// Class: MulticastInitEvent: Carries the multicast socket     //
//                              information until the udpcomplete   //
//                                                                  //                            //
// Author: M.Joao Monteiro, 12/2001                                 //
//                                                                  //
//////////////////////////////////////////////////////////////////////


import java.net.SocketAddress;

import net.sf.appia.core.*;


/**
 * MulticastInitEvent is the event that must be received by UdpCompleteSession
 * in order to initializa multicast communication. It notifies the UdpCompleteSession
 * of the multicast address to be used and also specifies if local multicast messages
 * are supposed to be forwarded.
 *
 * @see Event
 * @see net.sf.appia.protocols.udpsimple.UdpSimpleSession
 * @author M.Joao Monteiro
 */

public class MulticastInitEvent extends Event {
  
  /**
   * The IP multicast address of the group.
   *
   */
  public SocketAddress ipMulticast;
  
  /**
   * Indicates whether local multicast messages are supposed to be forwarded.
   * (False means that local multicast messages are discarded in UdpCompleteSession.)
   * (True makes UdpCompleteSession forward every messages without distinction.)
   *
   */
  public boolean fullDuplex;
  
  /**
   * Indicator of error condition.
   */
	public boolean error; 

  /**
   * Creates an initialized MulticastInitEvent.
   *
   * @param ipMulticast the IP multicast address
   * @param fullDuplex  indicates whether local multicast messages are supposed to be forwarded
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @param source the {@link net.sf.appia.core.Session Session} that is generating the Event
   * @throws AppiaEventException as the result of calling
   */
  public MulticastInitEvent(SocketAddress ipMulticast, boolean fullDuplex, Channel channel, int dir, Session source) 
  throws AppiaEventException,NullPointerException {
    
    super(channel,dir,source);
    
    if (ipMulticast == null)
      throw new NullPointerException("appia:udpcomplete:MulticastInitEvent: Multicast address not supplied.");
    
    this.ipMulticast=ipMulticast;
    this.fullDuplex=fullDuplex;
  }
}

