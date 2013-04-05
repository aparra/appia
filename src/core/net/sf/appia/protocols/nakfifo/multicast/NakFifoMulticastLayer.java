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
 * Fifo2Layer.java
 *
 * Created on 10 de Julho de 2003, 15:43
 */

package net.sf.appia.protocols.nakfifo.multicast;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.common.SendableNotDeliveredEvent;
import net.sf.appia.protocols.frag.MaxPDUSizeEvent;



/** Layer of a protocol that provides reliable point-to-point communication.
 * This protocol operates better when using network multicast support.
 * <b>It only operates if destination is a <i>AppiaMulticast</i></b>.
 * @author Alexandre Pinto
 * @see net.sf.appia.core.events.AppiaMulticast
 */
public class NakFifoMulticastLayer extends Layer {
  
  /** Creates a new instance of nakfifoLayer */
  public NakFifoMulticastLayer() {
    evProvide=new Class[] {
    		net.sf.appia.protocols.nakfifo.NackEvent.class,
    		net.sf.appia.protocols.nakfifo.NakFifoTimer.class,
    		net.sf.appia.protocols.nakfifo.IgnoreEvent.class,
    		net.sf.appia.protocols.nakfifo.PingEvent.class,
    		net.sf.appia.protocols.common.FIFOUndeliveredEvent.class,
    		UpdateEvent.class,
    		ConfirmEvent.class,
    };
    
    evRequire=new Class[0];
    
    evAccept=new Class[] {
    		net.sf.appia.protocols.nakfifo.NackEvent.class,
    		net.sf.appia.protocols.nakfifo.NakFifoTimer.class,
    		net.sf.appia.protocols.nakfifo.IgnoreEvent.class,
    		net.sf.appia.protocols.nakfifo.PingEvent.class,
    		UpdateEvent.class,
    		ConfirmEvent.class,
            SendableNotDeliveredEvent.class,
    		net.sf.appia.core.events.SendableEvent.class,
    		net.sf.appia.core.events.channel.ChannelInit.class,
    		net.sf.appia.core.events.channel.ChannelClose.class,
            MaxPDUSizeEvent.class,
    };
  }
  
  /** Creates a new instance of nakfifoLayer */  
  public Session createSession() {
    return new NakFifoMulticastSession(this);
  }
}
