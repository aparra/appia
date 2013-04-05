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
 * PerfSession.java
 *
 * Created on June 24, 2002, 2:42 PM
 */

package net.sf.appia.test.perf;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.RandomAccessFile;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.message.MsgBuffer;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.udpsimple.MulticastInitEvent;
import net.sf.appia.protocols.utils.ParseUtils;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;



/**
 *
 * @author  Alexandre Pinto
 */
public class PerfSession extends Session implements InitializableSession {
  
  public static final long SECOND=1000000; 
  public static final int RING=1;
  public static final int RT=2;
  public static final int LATENCY=3;
  public static final int DEBIT=4;
  public static final int DEBIT2=5;
  public static final int STACK=10;
  public static final int VSYNCVALID=20;

  public static final int DEFAULT_TEST=RING;
  public static final int DEFAULT_GOSSIP_PORT=10000;
  public static final int DEFAULT_MULTICAST_PORT=7000;
  public static final int DEFAULT_N_MEMBERS=3;
  public static final int DEFAULT_N_ROUNDS=10;
  public static final int DEFAULT_N_MSGS=10;
  public static final int DEFAULT_MSG_SIZE=0;
  
  public static final int DEFAULT_N_FAILURES=1;
  
  public static final long DEFAULT_WARMUP_TIME=1000; // 1 sec
  public static final long DEFAULT_SHUTDOWN_TIME=2000; // 2 sec  
  
  private static int instances=0;
  public static void instances(int n) {
    instances=n;
  }
  private static void terminated(Channel channel, Session src, ViewState vs) {
    //try { Thread.sleep(2000); } catch (Exception ex) { ex.printStackTrace(); }
    
    if (--instances <= 0) {
        System.exit(0);
      
//      try {
//        LeaveEvent ev=new LeaveEvent(channel,Direction.DOWN,src,vs.group,vs.id);
//        ev.go();
//      } catch (AppiaEventException ex) {
//        ex.printStackTrace();
//        System.exit(1);
//      }
    }
  }
  
  /** Creates new PerfSession */
  public PerfSession(Layer layer) {
    super(layer);
  }
  
  public void handle(Event event) {
    // ChannelInit
    if (event instanceof ChannelInit)
      handleChannelInit((ChannelInit)event);
    // ChannelClose
    else if (event instanceof ChannelClose)
      handleChannelClose((ChannelClose)event);
    // BlockOk
    else if (event instanceof BlockOk)
      handleBlockOk((BlockOk)event);
    // View
    else if (event instanceof View)
      handleView((View)event);
    // PrefCastEvent
    else if (event instanceof PerfCastEvent)
      handlePerfCastEvent((PerfCastEvent)event);
    // PrefSendEvent
    else if (event instanceof PerfSendEvent)
      debug("PerfSendEvent received but not yet supported.");
    // RegisterSocketEvent
    else if (event instanceof RegisterSocketEvent)
      handleRSE((RegisterSocketEvent)event);
    // PerfTimer
    else if (event instanceof PerfTimer)
      handlePerfTimer((PerfTimer)event);
    else {
      debug("Unwanted event (\""+event.getClass().getName()+"\") received. Continued...");
      try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }
  }
  
