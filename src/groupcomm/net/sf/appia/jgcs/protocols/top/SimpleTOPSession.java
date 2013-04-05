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

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.jgcs.MessageSender;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.udpsimple.MulticastInitEvent;
import net.sf.appia.protocols.utils.ParseUtils;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.jgcs.utils.Mailbox;

import org.apache.log4j.Logger;

/**
 * This class defines a SimpleTOPSession
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class SimpleTOPSession extends Session implements InitializableSession {

	private static final int DEFAULT_MULTICAST_PORT = 7000;
	private static final int DEFAULT_LOCAL_PORT     = 27752;
	
	private Mailbox<Event> mailbox;

	private Queue<JGCSSendableEvent> eventsPending;
	private InetSocketAddress multicast=null;
	private InetSocketAddress myAddress = null;
	private boolean sentRSE = false;
	
	private boolean receivedRSE = false;
	
	private static Logger logger = Logger.getLogger(SimpleTOPSession.class);
	

	/**
	 * Creates a new SimpleTOPSession.
	 * @param layer
	 */
	public SimpleTOPSession(Layer layer) {
		super(layer);
		eventsPending = new LinkedList<JGCSSendableEvent>();
	}

	/**
	 * Initializes the session using the parameters given in the XML configuration.
	 * Possible parameters:
	 * <ul>
	 * <li><b>multicast</b> the multicast address (optional) in the format IP:port.
	 * <li><b>gossip_address</b> an array of gossip addresses, in the format IP1:port1,IP2:port2,etc.
	 * By default, it gossips on <code>224.0.0.1:7000</code>. 
	 * <li><b>local_address</b> the local address to be binded by Appia, in the format IP:port.
	 * By default, it binds on eth0, on port 27752. 
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
		if (event instanceof JGCSSendableEvent)
			handleSendableEvent((JGCSSendableEvent)event);
		else if(event instanceof MessageSender)
			handleMessageSender((MessageSender)event);
		else if(event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);        
		else if (event instanceof RegisterSocketEvent)
			handleRegisterSocket((RegisterSocketEvent) event);
		else if(event instanceof MulticastInitEvent)
			handleMulticastInit((MulticastInitEvent)event);
		else if(event instanceof ChannelClose)
			handleChannelClose((ChannelClose) event);
		else
			super.handle(event);
	}

	private void handleMessageSender(MessageSender sender) {
		// event from the network
		if(sender.getDir() == Direction.DOWN){
			// event from application
			JGCSSendableEvent event = null;
			try {
				event = new JGCSSendableEvent(sender.getChannel(),Direction.DOWN,this,sender.getDestination());
				event.setMessage(sender.getMessage());
			} catch (AppiaEventException e1) {
				e1.printStackTrace();
			}
			if(!receivedRSE){
				eventsPending.add(event);
				return;
			}
			else {
				try {
					event.go();
				} catch (AppiaEventException e) {
					e.printStackTrace();
				}
			}
		}
		// UP should not happen
		else{
			logger.warn("MessageSender event arrived from a bottom layer. This kind of events should appear only from the application.");
		}
	}

	private void handleSendableEvent(JGCSSendableEvent event) {
		// event from the network
		if(event.getDir() == Direction.UP){
			mailbox.add(event);
			try {
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
		// event from application
		else{
			logger.warn("JGCSSendable event arrived from the application. This kind of events should appear only from the network.");
		}
//			if(!receivedRSE){
//				eventsPending.add(event);
//				return;
//			}
//			else {
//				try {
//					event.go();
//				} catch (AppiaEventException e) {
//					e.printStackTrace();
//				}
//			}
//		}
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
		
		if(!eventsPending.isEmpty()){
			Iterator<JGCSSendableEvent> it = eventsPending.iterator();
			while(it.hasNext()){
				try {
					it.next().go();
				} catch (AppiaEventException e1) {
					e1.printStackTrace();
				}
			}
			eventsPending.clear();
		}
	}
	
	/*
	 * Handles Multicastinit event. response to a previously request to open a multicast socket.
	 */
	private void handleMulticastInit(MulticastInitEvent event) {
		if(event.error){
			logger.warn("Impossible to register multicast address. Using Point to Point");
		}		
	}

	/*
	 * handles Channelinit. This is the first event received by any session
	 * and must be the first to be forwarded. 
	 */
	private void handleChannelInit(ChannelInit e) {
		/* Forwards channel init event. New events must follow this one */
		try {
			e.go();
		} catch (AppiaEventException e1) {
			e1.printStackTrace();
		}
		
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
							+ "UdpSimpleSession is not being acepted");
					break;
				default :
					System.err.println(
							"Unexpected exception in " + this.getClass().getName());
				break;
				}
				
			}
			
			if (multicast != null) {
				try {
					MulticastInitEvent amie=new MulticastInitEvent(multicast,false,e.getChannel(),Direction.DOWN,this);
					amie.go();
				} catch (AppiaEventException ex) {
					ex.printStackTrace();
					System.exit(1);
				}
			}
		} // end of if(!sentRSE)
		
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
	}

}
