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
 package net.sf.appia.test.appl;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.SocketAddress;

import java.util.StringTokenizer;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.events.channel.Debug;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.common.AppiaThreadFactory;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.group.*;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.ExitEvent;
import net.sf.appia.protocols.group.leave.LeaveEvent;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.sslcomplete.SslRegisterSocketEvent;
import net.sf.appia.protocols.udpsimple.MulticastInitEvent;
import net.sf.appia.protocols.utils.HostUtils;


// TESTING
//import test.PrecisionTime;
//import test.TestOptimized;

/*
 * Change Log:
 * Nuno Carvalho: 7/Aug/2002 - removed deprecated code;
 * Alexandre Pinto: 15/Oct/2002 - Added configuration options.
 */

/**
 * Class ApplicationSession provides the dynamic behaviour for the
 * simple user interface with Appia. This interface accepts a predefined
 * set of commands, forwarding them to Appia. It is mainly used for testing
 * @author Hugo Miranda (altered by Alexandre Pinto and Nuno Carvalho)
 * @see    Session
 */

public class ApplSession extends Session {
    
    private final int INVALIDCOMMAND = -1;
    private final int CASTMESSAGE = 1;
    private final int SENDMESSAGE = 2;
    private final int DEBUGMESSAGE = 3;
    private final int HELP = 4;
    private final int LEAVE = 5;
    
    /* User IO */
    private PrintStream out = System.out;
    
    /* Appia */
    public Channel channel = null;
    private SocketAddress mySocketAddress = null;
    private int myPort = -1;
    private SocketAddress multicast=null;
    private SocketAddress[] initAddrs=null;
    private ApplReader reader;

    
    /* Group */
    private Group myGroup=new Group("Appl Group");
    private Endpt myEndpt=null;
    private SocketAddress[] gossips = null;
    private ViewState vs=null;
    private LocalState ls=null;
    private boolean isBlocked = true;
    
    /* SSL */
    private boolean ssl=false;
    private String keystoreFile=null;
    private String keystorePass=null;
    
    /**
     * Mainly used for corresponding layer initialization
     */
    
    public ApplSession(ApplLayer l) {
        super(l);
    }
    
    public void init(int port, SocketAddress multicast, SocketAddress[] gossips, Group group, SocketAddress[] viewAddrs) {
      this.myPort=port;
      this.multicast=multicast;
      this.gossips=gossips;
      this.initAddrs=viewAddrs;
      if (group != null)
        myGroup=group;
    }
    
    public void initWithSSL(int port, SocketAddress multicast, SocketAddress[] gossips, Group group, SocketAddress[] viewAddrs, String keystoreFile, String keystorePass) {
      init(port,multicast,gossips,group,viewAddrs);
      this.ssl=true;
      this.keystoreFile=keystoreFile;
      this.keystorePass=keystorePass;
    }

      
    private int parseCommand(String command) {
        if (command.equals("cast"))
            return CASTMESSAGE;
        if (command.equals("debug"))
            return DEBUGMESSAGE;
        if (command.equals("help"))
            return HELP;
        if (command.equals("send"))
            return SENDMESSAGE;
        if (command.equals("leave"))
            return LEAVE;
        return INVALIDCOMMAND;
    }
    
    private void printHelp() {
        out.println("help\tPrints this message");
        out.println(
        "cast r i m\t Sends message m r times "
        + "with i tens of a second between them");
        out.println(
        "send d r i m\t Sends message m r times "
        + "with i tens of a second between them, to member with rank d");
        out.println("debug {start|stop|now} [outputFile]");
    }
    
    private void sendDebug(StringTokenizer args) {
        if (args.countTokens() != 1 && args.countTokens() != 2) {
            out.println("Invalid arguments number. Usage:");
            out.println(
            "debug one of this options: start, stop, now and an optional file name");
            return;
        }
        
        try {
            int eq;
            
            String action = args.nextToken();
            
            if (action.equals("start"))
                eq=EventQualifier.ON;
            else if (action.equals("stop"))
                eq=EventQualifier.OFF;
            else if (action.equals("now"))
                eq=EventQualifier.NOTIFY;
            else {
                out.println("Invalid action. Use one of start, stop and now");
                return;
            }
            
            OutputStream debugOut = out;
            
            if (args.hasMoreTokens()) {
                try {
                    debugOut = new FileOutputStream(args.nextToken(), false);
                } catch (FileNotFoundException ex) {
                    out.println("File could not be oppened for output");
                    return;
                }
            }
            
            Debug e =
            new Debug(debugOut);
            e.setChannel(channel);
            e.setDir(Direction.DOWN);
            e.setSourceSession(this);
            e.setQualifierMode(eq);
            
            e.init();
            e.go();
        } catch (AppiaEventException ex) {
            out.println("Unexpected exception when sending debug event");
        }
    }
    