  /**
   * Initializes the session using the parameters given in the XML configuration.
   * Possible parameters:
   * <ul>
   * <li><b>test</b> the type of test to run. Could be 'ring' or 'vsyncvalid'.
   * <li><b>gossip</b> an array of gossip addresses, like IP1:por1,IP2:port2, etc.
   * <li><b>multicast</b> the multicast address to use (optional).
   * <li><b>group</b> the name of the group.
   * <li><b>warmup</b> warmup time, before starting to make measurements.
   * <li><b>shutdown</b> shutdown time, after stopping the measurements.
   * <li><b>lo</b> boolean that defines if the test should count with self messages.
   * <li><b>port</b> the local port.
   * <li><b>n</b> number of members of the group.
   * <li><b>r</b> number of rounds of messages.
   * <li><b>k</b> number of messages for each round.
   * <li><b>m</b> message size.
   * <li><b>i</b> message (size) increment.
   * <li><b>fails</b> number of failures.
   * <li><b>inpayload</b> name of the file that contains the messages payload.
   * <li><b>outpayload</b> name of the file to dump the message payloads.
   * <li><b>debug</b> boolean to define if the debug mode should be turned on.
   * </ul>
   * 
   * @param params The parameters given in the XML configuration.
   */
  public void init(SessionProperties params) {
    
    if (params.containsKey("test")) {
      String s=params.getString("test");
      if (s.equals("ring"))
        test=RING;
      else if (s.equals("vsyncvalid"))
        test=VSYNCVALID;
      else {
        System.err.println("Test \""+s+"\" unknown or currently unsuported.");
        System.exit(1);
      }
    }
    if (params.containsKey("gossip")) {
      try {
        gossips=ParseUtils.parseSocketAddressArray(params.getString("gossip"),InetAddress.getLocalHost(),DEFAULT_GOSSIP_PORT);
      } catch (UnknownHostException ex) {
        System.err.println("Unknown host \""+ex.getMessage()+"\"");
        System.exit(1);
      } catch (NumberFormatException ex) {
        System.err.println("Number format error "+ex.getMessage());
        System.exit(1);
      } catch (Exception ex) {
        //ex.printStackTrace();
        System.err.println(ex.getMessage());
        System.exit(1);
      }
    }
    if (params.containsKey("multicast")) {
      try {
        multicast=ParseUtils.parseSocketAddress(params.getString("multicast"),null,DEFAULT_MULTICAST_PORT);
      } catch (UnknownHostException ex) {
        System.err.println("Unknown host \""+ex.getMessage()+"\"");
        System.exit(1);
      } catch (NumberFormatException ex) {
        System.err.println("Number format error "+ex.getMessage());
        System.exit(1);
      } catch (Exception ex) {
        //ex.printStackTrace();
        System.err.println(ex.getMessage());
        System.exit(1);
      }
      if (!multicast.getAddress().isMulticastAddress()) {
        System.err.println("Multicast address given is not IP-Multicast address.");
        System.exit(1);
      }
    }
    if (params.containsKey("addrs")) {
      try {
        addresses=ParseUtils.parseSocketAddressArray(params.getString("addrs"),null,-1);
      } catch (UnknownHostException ex) {
        System.err.println("Unknown host \""+ex.getMessage()+"\"");
        System.exit(1);
      } catch (NumberFormatException ex) {
        System.err.println("Number format error "+ex.getMessage());
        System.exit(1);
      } catch (Exception ex) {
        //ex.printStackTrace();
        System.err.println(ex.getMessage());
        System.exit(1);
      }
    }
    if (params.containsKey("group"))
      group_name=params.getString("group");
    if (params.containsKey("warmup"))
      warmup_time=params.getLong("warmup");
    if (params.containsKey("shutdown"))
      shutdown_time=params.getLong("shutdown");
    if (params.containsKey("lo"))
      receiveOwn=params.getBoolean("lo");
    if (params.containsKey("port"))
      myPort=params.getInt("port");
    if (params.containsKey("n"))
      nmembers=params.getInt("n");
    if (params.containsKey("r"))
      nrounds=params.getInt("r");
    if (params.containsKey("k"))
      nmsgs=params.getInt("k");
    if (params.containsKey("m"))
      msg_size=params.getInt("m");
    if (params.containsKey("i")) {
      msg_size_inc=params.getInt("i");
      init_msg_size=msg_size;
    }
    
    if (params.containsKey("fails"))
      nfailures=params.getInt("fails");
    if (params.containsKey("inpayload")) {
      try {
        payloadIn=new RandomAccessFile(params.getString("inpayload"),"r");
      } catch (FileNotFoundException e) {
        System.err.println("Error opening payload input file:"+e.getMessage());
        System.exit(1);
      }
    }
    if (params.containsKey("outpayload")) {
      try {
        File f=new File(params.getString("outpayload"));
        if (f.exists())
          f.delete();
        payloadOut=new RandomAccessFile(f,"rw");
      } catch (FileNotFoundException e) {
        System.err.println("Error opening payload output file:"+e.getMessage());
        System.exit(1);
      }
    }
    
    if (params.containsKey("debug"))
      debugOn=params.getBoolean("debug");
  }
  
  // Perf specific
  private int test=DEFAULT_TEST;
  private int nmembers=DEFAULT_N_MEMBERS;
  private int nrounds=DEFAULT_N_ROUNDS;
  private int msg_size=DEFAULT_MSG_SIZE;
  private long warmup_time=DEFAULT_WARMUP_TIME;
  private long shutdown_time=DEFAULT_SHUTDOWN_TIME;
  private boolean running=false;
  private boolean warmingup=false;
  private boolean shuttingdown=false;
  private boolean receiveOwn=false;
  
