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
 package net.sf.appia.protocols.sslcomplete;

import java.net.InetSocketAddress;

import net.sf.appia.core.*;
import net.sf.appia.protocols.tcpcomplete.TcpUndeliveredEvent;


/**
 * Event used to notify upper layers that a connection was closed.
 */
public class SslUndeliveredEvent extends TcpUndeliveredEvent {

    /**
     * Creates a new SslUndeliveredEvent.
     * @param channel channel
     * @param dir direction
     * @param session source session
     * @param iwp the failed address
     * @throws AppiaEventException
     */
	public SslUndeliveredEvent(Channel channel, int dir, Session session, InetSocketAddress iwp) throws AppiaEventException {
		super(channel, dir, session,iwp);
	}
}
