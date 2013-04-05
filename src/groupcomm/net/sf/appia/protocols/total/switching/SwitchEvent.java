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

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.events.GroupSendableEvent;


/**
 * This class defines a SwitchEvent that is used to signal the start of
 * the Total Order Switching procedure.
 * 
 * @author Jose Mocito
 * @version 0.7
 */
public class SwitchEvent extends GroupSendableEvent {

    /**
     * Name of the bottom channel that will replace the current one.
     */
    protected String nextChannelName;
    
    public SwitchEvent() {
        super();
    }
    
    public SwitchEvent(Message msg) {
        super(msg);
    }
    
	public SwitchEvent(Channel channel, int dir, Session source, Group group,
			ViewID view_id) throws AppiaEventException {
		super(channel, dir, source, group, view_id);
	}

	public SwitchEvent(Channel channel, int dir, Session source, Group group,
			ViewID view_id, Message msg) throws AppiaEventException {
		super(channel, dir, source, group, view_id, msg);
    }

    /**
     * Sets the name of the channel that will replace the current one.
     * 
     * @param name Name of the replacing channel.
     */
    public void setNextChannelName(String name) {
        nextChannelName = name;
    }
}
