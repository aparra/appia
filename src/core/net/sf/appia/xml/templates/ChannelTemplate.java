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
package net.sf.appia.xml.templates;

import java.util.Hashtable;
import java.util.LinkedList;

import net.sf.appia.core.AppiaCursorException;
import net.sf.appia.core.AppiaInvalidQoSException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.ChannelCursor;
import net.sf.appia.core.EventScheduler;
import net.sf.appia.core.Layer;
import net.sf.appia.core.QoS;
import net.sf.appia.core.Session;
import net.sf.appia.core.memoryManager.MemoryManager;
import net.sf.appia.management.jmx.JMXConfiguration;
import net.sf.appia.xml.AppiaXMLException;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.ChannelProperties;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.appia.xml.utils.SharingState;



/**
 * This class implements a channel template. It is used to generate one or more
 * channels that have identical QoS.
 * 
 * @author Jose Mocito
 *
 */
public class ChannelTemplate {
	
		// Template name
		private String name;
		// Session templates
		private LinkedList sessionTemplates;
		
		/**
		 * Builds a channel template.
		 * 
		 * @param name the template's name.
		 */
		public ChannelTemplate(String name) {
			this.name = name;
			sessionTemplates = new LinkedList();
		}
		
		/**
		 * @return Returns the name.
		 */
		public String getName() {
			return name;
		}
				
		/**
		 * @return Returns the sessionTemplates.
		 */
		public LinkedList getSessionTemplates() {
			return sessionTemplates;
		}
		
		/**
		 * Adds a session to the channel template.
		 * <p>
		 * First session added corresponds to the bottom most layer.
		 * 
		 * @param name the name of the session.
		 * @param sharing the sharing property of the session.
		 * @param layer the layer associated with the session.
		 * @see SharingState
		 */
		public void addSession(String name, int sharing, Layer layer) {
			sessionTemplates.add(new SessionTemplate(name,sharing,layer));
		}
		
		/**
		 * <p>Returns the number of layers defined in this template.</p>
		 * 
		 * @return the number of layers defined in this template.
		 */
		public int numberOfLayers() {
			return sessionTemplates.size();
		}

		/**
		 * Creates a channel.
		 * <p>
		 * Channel returned is not initialized!
		 * 
		 * @param name the name of the channel.
		 * @param label the label of the channel or null if none is defined.
		 * @param params the parameters passed to the channel.
		 * @param globalSessions Hashtable containing the 
		 * 	shared "global sessions".
		 * @param labelSessions Hashtable containing the
		 * 	shared "label sessions".
		 * @return the channel created.
		 * @throws AppiaException
		 */
		/*public Channel createChannel(
				String name,
				String label,
				ChannelProperties params,
				Hashtable globalSessions,
				Hashtable labelSessions)
		throws AppiaException {
			return createChannel(name,label,params,globalSessions,labelSessions,null);
		}*/
		
		/**
		 * Creates a channel.
		 * <p>
		 * Channel returned is not initialized!
		 * 
		 * @param name the name of the channel.
		 * @param label the label of the channel or null if none is defined.
		 * @param params the parameters passed to the channel.
		 * @param globalSessions Hashtable containing the 
		 * 	shared "global sessions".
		 * @param labelSessions Hashtable containing the
		 * 	shared "label sessions".
		 * @param eventScheduler the EventScheduler associated with the channel.
		 * @return the channel created.
		 * @throws AppiaXMLException
		 */
		public Channel createChannel(
				String name,
				String label,
				ChannelProperties params,
				Hashtable globalSessions,
				Hashtable labelSessions,
				EventScheduler eventScheduler, 
                MemoryManager memoryManager,
                JMXConfiguration jmxConfig) 
		throws AppiaXMLException {
			// Complete name is equal to the given name plus the template name
			//String completeName = name + " " + this.name;
			final int numberOfSessions = sessionTemplates.size();
			final Layer[] qosList = new Layer[numberOfSessions];
			SessionTemplate currSession = null;
			// Generates the QoS
			for (int i = 0; i < numberOfSessions; i++) {
				currSession = (SessionTemplate) sessionTemplates.get(i);
				qosList[i] = currSession.layerInstance();
			}
			QoS qos = null;
			try {
                qos = new QoS(name+" QoS",qosList);
            } catch (AppiaInvalidQoSException e) {
                throw new AppiaXMLException("Unable to create QoS: "+name+" QoS",e);
            }
			// Creates the channel based on the QoS
			Channel channel;
			if (eventScheduler == null && memoryManager == null)
				channel = qos.createUnboundChannel(name,jmxConfig);
			else if (eventScheduler == null && memoryManager != null)
				channel = qos.createUnboundChannel(name,memoryManager,jmxConfig);
			else if (eventScheduler != null && memoryManager == null)
				channel = qos.createUnboundChannel(name,eventScheduler,jmxConfig);
			else
				channel = qos.createUnboundChannel(name,eventScheduler,memoryManager,jmxConfig);
			final ChannelCursor cc = channel.getCursor();
			cc.bottom();
			// Associates the sessions to their corresponding layers
			for (int i = 0; i < numberOfSessions; i++) {
				currSession = (SessionTemplate) sessionTemplates.get(i);
				Session sessionInstance = null;
				// if "global session" then use only global sessions table.
				if (currSession.getSharingState() == SharingState.GLOBAL)
					sessionInstance = currSession.sessionInstance(label,globalSessions);
				// else (is label or private) use only label sessions table.
				else
					sessionInstance = currSession.sessionInstance(label,labelSessions);
				// Verifies if the session accepts parameters and if
				// they are present in the configuration passes them to 
				// the session.
				if (sessionInstance instanceof InitializableSession &&
						params != null &&
						params.containsKey(currSession.getName())) {
					final SessionProperties parameters =
						params.getParams(currSession.getName());
					((InitializableSession)sessionInstance).init(parameters);
				}
				try {
                    cc.setSession(sessionInstance);
                } catch (AppiaCursorException e) {
                    throw new AppiaXMLException("Unable to set the session "+sessionInstance+
                            " on channel " + channel.getChannelID()+": "+e.getMessage(),e);
                }
                try {
                    cc.up();
                } catch (AppiaCursorException e) {
                    throw new AppiaXMLException("Unable to move the cursor up, on channel " + channel.getChannelID()+".",e);
                }
			}
			return channel;
		}
		
		/**
		 * <b>FOR TESTING PURPOSES ONLY!</b>
		 */
		public void printChannelTemplate() {
			final Object [] staux = sessionTemplates.toArray();
			final SessionTemplate[] st = new SessionTemplate[staux.length];
			for (int i = 0; i < staux.length; i++)
				st[i] = (SessionTemplate) staux[i];
			
			System.out.println("Template Name: "+name);
			for (int i = 0; i < sessionTemplates.size(); i++)
				st[i].printSessionTemplate();
		}
}