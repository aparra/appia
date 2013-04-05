
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

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.total.common.UniformServiceEvent;


/**
 * @author Jose' Mocito
 *
 */
public class UniformLayer extends Layer {

	public UniformLayer(){
		super();
		evAccept = new Class[]{
				ChannelInit.class,
				ChannelClose.class,
				GroupSendableEvent.class,
				View.class,
				BlockOk.class,
				UniformTimer.class,
				UniformInfoEvent.class,
		};
		
		evRequire = new Class[]{
				ChannelInit.class,
				View.class,
				BlockOk.class,
		};
		
		evProvide = new Class[]{
				UniformServiceEvent.class,
		};
	}
	
	public Session createSession() {
		return new UniformSession(this);
	}

}
