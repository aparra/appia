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

package net.sf.appia.jgcs;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.jgcs.protocols.top.JGCSGroupEvent;
import net.sf.appia.jgcs.protocols.top.JGCSSendEvent;
import net.sf.appia.jgcs.protocols.top.JGCSSendableEvent;
import net.sf.appia.protocols.common.ServiceEvent;
import net.sf.appia.protocols.group.events.GroupEvent;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.leave.ExitEvent;
import net.sf.appia.protocols.total.common.RegularServiceEvent;
import net.sf.appia.protocols.total.common.SETOServiceEvent;
import net.sf.appia.protocols.total.common.UniformServiceEvent;
import net.sf.jgcs.AbstractDataSession;
import net.sf.jgcs.Annotation;
import net.sf.jgcs.ClosedSessionException;
import net.sf.jgcs.JGCSException;
import net.sf.jgcs.Message;
import net.sf.jgcs.NotJoinedException;
import net.sf.jgcs.Service;
import net.sf.jgcs.UnsupportedServiceException;
import net.sf.jgcs.utils.Mailbox;

import org.apache.log4j.Logger;

/**
 * This class defines a AppiaDataSession and implements the DataSession of jGCS.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class AppiaDataSession extends AbstractDataSession {

	private static Logger logger = Logger.getLogger(AppiaDataSession.class);
	private static Logger workerlog = Logger.getLogger(PullPushWorker.class);

	private AppiaControlSession controlSession;

	private PullPushWorker worker;
	private Mailbox<Event> mailbox;
	private boolean isSessionOpen;
	
	private Map<AppiaService,Channel> channelsMap;
	private AppiaService defaultSendService;
	private Map<AppiaMessage,Object>servicesMap;
	
	public AppiaDataSession(AppiaProtocol proto, AppiaGroup group, Mailbox<Event> mbox, 
			AppiaControlSession control, List<Channel> channels) {
		super(proto,group);
		mailbox = mbox;
		controlSession = control;
		worker = new PullPushWorker();
		worker.start();
		channelsMap = new HashMap<AppiaService,Channel>();
		servicesMap = new Hashtable<AppiaMessage,Object>();
		logger.debug("Number of channels: "+channels.size());
		for(Channel ch : channels){
			logger.debug("Channel: "+ch.getChannelID());
			defaultSendService = new AppiaService(ch.getChannelID());
			channelsMap.put(defaultSendService, ch);
		}
		isSessionOpen = true;
	}

	public void close() {
		isSessionOpen = false;
		worker.stop();
		super.close();
	}

	public Message createMessage() throws ClosedSessionException {
		return new AppiaMessage();
	}

	public void multicast(Message msg, Service service, Object cookie,
			Annotation... annotation) throws IOException, UnsupportedServiceException {
		// message with null dest will be sent to all members of the group
		sendMessage(msg, service, cookie, null, annotation);			
	}
	
	public void send(Message msg, Service service, Object cookie, SocketAddress destination, 
			Annotation... annotation) throws IOException, UnsupportedServiceException {
		sendMessage(msg, service, cookie, destination, annotation);
	}

	private void sendMessage(Message msg, Service service, Object cookie, SocketAddress destination, 
			Annotation... annotation) throws IOException, UnsupportedServiceException {
		// TODO: cookie and annotations are future work
		if(!isSessionOpen)
			throw new ClosedSessionException("Channel is closed.");
		Channel channel = null;
		if(service != null){
			if(logger.isDebugEnabled())
				logger.debug("Service on send: "+((AppiaService)service).getService());
			if( ! (service instanceof AppiaService))
				throw new UnsupportedServiceException("Service "+service+" is not supported.");
			channel = channelsMap.get(service);
		}
		else
			channel = channelsMap.get(defaultSendService);
        if(channel == null)
            throw new UnsupportedServiceException("There is no Appia channel for the service "+service);

		try {
			final MessageSender event = new MessageSender(channel, Direction.DOWN,(AppiaMessage)msg, destination);
			event.asyncGo(channel,Direction.DOWN);
		} catch (AppiaEventException e) {
			throw new IOException("Failed to send message due to an Appia Event Exception:"+
					e.getMessage());
		}
		if(logger.isDebugEnabled())
			logger.debug("Message "+msg+" delivered to the Appia channel with service "+service);
	}

	/**
	 * Thread that receives events from the mailbox and deliver them to 
	 * the listeners.
	 * @author nuno
	 *
	 */
	class PullPushWorker implements Runnable {

		private AtomicBoolean running = null;
		private Thread reader = null;
		
		PullPushWorker(){}
		
		public void start(){
			if(reader == null){
				reader = new Thread(this,"PullPushThread");
				reader.setDaemon(true);
				running = new AtomicBoolean(true);
				reader.start();
				Thread.yield();
			}
		}
		
		public void stop(){
			if(reader != null && reader.isAlive()){
				running.set(false);
				reader.interrupt();
			}
		}
		
		public void run() {
            Event event=null;
		    try{
		        while(running.get()){
		            workerlog.debug("before receive");
		            event = mailbox.removeNext();
		            if(event == null){
		                if(workerlog.isDebugEnabled())
		                    workerlog.debug("Received null event from Appia mailbox");
		                continue;
		            }
		            if(workerlog.isDebugEnabled())
		                workerlog.debug("after receive: "+event);
		            if(event instanceof JGCSGroupEvent || event instanceof JGCSSendEvent){
	                    if(!controlSession.isJoined())
	                        continue;
		                AppiaMessage msg=null;
		                try {
                            if(event instanceof JGCSGroupEvent)
                                msg = (AppiaMessage) ((JGCSGroupEvent) event).getMessage();
                            else
                                msg = (AppiaMessage) ((JGCSSendEvent) event).getMessage();
                        } catch (RuntimeException e) {
                            e.printStackTrace();
                            if(event instanceof JGCSGroupEvent)
                                System.out.println("ON EVENT "+((JGCSGroupEvent)event).toString());

                        }
		                SocketAddress sender_addr=null;
		                try {
		                    sender_addr = controlSession.getMembership().getMemberAddress(((GroupSendableEvent)event).orig);
		                } catch (NotJoinedException e1) {
		                    workerlog.debug("Received message but I'm not in the group: "+event,e1);
		                    notifyExceptionListeners(new JGCSException("Received message but I'm not in the group: "+event,e1));
		                }
		                msg.setSenderAddress(sender_addr);
		                if(workerlog.isDebugEnabled())
		                    workerlog.debug("Delivering message: "+msg);
		                final Object ctx = notifyMessageListeners(msg);
		                if(ctx != null){
		                    servicesMap.put(msg,ctx);
		                    if(workerlog.isDebugEnabled())
		                        workerlog.debug("Received context for this message. Adding to the services map:\nMessage:: "+msg+
		                                " --> Context:: "+ctx+" [ SIZE OF MAP:: "+servicesMap.size()+" ]");
		                }
		            }
		            else if(event instanceof JGCSSendableEvent){
		                final JGCSSendableEvent sendableEvent = (JGCSSendableEvent) event;
		                AppiaMessage msg = (AppiaMessage) sendableEvent.getMessage();
		                msg.setSenderAddress((SocketAddress) sendableEvent.source);
		                if(workerlog.isDebugEnabled())
		                    workerlog.debug("Delivering message coming from outside of the group: "+msg);
		                Object ctx = notifyMessageListeners(msg);
		                if(ctx != null){
		                    logger.warn("The Service feature is not supported for this kind of messages. Ignoring it.");
		                    notifyExceptionListeners(new JGCSException("The Service feature is not supported for this kind of messages. Ignoring it."));
		                }
		            }
		            else if(event instanceof GroupEvent){
		                if(logger.isDebugEnabled())
		                    workerlog.debug("Received group event.");
		                // This could be a View or a BlockOk.
		                // the event received that contains the view is READ ONLY.
		                // deliver to control session
		                controlSession.notifyListeners((GroupEvent) event);
		            }
		            else if(event instanceof ServiceEvent){
                        if(!controlSession.isJoined())
                            continue;
                        else
                            handleServiceEvent((ServiceEvent)event);
		            }
		            else if(event instanceof ExitEvent){
		                controlSession.notifyMemberRemoved();
		            }
		            else
		                notifyExceptionListeners(new JGCSException("Received unrecognized event from Appia: "+event));
		        }
		    }
		    catch(RuntimeException rte){
		        workerlog.warn("Exception in the worker Thread: "+rte+"\nwhile processing event "+event.toString());
		        rte.printStackTrace();
                notifyExceptionListeners(new JGCSException("RuntimeException while processing received event: "+event,rte));
		    }
		} // end of run()

		private void handleServiceEvent(ServiceEvent event) {
			if(workerlog.isDebugEnabled())
				workerlog.debug("Received service event from Appia "+event);

			Service currentService = null;
			boolean isLastService = false;
			if(event instanceof SETOServiceEvent)
				currentService = new AppiaService("seto_total_order");
			else if(event instanceof RegularServiceEvent)
				currentService = new AppiaService("regular_total_order");
			else if(event instanceof UniformServiceEvent){
				currentService = new AppiaService("uniform_total_order");
				isLastService = true;
			}
            else if(event instanceof ServiceEvent){
                currentService = new AppiaService("all");
                isLastService = true;                
            }
			else{
				notifyExceptionListeners(new JGCSException("Received unrecognized Service event from Appia: "+event));
				if(workerlog.isDebugEnabled())
					workerlog.debug("Received unrecognized Service event from Appia: "+event);
				return;
			}
			Object context = servicesMap.get(event.getMessageID());
			if(workerlog.isDebugEnabled())
				workerlog.debug("Application context for message="+event.getMessageID()+" is ctx="+context);
			if(context != null){
				notifyServiceListeners(context,currentService);
				if(isLastService){
					context = servicesMap.remove(event.getMessageID());
					if(workerlog.isDebugEnabled())
						workerlog.debug("Last service notified. Removing entry from hashtable --> Context removed: "+context+
                                " MAP SIZE: "+servicesMap.size());
				}
			}			
		}

	}

}

