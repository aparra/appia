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
 * Title:        Appia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Alexandre Pinto & Hugo Miranda & Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto & Hugo Miranda & Luis Rodrigues
 * @version 1.0
 */

package net.sf.appia.gossip;
import net.sf.appia.core.*;
import net.sf.appia.protocols.fifo.FifoLayer;
import net.sf.appia.protocols.gossipServer.GossipServerLayer;
import net.sf.appia.protocols.gossipServer.GossipServerSession;
import net.sf.appia.protocols.group.bottom.GroupBottomLayer;
import net.sf.appia.protocols.group.heal.GossipOutLayer;
import net.sf.appia.protocols.group.heal.GossipOutSession;
import net.sf.appia.protocols.group.heal.HealLayer;
import net.sf.appia.protocols.group.inter.InterLayer;
import net.sf.appia.protocols.group.intra.IntraLayer;
import net.sf.appia.protocols.group.leave.LeaveLayer;
import net.sf.appia.protocols.group.stable.StableLayer;
import net.sf.appia.protocols.group.suspect.SuspectLayer;
import net.sf.appia.protocols.group.sync.VSyncLayer;
import net.sf.appia.protocols.udpsimple.UdpSimpleLayer;
import net.sf.appia.xml.utils.SessionProperties;



/**
 * This class implements a gossip server for the group communication 
 * protocols.
 */ 
public class GossipServer {

    private GossipServer() {}
    
  public static final String DEFAULT_UDP_LAYER=GossipOutSession.DEFAULT_UDP_LAYER;
  
  private static String udpLayer=DEFAULT_UDP_LAYER;
  private static boolean solo=false;
  
  public static void main(String[] args) {

    final SessionProperties params=new SessionProperties();

    if (!parse(args,0,params))
       System.exit(1);

    final GossipServerLayer glayer=new GossipServerLayer();
    final GossipServerSession gsession=(GossipServerSession) glayer.createSession();

    gsession.init(params);
    
    try {
      final Layer[] l={
          (Layer)Class.forName(udpLayer).newInstance(),
          new FifoLayer(),
          glayer,
      };

      final QoS qos=new QoS("Gossip Client QoS",l);
      final Channel channel=qos.createUnboundChannel("Gossip Channel");

      final ChannelCursor cc=channel.getCursor();
      cc.top();
      cc.setSession(gsession);
      
      channel.start();
    } catch (AppiaException ex) {
      ex.printStackTrace();
      System.err.println("Impossible to create and/or start client channel");
      System.exit(1);
    } catch (InstantiationException e) {
      e.printStackTrace();
      System.err.println("Impossible to create and/or start client channel");
      System.exit(1);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      System.err.println("Impossible to create and/or start client channel");
      System.exit(1);
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
      System.err.println("Impossible to create and/or start client channel");
      System.exit(1);
    }

    if (solo) {
      Appia.run();
      return;
    }
    
    try {
      final Layer[] l={
          new UdpSimpleLayer(), 
          new FifoLayer(),
          new GroupBottomLayer(),
          new GossipOutLayer(),
          new SuspectLayer(),
          new IntraLayer(),
          new InterLayer(),
          new HealLayer(),
          new StableLayer(),
          new LeaveLayer(),
          new VSyncLayer(),
          glayer,
      };

      final QoS qos=new QoS("Gossip Group QoS",l);
      final Channel channel=qos.createUnboundChannel("Gossip Group Channel");

      final ChannelCursor cc=channel.getCursor();
      cc.top();
      cc.setSession(gsession);
      
      channel.start();
    } catch (AppiaException ex) {
      ex.printStackTrace();
      System.err.println("Impossible to create and/or start group channel");
      System.exit(1);
    }

    Appia.run();
  }

  // Must end with a '-'
  private static final String SESSION_PARAMS="-port-gossips-remove_time-timer-";
    
  private static boolean parse(String[] args, int i, SessionProperties params) {

    if (i >= args.length)
      return true;

    if (SESSION_PARAMS.indexOf(args[i]+"-") >= 0) {
      if (i+1 >= args.length) {
        System.err.println("Missing port value");
        printHelp();
        return false;
      }
      
      params.put(args[i].substring(1), args[i+1]);
      return parse(args,i+2,params);
    }

    if (args[i].equals("-debug")) {
      params.put("debug","true");
      return parse(args,i+1,params);
    }
    
    if (args[i].equals("-solo")) {
      solo=true;
      return parse(args, i+1, params);
    }

    if (args[i].equals("-udp")) {
      if (i+1 >= args.length) {
        System.err.println("Missing UDP layer name.");
        printHelp();
        return false;
      }
 
      udpLayer=args[i+1];
      return parse(args,i+2,params);
    }
    
    if (args[i].equals("-help")) {
      printHelp();
      return true;
    }

    System.err.println("Unknown argument: "+args[i]);
    printHelp();
    return false;
  }

  private static void printHelp() {
    System.out.println("java GossipServer [-port <port>] [-udp <udp_layer_name>] [-debug] [-help] [-solo] [-gossips <ip>[:<port>][,...]]");
  }
}