  /* Appia */
  private int myPort=RegisterSocketEvent.FIRST_AVAILABLE;
  private InetSocketAddress multicast=null;
  private TimeProvider clock;
  
  /* Group */
  private String group_name="Perf Group";
  private InetSocketAddress[] gossips = null;
  private InetSocketAddress[] addresses = null;
  private ViewState vs=null;
  private LocalState ls=null;
  private boolean isBlocked = true;
  
  // All Tests
  private long start_time=0;
  private long end_time=0;
  private int k,r;
  
  // RING
  private int nmsgs=DEFAULT_N_MSGS;
  private int kmsgs;
  
  // DEBIT
  private int msg_size_inc=0;
  private long[] times=null;
  private int init_msg_size=0;
  
  // DEBIT2
  private long[] start_times=null;
  private long[] end_times=null;
  
  // LATENCY
  private int peerRank;

  // VSYNCVALID
  private int nfailures=DEFAULT_N_FAILURES;
  private RandomAccessFile payloadOut;
  private RandomAccessFile payloadIn;
  
  private void handleChannelInit(ChannelInit ev) {
    try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    
    clock=ev.getChannel().getTimeProvider();
    
    if (debugOn)
      sendDebug(ev.getChannel());
    
    try {
      RegisterSocketEvent rse=new RegisterSocketEvent(ev.getChannel(),Direction.DOWN,this,myPort);
      rse.go();
    } catch (AppiaEventException ex) {
      ex.printStackTrace();
      System.exit(1);
    }
    
    if (multicast != null) {
      try {
        MulticastInitEvent amie=new MulticastInitEvent(multicast,false,ev.getChannel(),Direction.DOWN,this);
        amie.go();
      } catch (AppiaEventException ex) {
        ex.printStackTrace();
        System.exit(1);
      }
    }
  }
  
  private void handleChannelClose(ChannelClose ev) {
    debug("Received ChannelClose. Ending.");
    System.exit(0);
  }
  
  private void handleRSE(RegisterSocketEvent ev) {
    if (ev.error) {
      debug("Impossible to register socket. Aborting");
      System.exit(1);
    }
    
    try {
    	Endpt myEndpt = new Endpt("Perf@"+ev.localHost.getHostAddress()+":"+ev.port);
    	/*
    	InetSocketAddress[] addrs=new InetSocketAddress[] {new InetSocketAddress(ev.localHost,ev.port)};
    	Endpt[] view=new Endpt[] {myEndpt};

    	vs=new ViewState("1",new Group(group_name),new ViewID(0,view[0]),new ViewID[0], view, addrs);

    	GroupInit ginit=new GroupInit(vs,view[0],multicast,gossips,ev.getChannel(),Direction.DOWN, this);
    	*/
    	GroupInit ginit=new GroupInit(group_name,new InetSocketAddress(ev.localHost,ev.port),ev.getChannel(),Direction.DOWN, this);
    	ginit.setEndpt(myEndpt);
    	if (gossips != null)
    		ginit.setGossip(gossips);
    	if (multicast != null)
    		ginit.setIPmulticast(multicast);

    	if (addresses != null) {
      	Endpt[] vbase=new Endpt[addresses.length];
    		for (int i=0 ; i < vbase.length ; i++)
    			vbase[i]=new Endpt("Perf@"+addresses[i].getAddress().getHostAddress()+":"+addresses[i].getPort());
    		ViewState vsbase=new ViewState("3",new Group(group_name),new ViewID(0,vbase[0]),new ViewID[0], vbase, addresses);
    		ginit.setBaseVS(vsbase);
    	}
  		ginit.go();
    } catch (AppiaException ex) {
      ex.printStackTrace();
      System.err.println("Impossible to initiate group communication. Aborting.");
      System.exit(1);
    }
  }
  
