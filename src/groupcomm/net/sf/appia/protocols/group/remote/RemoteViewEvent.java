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

import java.net.SocketAddress;

import net.sf.appia.core.*;
import net.sf.appia.protocols.group.*;
import net.sf.appia.protocols.group.heal.GossipOutEvent;


/**
 * This event is used to request a list of addresses for a given group.
 * On reply, the message carried by this event contains the addresses of the
 * members of the group specified, as well as the group's id.
 *
 * @see net.sf.appia.core.message.Message 
 */
public class RemoteViewEvent extends GossipOutEvent {
	private Group group;
	private SocketAddress[] addresses;
	
	/**
	 * The simple constructor.
	 */
	public RemoteViewEvent() {
		super();	
	}
	
	/**
	 * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
	 * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
	 * @param source the {@link net.sf.appia.core.Session Session} that is generating 
	 *               the Event
	 */
	public RemoteViewEvent(Channel channel, int dir, Session source)
	throws AppiaEventException {
		super(channel,dir,source);	
	}
	
	/**
	 * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
	 * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
	 * @param source the {@link net.sf.appia.core.Session Session} that is generating 
	 *               the Event
	 * @param g the {@link net.sf.appia.protocols.group.Group} that will be given
	 *          member information.
	 */
	public RemoteViewEvent(Channel channel, int dir, Session source, Group g)
	throws AppiaEventException {
		super(channel,dir,source);
		group=g;
	}
	
	/**
	 * @return returns the Group ID
	 */
	public Group getGroup(){
		return group;
	}
	
	/**
	 * 
	 * @param g The group to be used in the message
	 */
	public void setGroup(Group g){
		group=g;
	}
	
	/**
	 * @return An array with the addresses of the group members.
	 */
	public SocketAddress[] getAddresses(){
		return addresses;	
	}
	
	/**
	 * @param addrs  An array with the addresses of the group members.
	 */
	public void setAddresses(SocketAddress addrs[]){
		addresses=addrs;
	}
}
