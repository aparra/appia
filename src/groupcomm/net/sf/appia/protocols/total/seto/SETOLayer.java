
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
 * Initial developer(s): Nuno Carvalho and Jose' Mocito.
 * Contributor(s): See Appia web page for a list of contributors.
 */
package net.sf.appia.protocols.total.seto;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.LeaveEvent;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.total.common.AckViewEvent;
import net.sf.appia.protocols.total.common.RegularServiceEvent;
import net.sf.appia.protocols.total.common.SETOServiceEvent;
import net.sf.appia.protocols.total.common.SeqOrderEvent;
import net.sf.appia.protocols.total.common.UniformInfoEvent;
import net.sf.appia.protocols.total.common.UniformServiceEvent;
import net.sf.appia.protocols.total.common.UniformTimer;


/**
 * @author nunomrc
 *
 */
public class SETOLayer extends Layer {

	public SETOLayer(){
		super();
		evAccept = new Class[]{
				ChannelInit.class,
				ChannelClose.class,
				GroupSendableEvent.class,
				View.class,
				BlockOk.class,
                AckViewEvent.class,
				SeqOrderEvent.class,
				SETOTimer.class,
				UniformTimer.class,
				UniformInfoEvent.class,
                LeaveEvent.class,
		};
		
		evRequire = new Class[]{};
		
		evProvide = new Class[]{
				UniformServiceEvent.class,
				RegularServiceEvent.class,
				SETOServiceEvent.class,
		};
	}
	
	/* (non-Javadoc)
	 * @see appia.Layer#createSession()
	 */
	public Session createSession() {
		return new SETOSession(this);
	}

}
