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

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.protocols.group.events.GroupEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.jgcs.ClosedSessionException;
import net.sf.jgcs.JGCSException;
import net.sf.jgcs.NotJoinedException;
import net.sf.appia.jgcs.protocols.top.JGCSJoinEvent;
import net.sf.appia.jgcs.protocols.top.JGCSLeaveEvent;
import net.sf.appia.jgcs.protocols.top.JGCSReleaseBlock;
import net.sf.jgcs.membership.AbstractBlockSession;
import net.sf.jgcs.membership.Membership;
import net.sf.jgcs.membership.MembershipID;

import org.apache.log4j.Logger;

public class AppiaControlSession extends AbstractBlockSession {

	private static Logger logger = Logger.getLogger(AppiaControlSession.class);

	// View state
	private Object membershipSync = new Object();
	//private Membership membership;
	//Appia
	private List<Channel> appiaChannels;
	private AppiaGroup group;
	private CountDownLatch leaveLatch;

	public AppiaControlSession(AppiaGroup g, List<Channel> channels) {
		super();
		//Appia
		group = g;
		appiaChannels = channels;
	}

	public void blockOk() throws NotJoinedException, JGCSException{
		if(!isJoined())
			throw new NotJoinedException();
		AppiaMembership am = (AppiaMembership) getMembership();
		if(am.blockEvent != null) {
			JGCSReleaseBlock releaseBlock = new JGCSReleaseBlock();
			releaseBlock.setBlockEvent(am.blockEvent);
			try {
				releaseBlock.asyncGo(am.blockEvent.getChannel(),Direction.DOWN);
			} catch (AppiaEventException e) {
				throw new JGCSException("Error releasing block appia event.",e);
			}
			logger.debug("Block released to the Appia channel");
			am.blockEvent = null;
		}
	}

	
	public boolean isBlocked() throws NotJoinedException{
		if(!isJoined())
			throw new NotJoinedException();
		return ((AppiaMembership)getMembership()).isBlocked;
	}

