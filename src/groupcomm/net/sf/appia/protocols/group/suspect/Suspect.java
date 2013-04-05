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
 
/**
 * Title:        Apia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Alexandre Pinto & Hugo Miranda & Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto & Hugo Miranda & Luis Rodrigues
 * @version 1.0
 */
package net.sf.appia.protocols.group.suspect;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.events.GroupSendableEvent;



/** Event to signal suspicion about member(s) of the group.
 * <br>
 * When a layer above the <I>Suspect layer</I> suspects that some members are no longer
 * functioning properly, it should send a <I>Suspect</I> event downwards. The <I>failed</I> array
 * must be the same size of the current view and the indexes of the suspected members must be
 * set to <i>true</i>. The remaining should be set to <i>false</i>.
 */
public class Suspect extends GroupSendableEvent {

  /** The array with the failed members.
   * <br>
   * It must be the same size of the current view, and the indexes of the suspected
   * members are set to true.
   */  
  public boolean[] failed;

  /** Creates an uninitialized Suspect event.
   * @see net.sf.appia.protocols.group.events.GroupSendableEvent
   */  
  public Suspect() {}

  /** Creates a initialized Suspect event, with the given failed array.
   * @see net.sf.appia.protocols.group.events.GroupSendableEvent
   * @param failed The suspected members array.
   */  
  public Suspect(boolean[] failed, Channel channel, int dir, Session source, Group group, ViewID view_id) throws AppiaEventException {
    super(channel,dir,source,group,view_id);
    this.failed=failed;
  }
  
  /** Creates a initialized Suspect event with an array ,with the given size, where
   * the only suspected member corresponds to the given rank.
   * @param failed_rank The rank of the suspected member.
   * @param failed_size The number of members, ie, the current view size.
   */  
  public Suspect(int failed_rank, int failed_size, Channel channel, int dir, Session source, Group group, ViewID view_id) throws AppiaEventException {
    super(channel,dir,source,group,view_id);
    failed=new boolean[failed_size];
    for (int i=0 ; i < failed_size ; i++)
      failed[i]= (i == failed_rank);
  } 
}