    private void castMessage(StringTokenizer args) {
        if (args.countTokens() < 3) {
            out.println("Wrong number of arguments for \"cast\"");
            return;
        }
        
        int period = 0, resends = 0;
        
        try {
            resends = Integer.parseInt(args.nextToken());
            period = Integer.parseInt(args.nextToken()) * 100;
        } catch (NumberFormatException ex) {
            out.println(
            "The repetitions number and the time  between messages must be "
            + "an integer. Write \"help\" to get more information");
            return;
        }
        
        String mensagem = new String();
        
        while (args.hasMoreElements()) {
            mensagem += args.nextToken() + " ";
        }
        
        /////////////////////////////////////////
        // TESTING
        /////////////////////////////////////////
//        test.TestOptimized.times=resends;
//        System.out.println(">>>>> Vou testar "+test.TestOptimized.times+" vezes");
        /////////////////////////////////////////
        
        try {
            ApplTimer msgTimer =
            new ApplTimer(channel, this, mensagem, resends, period, null);
            msgTimer.go();
        } catch (AppiaEventException ex) {
            out.println("There was a problem with message sending");
        } catch (AppiaException ex) {
            out.println("The time between messages must be >= 0");
        }
    }
    
    private void sendMessage(StringTokenizer args) {
        if (args.countTokens() < 4) {
            out.println("Wrong number of args to cast");
            return;
        }
        
        int period = 0, resends = 0;
        int[] dest = new int[1];
        
        try {
            dest[0] = Integer.parseInt(args.nextToken());
            resends = Integer.parseInt(args.nextToken());
            period = Integer.parseInt(args.nextToken()) * 100;
        } catch (NumberFormatException ex) {
            out.println(
            "The repetitions number and the time  between messages must be "
            + "an integer. Write \"help\" to get more information");
            return;
        }
        
        String mensagem = new String();
        
        while (args.hasMoreElements()) {
            mensagem += args.nextToken() + " ";
        }
        
        try {
            ApplTimer msgTimer =
            new ApplTimer(channel, this, mensagem, resends, period, dest);
            msgTimer.go();
        } catch (AppiaEventException ex) {
            out.println("There was a problem with message sending");
        } catch (AppiaException ex) {
            out.println("The time between messages must be >= 0");
        }
    }
    
    
    /**
     * Main Event handler function. Accepts all incoming events and
     * dispatches them to the appropriate functions
     * @param e The incoming event
     * @see Session
     */
    public void handle(Event e) {
        if (e instanceof ChannelInit)
            handleChannelInit((ChannelInit) e);
        else if (e instanceof ChannelClose)
            handleChannelClose((ChannelClose) e);
        else if (e instanceof ApplCastEvent)
            receiveData((GroupSendableEvent) e);
        else if (e instanceof ApplSendEvent)
            receiveData((GroupSendableEvent) e);
        else if (e instanceof View)
            handleNewView((View) e);
        else if (e instanceof BlockOk)
            handleBlock((BlockOk) e);
        else if (e instanceof ApplTimer)
            handleTimer((ApplTimer) e);
        else if (e instanceof ExitEvent)
            handleExitEvent((ExitEvent) e);
        else if (e instanceof ApplAsyncEvent)
            handleApplAsyncEvent((ApplAsyncEvent) e);
        else if (e instanceof RegisterSocketEvent)
          handleRSE((RegisterSocketEvent)e);
    }
    
    /**
     * Method handleApplAsyncEvent.
     * @param applAsyncEvent
     */
    private void handleApplAsyncEvent(ApplAsyncEvent applAsyncEvent) {
        String sComLine = applAsyncEvent.getComLine();
        StringTokenizer comLine = new StringTokenizer(sComLine);
        
        if (comLine.hasMoreTokens()) {
            String command = comLine.nextToken();
            
            switch (parseCommand(command)) {
                case HELP :
                    printHelp();
                    break;
                case CASTMESSAGE :
                    castMessage(comLine);
                    break;
                case DEBUGMESSAGE :
                    sendDebug(comLine);
                    break;
                case SENDMESSAGE :
                    sendMessage(comLine);
                    break;
                case LEAVE :
                    sendLeave();
                    break;
                default :
                    System.out.println("Invalid command");
            }
        }
    }
    
