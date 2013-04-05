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
package net.sf.appia.protocols.group.leave;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;


/**
 * Layer that gracefully removes the member from the Group.
 * <br>
 * When a member wishes to leave the group, it must send a LeaveEvent
 * downwards. When the protocol finishes a ExitEvent will be received.
 * <!-- Also at the end of the protocol the Channel will be automatically closed. -->
 * <br>
 * If the group is {@link net.sf.appia.protocols.group.sync.BlockOk blocked} there
 * isn't any guarantee that a leave wil succeed. If after the sending of a
 * LeaveEvent a View event is received instead of the ExitEvent then the
 * leave request should be retransmitted in the new view.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.leave.LeaveEvent
 * @see net.sf.appia.protocols.group.leave.ExitEvent
 * @see net.sf.appia.core.Channel#end
 * @see net.sf.appia.protocols.group.intra.View
 * @see net.sf.appia.protocols.group.sync.BlockOk
 */
public class LeaveLayer extends Layer {

    /**
     * Creates LeaveLayer.
     * <br>
     *
     * <b>Events Provided</b><br>
     * <ul>
     * <li>appia.protocols.group.intra.ViewChange
     * <li>appia.protocols.group.leave.ExitEvent
     * </ul>
     *
     * <b>Events Required</b><br>
     * <ul>
     * <li>appia.protocols.group.intra.View
     * <li>appia.protocols.group.intra.PreView
     * </ul>
     *
     * <b>Events Accepted</b>
     * <ul>
     * <li>appia.protocols.group.intra.View
     * <li>appia.protocols.group.intra.PreView
     * <li>appia.protocols.group.leave.LeaveEvent
     * <li>appia.protocols.group.leave.ExitEvent
     * </ul>
     */
    public LeaveLayer() {
        Class view=net.sf.appia.protocols.group.intra.View.class;
        Class change=net.sf.appia.protocols.group.intra.ViewChange.class;
        Class preview=net.sf.appia.protocols.group.intra.PreView.class;
        Class leave=net.sf.appia.protocols.group.leave.LeaveEvent.class;
        Class exit=net.sf.appia.protocols.group.leave.ExitEvent.class;

        evProvide=new Class[] {
                change,
                exit,
        };

        evRequire=new Class[] {
                view,
                preview,
        };

        evAccept=new Class[] {
                view,
                preview,
                leave,
                exit,
        };

    }

    /**
     * Creates a new LeaveSession.
     */
    public Session createSession() {
        return new LeaveSession(this);
    }
}