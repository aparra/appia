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
 * Initial developer(s): Nuno Carvalho.
 * Contributor(s): See Appia web page for a list of contributors.
 */

package net.sf.appia.jgcs;

import java.net.SocketAddress;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

/**
 * This class defines a MessageSender
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class MessageSender extends Event {

	private AppiaMessage message;
	private SocketAddress destination;
	
	/**
	 * Creates a new MessageSender.
	 */
	public MessageSender() {
		super();
	}

	/**
	 * Creates a new MessageSender.
	 * @param channel the Appia channel
	 * @param dir direction
	 * @param m message
     * @param dest destination
	 * @throws AppiaEventException
	 */
	public MessageSender(Channel channel, int dir, AppiaMessage m, SocketAddress dest)
			throws AppiaEventException {
		super(channel, dir, null);
		message = m;
		destination = dest;
	}
	
	/**
	 * Creates a new MessageSender.
	 * @param channel
	 * @param dir
	 * @param src
	 * @throws AppiaEventException
	 */
	public MessageSender(Channel channel, int dir, Session src)
			throws AppiaEventException {
		super(channel, dir, src);
	}

	/**
	 * @return the destination
	 */
	public SocketAddress getDestination() {
		return destination;
	}

	/**
	 * @param destination the destination to set
	 */
	public void setDestination(SocketAddress destination) {
		this.destination = destination;
	}

	/**
	 * @return the message
	 */
	public AppiaMessage getMessage() {
		return message;
	}

	/**
	 * @param message the message to set
	 */
	public void setMessage(AppiaMessage message) {
		this.message = message;
	}

}
