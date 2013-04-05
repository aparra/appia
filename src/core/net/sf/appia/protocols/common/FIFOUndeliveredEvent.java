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
// Class: FIFOUndelivered. Event that notifies upper layers that    //
//        FIFO has not delivered one message to a peer.             //
//                                                                  //
// Author: Hugo Miranda, 10/2000                                    //
//                                                                  //
// Change Log:                                                      //
//                                                                  //
//////////////////////////////////////////////////////////////////////

import net.sf.appia.core.*;
import net.sf.appia.core.events.SendableEvent;


/**
 * This event is how FIFO notifies upper layers that a message could not
 *  be delivered. The event contains the undelivered message.
 * @see Event
 * @see net.sf.appia.protocols.fifo.FifoSession
 * @see SendableEvent
 * @author Hugo Miranda
 */
public class FIFOUndeliveredEvent extends SendableNotDeliveredEvent {
  
  /**
   * Constructor avoiding explicit initialization. The event always goes up.
   *
   * @param c The channel where the event will flow
   * @param gen The session creating the event
   * @param what The undelivered event
   * @see Channel
   * @see Session
   * @see SendableEvent
   */
  public FIFOUndeliveredEvent(Channel c, Session gen, SendableEvent what)
  throws AppiaEventException {
    
    super(c,gen,what);
  }
}
