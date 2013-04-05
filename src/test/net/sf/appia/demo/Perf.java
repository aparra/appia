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
 package net.sf.appia.demo;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.concurrent.ThreadFactory;

import net.sf.appia.core.*;
import net.sf.appia.protocols.common.AppiaThreadFactory;
import net.sf.appia.test.perf.PerfLayer;
import net.sf.appia.test.perf.PerfSession;
import net.sf.appia.xml.utils.SessionProperties;



/**
 * Test application for the group communication protocols.
 * @see net.sf.appia.protocols.group
 * @author Hugo Miranda and Alexandre Pinto
 */
public class Perf {
    
    private Perf() {}
  
  private static Layer[] qos={
      //new appia.protocols.udpsimple.UdpSimpleLayer(),
      //new appia.protocols.fifo.fifomulticast.FifoMulticastLayer(),
      //new appia.protocols.fifo.fifodual.FifoDualLayer(),
      //new appia.protocols.fifo.FifoLayer(),
      //new NakFifoLayer(),
      //new appia.protocols.frag.FragLayer(),
      new net.sf.appia.protocols.tcpcomplete.TcpCompleteLayer(),
      new net.sf.appia.protocols.group.bottom.GroupBottomLayer(),
      new net.sf.appia.protocols.group.heal.GossipOutLayer(),
      new net.sf.appia.protocols.group.suspect.SuspectLayer(),
      new net.sf.appia.protocols.group.intra.IntraLayer(),
      new net.sf.appia.protocols.group.inter.InterLayer(),
      new net.sf.appia.protocols.group.heal.HealLayer(),
      new net.sf.appia.protocols.group.stable.StableLayer(),
      new net.sf.appia.protocols.group.leave.LeaveLayer(),
      new net.sf.appia.protocols.group.sync.VSyncLayer(),
      new PerfLayer(),
  };

  // Parameters processed by PerfSession
  private static final String SESSION_PARAMS="-test#1-n#1-r#1-k#1-m#1-i#1-gossip#1-multicast#1" +
        "-warmup#1-shutdown#1-lo#0-inpayload#1-outpayload#1-fails#1-port#1-addrs#1";
  
  private static boolean debug=false;
  private static int instances=1; 
  private static int groups=1;
  // FIXME: This is never used?!?
  private static boolean lwg=false;
  
