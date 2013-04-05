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
 package net.sf.appia.protocols.tcpcomplete;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.RegisterSocketEvent;



/**
 * Layer definition of the TCP protocol.
 * 
 * @author Pedro Vicente, Alexandre Pinto, Nuno Carvalho
 * @see TcpSimpleSession
  */
public class TcpCompleteLayer extends Layer implements net.sf.appia.core.events.AppiaMulticastSupport {

	public TcpCompleteLayer(){
		evProvide = new Class[]{
				TcpUndeliveredEvent.class,
				SendableEvent.class,
				TcpTimer.class,
		};
		
		evAccept = new Class[]{
				RegisterSocketEvent.class,
				SendableEvent.class,
				ChannelInit.class,
				ChannelClose.class,
				TcpTimer.class,
				CloseTcpSocket.class,
		};
		
		evRequire = new Class[]{
				RegisterSocketEvent.class,
				SendableEvent.class,
				ChannelInit.class,
		};
	}

	/**
	 * @see net.sf.appia.core.Layer#createSession()
	 */
	public Session createSession() {
		return new TcpCompleteSession(this);
	}

}
