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

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.events.channel.Debug;
import net.sf.appia.core.events.channel.EchoEvent;


//////////////////////////////////////////////////////////////////////
//                                                                  //
// Appia: protocol development and composition framework            //
//                                                                  //
// Class: FragLayer: Layer class of the Frag protocol               //
//                                                                  //
// Author: Hugo Miranda, 09/2000                                    //
//                                                                  //
// Change Log:                                                      //
//                                                                  //
//////////////////////////////////////////////////////////////////////


/**
 * Layer component for the fragmentation
 * protocol.
 *
 * This protocol provides the following events:
 * <ul>
 * <li>FragEvent: used by the protocol to pass message
 * fragments.
 * <li>EchoEvent: For carrying the MaxPDUSizeEvent
 * <li>MaxPDUSizeEvent: Event that queries the lower layers for the maximum
 * PDU size
 *</ul>
 * The protocol accepts the following events:
 * <ul>
 * <li>SendableEvent (require): Messages to be fragmented and
 * reassembled (inc. FragEvent)
 * <li>ChannelInit (require): Used by the session to learn that it belongs
 * to a new channel and that it must discover the maximum PDU size for it.
 * <li>ChannelClose (accept): Frees the channel when there are no more
 * fragments to send.
 * <li>Debug (accept): This protocol handles correctly the Debug event.
 * <li>MaxPDUSizeEvent (accept)
 * </ul>
 * @author Hugo Miranda
 * @see FragSession
 * @see net.sf.appia.core.Layer
 * @see FragEvent
 * @see net.sf.appia.core.events.channel.EchoEvent
 * @see net.sf.appia.protocols.frag.MaxPDUSizeEvent
 * @see net.sf.appia.core.events.SendableEvent
 * @see net.sf.appia.core.events.channel.ChannelInit
 * @see net.sf.appia.core.events.channel.ChannelClose
 * @see net.sf.appia.core.events.channel.Debug
 * @see net.sf.appia.core.Event
 */

public class FragLayer extends Layer {
  
  /**
   * Standard Empty constructor.
   *
   * @see Layer
   */
  public FragLayer() {
      evRequire=new Class[]{
              SendableEvent.class,
              ChannelInit.class,
      };
    evAccept=new Class[]{
            SendableEvent.class,
            ChannelInit.class,
            ChannelClose.class,
            Debug.class,
            net.sf.appia.protocols.frag.MaxPDUSizeEvent.class,
            net.sf.appia.protocols.frag.FragTimer.class,
    };
    evProvide=new Class[]{
            net.sf.appia.protocols.frag.FragEvent.class,
            EchoEvent.class,
            net.sf.appia.protocols.frag.MaxPDUSizeEvent.class,
            net.sf.appia.protocols.frag.FragTimer.class,
    };
  }
  
  /**
   * Session instantiation
   */
  public Session createSession() {
    return new FragSession(this);
  }
}
