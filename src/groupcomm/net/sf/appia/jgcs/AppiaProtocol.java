/**
 * APPIA implementation of JGCS - Group Communication Service
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
 * 
 *  * Contact
 * 	Address:
 * 		LASIGE, Departamento de Informatica, Bloco C6
 * 		Faculdade de Ciencias, Universidade de Lisboa
 * 		Campo Grande, 1749-016 Lisboa
 * 		Portugal
 * 	Email:
 * 		jgcs@lasige.di.fc.ul.pt
 */
 
package net.sf.appia.jgcs;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.xml.AppiaXMLException;
import net.sf.jgcs.AbstractControlSession;
import net.sf.jgcs.AbstractDataSession;
import net.sf.jgcs.AbstractProtocol;
import net.sf.jgcs.ControlSession;
import net.sf.jgcs.DataSession;
import net.sf.jgcs.GroupConfiguration;
import net.sf.jgcs.JGCSException;
import net.sf.jgcs.utils.Mailbox;

import org.apache.log4j.Logger;

public class AppiaProtocol extends AbstractProtocol {
	
	private static 
	Logger logger = Logger.getLogger(AppiaProtocol.class);
	
	private Map<GroupConfiguration,List<Channel>> channelList;
	
	AppiaProtocol() {
		super();
	}

	protected void boot(){
		if(channelList != null)
			return;
		channelList = new HashMap<GroupConfiguration,List<Channel>>();
		super.boot();
	}
	
	protected synchronized void putSessions(GroupConfiguration g, AbstractControlSession control, AbstractDataSession data, List<Channel> chList) {
		super.putSessions(g,control,data);
		channelList.put(g,chList);
	}

	protected synchronized void removeSessions(GroupConfiguration g) {
		super.removeSessions(g);
		channelList.remove(g);
	}

	
	/**
	 * Opens a new Appia DataSession, using the given template. If the session already exists, 
	 * returns the previously created session. Creates a new Appia instance without 
	 * opening any channel.
	 * 
	 * @param group the group configuration with the given template. 
	 */
	public DataSession openDataSession(GroupConfiguration group) throws JGCSException {
		if(group == null)
			throw new JGCSException("Null GroupConfiguration is not valid");
		DataSession data = lookupDataSession(group);
		if(data == null){
			AppiaGroup appiaGroup = null;
			if( group instanceof AppiaGroup)
				appiaGroup = (AppiaGroup) group;
			else
				throw new JGCSException("Wrong type of the given Group: "+group.getClass().getName()+
						"should be of type "+AppiaGroup.class.getName());			
			createSessions(appiaGroup);			
			data = lookupDataSession(group);
		}
		return data;
	}

	public ControlSession openControlSession(GroupConfiguration group) throws JGCSException {
		ControlSession control = lookupControlSession(group);
		if(control == null){
			AppiaGroup appiaGroup = null;
			if( group instanceof AppiaGroup)
				appiaGroup = (AppiaGroup) group;
			else
				throw new JGCSException("Wrong type of the given Group: "+group.getClass().getName()+
						"should be of type "+AppiaGroup.class.getName());			
			createSessions(appiaGroup);
			control = lookupControlSession(group);
		}
		return control;
	}
	
	private void createSessions(AppiaGroup group)
		throws JGCSException{
		logger.debug("Opening new session.");
			// create mail box
			Mailbox<Event> mbox = new Mailbox<Event>();
			// create Appia and its thread
			Channel[] channelArray = createAppia(group,mbox);
			List<Channel> chList = 
				new ArrayList<Channel>(AppiaUtils.toCollection(channelArray)); 
			// bind sessions to group
			// create sessions
			AppiaControlSession controlSession = new AppiaControlSession(group,chList);
			AppiaDataSession dataSession = 
				new AppiaDataSession(this,group,mbox, controlSession, chList);
			
			putSessions(group,controlSession,dataSession, chList);			
	}

	private Channel[] createAppia(AppiaGroup group, Mailbox<Event> mbox) throws JGCSException{
		jGCSAppiaRunnable runnable = null;
		try {
			runnable = new jGCSAppiaRunnable(group.getConfigFile(),group.getManagementMBeanID(),mbox);
		} catch (AppiaXMLException e1) {
			throw new JGCSException("Could not load configuration.",e1);
		}		
		try {
			runnable.setup();
		} catch (AppiaException e) {
			throw new JGCSException("Could not setup Appia.", e);			
		}
		Channel[] channels = runnable.getAppiaXML().instanceGetChannelList();
		Thread appiaThread = new Thread(runnable);
		appiaThread.setPriority(Thread.MAX_PRIORITY);
		appiaThread.setDaemon(true);
		appiaThread.setName("jGCS Appia Thread");
		appiaThread.start();
		return channels;
	}

}

