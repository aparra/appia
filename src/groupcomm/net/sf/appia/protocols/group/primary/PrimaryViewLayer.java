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
package net.sf.appia.protocols.group.primary;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.LeaveEvent;
import net.sf.appia.protocols.group.sync.BlockOk;


/**
 * This class defines a PrimaryViewLayer.
 * 
 * @author Jose Mocito
 * @version 1.0
 */
public class PrimaryViewLayer extends Layer {

    public PrimaryViewLayer() {
       super();
       
       evAccept = new Class[]{
                View.class,
                BlockOk.class,
                ProbeEvent.class,
                DeliverViewEvent.class,
                KickEvent.class,
                EchoEvent.class,
                GroupSendableEvent.class,
                EchoProbeEvent.class,
                LeaveEvent.class,
        };
        
        evRequire = new Class[]{
                View.class,
                BlockOk.class,
        };
        
        evProvide = new Class[]{
                ProbeEvent.class,
                DeliverViewEvent.class,
                KickEvent.class,
                EchoProbeEvent.class,
                LeaveEvent.class,
        };
    }

    /**
     * Creates a new primary view session.
     * 
     * @see net.sf.appia.core.Layer#createSession()
     */
    public Session createSession() {
       return new PrimaryViewSession(this);
    }

}
