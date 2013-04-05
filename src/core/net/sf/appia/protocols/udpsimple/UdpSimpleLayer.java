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

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.AppiaMulticastSupport;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.events.channel.Debug;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.common.SendableNotDeliveredEvent;
import net.sf.appia.protocols.frag.MaxPDUSizeEvent;




////
//Appia: protocol development and composition framework            //
////
//Version: 1.0/J                                                   //
////
//Copyright, 2000, Universidade de Lisboa                          //
//All rights reserved                                              //
//See license.txt for further information                          //
////
//Class: UdpSimpleLayer:   Unreliable send/receive using UDP/IP,   //
//allowing point-to-point or multicast    // 
//communication                           // 
////
//Author: Hugo Miranda, 05/2000                                    //
//M.Joao Monteiro, 12/2001                                 //
////
//// 



/**
 * Class UdpSimpleLayer is the Layer subclassing for UdpSimple protocol. This
 * protocol serializes and deserializes SendableEvents to/from the network using
 * UDP point-to-point or multicast sockets.
 *
 * The UdpSimple protocol provides the following events:
 * <ul>
 * <li>SendableEvent: or subclasses of it, depending on the messages received from
 * the network.
 *
 * <li>SendableNotDeliveredEvent: to notify upper protocols that the message could not
 * be delivered. The absense of such an event does not prove the contrary. That is,
 * this protocol doesn't provide reliable delivery.
 *
 * <li>UdpAsyncEvent: do not use. Used for inter-thread communication inside the protocol instance.
 * </ul>
 * The protocol accepts the following events:
 * <ul>
 * <li>RegisterSocketEvent (Require): This event instructs the protocol to bind to a specific
 * UDP port.
 *
 * <li>SendableEvent (Accept): sends SendableEvents to the network using its UDP socket
 *
 * <li>ChannelInit (Accept): Initialization procedures
 *
 * <li>Debug(Accept): Dumping status information as defined in the Appia specification.
 *
 * <li>ChannelClose (Accept): closing procedures.
 *
 * <li>MaxPDUSizeEvent (Accept): if requested, replies with the maximum datagram size for IP.
 *
 * <li>MulticastInitEvent (Accept) : This event instructs the protocol to open a socket multicast.
 *
 *</ul>
 * @see Layer
 * @see UdpSimpleSession
 * @see SendableEvent
 * @see SendableNotDeliveredEvent
 * @see RegisterSocketEvent
 * @see MulticastInitEvent
 * @see net.sf.appia.core.events.channel.ChannelInit
 * @see net.sf.appia.core.events.channel.Debug
 * @see net.sf.appia.core.events.channel.ChannelClose
 * @see net.sf.appia.protocols.frag.MaxPDUSizeEvent
 * @author Hugo Miranda, M.Joao Monteiro, Alexandre Pinto
 */

public class UdpSimpleLayer extends Layer implements AppiaMulticastSupport {

    /**
     * Standard empty constructor
     */
    public UdpSimpleLayer() {
        super();

        evProvide = new Class[] {
                SendableEvent.class,
                SendableNotDeliveredEvent.class,
        };

        evRequire = new Class[0];

        evAccept = new Class[] {
                SendableEvent.class,
                ChannelInit.class,
                RegisterSocketEvent.class,
                ChannelClose.class,
                MaxPDUSizeEvent.class,
                MulticastInitEvent.class,
                Debug.class,
        };
    }

    public Session createSession() {
        return new UdpSimpleSession(this);
    }
}
