
/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2007 University of Lisbon
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
 * Initial developer(s): Jose Mocito.
 * Contributor(s): See Appia web page for a list of contributors.
 */
package net.sf.appia.protocols.uniform;

import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.events.GroupSendableEvent;


/**
 * This class defines a MessageContainer
 * 
 * @author Jose Mocito
 * @version 1.0
 */
public class MessageContainer {

	private long sn;
	private GroupSendableEvent sendableEvent;
	
	public MessageContainer(long sn, GroupSendableEvent e) {
		this. sn = sn;
		this.sendableEvent = e;
	}
    
	public GroupSendableEvent getSendableEvent() {
        return sendableEvent;
    }

    public void setSendableEvent(GroupSendableEvent sendableEvent) {
        this.sendableEvent = sendableEvent;
    }

    public int getOrig() {
		return sendableEvent.orig;
	}

	public long getSn() {
		return sn;
	}

	public void setSn(long sn) {
		this.sn = sn;
	}

	public boolean equals(MessageContainer cont) {
		if (this.getOrig() == cont.getOrig() && sn == cont.getSn())
			return true;
		return false;
	}
}
