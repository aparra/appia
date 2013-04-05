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

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;
import net.sf.appia.core.message.Message;

/**
 * This class defines a ServiceEvent.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class ServiceEvent extends Event {
	
	private Message message;

	/**
	 * Basic Service event constructor. The argument passed should be used only to distinguish between several messages (as msgID).
	 * @param m the message concerning the notification service.
	 */
	public ServiceEvent(Message m) {
		super();
		message = m;
	}

	public ServiceEvent(Channel channel, int dir, Session src, Message msg)
			throws AppiaEventException {
		super(channel, dir, src);
		message = msg;
	}

	public Message getMessageID() {
		return message;
	}

}
