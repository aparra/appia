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
 * Initial developer(s): Nuno Carvalho.
 * Contributor(s): See Appia web page for a list of contributors.
 */

package net.sf.appia.jgcs.protocols.top;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.jgcs.MessageSender;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.common.ServiceEvent;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.ExitEvent;
import net.sf.appia.protocols.group.leave.LeaveEvent;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.udpsimple.MulticastInitEvent;


/**
 * This class defines a TOPLayer. This protocol makes the bridge between the 
 * Appia channels and the Data and Control Sessions.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class TOPLayer extends Layer {

	public TOPLayer() {
		super();
		
		evProvide=new Class[]{
				RegisterSocketEvent.class,
				GroupInit.class,
				JGCSGroupEvent.class,
				JGCSSendEvent.class,
				LeaveEvent.class,
				MulticastInitEvent.class,
				JGCSLeaveTimer.class,
		};
	
		evRequire=new Class[]{};
		
		evAccept=new Class[]{
				ChannelInit.class,
				ChannelClose.class,
				JGCSGroupEvent.class,
				JGCSSendEvent.class,
				JGCSSendableEvent.class,
				MessageSender.class,
				RegisterSocketEvent.class,
				View.class,
				BlockOk.class,					
				ExitEvent.class,
				MulticastInitEvent.class,
				JGCSJoinEvent.class,
				JGCSLeaveEvent.class,
				JGCSReleaseBlock.class,
				ServiceEvent.class,
				JGCSLeaveTimer.class,
		};

	}

	@Override
	public Session createSession() {
		return new TOPSession(this);
	}

}
