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
package net.sf.appia.protocols.group.bottom;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.group.events.GroupEvent;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;


/**
 * The <i>group communication</i> bottom layer.
 * <br>
 * As the name implies, this should be the lowest of the group coomunication
 * layers in the protocols stack.
 *
 * @version 0.1
 * @author Alexandre Pinto
 */
public class GroupBottomLayer extends Layer {

    /**
     * Creates GroupBottomLayer.
     * <br>
     *
     * <b>Events Provided</b><br>
     * <i>none</i>
     *
     * <b>Events Required</b><br>
     * <ul>
     * <li>appia.protocols.group.intra.View
     * </ul>
     *
     * <b>Events Accepted</b>
     * <ul>
     * <li>appia.protocols.group.events.GroupSendableEvent
     * <li>appia.protocols.group.intra.View
     * <li>appia.protocols.group.events.GroupInit
     * <li>appia.protocols.group.events.GroupEvent
     * </ul>
     */
    public GroupBottomLayer() {
        Class view=View.class;
        Class other=net.sf.appia.protocols.group.bottom.OtherViews.class;

        evProvide=new Class[] {
                other
        };

        evRequire=new Class[] {
                view
        };

        evAccept=new Class[] {
                GroupSendableEvent.class,
                view,
                GroupInit.class,
                GroupEvent.class,
        };
    }

    /**
     * Creates a new GroupBottomSession.
     */
    public Session createSession() {
        return new GroupBottomSession(this);
    }
}