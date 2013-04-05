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
 
package net.sf.appia.protocols.group.events;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.group.AppiaGroupException;
import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.ViewState;




/**
 * {@link net.sf.appia.core.Event Event} that initializes the
 * <i>Group Communication protocols</i>.
 * <br>
 * In reality the several <i>Group Communication protocols</i> will only start
 * operating upon receiving the first
 * {@link net.sf.appia.protocols.group.intra.View View event}, but the
 * <i>GroupInit</i> is required to create that first <i>view</i>.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.intra.View
 */
public class GroupInit extends Event {

  private ViewState vs;
  private int rank = -1;
  private SocketAddress ip_multicast;
  private SocketAddress[] gossip;
  
  private ViewState baseVS;
  private Endpt endpt;
  private Group group;
  private boolean generateIPmulticast=false;
  private SocketAddress localAddress;
 
  /**
   * Creates an initialized <i>GroupInit</i>.
   *
   * @param vs the initial <i>view</i>
   * @param endpt the {@link net.sf.appia.protocols.group.Endpt Endpt} of the member
   * @param ipMulticast the <i>IP multicast</i> address, or <b>null</b>
   * @param ipGossip the IP of the gossip service
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @param source the {@link net.sf.appia.core.Session Session} that is generating the Event
   * @throws AppiaEventException as the result of calling
   * {@link net.sf.appia.core.Event#Event(Channel,int,Session)
   * Event(Channel,Direction,Session)}
   */
  public GroupInit(
          ViewState vs,
          Endpt endpt,
          SocketAddress ipMulticast,
          SocketAddress[] ipGossip,
          Channel channel, int dir, Session source)
    throws AppiaEventException,NullPointerException,AppiaGroupException {

    super(channel,dir,source);

    if ((vs == null) || (endpt == null))
       throw new NullPointerException("appia:group:GroupInit: view state or endpoint not given");

    this.vs=vs;
    if ((rank=vs.getRank(endpt)) < 0)
       throw new AppiaGroupException("GroupInit: endpoint given doesn't belong to view");
    this.ip_multicast=ipMulticast;
    this.gossip=ipGossip;
  }
  
  
  /**
   * Creates a new GroupInit.
   * 
   * @param group_name the group name.
   * @param localAddress the local address. 
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @param source the {@link net.sf.appia.core.Session Session} that is generating the Event
   * @throws AppiaEventException as the result of calling
   * {@link net.sf.appia.core.Event#Event(Channel,int,Session)
   * Event(Channel,Direction,Session)}
   */
  public GroupInit(String group_name, SocketAddress localAddress, Channel channel, int dir, Session source) throws AppiaEventException {
  	super(channel,dir,source);
  	
  	if ((group_name == null) || (group_name.length() == 0))
  		throw new IllegalArgumentException("Illegal group name: "+group_name);
  	this.group=new Group(group_name);
  	
  	if (localAddress == null)
  		throw new IllegalArgumentException("Illegal local address: "+localAddress);
  	this.localAddress=localAddress;
  }
  
  /**
   * Returns the initial view.<br>
   * If none was given, a default view with a single member will be generated.
   * 
   * @return the initial view. 
   */
  public ViewState getVS() {
  	if (vs == null)
  		generateVS();
  	return vs;
  }

	/**
	 * Sets the initial view.
	 * 
   * @param vs The initial view.
   */
  public void setVS(ViewState vs) {
  	this.vs = vs;
  	if (endpt != null) {
  		rank=vs.getRank(endpt);
  	} else if (rank >= 0) {
  		endpt=vs.view[rank];
  	}
  }

	/**
	 * Returns the local endpoint identifier.<br>
	 * If none was given, a new one will be generated.
	 * 
   * @return the local endpoint identifier.
   */
  public Endpt getEndpt() {
  	if (endpt == null) {
  		if (vs == null) {
  			generateVS();
  		} else if (rank >= 0) {
  			endpt=vs.view[rank];
  		}
  	}
  	return endpt;
  }

	/**
	 * Sets the local endpoint identifier.
	 * 
   * @param endpt the local endpoint identifier.
   */
  public void setEndpt(Endpt endpt) {
  	this.endpt = endpt;
  	if (vs != null)
  		rank=vs.getRank(endpt);
  }

  /**
   * Returns the gossip servers addresses list.
   * 
   * @return the gossip servers addresses list.
   */
  public SocketAddress[] getGossip() {
  	return gossip;
  }

	/**
	 * Sets the gossip servers addresses list.
	 * 
   * @param gossip the gossip servers addresses list to set.
   */
  public void setGossip(SocketAddress[] gossip) {
  	this.gossip = gossip;
  }

	/**
	 * Returns the IP-Multicast address to use in group communication.
	 * 
   * @return the IP-Multicast address.
   */
  public SocketAddress getIPmulticast() {
  	if ((ip_multicast == null) && generateIPmulticast) {
  		if (vs == null)
  			generateVS();
  		generateMulticast();
  	}
  	return ip_multicast;
  }

	/**
	 * Sets the IP-Multicast address to use in group communication.
	 * 
   * @param ip_multicast the IP-Multicast address
   */
  public void setIPmulticast(SocketAddress ip_multicast) {
  	this.ip_multicast = ip_multicast;
  	generateIPmulticast=false;
  }
  
	/**
	 * Returns true if an IP-Multicast address should be generated.<br>
	 * The generated address is based on the group identification.<br>
	 * <b>Default is false</b>.
	 * 
   * @return is an IP-Multicast address to be generated. 
   */
  public boolean isGenerateIPmulticast() {
  	return generateIPmulticast;
  }

	/**
	 * Sets whether an IP-Multicast address should be generated.<br>
	 * The generated address is based on the group identification.
	 * 
   * @param generateIPmulticast is an IP-Multicast address to be generated.
   */
  public void setGenerateIPmulticast(boolean generateIPmulticast) {
  	this.generateIPmulticast = generateIPmulticast;
  }

	public ViewState getBaseVS() {
      return baseVS;
  }

  public void setBaseVS(ViewState baseVS) {
      this.baseVS = baseVS;
  }

	private void generateVS() {
		if (endpt == null) {
			if ((rank >= 0) && (vs != null))
				endpt=vs.view[rank];
			else {
				InetSocketAddress addr=(InetSocketAddress) localAddress;
				endpt=new Endpt("Endpt@"+addr.getAddress().getHostAddress()+":"+addr.getPort()+":"+System.currentTimeMillis());
			}
		}
		try {
	    vs=new ViewState(
	    		"3",
	    		group,
	    		new ViewID(0,endpt),
	    		new ViewID[0],
	    		new Endpt[] {endpt}, 
	    		new SocketAddress[] {localAddress});
    } catch (NullPointerException e) {
	    e.printStackTrace();
    } catch (AppiaGroupException e) {
	    e.printStackTrace();
    }

    rank=0;
  }

	private void generateMulticast() {
		int groupHash=vs.group.hashCode();
		byte[] addr=new byte [4];
		addr[0]=(byte) 224;
		addr[1]=(byte) ((groupHash >> 24) & 0xFF);
		addr[2]=(byte) ((groupHash >> 16) & 0xFF);
		addr[3]=(byte) ((groupHash >>  8) & 0xFF);
		int port=1024+(groupHash & 0x7FFF);
		try {
      ip_multicast=new InetSocketAddress(InetAddress.getByAddress(addr),port);
    } catch (UnknownHostException e) {
      e.printStackTrace();
    }
  }
}
