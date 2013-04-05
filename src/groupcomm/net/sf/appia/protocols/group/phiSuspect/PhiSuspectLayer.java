/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006-2009 University of Lisbon / Technical University of Lisbon / INESC-ID
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
 
package net.sf.appia.protocols.group.phiSuspect;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.protocols.common.FIFOUndeliveredEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.suspect.Alive;
import net.sf.appia.protocols.group.suspect.Fail;
import net.sf.appia.protocols.group.suspect.Suspect;
import net.sf.appia.protocols.tcpcomplete.TcpUndeliveredEvent;


/** <I>Phi</I> failure detector.
 * 
 * Based on the work of Naohiro Hayashibara
 * See the paper at http://ddsg.jaist.ac.jp/pub/HDY+04.pdf
 * 
 * Each round lasts a specified time (<I>suspect_sweep</I>).
 * In each round, each member must send a cast message.
 * 
 * If the application doesn't send any message the layer will send an <I>Alive</I>.
 * <br>
 * 
 * 
 * @author Dan Mihai Dumitriu
 */
public class PhiSuspectLayer extends Layer {
  
  /** Creates a new layer 
   */
  public PhiSuspectLayer() {
    evProvide=new Class[] {
        Alive.class,
        Suspect.class,
        Fail.class,
        SuspectTimer.class,
        EchoEvent.class,
    };
    
    evRequire=new Class[] {
        View.class,
    };
    
    evAccept=new Class[] {
        //GroupSendableEvent.class,
    	Alive.class,
        Suspect.class,
        View.class,
        SuspectTimer.class,
        FIFOUndeliveredEvent.class,
        TcpUndeliveredEvent.class,
        ChannelInit.class,
    };
  }
  
  /** Creates a new Suspect session.
   * @return The new Suspect session.
   */
  public Session createSession() {
    return new PhiSuspectSession(this);
  }
  
}
