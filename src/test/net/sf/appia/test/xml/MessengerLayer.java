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
 /*
 * Created on 16/Mar/2004
 */
package net.sf.appia.test.xml;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;



/**
 * @author Nuno Almeida
 *
 */
public class MessengerLayer extends Layer {
	
	public MessengerLayer() {
		evRequire = new Class[2];
		evRequire[0] = net.sf.appia.core.events.channel.ChannelInit.class;
		evRequire[1] = net.sf.appia.protocols.group.intra.View.class;
		
		evAccept = new Class[12];
		evAccept[0] = net.sf.appia.core.events.channel.ChannelInit.class;
		evAccept[1] = net.sf.appia.core.events.channel.ChannelClose.class;
		evAccept[2] = net.sf.appia.protocols.group.intra.View.class;
		evAccept[3] = net.sf.appia.protocols.group.sync.BlockOk.class;
		evAccept[4] = net.sf.appia.protocols.group.leave.ExitEvent.class;
		evAccept[5] = TextEvent.class;
		evAccept[6] = DrawEvent.class;
		evAccept[7] = MouseButtonEvent.class;
		evAccept[8] = ImageEvent.class;
		evAccept[9] = ClearWhiteBoardEvent.class;
		evAccept[10] = net.sf.appia.protocols.common.RegisterSocketEvent.class;
		evAccept[11] = TimerEvent.class;

		evProvide = new Class[4];
		evProvide[0] = net.sf.appia.protocols.common.RegisterSocketEvent.class;
		evProvide[1] = net.sf.appia.protocols.group.events.GroupInit.class;
		evProvide[2] = net.sf.appia.protocols.group.leave.LeaveEvent.class;
		evProvide[3] = TimerEvent.class;
	}	
	    
	public Session createSession() {
		return new MessengerSession(this);
	}
}
