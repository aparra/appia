/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2008 University of Lisbon and INESC-ID/IST
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
 * Initial developer(s): Nuno Carvalho
 * Contributor(s): See Appia web page for a list of contributors.
 */

package net.sf.appia.protocols.tcpcomplete;

import java.net.SocketAddress;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

/**
 * This class defines a CloseTcpSocket
 * 
 * @author Nuno Carvalho
 * @version 1.0
 */
public class CloseTcpSocket extends Event {

    private SocketAddress address;
    
    /**
     * Creates a new CloseTcpSocket.
     */
    public CloseTcpSocket() {
        super();
    }

    /**
     * Creates a new CloseTcpSocket.
     * @param channel
     * @param dir
     * @param src
     * @throws AppiaEventException
     */
    public CloseTcpSocket(Channel channel, int dir, Session src, SocketAddress addr)
            throws AppiaEventException {
        super(channel, dir, src);
        address = addr;
    }

    /**
     * @return Returns the address.
     */
    public SocketAddress getAddress() {
        return address;
    }

    /**
     * @param address The address to set.
     */
    public void setAddress(SocketAddress address) {
        this.address = address;
    }

}
