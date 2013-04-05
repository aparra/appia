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

package net.sf.appia.protocols.group.primary;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.events.Send;


/**
 * 
 * This class defines a DeliverViewEvent.
 * 
 * @author Jose Mocito
 * @version 1.0
 */
public class DeliverViewEvent extends GroupSendableEvent implements Send {

    public DeliverViewEvent(Channel channel, int dir, Session source,
            Group group, ViewID viewId) throws AppiaEventException {
        super(channel, dir, source, group, viewId);
    }

    public DeliverViewEvent() {
    }

    public DeliverViewEvent(Channel channel, int dir, Session source,
            Group group, ViewID viewId, Message omsg)
            throws AppiaEventException {
        super(channel, dir, source, group, viewId, omsg);
    }

    public DeliverViewEvent(Message omsg) {
        super(omsg);
    }

}
