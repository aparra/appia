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
package net.sf.appia.protocols.group.intra;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.suspect.Fail;



public class IntraLayer extends Layer {

    public IntraLayer() {
        Class view=net.sf.appia.protocols.group.intra.View.class;
        Class install=net.sf.appia.protocols.group.intra.InstallView.class;
        Class init=GroupInit.class;
        Class preview=net.sf.appia.protocols.group.intra.PreView.class;
        Class newview=net.sf.appia.protocols.group.intra.NewView.class;

        evProvide=new Class[] {
                view,
                install,
                EchoEvent.class,
                preview,
                newview,
        };

        evRequire=new Class[] {
                init,
        };

        evAccept=new Class[] {
                install,
                Fail.class,
                init,
                view,
                net.sf.appia.protocols.group.intra.ViewChange.class,
                preview,
                newview,
        };
    }

    public Session createSession() {
        return new IntraSession(this);
    }
}