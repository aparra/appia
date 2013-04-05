/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006-2007 University of Lisbon
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
package net.sf.appia.protocols.total.switching;

import net.sf.appia.core.events.SendableEvent;

/**
 * Container for events stored by the Total Order Switching protocol.
 * 
 * @author Jose Mocito
 * @version 0.7
 */
public class EventContainer {

	protected int source;
	protected long sn;
	protected SendableEvent event;
	
	public EventContainer(int source, long sn, SendableEvent event) {
		super();
		this.source = source;
		this.sn = sn;
		this.event = event;
	}

	public boolean equals(Object obj) {
		if (obj instanceof EventContainer &&
				source == ((EventContainer) obj).source &&
				sn == ((EventContainer) obj).sn)
			return true;
		return false;
	}
	
	public String toString() {
		return source+":"+sn;
	}
}