	public void join() throws ClosedSessionException, JGCSException{
		if(!hasAllListeners())
			throw new JGCSException("Must register the missing listeners."+
					" Make sure that you have at least one listener of the membership and another for the block notification.");
		
		JGCSJoinEvent joinEvent = new JGCSJoinEvent();
		joinEvent.setGroupName(group.getGroupName());
		try {
			joinEvent.asyncGo(appiaChannels.get(0),Direction.DOWN);
		} catch (AppiaEventException e) {
			if(e.type == AppiaEventException.CLOSEDCHANNEL)
				throw new ClosedSessionException("Session is closed.",e);
			else
				throw new JGCSException("Error sending Join event.",e);
		}
		synchronized (membershipSync) {
			while(!isJoined())
				try {
					membershipSync.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
	}

	public void leave() throws ClosedSessionException, JGCSException{
		if(logger.isDebugEnabled())
			logger.debug("Leaving from Appia Session... [ControlSession.leave()]");
		JGCSLeaveEvent leaveEvent = new JGCSLeaveEvent();
		leaveLatch = new CountDownLatch(1);
		leaveEvent.setLatch(leaveLatch);
		try {
			leaveEvent.asyncGo(appiaChannels.get(0),Direction.DOWN);
		} catch (AppiaEventException e) {
			if(e.type == AppiaEventException.CLOSEDCHANNEL)
				throw new ClosedSessionException("Channel is closed.",e);
			else
				throw new JGCSException("Error sending Leave event.",e);
		}
		
		try {
			leaveLatch.await();
		} catch (InterruptedException e) {
			throw new JGCSException("Error leaving the group.",e);
		}
		leaveLatch = null;		
		for(Channel ch : appiaChannels)
			ch.end();
        setMembership(null);
        System.out.println("LEAVE end");
	}

	public SocketAddress getLocalAddress() {
		synchronized (membershipSync) {
			while(!isJoined())
				try {
					membershipSync.wait();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		}
		AppiaMembership am = null;
		try {
			am = (AppiaMembership) getMembership();
		} catch (NotJoinedException e1) {
			// verified in isJoined()
		}	
		return am.addresses.get(am.myRank);
	}
		
	
	/**
     * 
     * @see net.sf.jgcs.membership.AbstractMembershipSession#getMembership()
     */
    @Override
    public synchronized Membership getMembership() throws NotJoinedException {
        return super.getMembership();
    }

    /**
     * 
     * @see net.sf.jgcs.membership.AbstractMembershipSession#setMembership(net.sf.jgcs.membership.Membership)
     */
    @Override
    protected synchronized void setMembership(Membership m) {
        super.setMembership(m);
    }

    /*
	 * This method is used by the thread that is waiting  for messages on the mailbox
	 * and is called by this thread to notify the controlListener.
	 * The Listener List is synchronized.
	 * @param event Appia event
	 */
	void notifyListeners(GroupEvent event){
		AppiaMembership currentMembership = null;
		try {
			currentMembership = (AppiaMembership) getMembership();
		} catch (NotJoinedException e1) {
			logger.debug("Exception on notifyListeners: "+e1);
			if(!(event instanceof View))
				return;
		}	
		
		if(event instanceof BlockOk){
			if(logger.isDebugEnabled())
				logger.debug("Received block event.");
			currentMembership.isBlocked = true;
			currentMembership.blockEvent = (BlockOk) event;
			this.notifyBlock();

		}
		else if (event instanceof View){
			AppiaMembership incomingMembership = null;
			if(currentMembership == null){
				if(logger.isDebugEnabled())
					logger.debug("Processing first view.");
				// if this is the first view
				incomingMembership = new AppiaMembership((View) event);
				incomingMembership.setNewMembers(null);
			}
			else{
				if(logger.isDebugEnabled())
					logger.debug("Processing next view.");
				incomingMembership =new AppiaMembership((View) event);
				incomingMembership.setNewMembers(currentMembership);
				incomingMembership.setOldAndFailedMembers(currentMembership);
			}
			
			setMembership(incomingMembership);
			synchronized (membershipSync) {
				membershipSync.notifyAll();
			}
			
			// notify and set the new membership
			this.notifyAndSetMembership(incomingMembership);
			// notify joined (control session)
			if(incomingMembership.getJoinedMembers() != null)
				for(SocketAddress peer : incomingMembership.getJoinedMembers())
					this.notifyJoin(peer);
			// notify leaved (control session)
			if(incomingMembership.getLeavedMembers() != null)
				for(SocketAddress peer : incomingMembership.getLeavedMembers())
					this.notifyLeave(peer);
			// notify failed (control session)
			if(incomingMembership.getFailedMembers() != null)
				for(SocketAddress peer : incomingMembership.getFailedMembers())
					this.notifyFailed(peer);
			
			if(logger.isDebugEnabled())
				logger.debug("currentMembership:"+incomingMembership.addresses);
		}
		else{
			logger.debug("Received wrong GroupEvent: "+event);
		}
	}
	
	void notifyMemberRemoved(){
		this.notifyRemoved();
	}
	
	public synchronized boolean isJoined() {
		try {
			return getMembership() != null;
		} catch (NotJoinedException e) {
			return false;
		}
	}

}

/**
 * This class defines an implementation of the jGCS Membership interface, using Appia.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
class AppiaMembership implements Membership {

	List<SocketAddress> addresses;
	transient int myRank, coordinator;
	transient boolean isBlocked;
	transient BlockOk blockEvent = null;
	transient AppiaMembershipID id;
	private boolean[] failed;
	List<SocketAddress> newMembersList = null;
	List<SocketAddress> failedmembersList = null;
	List<SocketAddress> oldMembersList = null;

	public AppiaMembership(View e){
		
		addresses = new ArrayList<SocketAddress>(e.vs.addresses.length);
		for (int i=0; i<e.vs.addresses.length; i++){
			e.vs.addresses[i].toString();
			addresses.add(e.vs.addresses[i]);
		}
		myRank = e.ls.my_rank;
		coordinator = e.ls.coord;
		isBlocked = false;
		id = new AppiaMembershipID(e.vs.id.ltime,e.vs.id.coord);
		failed = e.ls.failed;
	}
	
	/**
	 * Gets the failed members on the current view
	 * @return
	 */
	public List<SocketAddress> getFailed(){
		final List<SocketAddress> failedMembers = new ArrayList<SocketAddress>();
		for(int i=0; i<failed.length; i++)
			if(failed[i])
				failedMembers.add(addresses.get(i));
		return failedMembers;
	}

	public int getLocalRank() throws NotJoinedException {
		return myRank;
	}

	public int getCoordinatorRank() {
		return coordinator;
	}

	public int getMemberRank(SocketAddress peer) {
		return addresses.indexOf(peer);
	}

	public SocketAddress getMemberAddress(int rank) {
		return addresses.get(rank);
	}

	public List<SocketAddress> getJoinedMembers() {
		return newMembersList;
	}

	public List<SocketAddress> getLeavedMembers() {
		return oldMembersList;
	}
	
	public List<SocketAddress> getFailedMembers() {
		return failedmembersList;
	}

	protected void setNewMembers(AppiaMembership oldMembership){		
		newMembersList = new ArrayList<SocketAddress>();
		if(oldMembership == null){
			for(SocketAddress peer : this.addresses)
				newMembersList.add(peer);
		}
		else
			for(SocketAddress peer : this.addresses)
				if(!oldMembership.addresses.contains(peer))
					newMembersList.add(peer);
		if(newMembersList.isEmpty())
			newMembersList = null;
	}
	
	protected void setOldAndFailedMembers(AppiaMembership oldMembership){		
		oldMembersList = new ArrayList<SocketAddress>();
		failedmembersList = oldMembership.getFailedMembers();
		for(SocketAddress peer : oldMembership.addresses)
			if(!this.addresses.contains(peer) && (failedmembersList == null || !failedmembersList.contains(peer)))
				oldMembersList.add(peer);
		if(oldMembersList.isEmpty())
			oldMembersList = null;
		if(failedmembersList != null && failedmembersList.isEmpty())
			failedmembersList = null;
		
	}

	public MembershipID getMembershipID() {
		return id;
	}

    /**
     * Retrieves the membership list.
     * 
     * @see net.sf.jgcs.membership.Membership#getMembershipList()
     */
	public List<SocketAddress> getMembershipList() {
		return addresses;
	}

	public String toString(){
		return "Appia Membership ID "+id+" Membership "+addresses+" size "+addresses.size();
	}
}
