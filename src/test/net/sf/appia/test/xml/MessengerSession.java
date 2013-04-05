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
 package net.sf.appia.test.xml;

import java.awt.Point;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.group.AppiaGroupException;
import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.ExitEvent;
import net.sf.appia.protocols.group.sync.BlockOk;




/**
 * @author Jose Mocito & Nuno Almeida
 */
public class MessengerSession extends Session {
	
	/* User IO */
	private PrintStream out = System.out;
	
	public Channel text = null;
	public Channel draw = null;
	
	// Local ports
	private InetSocketAddress myPort;
	private int currPort = 6666;
	
	/* Messenger INTERFACE */
	private MessengerInterface msg;
	
	private String username;
	
	/* Group */
    private Group myGroup;
    private Endpt myEndpt=null;
    private InetSocketAddress[] gossips = null;
    private ViewState vs=null, vs_old = null;
    private LocalState ls=null, ls_old = null;
    private boolean isBlocked;
	
    private int channelCounter;
    
    private boolean notNew;
    
	/**
	 * Mainly used for corresponding layer initialization
	 */
	
	public MessengerSession(MessengerLayer l) {
		super(l);
	}
	
	public void init(String user,
			String groupName,
			String gossipHost,
			int gossipPort) {
				
		username = user;
		myGroup = new Group(groupName);
		System.out.println("Group name: " + groupName +".");
		
		try {
			gossips = new InetSocketAddress[1];
			gossips[0] = new InetSocketAddress(InetAddress.getByName(gossipHost),gossipPort);
			System.out.println("Gossip: "+gossips[0]);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
		}
	}
	
