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
 
package net.sf.appia.protocols.group.suspect;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.events.GroupEvent;



/**
 * {@link net.sf.appia.protocols.group.events.GroupEvent GroupEvent} that notifies that
 * one, or more, members of the group have failed.
 * <br>
 * The Event is sent by the
 * {@link net.sf.appia.protocols.group.suspect.SuspectLayer suspect layer} and contains a
 * bolleans array, of the same
 * {@link net.sf.appia.protocols.group.ViewState#view size of the current view}. Each
 * postion in the boolean array corresponds to the same position in the
 * {@link net.sf.appia.protocols.group.ViewState#view view array}. All the members
 * that have failed since the last <i>view change</i> or <i>Fail</i> Event have
 * their correspondent postion marked as <i>true</i>.
 * <br>
 * Notice that a failed member may have his position marked with false, if the
 * failure was anouced by a previous <i>Fail</i> Event.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.suspect.SuspectLayer
 * @see net.sf.appia.protocols.group.suspect.SuspectSession
 * @see net.sf.appia.protocols.group.ViewState
 */
public class Fail extends GroupEvent implements Cloneable {

  /**
   * The group members that have failed since the last <i>view change</i> or
   * <i>Fail</i> Event.
   */
  public boolean[] failed;

  /**
   * Constructs an uninitialized <i>Fail</i>
   * {@link net.sf.appia.protocols.group.events.GroupEvent Event}
   * with the given array of failed members.
   * <br>
   * It calls
   * {@link net.sf.appia.protocols.group.events.GroupEvent#GroupEvent(Group,ViewID)
   * GroupEvent(Group,ViewID)}.
   *
   * @param failed the array of failed members
   * @param group the {@link net.sf.appia.protocols.group.Group Group} of the Event
   * @param view_id the {@link net.sf.appia.protocols.group.ViewID ViewID} of the Event
   */
  public Fail(boolean[] failed, Group group, ViewID view_id) {
    super(group,view_id);
    this.failed=failed;
  }

  /**
   * Constructs an initialized <i>Fail</i> Event with the given array of failed
   * members.
   *
   * @param failed the array of failed members
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @param source the {@link net.sf.appia.core.Session Session} that is generating the Event
   * @param group the {@link net.sf.appia.protocols.group.Group Group} of the Event
   * @param view_id the {@link net.sf.appia.protocols.group.ViewID ViewID} of the Event
   * @throws AppiaEventException as the result of calling
   * {@link net.sf.appia.protocols.group.events.GroupEvent#GroupEvent(Channel,int,Session,Group,ViewID)
   * GroupEvent(Channel,int,Session,Group,ViewID)}
   */
  public Fail(boolean[] failed, Channel channel, int dir, Session source, Group group, ViewID view_id) throws AppiaEventException {
    super(channel,dir,source,group,view_id);
    this.failed=failed;
  }
  
  /**
   * Clones the Event.
   * @return Event
   * @throws CloneNotSupportedException
   */
  public Event cloneEvent() throws CloneNotSupportedException {
	  Fail ev = (Fail) super.cloneEvent();
	  boolean[] clonedArray = new boolean[failed.length];
	  System.arraycopy(failed,0,clonedArray,0,failed.length);
	  ev.failed = clonedArray;
	  return ev;
  }

}