    private void handleNewView(View e) {
        vs = e.vs;
        ls = e.ls;
        isBlocked = false;
        
        out.println("New view delivered:");
        out.println("View members (IP:port):");
        for (int i = 0; i < vs.addresses.length; i++)
            out.println("{" + vs.addresses[i] + "} ");
        out.println(
        (e.ls.am_coord ? "I am" : "I am not") + " the group coordinator");
                /*
                  out.println(e.vs.toString());
                  out.println(e.ls.toString());
                 */
        try {
            e.go();
        } catch (AppiaEventException ex) {
            out.println("Exception while sending the new view event");
        }
    }
    
    private void handleBlock(BlockOk e) {
        out.println("The group was blocked. Impossible to send messages.");
        isBlocked = true;
        try {
            e.go();
        } catch (AppiaEventException ex) {
            out.println("Exception while forwarding the block ok event");
        }
    }
    
    private void handleTimer(ApplTimer e) {
        
        if (isBlocked) {
            try {
                e.setSourceSession(this);
                e.setDir(Direction.invert(e.getDir()));
                e.setQualifierMode(EventQualifier.ON);
                e.setTimeout(e.getChannel().getTimeProvider().currentTimeMillis() + e.period);
                e.init();
                e.go();
            } catch (AppiaEventException ex) {
                out.println(
                "Exception while sending the timer event in a blocked group");
            } catch (AppiaException ex) {
                out.println(
                "Exception while sending the timer event in a blocked group");
            }
            return;
        }
        try {
            GroupSendableEvent msgEvent;
            
            if (e.dest != null)
                msgEvent =
                new ApplSendEvent(channel, this, vs.group, vs.id, e.dest);
            else
                msgEvent = new ApplCastEvent(channel, this, vs.group, vs.id);
            
            msgEvent.source = vs.view[ls.my_rank];
            
            ApplMessageHeader header = new ApplMessageHeader(e.msg,e.thisResend);
            header.pushMySelf(msgEvent.getMessage());
            msgEvent.go();

            /////////////////////////////////////////
            // TESTING
            /////////////////////////////////////////
//            if (test.TestOptimized.mode == test.TestOptimized.TEST1) {
//              if (test.TestOptimized.times != 0) {
//                if (test.TestOptimized.beginTimes == 0)
            // FIXME: If this is ever uncommented, the System.currentTimeMillis() should be fixed.
//                  test.TestOptimized.beginTime1=System.currentTimeMillis();
//                test.TestOptimized.beginTimes++;
//              }
//            } else if (test.TestOptimized.mode == test.TestOptimized.TEST2) {
//              if (test.TestOptimized.times != 0) {
//                if (test.TestOptimized.beginTimes == 0) {
//                  test.TestOptimized.beginTime2=new long[test.TestOptimized.times];
//                  test.TestOptimized.endTime2=new long[test.TestOptimized.times];
//                }
//                test.TestOptimized.beginTime2[test.TestOptimized.beginTimes]=test.PrecisionTime.currentTimeMicros();
//                test.TestOptimized.beginTimes++;
//              }
//            }
            /////////////////////////////////////////

            //	    out.println("Enviada mensagem: "+new String(data));
            if (e.hasMore()) {
                e.prepareNext();
                e.go();
            }
        } catch (AppiaEventException ex) {
            System.err.println("Unexpected exception in Application Session");
            switch (ex.type) {
                case AppiaEventException.NOTINITIALIZED :
                    System.err.println(
                    "Event not initialized in message "
                    + "sending (Application Session)");
                    break;
                case AppiaEventException.ATTRIBUTEMISSING :
                    System.err.println(
                    "Missing attribute in message "
                    + "sending (Application Session)");
                    break;
                case AppiaEventException.UNKNOWNQUALIFIER :
                    System.err.println(
                    "Unknown qualifier (impossible) in "
                    + " message sending (Application Session)");
                    break;
                case AppiaEventException.UNKNOWNSESSION :
                    System.err.println(
                    "Unknown session in message "
                    + "sending (Application Session)");
                    break;
                case AppiaEventException.UNWANTEDEVENT :
                    System.err.println(
                    "Unwanted event in message "
                    + "sending (Application Session)");
                    break;
                case AppiaEventException.CLOSEDCHANNEL :
                    System.err.println(
                    "Channel closed in message "
                    + "sending (Application Session)");
                    break;
            }
        } catch (AppiaException ex) {
            out.println(
            "Exception while sending the timer for the next message");
        }
    }
    
    private void receiveData(GroupSendableEvent e) {
        
        /* Echoes received messages to the user */
        if (e.getDir() == Direction.UP) {
            Message m = e.getMessage();
            
            if (e instanceof ApplSendEvent)
                out.print("Message (pt2pt)");
            else
                out.print("Message (multicast)");
            
            out.println(" received from " + ((Endpt) e.source).toString());
            ApplMessageHeader header = new ApplMessageHeader(m);
            // OR
            // header.popMySelf(m);
            
            System.out.println("("+header.number+") "+header.message);
        }
        try {
            e.go();
        } catch (AppiaEventException ex) {
            System.err.println(
            "Unexpected exception in Application " + "session");
        }
    }
    
