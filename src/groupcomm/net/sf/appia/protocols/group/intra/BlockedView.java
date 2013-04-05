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
 package net.sf.appia.protocols.group.intra;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;



/**
 * Event notifying the installation of a new group view.
 * <br>
 * It differs from the {@link net.sf.appia.protocols.group.intra.View View event} only
 * in that it doesn't release the blocking on sending messages. This means
 * the group remains blocked because another view change will take place.
 *
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.intra.View
 * @see net.sf.appia.protocols.group.sync.BlockOk
 * @see net.sf.appia.protocols.group.ViewState
 * @see net.sf.appia.protocols.group.LocalState
 */

public class BlockedView extends View {

  /**
   * Constructs an uninitialized <i>BlockedView</i> event.
   *
   * @param vs the new {@link net.sf.appia.protocols.group.ViewState ViewState}
   * @param ls the new {@link net.sf.appia.protocols.group.LocalState LocalState}
   */
  public BlockedView(ViewState vs, LocalState ls) {
    super(vs,ls);
  }

  /**
   * Constructs an initialized <i>View</i> event.
   *
   * @param vs the new {@link net.sf.appia.protocols.group.ViewState ViewState}
   * @param ls the new {@link net.sf.appia.protocols.group.LocalState LocalState}
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @param source the {@link net.sf.appia.core.Session Session} that is generating the Event
   * @throws AppiaEventException as the result of calling
   * {@link net.sf.appia.protocols.group.events.GroupEvent#GroupEvent(Channel,int,Session,Group,ViewID)
   * GroupEvent(Channel,int,Session,Group,ViewID)}
   */
  public BlockedView(ViewState vs, LocalState ls, Channel channel, int dir, Session source) throws AppiaEventException {
    super(vs,ls,channel,dir,source);
  }
}
