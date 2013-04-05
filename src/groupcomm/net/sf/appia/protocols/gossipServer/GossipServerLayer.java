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
package net.sf.appia.protocols.gossipServer;

import net.sf.appia.core.*;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.sync.BlockOk;



public class GossipServerLayer extends Layer {
    
  public GossipServerLayer() {
    
    Class gossip=net.sf.appia.protocols.group.heal.GossipOutEvent.class;
    Class debugev=net.sf.appia.core.events.channel.Debug.class;
    Class undelivered=net.sf.appia.protocols.common.FIFOUndeliveredEvent.class;
    Class rse=net.sf.appia.protocols.common.RegisterSocketEvent.class;
    Class init=net.sf.appia.core.events.channel.ChannelInit.class;
    Class timer=net.sf.appia.protocols.gossipServer.GossipServerTimer.class;
    Class group=GossipGroupEvent.class;
      
    evProvide=new Class[] {
        debugev,
        gossip,
        rse,
        timer,
        group,
        GroupInit.class
    };
    
    evRequire=new Class[] {};
    
    evAccept=new Class[] {
        gossip,
        undelivered,
        init,
        timer,
        group,
        View.class,
        BlockOk.class,
        rse
    };
  }
  
  public Session createSession() {
    return new GossipServerSession(this);
  }
}