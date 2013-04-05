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
 
package net.sf.appia.protocols.group.events;

/**
 * Any subclass of
 * {@link net.sf.appia.protocols.group.events.GroupSendableEvent GroupSendableEvent},
 * that implements <i>Send</i> will be sent to the specified destinations,
 * instead of the entire group.
 * <br>
 * The destinations are given as an array of their ranks in the current
 * {@link net.sf.appia.protocols.group.ViewState view}. The array is placed in the
 * {@link net.sf.appia.core.events.SendableEvent#dest dest} attribute.
 * <br>
 * <b>Ex:</b> The current view is [A,B,C,D,E] and we wish to send a message to members
 * A, C and D. In "dest" we will put the array [0,2,3].
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.events.GroupSendableEvent
 */
public interface Send {}