/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2008 Technical University of Lisboa
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
 * Developer(s): Nuno Carvalho
 * Contributor(s): See Appia web page for a list of contributors.
 */

package net.sf.appia.protocols.group.suspect;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;

/**
 * This class defines a SuspectedMemberEvent
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class SuspectedMemberEvent extends Event {

    private int suspectedMember;
    private Group group;
    private ViewID viewID;
    
    /**
     * Creates a new SuspectedMemberEvent.
     */
    public SuspectedMemberEvent() {
        super();
    }

    /**
     * Creates a new SuspectedMemberEvent.
     * @param channel
     * @param dir
     * @param src
     * @throws AppiaEventException
     */
    public SuspectedMemberEvent(Channel channel, int dir, Session src)
            throws AppiaEventException {
        super(channel, dir, src);
    }

    public int getSuspectedMember() {
        return suspectedMember;
    }

    public void setSuspectedMember(int suspectedMember) {
        this.suspectedMember = suspectedMember;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public ViewID getViewID() {
        return viewID;
    }

    public void setViewID(ViewID viewID) {
        this.viewID = viewID;
    }

}
