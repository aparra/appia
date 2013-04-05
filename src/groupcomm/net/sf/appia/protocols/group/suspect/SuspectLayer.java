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
 * Initial developer(s): Alexandre Pinto and Hugo Miranda and Nuno Carvalho.
 * Contributor(s): See Appia web page for a list of contributors.
 */
 
/**
 * Title:        Appia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) DIALNP - LaSIGE
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto
 * @version 1.6
 */
package net.sf.appia.protocols.group.suspect;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.protocols.common.FIFOUndeliveredEvent;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.tcpcomplete.TcpUndeliveredEvent;



/** <I>Appia</I> failure detector.
 * It works by rounds.
 * Each round lasts a specified time (<I>suspect_sweep</I>).
 * In each round, each member must send a cast message.
 * If the application doesn't send any message the layer will send an <I>Alive</I>.
 * <br>
 * If no message is received from a particular member in the last <I>n</I> rounds
 * the member is suspected.
 * @author Alexandre Pinto
 */
public class SuspectLayer extends Layer {
  
  /** Creates a new layer 
   */
  public SuspectLayer() {
    evProvide=new Class[] {
        net.sf.appia.protocols.group.suspect.Alive.class,
        Suspect.class,
        Fail.class,
        SuspectTimer.class,
        EchoEvent.class,
    };
    
    evRequire=new Class[] {
        View.class,
    };
    
    evAccept=new Class[] {
        GroupSendableEvent.class,
        Suspect.class,
        View.class,
        SuspectTimer.class,
        FIFOUndeliveredEvent.class,
        TcpUndeliveredEvent.class,
        ChannelInit.class,
        SuspectedMemberEvent.class,
    };
  }
  
  /** Creates a new Suspect session.
   * @return The new Suspect session.
   */
  public Session createSession() {
    return new SuspectSession(this);
  }
}