  public static void main(String args[]) {
    int i;
    LineNumberReader file=null;
    final SessionProperties params=new SessionProperties();
    
    for (i=0 ; i < args.length ; i++) {
      
      // SESSION PARAMS #1
      if (SESSION_PARAMS.indexOf(args[i]+"#1") >= 0) {
        final String key=args[i].substring(1);
        if (++i >= args.length)
          argInvalid("missing "+key+" value");
        params.put(key,args[i]);
        
      // SESSION PARAMS #0
      } else if (SESSION_PARAMS.indexOf(args[i]+"#0-") >= 0) {
          final String key=args[i].substring(1);
          params.put(key,"true");
        
      // DEBUG 
      } else if (args[i].equals("-debug")) {
        params.put("debug","true");
        debug=true;
        
      // QOS
      } else if (args[i].equals("-qos")) {
        if (++i >= args.length)
          argInvalid("missing qos file name");
        try {
          file=new LineNumberReader(new FileReader(args[i]));
          String s=null;
          int j=0;
          while ((s=file.readLine()) != null) {
            s=s.trim();
            if (s.length() > 0) {
              if (j == qos.length-1) {
                final Layer[] aux=new Layer[qos.length*2];
                System.arraycopy(qos,0,aux,0,j);
                qos=aux;
              }
              qos[j++]=(Layer)Class.forName(s).newInstance();
            }
          }
          if (j < qos.length-1) {
            final Layer[] aux=new Layer[j+1];
            System.arraycopy(qos,0,aux,0,j);
            qos=aux;
          }
          qos[j]=new PerfLayer();
        } catch (FileNotFoundException ex) {
          argInvalid("qos file not found: \""+args[i]+"\"");
        } catch (IOException ex) {
          ex.printStackTrace();
          argInvalid("impossible to read qos file: \""+args[i]+"\"");
        } catch (ClassNotFoundException ex) {
          argInvalid("class not found: "+ex.getMessage());
        } catch (InstantiationException ex) {
          argInvalid("impossible to create layer: "+ex.getMessage());
        } catch (IllegalAccessException ex) {
          argInvalid("impossible to create layer: "+ex.getMessage());
        }
        
        // G
      } else if (args[i].equals("-g")) {
        if (++i >= args.length)
          argInvalid("missing \""+args[i-1]+"\" value");
        try {
          groups=Integer.parseInt(args[i]);
        } catch (NumberFormatException ex) {
          argInvalid("illegal value: "+args[i]);
        }
        
        // INSTANCES
      } else if (args[i].equals("-instances")) {
        if (++i >= args.length)
          argInvalid("missing \""+args[i-1]+"\" value");
        try {
          instances=Integer.parseInt(args[i]);
        } catch (NumberFormatException ex) {
          argInvalid("illegal value: "+args[i]);
        }

        // LWG
      } else if (args[i].equals("-lwg")) {
        lwg=true;
        
        // HELP
      } else if (args[i].equals("-help")) {
        printUsage();
        System.exit(0);
        
        // DEFAULT
      } else {
        argInvalid("Invalid parameters: "+args[i]);
      }
    }
    
    System.out.println("Perf: QoS");
    for (int k=0 ; k < qos.length ; k++) {
      System.out.println(" "+k+": "+qos[k]);
    }
    
    System.out.println("instances="+instances+" groups="+groups);
    PerfSession.instances(instances*groups);
    
    /* Create a QoS */
    QoS myQoS=null;
    try {
      myQoS=new QoS("Perf QoS",qos);
    } catch(AppiaInvalidQoSException ex) {
      System.err.println("Invalid QoS");
      System.err.println(ex.getMessage());
      System.exit(1);
    }
    
    for (; instances > 0; instances--) {
      final Appia appiaInstance=new Appia();
      ThreadFactory threadFactory = null;
      
      // TODO: test schedulers
      final EventScheduler es=new EventScheduler(appiaInstance);
      
      if (debug)
        System.out.println("New Instance \""+appiaInstance+"\" with Scheduler \""+es+"\"");
      
      //Session lwg_session=null;
      for (int g=groups ; g > 0 ; g--) {
        final Channel myChannel=myQoS.createUnboundChannel("Perf Channel "+g,es);
        threadFactory = myChannel.getThreadFactory();
        final PerfSession ps=(PerfSession)qos[qos.length-1].createSession();
        params.put("group","Perf Group "+g);
        ps.init(params);
        
        final ChannelCursor cc=myChannel.getCursor();
        try {
          cc.top();
          cc.setSession(ps);
        } catch(AppiaCursorException ex) {
          System.err.println("Unexpected exception in main. Type code:"+
              ex.type);
          System.exit(1);
        }
        
//      if (lwg) {
//      try {
//      cc.down();
//      while (cc.isPositioned()) {
//      if (cc.getLayer() instanceof appia.protocols.lwg.LwgLayer) {
//      if (lwg_session == null)
//      lwg_session=cc.getLayer().createSession();
//      cc.setSession(lwg_session);
//      }
//      cc.down();
//      }
//      } catch(AppiaCursorException ex) {
//      System.err.println("Unexpected exception in main. Type code:"+
//      ex.type);
//      System.exit(1);
//      }
//      }
        
        /* Remaining ones are created by default. Just tell the channel to start */
        try {
          myChannel.start();
        } catch(AppiaDuplicatedSessionsException ex) {
          System.err.println("Sessions binding strangely resulted in "+
              "one single sessions occurring more than "+
          "once in a channel");
          System.exit(1);
        }
      }
      
      if (instances > 1) {
    	    final Thread t = threadFactory.newThread(new Runnable(){
    		  public void run() {
    			  System.out.println("Instance "+appiaInstance+" running on thread "+this);
    			  appiaInstance.instanceRun();
    		  }    		  
    	    });
            t.setName("Perf");
            t.start();
      } else {
        System.out.println("Instance "+appiaInstance+" running on thread "+Thread.currentThread());
        appiaInstance.instanceRun();
      }
    }
  }
  
  private static void argInvalid(String s) {
    System.err.println("Perf: "+s);
    printUsage();
    System.exit(1);
  }
  
  private static void printUsage() {
    System.err.println("Usage: \n\t java Perf [options]");
    System.err.println("Options:"+
    "\n    -test <Test>                       Test to run: ring, vsyncvalid"+//", rt, stack"+
    "\n    -gossip <IP:Port>,<IP:Port>,...    Gossip Servers addresses"+
    "\n    -multicast <Multicast_IP:Port>     Multicast Address used for communication"+
    "\n    -qos <file>                        File with QoS to use"+
    "\n    -debug                             Debug is active"+
    "\n    -n <value>                         Number of members in group"+
    "\n    -k <value>                         Number of messages sent per round"+
    "\n    -r <value>                         Number of rounds"+
    "\n    -m <value>                         Message size"+
    "\n    -g <value>                         Number of groups running test simultaneously"+
    "\n    -lo                                Also receives sent messages"+
    "\n    -warmup <value>                    Warm Up time in milliseconds"+
    "\n    -shutdown <value>                  Time between test termination and program exit"+
    "\n"+
    "\n    Only for test \"vsyncvalid\":"+
    "\n    -inpayload <file>                  File to use for content of the sent messages"+
    "\n    -outpayload <file>                 File to store content of received messages"
    );
  }
}
