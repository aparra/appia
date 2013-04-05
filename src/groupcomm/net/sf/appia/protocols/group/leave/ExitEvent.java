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
 package net.sf.appia.protocols.group.leave;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;




/**
 * Event received when the member has left the group.
 * <br>
 * See {@link net.sf.appia.protocols.group.leave.LeaveLayer LeaveLayer} for more
 * details.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.leave.LeaveLayer
 * @see net.sf.appia.protocols.group.ViewState
 */
public class ExitEvent extends SendableEvent {

  /**
   * The group left.
   */
  public Group group;

  /**
   * The view id which it left.
   */
  public ViewID view_id;

  /**
   * Constructs an initialized <i>ExitEvent</i>.
   *
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @param source the {@link net.sf.appia.core.Session Session} that is generating the Event
   * @throws AppiaEventException as the result of calling
   * {@link net.sf.appia.core.Event#Event(Channel,int,Session)
   * Event(Channel,int,Session)}
   */
  public ExitEvent(Channel channel, int dir, Session source) throws AppiaEventException {
    super(channel,dir,source);
  }

  /**
   * Constructs an uninitialized <i>ExitEvent</i>.
   */
  public ExitEvent() {
    super();
  }

}