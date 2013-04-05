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

package net.sf.appia.jgcs.protocols.top;

import java.net.SocketAddress;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.message.Message;
import net.sf.appia.jgcs.AppiaMessage;


/**
 * This class defines a JGCSSendableEvent
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class JGCSSendableEvent extends SendableEvent {

	/**
	 * Creates a new JGCSSendableEvent.
	 */
	public JGCSSendableEvent() {
		super(new AppiaMessage());
	}

	/**
	 * Creates a new initialized JGCSSendableEvent.
	 * @param channel the channel to send the event
	 * @param dir the direction of the event
	 * @param source the session that created the event
	 * @param destAddress destination address
	 * @throws AppiaEventException if an error occurs
	 */
	public JGCSSendableEvent(Channel channel, int dir, Session source, SocketAddress destAddress)
			throws AppiaEventException {
		super(channel, dir, source);
		this.dest = destAddress;
	}

	/**
	 * Creates a new JJGCSSendableEvent.
	 * @param msg the message.
	 * @param destAddress destination address
	 */
	public JGCSSendableEvent(SocketAddress destAddress, Message msg){
		super(msg);
		this.dest = destAddress;
	}

}
