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
 package net.sf.appia.protocols.causalWaiting;

import net.sf.appia.protocols.group.events.GroupSendableEvent;

/**
 * Container for storing events and associated vectors to be used by
 * the Waiting Causal Order protocol.
 * 
 * @see CausalWaitingSession
 * @author Jose Mocito
 */
public class EventContainer {

	/**
	 * The event stored in the container.
	 */
	private GroupSendableEvent event;
	
	/**
	 * Causality information vector associated with the stored event.
	 */
	private long[] VC;
	
	/**
	 * Creates a new EventContainer storing the given event and causality
	 * information vector.
	 * 
	 * @param event the event to be stored in the container.
	 * @param VC the causality information vector associated with the event stored.
	 */
	public EventContainer(GroupSendableEvent event, long[] VC) {
		this.event = event;
		this.VC = VC;
	}
	
	/**
	 * Sets the event stored in the container.
	 * 
	 * @param event the event to be stored in the container.
	 */
	public void setEvent(GroupSendableEvent event) {
		this.event = event;
	}
	
	/**
	 * Sets the causality information vector.
	 * 
	 * @param VC the causality information vector associated with the event stored.
	 */
	public void setVC(long[] VC) {
		this.VC = VC;
	}

	/**
	 * Returns the event stored in the container.
	 * 
	 * @return the event stored in the container.
	 */
	public GroupSendableEvent getEvent() {
		return event;
	}

	/**
	 * The causality information vector associated with the stored event.
	 * 
	 * @return the causality information vector associated with the stored event.
	 */
	public long[] getVC() {
		return VC;
	}
}
