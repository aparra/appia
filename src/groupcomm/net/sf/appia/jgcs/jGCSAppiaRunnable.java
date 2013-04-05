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

import java.io.File;

import net.sf.appia.core.AbstractAppiaRunnable;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.ChannelCursor;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.message.MessageFactory;
import net.sf.appia.jgcs.protocols.top.SimpleTOPLayer;
import net.sf.appia.jgcs.protocols.top.SimpleTOPSession;
import net.sf.appia.jgcs.protocols.top.TOPLayer;
import net.sf.appia.jgcs.protocols.top.TOPSession;
import net.sf.appia.xml.AppiaXMLException;
import net.sf.jgcs.utils.Mailbox;

import org.apache.log4j.Logger;

public class jGCSAppiaRunnable extends AbstractAppiaRunnable {

	private Mailbox<Event> mbox;
	
	private static 
	Logger logger = Logger.getLogger(jGCSAppiaRunnable.class);
	
	public jGCSAppiaRunnable(File xmlConfig, String managementID, Mailbox<Event> mb) 
	throws AppiaXMLException {
		super(xmlConfig,managementID);
		mbox = mb;
	}

	@Override
	public void setup() throws AppiaException {
		// Verifies if the TOP Session is the session that we want
		// and if the global sharing is correct
		Channel[] channels=getAppiaXML().instanceGetChannelList();
		// There are no channels
		if(channels.length == 0)
			throw new AppiaXMLException("There are no Channels in the given configuration");
		
		MessageFactory msgFactory = new MessageFactoryImpl();
		for(int i=0; i<channels.length; i++){
			ChannelCursor cc = channels[i].getCursor();
			cc.top();
			Layer layer = cc.getLayer();
			// The TOP session is not the expected
			if( ! (layer instanceof TOPLayer) && ! (layer instanceof SimpleTOPLayer) )
					throw new AppiaXMLException("Bad configuration: TOP Layer of Channel "+
							channels[i].getChannelID()+" is "+layer.getClass().getName() +
							" and should be "+TOPLayer.class.getName()+" or "+SimpleTOPLayer.class.getName());
			if(channels[i].isStarted())
				throw new AppiaXMLException("Channels should not be started in the XML configuration file when used with jGCS.");
			channels[i].setMessageFactory(msgFactory);
			if(cc.getSession() == null){
				if(layer instanceof TOPLayer){
					TOPSession s = (TOPSession) layer.createSession();
					s.setMailbox(mbox);
					cc.setSession(s);
				}
				else if (layer instanceof SimpleTOPLayer){
					SimpleTOPSession s = (SimpleTOPSession) layer.createSession();
					s.setMailbox(mbox);
					cc.setSession(s);
				}
			}
			else {
				if(layer instanceof TOPLayer){
					TOPSession s = (TOPSession) cc.getSession();
					s.setMailbox(mbox);
				}
				else if (layer instanceof SimpleTOPLayer){
					SimpleTOPSession s = (SimpleTOPSession) cc.getSession();
					s.setMailbox(mbox);
				}
			}
			channels[i].start();
			logger.debug("Starting Channel: "+channels[i].getChannelID());
		}
	}

}