  private void handleBlockOk(BlockOk ev) {
    isBlocked=true;
    debug("Group is blocked !!!!");
    
    try {ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
  }
  
  private void handleView(View ev) {    
    vs=ev.vs;
    ls=ev.ls;
    isBlocked=false;
    
    System.out.println("Received new view with "+vs.view.length+" members. (my_rank="+ls.my_rank+")");
    if (debugFull) {
      debug(vs.toString());
      debug(ls.toString());
    }
    
    try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    
    if (running) {
      switch (test) {
        case VSYNCVALID:
          if (vs.view.length <= nmembers-nfailures) {
            end_time=clock.currentTimeMicros();
            finnish(ev.getChannel());
          }
          if (receiveOwn)
            kmsgs=vs.view.length*nmsgs;
          else
            kmsgs=(vs.view.length-1)*nmsgs;
          break;
        default: 
          debug("Received View while in test. Aborting.");
          System.exit(1);
          break;
      }
    } else {
      if (vs.view.length == nmembers) {
        if (warmup_time > 0) {
          try {
            PerfTimer timer=new PerfTimer(warmup_time,this+" WARMUP TIMER",ev.getChannel(),Direction.UP,this,EventQualifier.ON);
            timer.go();
            warmingup=true;
            debug("Sent WarmUp timer.");
          } catch (AppiaException ex) {
            ex.printStackTrace();
            System.err.println("No WarmUp time.");
          }
        }
        
        if (!warmingup)
          start(ev.getChannel());
      }
    }
  }
  
  private void handlePerfTimer(PerfTimer ev) {
    if (warmingup) {
      debug("Warmup time elapsed.");
      start(ev.getChannel());
      return;
    }
    
    if (shuttingdown) {
      debug("Shutdown time elapsed.");
      terminated(ev.getChannel(),this,vs);
      return;
    }
  }
  
  private void handlePerfCastEvent(PerfCastEvent ev) {    
    if (warmingup) {
      debug("Received Cast while warming up. Starting test.");
      start(ev.getChannel());
    }
    
    if (!running) {
      debug("Received Cast but not running test. Ignoring it.");
      return;
    }
    
    if (ev.getMessage().length() != msg_size) {
      throw new AppiaError("Received message of incorrect size. Aborting.");
    }
    
    switch (test) {
      case RING:
        if (debugFull)
          debug("Received "+ev.group.toString()+": k="+(k+1)+"("+kmsgs+")"+" r="+r+"("+nrounds+")");
        if (++k >= kmsgs) {
          k=0;
          if (++r >= nrounds) {
            end_time=clock.currentTimeMicros();
            finnish(ev.getChannel());
          } else {
            sendNCast(ev.getChannel(),nmsgs,null);
          }
        }
        break;
        
      case DEBIT:
        if (debugFull)
          debug("Received "+ev.group.toString()+": k="+(k+1)+"("+20+")"+" r="+r+"("+nrounds+")");
        if (++k >= (nmembers-1)*100) {
          times[r]=clock.currentTimeMicros();
          System.gc();
          if (++r >= nrounds) {
            finnish(ev.getChannel());
          }
          msg_size+=msg_size_inc;
          k=0;
        }
        if (k % (nmembers-1) == 0)
          sendNCast(ev.getChannel(),1,null);          
        break;
        
      case DEBIT2:
        if (debugFull)
          debug("Received "+ev.group.toString()+": k="+(k+1)+"("+20+")"+" r="+r+"("+nrounds+")");
        if (++k >= (nmembers-1)*100) {
          end_times[r]=clock.currentTimeMicros();
          System.gc();
          if (++r >= nrounds) {
            finnish(ev.getChannel());
          }
          msg_size+=msg_size_inc;
          k=0;
          start_times[r]=clock.currentTimeMicros();
        }
        if (k % (nmembers-1) == 0)
          sendNCast(ev.getChannel(),1,null);          
        break;
        
      case LATENCY:
        if (debugFull)
          debug("Received "+ev.group.toString()+": r="+r+"("+nrounds+")");
        if (ev.orig == peerRank) {
          if (r == 0)
            start_time=clock.currentTimeMicros();
          if (r == nrounds) {
            end_time=clock.currentTimeMicros();
            if (ls.my_rank < peerRank)
              sendNCast(ev.getChannel(),1,null);
            finnish(ev.getChannel());
          } else {
            sendNCast(ev.getChannel(),1,null);
            r++;
          }
        }   
        break;
        
      case VSYNCVALID:
        if (debugFull)
          debug("Received "+ev.group.toString()+": k="+(k+1)+"("+kmsgs+")"+" r="+r+" size="+ev.getMessage().length());
        if (payloadOut != null) {
          MsgBuffer mbuf=new MsgBuffer();
          mbuf.len=ev.getMessage().length();
          ev.getMessage().pop(mbuf);
          readPayload(mbuf, payloadOut);
        }
        if (++k >= kmsgs) {
          k=0;
          if (++r >= nrounds) {
            end_time=clock.currentTimeMicros();
            finnish(ev.getChannel());
          } else {
            sendNCast(ev.getChannel(),nmsgs,payloadIn);
          }
        }
        break;
        
      default:
        debug("Unknown test. Aborting");
      System.exit(1);
    }
  }
    
  private void start(Channel channel) {
    warmingup=false;
    
    switch (test) {
      case RING:
        System.out.println("Starting RING("+test+") test");
        if (receiveOwn)
          kmsgs=nmembers*nmsgs;
        else
          kmsgs=(nmembers-1)*nmsgs;
        start_time=clock.currentTimeMicros();
        sendNCast(channel,nmsgs,null);
        k=r=0;
        running=true;
        break;
        
      case DEBIT:
        System.out.println("Starting DEBIT test");
        times=new long[nrounds];
        start_time=clock.currentTimeMicros();
        sendNCast(channel,1,null);
        k=r=0;
        running=true;
        break;
        
      case DEBIT2:
        System.out.println("Starting DEBIT2 test");
        start_times=new long[nrounds];
        end_times=new long[nrounds];
        start_times[0]=clock.currentTimeMicros();
        sendNCast(channel,1,null);
        k=r=0;
        running=true;
        break;
        
      case LATENCY:
        System.out.println("Starting LATENCY test");
        if ((ls.my_rank % 2) == 0)
          peerRank=ls.my_rank+1;
        else 
          peerRank=ls.my_rank-1;
        r=0;
        if (ls.my_rank < peerRank) {
          start_time=clock.currentTimeMicros();
          sendNCast(channel,1,null);
          r++;
        }
        running=true;
        break;
        
      case VSYNCVALID:
        System.out.println("Starting VSYNCVALID("+test+") test");
        if (receiveOwn)
          kmsgs=nmembers*nmsgs;
        else
          kmsgs=(nmembers-1)*nmsgs;
        if (nrounds <= 0)
          nrounds=Integer.MAX_VALUE;
        start_time=clock.currentTimeMicros();
        sendNCast(channel,nmsgs,payloadIn);
        k=r=0;
        running=true;
        break;
        
      default:
        debug("Unknown test. Aborting");
      System.exit(1);
    }    
  }
  
  private void finnish(Channel channel) {
    switch (test) {
      case RING:
        double time=((double)(end_time-start_time))/SECOND;
        System.out.println("RING test Results (Group="+vs.group.toString()+"):");
        System.out.println("(Parameters: members="+nmembers+" msgs="+nmsgs+" rounds="+nrounds+" msg_size="+msg_size+")");        
        System.out.println("latency: "+(time/nrounds));
        System.out.println("msgs/sec: "+(nrounds*nmsgs*nmembers)/time);
        System.out.println("msgs/mbr/sec: "+((nrounds*nmsgs*nmembers)/time)/nmembers);
        System.out.println("time: "+time);
        System.out.println("bytes/sec: "+(nrounds*nmsgs*nmembers*msg_size)/time);
        System.out.println("bytes/mbr/sec: "+(nrounds*nmsgs*msg_size)/time);        
        System.out.println();
        break;
        
      case DEBIT:
        System.out.println("DEBIT test Results (Group="+vs.group.toString()+"):");
        System.out.println("(Parameters: members="+nmembers+" rounds="+nrounds+" msg_size="+msg_size+" msg_size_inc="+msg_size_inc+")");        
        for (int i=0 ; i < times.length ; i++) {
          int msize=init_msg_size+i*msg_size_inc;
          double dtime=((double)(i == 0 ? times[i]-start_time : times[i]-times[i-1]))/SECOND;
          double bytes_per_sec=((double)msize*1000)/dtime;
          // System.out.println((i+1)+"\t"+msize+" bytes/msg\t"+round.Rounding.toString(bytes_per_sec,2)+" bytes/sec");
          System.out.println((i+1)+"\t"+msize+" bytes/msg\t"+bytes_per_sec+" bytes/sec");
        }
        System.out.println();
        break;
        
      case DEBIT2:
        System.out.println("DEBIT2 test Results (Group="+vs.group.toString()+"):");
        System.out.println("(Parameters: members="+nmembers+" rounds="+nrounds+" msg_size="+msg_size+" msg_size_inc="+msg_size_inc+")");        
        for (int i=0 ; i < start_times.length ; i++) {
          int msize=init_msg_size+i*msg_size_inc;
          double dtime=((double)(end_times[i]-start_times[i]))/SECOND;
          double bytes_per_sec=((double)msize*1000)/dtime;
          // System.out.println((i+1)+"\t"+msize+" bytes/msg\t"+round.Rounding.toString(bytes_per_sec,2)+" bytes/sec");
          System.out.println((i+1)+"\t"+msize+" bytes/msg\t"+bytes_per_sec+" bytes/sec");
        }
        System.out.println();
        break;
        
      case LATENCY:
        double ltime=((double)(end_time-start_time))/SECOND;
        System.out.println("LATENCY test Results (Group="+vs.group.toString()+"):");
        System.out.println("(Parameters: members="+nmembers+" rounds="+nrounds+" msg_size="+msg_size+")");        
        System.out.println("latency: "+(ltime/nrounds));
        System.out.println("time: "+ltime);
        System.out.println();        
        break;
        
      case VSYNCVALID:
        double vvtime=((double)(end_time-start_time))/SECOND;
        System.out.println("VSYNCVALID test Results (Group="+vs.group.toString()+"):");
        System.out.println("(Parameters: members="+nmembers+" msgs="+nmsgs+" rounds="+nrounds+" msg_size="+msg_size+")");        
        System.out.println("time: "+vvtime);
        System.out.println("messages sent: "+((r == nrounds ? nrounds : r+1)*nmsgs));
        System.out.println("messages received: "+(r*kmsgs+k));
        System.out.println("bytes received: "+((r*kmsgs+k)*msg_size));
        System.out.println();
        break;
    }
    
    if (shutdown_time > 0) {
      try {
        PerfTimer timer=new PerfTimer(shutdown_time,this+" SHUTDOWN TIMER",channel,Direction.UP,this,EventQualifier.ON);
        timer.go();
        shuttingdown=true;
      } catch (AppiaException ex) {
        ex.printStackTrace();
        System.err.println("No ShutDown time.");
      }
    }
    
    if (!shuttingdown)
      terminated(channel,this, vs);    
  }

  private void sendNCast(Channel channel, int n, RandomAccessFile payload) {
    if (isBlocked) {
      debug("Trying to send while blocked. Discarding messages.");
      return;
    }
    
    if (debugFull)
      debug("Sending "+vs.group.toString()+" "+n+" messages in round "+r+" ("+msg_size+" bytes each)");
    
    try {
      while (n > 0) {
        PerfCastEvent ev=new PerfCastEvent(channel,Direction.DOWN,this,vs.group,vs.id);
        if (msg_size > 0) {
          MsgBuffer mbuf=new MsgBuffer();
          mbuf.len=msg_size;
          ev.getMessage().push(mbuf);
          
          if (payload != null)
            writePayload(mbuf, payload);
        }
        ev.go();
        n--;
      }
    } catch (AppiaEventException ex) {
      ex.printStackTrace();
      debug("Problem while sending event. Aborting.");
      System.exit(1);
    }
  }
  
  private void writePayload(MsgBuffer mbuf, RandomAccessFile payload) {
    try {
      int i,r=0;
      for (i=0; i < mbuf.len ; i+=r) {
        r=payload.read(mbuf.data, mbuf.off+i, mbuf.len-i);
        if (r < mbuf.len-i) {
          payload.seek(0);
          if (r < 0)
            r=0;
        }
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private void readPayload(MsgBuffer mbuf, RandomAccessFile payload) {
    try {
      payloadOut.write(mbuf.data,mbuf.off,mbuf.len);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
  
  // DEBUG
  public static final boolean debugFull=false;
  
  private boolean debugOn=false;
  private PrintStream debug=System.out;
  
  private void debug(String s) {
    if ((debugOn || debugFull) && (debug != null))
      debug.println(">> appia:test:PerfSession: "+s);
  }  
  
  private void sendDebug(Channel channel) {
    try {
      net.sf.appia.core.events.channel.Debug e = new net.sf.appia.core.events.channel.Debug(debug);
      e.setChannel(channel);
      e.setDir(Direction.DOWN);
      e.setSourceSession(this);
      e.setQualifierMode(EventQualifier.ON);
      
      e.init();
      e.go();
    } catch (net.sf.appia.core.AppiaEventException ex) {
      ex.printStackTrace();
      debug("Unexpected exception when sending debug event");
    }
  }
}