	/**
	 * Main Event handler function. Accepts all incoming events and
	 * dispatches them to the appropriate functions
	 * @param ev The incoming event
	 * @see net.sf.appia.core.Session
	 */
	public void handle(Event ev) {
		if (ev instanceof ChannelInit)
			handleChannelInit((ChannelInit) ev);
		else if (ev instanceof ChannelClose)
            handleChannelClose((ChannelClose) ev);
        else if (ev instanceof View)
            handleNewView((View) ev);
        else if (ev instanceof BlockOk)
            handleBlock((BlockOk) ev);
        else if (ev instanceof ExitEvent)
            handleExitEvent((ExitEvent) ev);
		else if (ev instanceof TextEvent)
			handleTextEvent((TextEvent)ev);
		else if (ev instanceof DrawEvent)
			handleDrawEvent((DrawEvent)ev);
		else if (ev instanceof MouseButtonEvent)
			handleMouseButtonEvent((MouseButtonEvent)ev);
		else if (ev instanceof ImageEvent)
			handleImageEvent((ImageEvent) ev);
		else if (ev instanceof ClearWhiteBoardEvent)
			handleCWBEvent((ClearWhiteBoardEvent)ev);
		else if (ev instanceof RegisterSocketEvent)
			handleRSE((RegisterSocketEvent)ev);
		else
			try {
				out.println("Received unexpected event!");
				ev.go();
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
	}
	
	/**
	 * Initializes channel
	 * 
	 * @param e
	 */
	private void handleChannelInit(ChannelInit e) {
		channelCounter++;
		System.out.println("Session: "+this);
		try {
			e.go();
		} catch (AppiaEventException ex) {
			System.err.println("Unexpected exception in Application " + "session");
		}
		
		Pattern textPattern = Pattern.compile("text.*");
		Pattern drawPattern = Pattern.compile("draw.*");
		
		Matcher textMatcher = textPattern.matcher(e.getChannel().getChannelID());
		Matcher drawMatcher = drawPattern.matcher(e.getChannel().getChannelID());
			
		System.out.println("init: " + e.getChannel().getChannelID());
		if (textMatcher.matches())
			text = e.getChannel();
		else if (drawMatcher.matches())
			draw = e.getChannel();
		try {
			if (channelCounter == 2) {
				RegisterSocketEvent rse = null;
				rse = new RegisterSocketEvent(e.getChannel(),Direction.DOWN,this,currPort);
				rse.go();
			}
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
		
		System.out.println("Open channel with name " + e.getChannel().getChannelID());
		
	}
	
	/**
	 * Detects if a channel was not registered
	 * 
	 * @param ev
	 */
	private void handleRSE(RegisterSocketEvent ev) {
		if (ev.error) {
    		try {
    			currPort++;
    			myPort = new InetSocketAddress(InetAddress.getLocalHost(),currPort);
    		} catch (UnknownHostException e) {
    			e.printStackTrace();
    		}
    	    try {
				RegisterSocketEvent rse = new RegisterSocketEvent(ev.getChannel(),Direction.DOWN,this,myPort.getPort());
				rse.go();
			} catch (AppiaEventException e1) {
				e1.printStackTrace();
			}
    	}
    	else {
    		try {
				myPort = new InetSocketAddress(InetAddress.getLocalHost(),ev.port);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
    		sendGroupInit();
    		msg = new MessengerInterface(text,draw,username);
    		System.out.println("Registered: " + ev.getChannel().getChannelID()+": "+ev.port);
    	}
    }
	
	private void sendGroupInit() {
		try {
			System.out.println("SendGroupInit");
		    InetSocketAddress myAddr=new InetSocketAddress(InetAddress.getLocalHost(),myPort.getPort());
		    myEndpt=new Endpt(username+"@"+myAddr.toString());
	        
		    Endpt[] view=null;
		    InetSocketAddress[] addrs=null;
		    
		    addrs=new InetSocketAddress[1];
		    addrs[0]=myAddr;
		    view=new Endpt[1];
		    view[0]=myEndpt;
			
		    vs = new ViewState("1", myGroup, new ViewID(0,view[0]), new ViewID[0], view, addrs);
		    
		    GroupInit gi =
		    	new GroupInit(vs,myEndpt,null,gossips,text,Direction.DOWN,this);
		    gi.go();
		} catch (AppiaEventException ex) {
		    System.err.println("EventException while launching GroupInit");
		} catch (NullPointerException ex) {
		    System.err.println("EventException while launching GroupInit");
		} catch (AppiaGroupException ex) {
		    System.err.println("EventException while launching GroupInit");
		} catch (UnknownHostException ex) {
		    ex.printStackTrace();
		    throw new AppiaError("!!!!!!!!!");
		}
	}
	
	public void handleChannelClose(ChannelClose ev) {
		out.println("Channel Closed");
        try {
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
        }
        System.exit(0);
	}
	
	public void handleNewView(View ev) {
		if (notNew) {
			vs_old = vs;
			ls_old = ls;
		}
		vs = ev.vs;
        ls = ev.ls;
        isBlocked = false;
        
        out.println("New view delivered:");
        out.println("View members (IP:port):");
        for (int i = 0; i < vs.addresses.length; i++)
            out.println(
			"{"
			+ ((InetSocketAddress)vs.addresses[i]).getAddress().getHostAddress()
			+ ":"
			+ ((InetSocketAddress)vs.addresses[i]).getPort()
			+ "} ");
                
        try {
            ev.go();
        } catch (AppiaEventException ex) {
            out.println("Exception while sending the new view event");
        }
        
        updateUsers();
        
        if (notNew && vs.getNewMembers(vs_old).length > 0) {
        	ImageEvent imgevent = new ImageEvent(msg.getImage());
        	try {
				imgevent.asyncGo(draw,Direction.DOWN);
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
        }
        //System.out.println("tamanho da vista " + vs.view.length);
        /*if(vs.view.length == 6){
        	TimerEvent t;
        	try {
        		t = new TimerEvent(text,Direction.DOWN,this);
	       		t.setUsername(username);
	       		t.init();
	       		t.go();
				System.out.println("enviou");
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
        }*/
        
	}
	
	private void updateUsers() {
		Endpt[] members = vs.view;
		String usrlist = new String();
		for (int i = 0; i < members.length; i++) {
			StringTokenizer st = new StringTokenizer(members[i].id,"@");
			usrlist = usrlist.concat(st.nextToken()+"\n");
		}
		msg.setUsers(usrlist);
	}
	
	 public void handleBlock(BlockOk ev) {
	 	out.println("The group was blocked. Impossible to send messages.");
        isBlocked = true;
        try {
            ev.go();
        } catch (AppiaEventException ex) {
            out.println("Exception while forwarding the block ok event");
        }
	 }
	 
	 public void handleExitEvent(ExitEvent ev) {
	 	out.println("Exit");
	 	try {
	 		ev.go();
	 		} 
	 	catch (AppiaEventException ex) {
	 		ex.printStackTrace();
	    }
	 }
	  
	private void handleTextEvent(TextEvent event) {
		Message message = event.getMessage();
		
		if(event.getDir() == Direction.DOWN){
			notNew = true;
			/*System.out.println(event.getChannel().getChannelID()+
					"send: "+
					event.getUsername()+
					" | "+
					event.getUserMessage());*/
			
			message.pushObject(event.getUsername());
			message.pushObject(event.getUserMessage());
			
			event.source =  myPort;
						
			System.out.println(myPort.getPort());
						
			try {
				sendEvent((GroupSendableEvent)event, text);
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
			
			msg.clearText();
		}
		else {
			//System.out.println("TXT "+ event.getUserMessage() +" : " + event.getUsername() + " : " + (new Timestamp(event.getChannel().getTimeProvider().currentTimeMillis())).toString());
			String usern = null, userm = null;
			if (!event.source.equals(myEndpt)) {
				event.setUserMessage((String)message.popObject());
				event.setUsername((String)message.popObject());
			
				msg.setWindow(event.getUsername(), event.getUserMessage());
				usern = event.getUsername();
				userm = event.getUserMessage();
			//	System.out.println("TXT "+ event.getUserMessage() +" : " + event.getUsername() + " : " + (new Timestamp(event.getChannel().getTimeProvider().currentTimeMillis())).toString());
	
				/*System.out.println(event.getChannel().getChannelID()+
					" receive: "+
					event.getUsername()+
					" | "+
					event.getUserMessage());*/
			}
			else {
				System.out.println("MIim");
				usern = event.getUsername();
				userm = event.getUserMessage();
			}
			System.out.println("TXT "+ userm +" : " + usern + " : " + (new Timestamp(event.getChannel().getTimeProvider().currentTimeMillis())).toString());
		}
	}
	/*
	 * Draw Event
	 */
	private void handleDrawEvent(DrawEvent event) {
		Message message = event.getMessage();
		
		if(event.getDir() == Direction.DOWN){
			notNew = true;
			message.pushObject(event.getPoint());
			
			event.source =  myPort;
			
			try {
				sendEvent((GroupSendableEvent)event, draw);
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
		else {
			if (!event.source.equals(myEndpt)) {
				event.setPoint((Point) message.popObject());
				msg.drawPoint(event.getPoint());
			}
		}
	}
	
	private void handleMouseButtonEvent(MouseButtonEvent event) {
		Message message = event.getMessage();
		
		if(event.getDir() == Direction.DOWN){
			notNew = true;
			
			message.pushBoolean(event.pressed());
			
			event.source = myPort;
			
			try {
				sendEvent((GroupSendableEvent)event, draw);
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
		else {
			if (!event.source.equals(myEndpt)) {
				event.setPressed(message.popBoolean());
				if (event.pressed())
					msg.mousePressed();
				else
					msg.mouseReleased();
			}
		}
	}
	
	public void handleCWBEvent(ClearWhiteBoardEvent event) {
		if(event.getDir() == Direction.DOWN) {
			notNew = true;
			event.source = myPort;
			try {
				sendEvent((GroupSendableEvent)event, draw);
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
		else {
			if (!event.source.equals(myEndpt))
				msg.clearWhiteBoard();
		}
	}
	
	public void handleImageEvent(ImageEvent event) {
		Message message = event.getMessage();
		
		if(event.getDir() == Direction.DOWN){
			message.pushObject(event.getImage());
			event.source = myPort;
			try {
				sendEvent((GroupSendableEvent)event, draw);
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
		else {
			if (!event.source.equals(myEndpt) && !notNew) {
				SerializableImage img = (SerializableImage)message.popObject();
				msg.setImage(img);
				notNew = true;
			}
		}
	}
	
	/**
	 * Sends an event
	 * 
	 * @param event sending event
	 * @param chn channel
	 * @throws AppiaEventException
	 */
	private void sendEvent(GroupSendableEvent event, Channel chn) throws AppiaEventException {
		if (!isBlocked) {
			event.setSourceSession(this);
			event.setChannel(chn);
			event.setDir(Direction.DOWN);
			event.init();
			event.go();
		}
	}
}
