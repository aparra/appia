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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;

import net.sf.appia.core.*;


/**
 * RegisterSocketEvent is the event that must be received by UdpSimpleSession or TcpCompleteSession
 * prior to any SendableEvent. It notifies the Session of the UDP ot TCP port number
 * to be used. The event is sent back with an error message (in the case that there is an error),
 * the local host and the port number to be used by other protocols.
 *
 * @see Event
 * @see net.sf.appia.protocols.udpsimple.UdpSimpleSession
 * @see net.sf.appia.protocols.tcpcomplete.TcpCompleteSession
 * @see net.sf.appia.core.events.SendableEvent
 * @author Hugo Miranda and Nuno Carvalho
 */

public class RegisterSocketEvent extends Event {
    
    /**
     * Using this constant in the port parameter of the constructor
     * the first port available will be assigned to the socket.
     */
    public static final int FIRST_AVAILABLE=0;
    
    /**
     * Using this constant in the port parameter of the constructor
     * a randomly but valid  port will be assigned to the socket.
     */
    public static final int RANDOMLY_AVAILABLE=-1;
    
    /**
     * Error code that indicates that the requested resource is not available.
     */
    public static final int MEDIA_NOT_AVAILABLE_ERROR = -1;
    
    /**
     * Error code that indicates that the requested resource is busy.
     * This means that the resource was bound by another process or entity.
     */
    public static final int RESOURCE_BUSY_ERROR = -2;
    
    /**
     * Error code that indicates that the requested resource is already
     * bound by the same process or entity (Appia instance).
     */
    public static final int RESOURCE_ALREADY_BOUND_ERROR = -3;
    
    /**
     * Local address of the socket.
     * <br>
     * The {@link net.sf.appia.protocols.udpsimple.UdpSimpleSession} binds its UDP socket
     * to this address. If null, one of the addresses of the machine is chosen, and bound to
     * the socket. The address chosing is done by {@link net.sf.appia.protocols.utils.HostUtils}. 
     * 
     * @see net.sf.appia.protocols.udpsimple.UdpSimpleSession
     */
    public InetAddress localHost=null;
    
    /**
     * The port to be used by the transport session.
     */
    public int port;
    
    /**
     * Indicator of error condition.
     */
    public boolean error;
    
    private int errorCode;
    private String errorDescription;
    
    /**
     * Creates a RegisterSocketEvent initializing all the necessary attributes.
     * Events using this constructor don't need to be initialized.
     * After a port is assigned the event is sent upwards.
     *
     * @param channel The Channel where the event will flow
     * @param dir The direction of the event
     * @param source The session generating the event
     * @param port The port number that will be binded to the UdpSimpleSession
     * @see Event
     */    
    public RegisterSocketEvent(Channel channel,int dir, Session source, int port)
    throws AppiaEventException {
        super(channel,dir,source);
        this.port=port;
        error=false;
    }
    
    /**
     * Creates a RegisterSocketEvent initializing all the necessary attributes.
     * Events using this constructor don't need to be initialized.
     * Using this constructor the system chooses an unused port.
     * After a port is assigned the event is sent informing
     * the interested layers of the actual actual used port.
     *
     * @param channel The Channel where the event will flow
     * @param dir The direction of the event
     * @param source The session generating the event
     */
    public RegisterSocketEvent(Channel channel,int dir, Session source)
    throws AppiaEventException {
        super(channel,dir,source);
        this.port=FIRST_AVAILABLE;
        error=false;
    }
    
    /**
     * @return Returns the errorCode.
     */
    public int getErrorCode() {
        return errorCode;
    }
    
    /**
     * @param errorCode The errorCode to set.
     */
    public void setErrorCode(int errorCode) {
        this.errorCode = errorCode;
    }
    
    /**
     * @return Returns the errorDescription.
     */
    public String getErrorDescription() {
        return errorDescription;
    }
    
    /**
     * @param errorDescription The errorDescription to set.
     */
    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }  
    
    /**
     * Gets the local address.
     * @return the local address.
     * @throws AppiaException if there is no address (for instance, there was an error in the sockets layer.
     */
    public SocketAddress getLocalSocketAddress() throws AppiaException {
        if(error)
            throw new AppiaException("There was an error upon socket registration: Error Code = "+errorCode+
                    " Description: "+errorDescription);
        if(this.getDir() == Direction.DOWN)
            throw new AppiaException("Event did not reach the socket session yet!");        
        return new InetSocketAddress(localHost,port);
    }
}

