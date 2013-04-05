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
 * Created on 24/04/2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package net.sf.appia.test.xml;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

/**
 * This layer has a pedagogical interest only.
 * 
 * @author Liliana Rosa
 *
 */
public class IntegrityLayer extends Layer {
	
	public IntegrityLayer() {
		super();
		
        evProvide=new Class[0];
        evRequire=new Class[1];
        evAccept=new Class[11];
       
        evRequire[0]= net.sf.appia.protocols.group.events.GroupSendableEvent.class;
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
     
	}
	
	public Session createSession() {
		return new IntegritySession(this);
	}

}
