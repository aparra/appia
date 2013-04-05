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
package net.sf.appia.protocols.total.sequencer;

import net.sf.appia.core.Channel;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;


/**
 * Defines the events that the layer accepts, provides and requires
 */
public class TotalSequencerLayer extends Layer {
	
	/** Constructor*/
	public TotalSequencerLayer() {
		
		evRequire=new Class[3];
		evRequire[0]=net.sf.appia.protocols.group.events.GroupSendableEvent.class;
		evRequire[1]=net.sf.appia.protocols.group.intra.View.class;
		evRequire[2]=net.sf.appia.protocols.total.sequencer.TotalOrderEvent.class;
		
		evAccept=new Class[5];
		evAccept[0]=evRequire[0];
		evAccept[1]=net.sf.appia.core.events.channel.ChannelInit.class;
		evAccept[2]=evRequire[1];
		evAccept[3]=evRequire[2];
		evAccept[4]=net.sf.appia.protocols.group.sync.BlockOk.class;
		evProvide=new Class[1];
		evProvide[0]= evRequire[2];
	}
	
	/** Creates a new session of this layer*/
	public Session createSession() {
		return new TotalSequencerSession(this);
	}
	
	public void channelDispose(Session session,Channel channel) {
	}
}




