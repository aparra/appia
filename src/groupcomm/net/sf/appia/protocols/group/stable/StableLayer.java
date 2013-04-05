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

/**
 * Title:        Apia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Alexandre Pinto & Hugo Miranda & Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto & Hugo Miranda & Luis Rodrigues
 * @version 1.0
 */
package net.sf.appia.protocols.group.stable;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.PeriodicTimer;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.suspect.Fail;
import net.sf.appia.protocols.group.suspect.SuspectedMemberEvent;


public class StableLayer extends Layer {

    public StableLayer() {
        evProvide=new Class[] {
                net.sf.appia.protocols.group.stable.StableGossip.class,
                net.sf.appia.protocols.group.stable.Retransmit.class,
                net.sf.appia.protocols.group.stable.Retransmission.class,
                SuspectedMemberEvent.class,
        };

        evRequire=new Class[] {
                View.class,
                PeriodicTimer.class,
        };

        evAccept=new Class[] {
                net.sf.appia.protocols.group.stable.StableGossip.class,
                View.class,
                net.sf.appia.protocols.group.stable.Retransmit.class,
                net.sf.appia.protocols.group.stable.Retransmission.class,
                Fail.class,
                PeriodicTimer.class,
                GroupSendableEvent.class,
        };
    }

    public Session createSession() {
        return new StableSession(this);
    }
}