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
 * Created on Mar 11, 2004
 *
 */
package net.sf.appia.xml;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.concurrent.ThreadFactory;

import net.sf.appia.core.Appia;
import net.sf.appia.core.AppiaDuplicatedSessionsException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.EventScheduler;
import net.sf.appia.core.Layer;
import net.sf.appia.core.memoryManager.MemoryManager;
import net.sf.appia.core.message.MessageFactory;
import net.sf.appia.management.jmx.JMXConfiguration;
import net.sf.appia.xml.templates.ChannelTemplate;
import net.sf.appia.xml.templates.SessionTemplate;
import net.sf.appia.xml.utils.ChannelInfo;
import net.sf.appia.xml.utils.ChannelProperties;
import net.sf.appia.xml.utils.SharingState;



/**
 * This class represents a configuration. This configuration is designed to
 * reproduce all the information contained in a XML configuration file or
 * string.
 * 
 * @author Jose Mocito
 *
 */
public class Configuration {
	
	// Hashtables containing the channels and their respective templates
	private Hashtable templates;
	private Hashtable channels;
	
	// Auxiliary current channel template attributes
	private ChannelTemplate currentChTemplate;
	
	// Auxiliary current session attributes
	private String currentSessionName;
	private int currentSessionSharingState;
	//private String currentSessionLabel;
	
	// Global sessions
	private Hashtable globalSessions;
	// Label sessions
	private Hashtable labelSessions;
	
	// Appia Instance
	private Appia appia;
	
	// Global EventScheduler
	private EventScheduler globalEventScheduler;
	
    //JMX global configuration
    private JMXConfiguration jmxConfiguration;
    
    private ThreadFactory threadFactory;

	// EventScheduler class and constructors
	private boolean schedulerClassIsSet;
	private Class eventSchedulerClass;
	private Constructor esWithAppiaConstructor;
	
	// EventScheduler mode
	private boolean multiSchedulers;
	
	/**
	 * Builds an empty configuration.
	 *
	 */
	public Configuration() {
		templates = new Hashtable();
		channels = new Hashtable();
		globalSessions = new Hashtable();
		labelSessions = new Hashtable();
		// Behavior by default: one global scheduler
		globalEventScheduler = new EventScheduler();
		//globalEventScheduler = getEventScheduler();
		//TODO: verify if this affects other parts of the code...
        jmxConfiguration = null;
	}
	
	/**
	 * Builds an empty configuration.
	 *
	 */
	public Configuration(Appia appia) throws AppiaXMLException {
		templates = new Hashtable();
		channels = new Hashtable();
		globalSessions = new Hashtable();
		labelSessions = new Hashtable();
		//sharedSessions = new Hashtable();
		this.appia = appia;
		// Behavior by default: one global scheduler
		globalEventScheduler = new EventScheduler(appia);
		//globalEventScheduler = getEventScheduler();
        jmxConfiguration = new JMXConfiguration(appia.getManagementMBeanID());
        try {
            if(threadFactory != null)
                appia.setThreadFactory(threadFactory);
        } catch (AppiaException e) {
            throw new AppiaXMLException("Could not create XML Configuration.",e);
        }
	}
	
	/*
	 * Builds an empty configuration.
	 *
	 */
	/*public Configuration(Appia appia, boolean multiSchedulers) {
		templates = new Hashtable();
		channels = new Hashtable();
		globalSessions = new Hashtable();
		labelSessions = new Hashtable();
		//sharedSessions = new Hashtable();
		this.appia = appia;
		if (!multiSchedulers)
			// Behavior by default: one global scheduler
			globalEventScheduler = new EventScheduler(appia);
		else
			// One scheduler per channel except when shared sessions
			// exist. In this case the scheduler is shared between
			// the channels
			this.multiSchedulers = true;
	}*/
	
	/**
	 * <p>Defines the type of event scheduler attribution.</p>
	 * <p>If multiSchedulers is false, use a global event scheduler.</p>
	 * <p>If multiSchedulers is true, use multiple schedulers.</p>
	 * 
	 * @param multiSchedulers true if using multi schedulers.
	 */
	public void useMultiSchedulers(boolean multiSchedulers) {
		this.multiSchedulers = multiSchedulers;
		if (multiSchedulers)
			globalEventScheduler = null;
	}
	
	/**
	 * Tests if configuration is using a global EventScheduler.
	 * 
	 * @return true if using a global EventScheduler. False otherwise.
	 */
	public boolean usesGlobalScheduler() {
		return !multiSchedulers;
	}
	
