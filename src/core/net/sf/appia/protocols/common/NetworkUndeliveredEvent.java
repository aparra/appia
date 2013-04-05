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

package net.sf.appia.protocols.common;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

/**
 * This class defines a NetworkUndeliveredEvent
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class NetworkUndeliveredEvent extends Event {

    private Object failedAddress;
    
    /**
     * Creates a new NetworkUndeliveredEvent.
     */
    public NetworkUndeliveredEvent() {
        super();
    }

    /**
     * Creates a new NetworkUndeliveredEvent.
     * @param channel the channel
     * @param dir the direction of the event
     * @param src the source session
     * @throws AppiaEventException if an error occurs.
     */
    public NetworkUndeliveredEvent(Channel channel, int dir, Session src)
            throws AppiaEventException {
        super(channel, dir, src);
    }

    /**
     * @return Returns the failedAddress.
     */
    public Object getFailedAddress() {
        return failedAddress;
    }

    /**
     * @param failedAddress The failedAddress to set.
     */
    public void setFailedAddress(Object failedAddress) {
        this.failedAddress = failedAddress;
    }

}
