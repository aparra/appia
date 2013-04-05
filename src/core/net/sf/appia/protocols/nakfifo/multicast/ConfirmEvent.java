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
package net.sf.appia.protocols.nakfifo.multicast;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;


/** Event used to signal the last received message and therfore allow the
 * sender to clean the unconfirmed messages list.
 * 
 * @author Alexandre Pinto
 */
public class ConfirmEvent extends SendableEvent {

	/** Creates a new instance of ConfirmEvent */
	public ConfirmEvent() {
		super();
		setPriority(130);
	}

	/** Creates a new instance of ConfirmEvent */
	public ConfirmEvent(Channel channel, Session source)
			throws AppiaEventException {
		super(channel, Direction.DOWN, source);
		setPriority(130);
	}
}