    /**
     * TODO: remove some exceptions!!!
     * @param className
     * @throws ClassNotFoundException
     * @throws SecurityException
     * @throws NoSuchMethodException
     * @throws IllegalArgumentException
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     */
	public void setEventScheduler(String className) 
	throws ClassNotFoundException, SecurityException, NoSuchMethodException, IllegalArgumentException, InstantiationException, IllegalAccessException, InvocationTargetException {
		eventSchedulerClass = Class.forName(className);
		esWithAppiaConstructor = eventSchedulerClass.getDeclaredConstructor(new Class[] {Appia.class});
		if (globalEventScheduler != null) {
			if  (appia == null)
				globalEventScheduler = (EventScheduler) eventSchedulerClass.newInstance();
			else
				globalEventScheduler = (EventScheduler) esWithAppiaConstructor.newInstance(new Object[] {appia});
		}
		schedulerClassIsSet = true;
	}
    
    public void setThreadFactory(String className) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        threadFactory = (ThreadFactory) Class.forName(className).newInstance();
    }    

	private EventScheduler getEventScheduler() throws AppiaXMLException {
		if (globalEventScheduler != null)
			return globalEventScheduler;
		else if (appia == null)
			if (schedulerClassIsSet) {
				try {
					return (EventScheduler) eventSchedulerClass.newInstance();
				} catch (InstantiationException e) {
                    throw new AppiaXMLException("Unable to create event scheduler instance of type:"+eventSchedulerClass.getName(),e);
				} catch (IllegalAccessException e) {
                    throw new AppiaXMLException("Unable to create event scheduler instance of type:"+eventSchedulerClass.getName(),e);
				}
			}
			else
				return new EventScheduler();
		else {
			if (schedulerClassIsSet) {
				try {
					return (EventScheduler) esWithAppiaConstructor.newInstance(new Object[] {appia});
				} catch (IllegalArgumentException e1) {
                    throw new AppiaXMLException("Unable to create event scheduler instance of type:"+esWithAppiaConstructor.getName(),e1);
				} catch (InstantiationException e1) {
                    throw new AppiaXMLException("Unable to create event scheduler instance of type:"+esWithAppiaConstructor.getName(),e1);
				} catch (IllegalAccessException e1) {
                    throw new AppiaXMLException("Unable to create event scheduler instance of type:"+esWithAppiaConstructor.getName(),e1);
				} catch (InvocationTargetException e1) {
                    throw new AppiaXMLException("Unable to create event scheduler instance of type:"+esWithAppiaConstructor.getName(),e1);
				}
			}
			else
				return new EventScheduler(appia);
		}
	}
	
	/**
	 * Adds a new template to the configuration.
	 * 
	 * @param name 	name of the template.
	 */
	public void addTemplate(String name) {
		currentChTemplate = new ChannelTemplate(name);
		templates.put(name,currentChTemplate);
	}
	
	/**
	 * Adds a new session to the current template.
	 * 
	 * @param name 		the name of the session.
	 * @param sharing 	the kind of sharing.
	 * @see 			SharingState
	 */
	public void addSession(String name, int sharing) {
		currentSessionName = name;
		currentSessionSharingState = sharing;
	}
	
	/**
	 * Associates a protocol to the last session added.
	 * 
	 * @param 	protocol 				the String description of the protocol.
	 * @throws 	ClassNotFoundException
	 * @throws 	InstantiationException
	 * @throws	IllegalAccessException
	 */
	public void setProtocol(String protocol) 
	throws ClassNotFoundException, InstantiationException, IllegalAccessException {
		Class protocolClass = null;
		protocolClass = Class.forName(protocol);
		Layer layerInstance = null;
		layerInstance = (Layer)protocolClass.newInstance();
		currentChTemplate.addSession(
				currentSessionName,
				currentSessionSharingState,
				layerInstance);
	}

    /**
     * Gets the instance of the JMX configuration.
     * @return the JMX configuration.
     */
    public JMXConfiguration getJMXConfiguration(){
        return jmxConfiguration;
    }

	// Holds the channel information for each channel to be created
	private LinkedList channelList = new LinkedList();
	
	// Each entry holds references to channel information of each
	// channel that shares the session associated with the entry
	// NOTE: each entry contains a LinkedList with the information
	//		 associated with each channel
	private Hashtable sessionChannels = new Hashtable();
	
	/**
	 * Stores all the information of a channel. The channel instance can 
	 * be created later with the createChannels() method.
	 * 
	 * @param 	name			the channel name.
	 * @param	templateName	the template name.
	 * @param	label 			the String label associated with the channel.
	 * @param	params 			the parameters to be passed to their respective sessions.
	 * @param	initialized 	whether or not the channel is returned initialized.
	 */
	public void storeChannel(String name,
			String templateName,
			String label,
			ChannelProperties params,
			boolean initialized,
			MemoryManager mm,
            boolean managed,
            String msgFact) {
		ChannelInfo cinfo = null;
		if(mm == null)
			cinfo = new ChannelInfo(name,templateName,label,params,initialized,msgFact);
		else
			cinfo = new ChannelInfo(name,templateName,label,params,initialized,mm, msgFact);
        cinfo.setManaged(managed);
		channelList.add(cinfo);
		final ChannelTemplate ctempl = (ChannelTemplate) templates.get(templateName);
		final LinkedList stls = ctempl.getSessionTemplates();
		// For every session in the template
		for (int i = 0; i < stls.size(); i++) {
			final SessionTemplate stempl = (SessionTemplate) stls.get(i);
			final String sname = stempl.getName();
			final int sharing = stempl.getSharingState();
			// Check if the session is sharable (global or label)
			if (sharing == SharingState.GLOBAL || sharing == SharingState.LABEL) {
				// Build a unique identifier associated with the session instance
				final String storeName = sharing == SharingState.LABEL ? sname+label : sname;
				if (!sessionChannels.containsKey(storeName))
					// Session is not referenced yet. Create an entry!
					sessionChannels.put(storeName,new LinkedList());
				else {
					// Session is already referenced. Build dependencies!
					final LinkedList clist = (LinkedList)sessionChannels.get(storeName);
					for (int j = 0; j < clist.size(); j++) {
						final ChannelInfo aux = (ChannelInfo) clist.get(j);
						aux.addDependency(cinfo);
						cinfo.addDependency(aux);
					}
				}
				// Add channel info to the entry associated with the session
				final LinkedList clist = (LinkedList)sessionChannels.get(storeName);
				clist.add(cinfo);
			}
		}
	}
	
	// This list holds the channel information transfered from the old list
	private LinkedList newChannelList = new LinkedList();
	
	/**
	 * Proccesses dependencies between channels, associating with each 
	 * channel the respective EventScheduler.
	 * @throws AppiaXMLException 
	 *
	 */
	private void proccessDependencies() throws AppiaXMLException {
		// Proccess each channel in the list
		while (!channelList.isEmpty()) {
			final ChannelInfo cinfo = (ChannelInfo) channelList.removeFirst();
			if (newChannelList.isEmpty())
				// No channels proccessed yet. No dependencies can be checked.
				// Create new EventScheduler for this channel
				cinfo.setEventScheduler(getEventScheduler());
				//if (appia == null)
					//cinfo.setEventScheduler(new EventScheduler);
				//else
					//cinfo.setEventScheduler(new EventScheduler(appia));
			else {
				// Check dependencias with already proccessed channels
				for (int i = 0; i < newChannelList.size(); i++) {
					final ChannelInfo aux = (ChannelInfo) newChannelList.get(i);
					if (cinfo.depends(aux)) {
						// Current channel depends from aux.
						if (aux.getEventScheduler() != null)
							// Aux already has a EventScheduler. Use it!
							cinfo.setEventScheduler(aux.getEventScheduler());
						else {
							cinfo.setEventScheduler(getEventScheduler());
							//if (appia == null)
							//	cinfo.setEventScheduler(new EventScheduler());
							//else
							//	cinfo.setEventScheduler(new EventScheduler(appia));
						}
					}
				}
			}
			if (cinfo.getEventScheduler() == null) {
				// Channel has no dependencies -> create a new EventScheduler
				//if (appia == null)
				//	cinfo.setEventScheduler(new EventScheduler());
				//else
				//	cinfo.setEventScheduler(new EventScheduler(appia));
				cinfo.setEventScheduler(getEventScheduler());
			}
			newChannelList.add(cinfo);
		}
	}
	
	
	/**
	 * Creates the channels stored in memory by the storeChannel() method.
	 * 
	 * @throws AppiaXMLException
	 */
	public void createChannels() throws AppiaXMLException {
		proccessDependencies();
		ChannelInfo ci;
		while (!newChannelList.isEmpty()) {
			ci = (ChannelInfo) newChannelList.removeFirst();
			createChannel(ci.getName(),ci.getTemplateName(),ci.getLabel(),
					ci.getParams(),ci.isInitialized(),ci.getEventScheduler(),ci.getMemoryManager(),
                    (ci.isManaged()? jmxConfiguration : null),ci.getMessageFactory());
		}
	}
	
	
	/**
	 * Creates a channel based on a chosen template with the given parameters.
	 * 
	 * @param	name 			the channel name.
	 * @param	templateName	the template name.
	 * @param	label 			the String label associated with the channel.
	 * @param	params 			the parameters to be passed to their respective sessions.
	 * @param	initialized 	whether or not the channel is returned initialized.
	 * @return 					the Channel created.
	 * @throws	AppiaException
	 * @see 					ChannelProperties
	 * 
	 */
	public Channel createChannel(String name,
			String templateName,
			String label,
			ChannelProperties params,
			boolean initialized, 
			MemoryManager mm, 
            boolean managed,
            String msgFactory) 
	throws AppiaXMLException {
        final ChannelTemplate chnt = ((ChannelTemplate) templates.get(templateName));
        if (chnt == null)
            throw new AppiaXMLException("Template '"+templateName+"' does not exist");
        final Channel chn = chnt.createChannel(name,label,params,globalSessions,labelSessions,globalEventScheduler,
                mm,(managed? jmxConfiguration : null));
		if(msgFactory != null && !msgFactory.equals("")){
		    try {
		        chn.setMessageFactory((MessageFactory) Class.forName(msgFactory).newInstance());
		    } catch (InstantiationException e1) {
		        throw new AppiaXMLException("Could not instantiate the message factory", e1);
		    } catch (IllegalAccessException e1) {
		        throw new AppiaXMLException("Could not instantiate the message factory", e1);
		    } catch (ClassNotFoundException e1) {
		        throw new AppiaXMLException("Could not instantiate the message factory", e1);
		    }
		}
		channels.put(name,chn);
		// Should the channel be returned already "started"?
		if (initialized){
            try {
                chn.start();
            } catch (AppiaDuplicatedSessionsException e) {
                throw new AppiaXMLException("Unable to start the channel: "+chn.getChannelID(),e);
            }            
        }
		return chn;
	}

	/**
	 * Creates a channel based on a chosen template with the given parameters.
	 * <br>
	 * This method should only be used in channels to be associated with a given EventScheduler.
	 * 
	 * @param	name 			the channel name.
	 * @param	templateName	the template name.
	 * @param	label 			the String label associated with the channel.
	 * @param	params 			the parameters to be passed to their respective sessions.
	 * @param	initialized 	whether or not the channel is returned initialized.
	 * @param 	eventScheduler	the EventScheduler to use in the channel.
	 * @return 					the Channel created.
	 * @throws	AppiaException
	 * @see 					ChannelProperties
	 * 
	 */
	public Channel createChannel(String name,
			String templateName,
			String label,
			ChannelProperties params,
			boolean initialized,
			EventScheduler eventScheduler,
			MemoryManager mm,
            JMXConfiguration jmxConfig,
            String msgFactory)
	throws AppiaXMLException {
		final ChannelTemplate chnt = ((ChannelTemplate) templates.get(templateName));
        if (chnt == null)
            throw new AppiaXMLException("Template '"+templateName+"' does not exist");
		Channel chn = chnt.createChannel(name,label,params,globalSessions,labelSessions,eventScheduler,mm,jmxConfig);
            if(msgFactory != null && !msgFactory.equals("")){
                try {
                    chn.setMessageFactory((MessageFactory) Class.forName(msgFactory).newInstance());
                } catch (InstantiationException e1) {
                    throw new AppiaXMLException("Could not instantiate the message factory", e1);
                } catch (IllegalAccessException e1) {
                    throw new AppiaXMLException("Could not instantiate the message factory", e1);
                } catch (ClassNotFoundException e1) {
                    throw new AppiaXMLException("Could not instantiate the message factory", e1);
                }
            }

		channels.put(name,chn);
		// Should the channel be returned already "started"?
		if (initialized){
            try {
                chn.start();
            } catch (AppiaDuplicatedSessionsException e) {
                throw new AppiaXMLException("Unable to start the channel: "+chn.getChannelID(),e);
            }
        }
		return chn;
	}

	/**
	 * Returns a chosen Channel based on its name.
	 * 
	 * @param 	name 	the channel's name.
	 * @return 			the requested Channel or null if it doesn't exist.
	 */
	public Channel getChannel(String name) {
		return (Channel) channels.get(name);
	}
	
	/**
	 * Returns the list of created channels..
	 * 
	 * @return 			array containing all the channels created.
	 */
	public Channel[] getChannelList() {
		final Object[] aux = channels.values().toArray();
		final Channel[] chl = new Channel[aux.length];
		for (int i = 0; i < aux.length; i++)
			chl[i] = (Channel) aux[i];
		return chl;
	}
	
	/**
	 * <b>FOR TESTING PURPOSES ONLY!</b>
	 */
	public void printConfig() {
		// show templates
		for (final Enumeration e = templates.elements(); e.hasMoreElements();)
			((ChannelTemplate)e.nextElement()).printChannelTemplate();
		// TODO show channels
	}
}