    private void handleChannelInit(ChannelInit e) {
        
        final Thread t = e.getChannel().getThreadFactory().newThread(new ApplReader(this));
        t.setName("Appl Reader Thread");
        t.start();
         
                /* Forwards channel init event. New events must follow this
                   one */
        try {
            e.go();
        } catch (AppiaEventException ex) {
            System.err.println("Unexpected exception in Application " + "session");
        }
        
        channel = e.getChannel();
        
                /* Informs layers bellow of the port
                   where messages will be received and sent */
        
        try {
            RegisterSocketEvent rse;
            
            if (ssl) {
              if ((keystoreFile != null) && (keystorePass != null))
                rse=new SslRegisterSocketEvent(channel,Direction.DOWN,this,keystoreFile,keystorePass.toCharArray());
              else
                rse=new SslRegisterSocketEvent(channel,Direction.DOWN,this);
            } else {
              rse=new RegisterSocketEvent(channel,Direction.DOWN,this,(myPort < 0) ? RegisterSocketEvent.FIRST_AVAILABLE : myPort);
            }
            
            rse.go();
        } catch (AppiaEventException ex) {
            switch (ex.type) {
                case AppiaEventException.UNWANTEDEVENT :
                    System.err.println(
                    "The QoS definition doesn't satisfy the "
                    + "application session needs. "
                    + "RegisterSocketEvent, received by "
                    + "UdpSimpleSession is not being acepted");
                    break;
                default :
                    System.err.println(
                    "Unexpected exception in " + "Application session");
                    break;
            }
        }
        
        if (multicast != null) {
            try {
                MulticastInitEvent amie =
                new MulticastInitEvent(multicast,false,channel,Direction.DOWN,this);
                amie.go();
            } catch (AppiaEventException ex) {
                System.err.println(
                "EventException while launching MulticastInitEvent");
            } catch (NullPointerException ex) {
                System.err.println(
                "EventException while launching MulticastInitEvent");
            }
        }
        
        out.println("Open channel with name " + e.getChannel().getChannelID());
    }

    private void handleChannelClose(ChannelClose ev) {
        out.println("Channel Closed");
        try {
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
        System.exit(0);
    }
    
    private void sendLeave() {
        try {
            LeaveEvent ev = new LeaveEvent(channel,Direction.DOWN,this,vs.group,vs.id);
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }
    
    private void handleExitEvent(ExitEvent ev) {
        out.println("Exit");
        try {
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
    }
    
    private void handleRSE(RegisterSocketEvent ev) {
      if (ev.error) {
        System.err.println("Error while registering socket: "+ev.port);
        System.exit(1);
      }
      
      if (myPort < 0) {
        myPort=ev.port;
        try {
            mySocketAddress = ev.getLocalSocketAddress();
            sendGroupInit();
        } catch (AppiaException e) {
            e.printStackTrace();
        }
      }  
    }
      
    private void sendGroupInit() {
      try {
        myEndpt=new Endpt("Appl@"+mySocketAddress);
        
        Endpt[] view=null;
        SocketAddress[] addrs=null;
        if (initAddrs == null) {
          addrs=new SocketAddress[1];
          addrs[0]=mySocketAddress;
          view=new Endpt[1];
          view[0]=myEndpt;
        } else {
          addrs=initAddrs;
          view=new Endpt[addrs.length];
          for (int i=0 ; i < view.length ; i++) {
            view[i]=new Endpt("Appl@"+addrs[i]);
          }
        }
        
        vs=new ViewState("1", myGroup, new ViewID(0,view[0]), new ViewID[0], view, addrs);
        
//        if (gossips != null) {
//          String s="GOSSIPS: ";
//          for (int i=0 ; i < gossips.length ; i++)
//            s+=(gossips[i].toString()+" ");
//          System.out.println(s+"\n");
//        }
//        System.out.println("INITIAL_VIEW: "+vs.toString());
        
        GroupInit gi =
        new GroupInit(vs,myEndpt,multicast,gossips,channel,Direction.DOWN,this);
        gi.go();
      } catch (AppiaEventException ex) {
        System.err.println("EventException while launching GroupInit: "+ex.getMessage());
      } catch (NullPointerException ex) {
        System.err.println("EventException while launching GroupInit: "+ex.getMessage());
      } catch (AppiaGroupException ex) {
        System.err.println("EventException while launching GroupInit: "+ex.getMessage());
        
      }
    }
      
}
