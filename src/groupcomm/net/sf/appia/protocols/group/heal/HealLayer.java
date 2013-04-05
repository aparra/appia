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
package net.sf.appia.protocols.group.heal;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.PeriodicTimer;
import net.sf.appia.protocols.group.bottom.OtherViews;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.intra.View;



public class HealLayer extends Layer {
  
  public static final long GOSSIP_TIME=10000; // 10 secs
  public static final long HELLO_MIN_TIME=2500; // 2.5 secs 
  
  public HealLayer() {
    Class view=View.class;
    Class gossip=net.sf.appia.protocols.group.heal.GossipOutEvent.class;
    Class periodic=PeriodicTimer.class;
    Class hello=HelloEvent.class;
    Class concurrent=net.sf.appia.protocols.group.heal.ConcurrentViewEvent.class;
    
    evProvide=new Class[] {
    		gossip,
    		hello,
    		concurrent
    };
    
    evRequire=new Class[] {
    		view,
    		periodic
    };
    
    evAccept=new Class[] {
    		view,
    		gossip,
    		hello,
    		concurrent,
    		OtherViews.class,
    		GroupInit.class,
    		periodic,
    };
  }
  
  public Session createSession() {
    return new HealSession(this,GOSSIP_TIME, HELLO_MIN_TIME);
  }
}