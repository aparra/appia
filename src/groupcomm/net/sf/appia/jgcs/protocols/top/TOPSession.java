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
 * Initial developer(s): Nuno Carvalho.
 * Contributor(s): See Appia web page for a list of contributors.
 */

package net.sf.appia.jgcs.protocols.top;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

import net.sf.appia.core.AppiaError;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.jgcs.MessageSender;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.common.ServiceEvent;
import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.ExitEvent;
import net.sf.appia.protocols.group.leave.LeaveEvent;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.udpsimple.MulticastInitEvent;
import net.sf.appia.protocols.utils.ParseUtils;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.jgcs.utils.Mailbox;

import org.apache.log4j.Logger;


/**
 * This class defines a TOPSession.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class TOPSession extends Session implements InitializableSession {
	
	private static final int DEFAULT_MULTICAST_PORT = 7000;
	private static final int DEFAULT_LOCAL_PORT     = 27752;
    private static final int DEFAULT_GOSSIP_PORT     = 10000;
    private static final int DEFAULT_TIME_PERIOD = 1000;
	
	private CountDownLatch openChannel, closeChannel, leaveChannel;
	
	private Mailbox<Event> mailbox;
	
	private boolean isBlocked;
	private ViewState vs;
	private Queue<GroupSendableEvent> eventsPending;
	private InetSocketAddress multicast=null;
	private InetSocketAddress[] gossips = null;
	private InetSocketAddress myAddress = null;
	private int numberOfChannels = 0, numberOfBlocks=1, numberOfViews = 1;
	private List<Channel>channels = null;
	private List<Event> pendingReceivedEvents = null;
	private ViewID currentWaitingViewID = null;
	private boolean sentRSE = false;
	private Group myGroup=null;
	private String jgcsGroupName;
	
	private boolean requestedJoin = false;
	private boolean receivedRSE = false;
	private boolean requestedLeave = false;
	
	private static Logger logger = Logger.getLogger(TOPSession.class);
	
	
	public TOPSession(Layer layer) {
		super(layer);
		openChannel = new CountDownLatch(1);
		closeChannel = new CountDownLatch(1);
		isBlocked = true;
		eventsPending = new LinkedList<GroupSendableEvent>();
		channels = new LinkedList<Channel>();
		pendingReceivedEvents = new LinkedList<Event>();
	}
	
	/**
	 * Initializes the session using the parameters given in the XML configuration.
	 * Possible parameters:
	 * <ul>
	 * <li><b>multicast</b> the multicast address (optional) in the format IP:port.
	 * <li><b>gossip_address</b> an array of gossip addresses, in the format IP1:port1,IP2:port2,etc.
	 * By default, it gossips on <code>224.0.0.1:10000</code>. 
	 * </ul>
	 * 
	 * @param params The parameters given in the XML configuration.
	 * @see net.sf.appia.xml.interfaces.InitializableSession#init(SessionProperties)
	 */
	public void init(SessionProperties params) {
		if (params.containsKey("multicast")) {
			try {
				multicast = ParseUtils.parseSocketAddress(params.getString("multicast"),null,DEFAULT_MULTICAST_PORT);
			} catch (UnknownHostException ex) {
				System.err.println("Unknown host \""+ex.getMessage()+"\"");
				System.exit(1);
			} catch (NumberFormatException ex) {
				System.err.println("Number format error "+ex.getMessage());
				System.exit(1);
			} catch (Exception ex) {
				System.err.println(ex.getMessage());
				System.exit(1);
			}
			if (!multicast.getAddress().isMulticastAddress()) {
				System.err.println("Invalid multicast address.");
				System.exit(1);
			}
		}
		
		if(params.containsKey("gossip_address")){
			try {
				gossips = ParseUtils.parseSocketAddressArray(params.getString("gossip_address"),
                        InetAddress.getByName("224.0.0.1"),DEFAULT_GOSSIP_PORT);
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		
		if (params.containsKey("local_address")){
			try{
				myAddress = ParseUtils.parseSocketAddress(params.getString("local_address"), null, DEFAULT_LOCAL_PORT);
			}
			catch (UnknownHostException e){ 
				e.printStackTrace();
			}
			catch (ParseException e){
				e.printStackTrace();
			}
		}
		
	}
	
	public void setMailbox(Mailbox<Event> mb){
		mailbox = mb;
	}
	
	@Override
	public void handle(Event event){
		if(logger.isDebugEnabled())
			logger.debug("TOP session received event "+event+" "+(event.getDir() == Direction.DOWN? "Down":"Up")
					+" from channel "+event.getChannel().getChannelID());
		if (event instanceof JGCSGroupEvent || event instanceof JGCSSendEvent)
			handleGroupEvent((GroupSendableEvent)event);
		else if(event instanceof JGCSSendableEvent)
			handleSendableEvent((JGCSSendableEvent)event);
		else if(event instanceof MessageSender)
			handleMessageSender((MessageSender)event);
		else if(event instanceof ServiceEvent)
			handleService((ServiceEvent)event);
		else if (event instanceof View)
			handleNewView((View) event);
		else if (event instanceof BlockOk)
			handleBlock((BlockOk) event);
		else if(event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);        
		else if (event instanceof RegisterSocketEvent)
			handleRegisterSocket((RegisterSocketEvent) event);
		else if(event instanceof MulticastInitEvent)
			handleMulticastInit((MulticastInitEvent)event);
		else if (event instanceof ExitEvent)
			handleExitEvent((ExitEvent)event);
		else if(event instanceof ChannelClose)
			handleChannelClose((ChannelClose) event);
		else if(event instanceof JGCSReleaseBlock)
			handleReleaseBlock((JGCSReleaseBlock)event);
		else if(event instanceof JGCSJoinEvent)
			handleJGCSJoin((JGCSJoinEvent)event);
		else if(event instanceof JGCSLeaveEvent)
			handleJGCSLeave((JGCSLeaveEvent)event);
		else if(event instanceof JGCSLeaveTimer)
			handleLeaveTimer((JGCSLeaveTimer)event);
		else
			super.handle(event);
	}
	
	private void handleMessageSender(MessageSender sender) {
		if(logger.isDebugEnabled())
			logger.debug("Received Message "+sender.getMessage()+" from the DataSession.");
		
		GroupSendableEvent event = null;
		if(sender.getDestination() == null){
			try {
				event = new JGCSGroupEvent(sender.getChannel(),Direction.DOWN,
						this, this.myGroup, this.vs.id);
				event.setMessage(sender.getMessage());
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
		else{
			try {
				event = new JGCSSendEvent(sender.getChannel(),Direction.DOWN,
						this, this.myGroup, this.vs.id);
				event.setMessage(sender.getMessage());
				((JGCSSendEvent)event).setDestination(vs.getRankByAddress((InetSocketAddress) sender.getDestination()));
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
		if(isBlocked){
			eventsPending.add(event);
            logger.warn("The group is blocked. Message "+sender.getMessage()+" added to pending events.");
			return;
		}
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		if(logger.isDebugEnabled())
			logger.debug("Message "+sender.getMessage()+" Forwarded to the Channel "+event.getChannel().getChannelID());
	}

	private void handleReleaseBlock(JGCSReleaseBlock block) {
        isBlocked = true;
		if(numberOfChannels > 1){
			final BlockOk myBlock = block.getBlockEvent(); 
			for(Channel c : channels){
				final BlockOk copy = new BlockOk(myBlock.group,myBlock.view_id);
				copy.setDir(myBlock.getDir());
				copy.setSourceSession(this);
				copy.setChannel(c);
				try {
					copy.init();
					copy.go();
				} catch (AppiaEventException e) {
					e.printStackTrace();
				}
			}
		}
		else
			try {
				block.getBlockEvent().go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
	}
	
	private void handleGroupEvent(GroupSendableEvent event) {
		// event from the network
		if(event.getDir() == Direction.UP){
		    if(isBlocked){
		        pendingReceivedEvents.add(event);
		        // this is needed in the case that Appia was configured to use several
		        // channels, the views were multiplexed and one of the channels is 
		        // holding the view. events will be flushed when all the views (one from
		        // each channel) are collected.
		    }
		    else{
	            mailbox.add(event);
	            try {
	                event.go();
	            } catch (AppiaEventException e) {
	                e.printStackTrace();
	            }		        
		    }
		}
	}

	private void handleSendableEvent(JGCSSendableEvent event) {
		// event from the network
		if(event.getDir() == Direction.UP) {
            if(isBlocked){
                pendingReceivedEvents.add(event);
                // this is needed in the case that Appia was configured to use several
                // channels, the views were multiplexed and one of the channels is 
                // holding the view. events will be flushed when all the views (one from
                // each channel) are collected.
            }
            else{
                mailbox.add(event);
                try {
                    event.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }               
            }
		}
	}

	private void handleService(ServiceEvent event) {
		mailbox.add(event);
	}

	
	private void sendLeave(Channel channel){
        try {
            new LeaveEvent(channel,Direction.DOWN,this,myGroup,vs.id).go();
            new JGCSLeaveTimer(DEFAULT_TIME_PERIOD,channel,this).go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        } catch (AppiaException e) {
            e.printStackTrace();
        }
	}
	private void handleJGCSLeave(JGCSLeaveEvent event) {
	    leaveChannel = event.getLatch();
	    if(vs == null){
	        sentGroupInit = true;
	        leaveChannel.countDown();
	    }
	    else if (!isBlocked)
	        sendLeave(event.getChannel());
	    else
	        requestedLeave = true;
	}
	
	private void handleLeaveTimer(JGCSLeaveTimer timer) {
		if(leaveChannel != null){
			try {
				new LeaveEvent(timer.getChannel(),Direction.DOWN,this,myGroup,vs.id).go();
				timer.setDir(Direction.invert(timer.getDir()));
				timer.setQualifierMode(EventQualifier.ON);
				timer.setSourceSession(this);
				timer.init();
				timer.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
            }
		}
	}
	
	private void handleJGCSJoin(JGCSJoinEvent event) {
		requestedJoin = true;
		jgcsGroupName = event.getGroupName();
		// this is done only once and when there are conditions to initialize the group.
		// the tests to ensure this are made inside the method
		sendGroupInit(event.getChannel());
		
	}
	
	private void handleExitEvent(ExitEvent event) {
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		if(leaveChannel != null){
			leaveChannel.countDown();		
			leaveChannel = null;
		}
		else
			mailbox.add(event);
	}
	
	/*
	 * handles a new view (received after a BlockOk)
	 * @param e the new view
	 */
	private void handleNewView(View e) {
//        System.out.println("TOP: Received view from channel "+e.getChannel().getChannelID()+" WITH ID "+e.vs.id);
		logger.debug("Received view from channel "+e.getChannel().getChannelID());
		
		// FIXME: reset if view ID is not the same... verify this code!!!
		if(currentWaitingViewID == null)
		    currentWaitingViewID = e.view_id;
		else if(!e.view_id.equals(currentWaitingViewID)){ //reset...
            numberOfViews = 1;
            currentWaitingViewID = e.view_id;
		}
		
		if(numberOfViews < numberOfChannels){
                numberOfViews++;
                return;
		}

		numberOfViews = 1;
		isBlocked = false;
		vs = e.vs;
		// forward event
		try {
			e.go();
		} catch (AppiaEventException e2) {
			e2.printStackTrace();
		}
		
		// dump events that were received by the application during view change
		while(!eventsPending.isEmpty()){
			final GroupSendableEvent event = eventsPending.remove();
			try {
				event.view_id = vs.id;
				event.setSourceSession(this);
				event.init();
				event.go();
			} catch (AppiaEventException e1) {
				e1.printStackTrace();
			}
		}		
		// Not sending clone. The JGCSChannel should not write in this event. It's READ ONLY!
		mailbox.add(e);
		
		if(requestedLeave && leaveChannel != null)
		    sendLeave(e.getChannel());
		
		while(!pendingReceivedEvents.isEmpty()){
		    Event pendingEvent = pendingReceivedEvents.remove(0);
		    mailbox.add(pendingEvent);
		    try {
                pendingEvent.go();
            } catch (AppiaEventException e1) {
                e1.printStackTrace();
            }
		}
	}
	
	/*
	 * handles the BlockOk sent by the group communication protocols.
	 */
	private void handleBlock(BlockOk e) {
		if(leaveChannel != null){
			if(logger.isDebugEnabled())
				logger.debug("Leave latch is present. Channel is closing. Forwarding BlockOk without delivering it to the Application.");
			try {
				e.go();
			} catch (AppiaEventException e1) {
				logger.debug("Error forwarding event: "+e1.getMessage());
			}
		}
		else{
			if(numberOfBlocks<numberOfChannels)
				numberOfBlocks++;
			else{
				if(logger.isDebugEnabled())
					logger.debug("Delivering BlockOk to the Application.");
				mailbox.add(e);
				numberOfBlocks = 1;
			}
		}
	}
	
	/*
	 * handles register socket event. this is the response to a previously request to register a socket.
	 */
	private void handleRegisterSocket(RegisterSocketEvent e) {		
		if (e.error) {
			if(e.getErrorCode() == RegisterSocketEvent.RESOURCE_ALREADY_BOUND_ERROR)
				logger.warn("The requested resource is already available.");
			else{
				logger.fatal("Impossible to register socket.");
			}
		}
		
		myAddress = new InetSocketAddress(e.localHost, e.port);
		receivedRSE = true;
		logger.debug("Socket Registered using the address: "+myAddress);
		//appiaGMS.setLocalAddress(myAddress);
		// this is done only once and when there are conditions to initialize the group.
		// the tests to ensure this are made inside the method
		sendGroupInit(e.getChannel());
	}
	
	/*
	 * Handles Multicastinit event. response to a previously request to open a multicast socket.
	 */
	private void handleMulticastInit(MulticastInitEvent event) {
		if(event.error){
			logger.warn("Impossible to register multicast address. Using Point to Point");
		}		
		// this is done only once and when there are conditions to initialize the group.
		// the tests to ensure this are made inside the method
		sendGroupInit(event.getChannel());
	}
	
	/*
	 * This function should only be called when both multicastInitEvent (if requested) and RegisterSocketEvent 
	 * where received.
	 */
	private boolean sentGroupInit = false;
	
	private void sendGroupInit(Channel channel){
		
		if(sentGroupInit)
			return;
		
		if (receivedRSE && requestedJoin){
			try {
				final InetSocketAddress[] addrs=new InetSocketAddress[1];
				addrs[0]=myAddress;
				final Endpt[] view=new Endpt[1];
				view[0] = new Endpt(jgcsGroupName+"@"+addrs[0].toString());
				myGroup = new Group(jgcsGroupName);
				final ViewState gvs = new ViewState("1",myGroup,new ViewID(0,view[0]),new ViewID[0], view, addrs);
				final GroupInit ginit=new GroupInit(gvs,view[0],multicast,gossips,channel,Direction.DOWN, this);
				ginit.go();
				sentGroupInit = true;
			} catch (AppiaException ex) {
				ex.printStackTrace();
                throw new AppiaError("Impossible to initiate group communication. Aborting.",ex);
			}
		}
	}
	
	/*
	 * handles Channelinit. This is the first event received by any session
	 * and must be the first to be forwarded. 
	 */
	private void handleChannelInit(ChannelInit e) {
		if(gossips == null || gossips.length == 0){
			logger.fatal("Received channel init but no gossip is configured.");
			throw new AppiaError("Received channel init but no gossip is configured.");
		}
		
		/* Forwards channel init event. New events must follow this one */
		try {
			e.go();
		} catch (AppiaEventException e1) {
			e1.printStackTrace();
		}
		channels.add(e.getChannel());
		numberOfChannels = channels.size();
		
		if(!sentRSE){
			RegisterSocketEvent rse = null;
			try {
				rse = new RegisterSocketEvent(e.getChannel(),Direction.DOWN,this,RegisterSocketEvent.FIRST_AVAILABLE);
				if(myAddress != null){
					rse.localHost = myAddress.getAddress();
					rse.port = myAddress.getPort();
				}
				rse.go();
				sentRSE = true;
			} catch (AppiaEventException ex) {
				switch (ex.type) {
				case AppiaEventException.UNWANTEDEVENT :
					System.err.println(
							"The QoS definition doesn't satisfy the "
							+ "application session needs. "
							+ "RegisterSocketEvent, received by "
							+ "UdpSimpleSession is not being accepted");
					break;
				default :
					System.err.println(
							"Unexpected exception in " + this.getClass().getName());
				break;
				}
				
			}
			
			if (multicast != null) {
				try {
					new MulticastInitEvent(multicast,false,e.getChannel(),Direction.DOWN,this).go();
				} catch (AppiaEventException ex) {
                    throw new AppiaError("Impossible to send Multicast Init Event.",ex);
				}
			}
		} // end of if(!sentRSE)
		
		openChannel.countDown();
	}    
	
	/*
	 * handles ChannelClose. This is the last event received by a session for one channel.
	 * Notifies that the channel is closed.
	 */
	private void handleChannelClose(ChannelClose ev) {
		try {
			ev.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		closeChannel.countDown();
		channels.remove(ev.getChannel());
		numberOfChannels = channels.size();
	}
	
}
