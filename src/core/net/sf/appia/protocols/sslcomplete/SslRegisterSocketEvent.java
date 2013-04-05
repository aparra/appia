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
 * Initial developer(s): Pedro Vicente.
 * Contributor(s): See Appia web page for a list of contributors.
 */
package net.sf.appia.protocols.sslcomplete;

import net.sf.appia.core.*;
import net.sf.appia.protocols.common.RegisterSocketEvent;


/**
 * This class defines a SslRegisterSocketEvent
 * 
 * @author Pedro Vicente and Alexandre Pinto
 * @version 1.0
 */
public class SslRegisterSocketEvent extends RegisterSocketEvent {
	
	/**
	 * Protocol used in secure communication.
	 * Ex: "SSL", "TLS"
	 */
	public String protocol = "SSL";
	
	/**
	 * Certificates implementation used in SSL authentication.<br>
	 * Used to create the KeyManagers and TrustManagers that determine, respectively,
	 * the certificate sent in authentication and if the certificate received is accepted.
	 */ 
	public String certificateManagers = "SunX509";
	
	/**
	 * KeyStore, format  used to store the keys.
	 * Ex: "JKS"
	 */
	public String keyStore = "JKS";
	
	/**
	 * Name of the file were the certificates are stored.
	 * The identity certificate sent in authentication, and the recognized/trusted certificates
	 * used to authenticate the peer must be in the file.
	 * <b>Required for peer authentication</b>.
	 */
	public String keystoreFile=null;
	
	/**
	 * Passphrase to access the file where the certificate is stored.
	 */
	public char[] passphrase=null;
	
	/**
	 * Enabled ciphers used in SSL.<br>
	 * See JSSE documentation
	 */
	public String[] enabledCiphers=null;
	
	/**
	 * Creates a SslRegisterSocketEvent with first available port.
	 * After a port is assigned the event is sent upwards.
	 *
	 * @param channel The Channel where the event will flow
	 * @param dir The direction of the event
	 * @param source The session generating the event
	 */
	public SslRegisterSocketEvent(Channel channel,int dir, Session source) 
	throws AppiaEventException {
		super(channel,dir,source);
		error=false;
	}
	
	/**
	 * Creates a SslRegisterSocketEvent with the given port.
	 * After a port is assigned the event is sent upwards.
	 *
	 * @param channel The Channel where the event will flow
	 * @param dir The direction of the event
	 * @param source The session generating the event
	 * @param port The port number that will be binded to the UdpSimpleSession
	 */
	public SslRegisterSocketEvent(Channel channel,int dir, Session source, int port) 
	throws AppiaEventException {
		super(channel,dir,source,port);
		error=false;
	}
	
	/**
	 * Creates a SslRegisterSocketEvent with first available port.
	 * After a port is assigned the event is sent upwards.
	 * Depending on the ciphers used peer authentication may be enabled.
	 *
	 * @param channel The Channel where the event will flow
	 * @param dir The direction of the event
	 * @param source The session generating the event
	 * @param certsFile The file where the certificates used for peer authentication are stored.
	 * @param passphrase The passphase to access the certificate file.
	 */
	
	public SslRegisterSocketEvent(Channel channel,int dir, Session source, String certsFile, char[] passphrase) 
	throws AppiaEventException {
		super(channel,dir,source);
		keystoreFile = certsFile;
		this.passphrase = passphrase;
		error=false;
	}
	
	/**
	 * Creates a SslRegisterSocketEvent with the given port.
	 * After a port is assigned the event is sent upwards.
	 * Depending on the ciphers used peer authentication may be enabled.
	 *
	 * @param channel The Channel where the event will flow
	 * @param dir The direction of the event
	 * @param source The session generating the event
	 * @param port The port number that will be binded
	 * @param certsFile The file where the certificates used for peer authentication are stored.
	 * @param passphrase The passphase to access the certificate file.
	 */
	
	public SslRegisterSocketEvent(Channel channel,int dir, Session source, int port, String certsFile, char[] passphrase) 
	throws AppiaEventException {
		super(channel,dir,source,port);
		keystoreFile = certsFile;
		this.passphrase = passphrase;
		error=false;
	}	
}
