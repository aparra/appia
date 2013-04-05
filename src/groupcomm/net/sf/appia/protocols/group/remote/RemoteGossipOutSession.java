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
 /**
 * Title:        Appia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Nuno Carvalho and Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto and Hugo Miranda
 * @version 1.0
 */
 package net.sf.appia.protocols.group.remote;

import java.net.InetSocketAddress;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.heal.GossipOutSession;

import org.apache.log4j.Logger;


/**
 * Extends the functionality of the 
 * {@link net.sf.appia.protocols.group.heal.GossipOutSession} by giving membership
 * information to non-members of the group.
 */
public class RemoteGossipOutSession extends GossipOutSession {

    private static Logger log = Logger.getLogger(RemoteGossipOutSession.class);

	private Group group;

	/**
	 * Default Constructor.
	 * @param layer The {@link net.sf.appia.core.Layer} associated to this session.
	 */
	public RemoteGossipOutSession(Layer layer) {
		super(layer);
	}

	/**
	 * Main handler.
	 * @param event The received event.
	 */
	public void handle(Event event) {
		if (event instanceof GroupInit) {
            // gets the group and forward the event to the super class
			group = ((GroupInit) event).getVS().group;
		} else if (event instanceof RemoteViewEvent) {
			handleRemoteView((RemoteViewEvent) event);
			return;
		}
		super.handle(event);
	}

	private void handleRemoteView(RemoteViewEvent ev) {

		try {
			final InetSocketAddress sourceAddr = (InetSocketAddress) ev.getMessage().popObject(); 
			//if this process is a member of the desired group
			final Group g = Group.pop((Message)ev.getMessage());
			if(log.isDebugEnabled())
				log.debug("Received remote view event from "+sourceAddr+" with group "+g+" ("+group+")");
			if (!group.equals(g)) {
				return;
			}

			ev.source = getOutAddress();
			ev.dest = sourceAddr;

			final Message msg = new Message();
			ViewState.push(getViewState(),msg);

			if(log.isDebugEnabled())
				log.debug("Sendig RemoteView to "+sourceAddr+" : "+getViewState());
			ev.setMessage(msg);
			ev.setChannel(getOutChannel());
			ev.setDir(Direction.DOWN);
			ev.setSourceSession(this);

			ev.init();
			ev.go();

		} catch (AppiaEventException ex) {
            log.debug("Exception sending event: "+ex);
		}
	}
}
