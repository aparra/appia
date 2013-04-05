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
 package net.sf.appia.protocols.group.heal;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;


/**
 * Event used to anounce the group within a IP multicast group.
 * 
 * @author Alexandre Pinto
 */
public class HelloEvent extends SendableEvent {

	/**
	 * Constructs an uninitialized event.<br>
	 * Uses a ExtendedMessage.
	 */
	public HelloEvent() {
		super();
	}

	/**
	 * Constructs an initialized event.<br>
	 * Uses a ExtendedMessage.
	 * 
	 * @see net.sf.appia.core.events.SendableEvent#SendableEvent(Channel,int,Session)
	 */
	public HelloEvent(Channel channel, int dir, Session source)
			throws AppiaEventException {
		super(channel, dir, source);
	}
}
