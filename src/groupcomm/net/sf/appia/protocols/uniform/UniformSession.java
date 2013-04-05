
/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2007 University of Lisbon
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
 * Initial developer(s): Jose' Mocito.
 * Contributor(s): See Appia web page for a list of contributors.
 */
package net.sf.appia.protocols.uniform;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.TimeProvider;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.total.common.UniformServiceEvent;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;


/**
 * Protocol that ensures uniformity in messages delivered by group members.
 * 
 * @author Jose Mocito
 */
public class UniformSession extends Session implements InitializableSession{
	
	private static final long DEFAULT_UNIFORM_INFO_PERIOD = 100;
		
	private long sn,unifInfoPeriod=DEFAULT_UNIFORM_INFO_PERIOD;
	private long[][] snInfoList;

	private boolean isBlocked = true;
	
	private ViewState vs;
	private LocalState ls;
	private TimeProvider timeProvider;
	
	
	private LinkedList<MessageContainer> receivedMessages = new LinkedList<MessageContainer>();
	
	private long timeLastMsgSent;
	private boolean utSet; // Uniform timer is set?
	
	/**
	 * Constructs a new UniformSession.
	 * 
	 * @param layer the corresponding layer.
	 */
	public UniformSession(Layer layer) {
		super(layer);
	}


    public void init(SessionProperties params) {
        if(params.containsKey("uniform_info_period")){
            unifInfoPeriod = params.getLong("uniform_info_period");
        }        
    }
	
	/** 
	 * Main handler of events.
	 * @see net.sf.appia.core.Session#handle(Event)
	 */
	public void handle(Event event)  {

		if(event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if(event instanceof ChannelClose)
			handleChannelClose((ChannelClose)event);
		else if(event instanceof BlockOk)
			handleBlockOk((BlockOk)event);
		else if(event instanceof View)
			handleNewView((View)event);
		else if (event instanceof UniformInfoEvent)
			handleUniformInfo((UniformInfoEvent) event);
		else if (event instanceof UniformTimer)
			handleUniformTimer((UniformTimer) event);
		else if(event instanceof GroupSendableEvent)
			handleGroupSendable((GroupSendableEvent)event);
		else{
			try {
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}

	private void handleChannelInit(ChannelInit init) {
		timeProvider = init.getChannel().getTimeProvider();
		try {
			init.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	private void handleChannelClose(ChannelClose close) {
		try {
			close.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * The group is blocked. It is going to change view.
	 * @param ok
	 */
	private void handleBlockOk(BlockOk ok) {
		isBlocked = true;
		
		if (vs.view.length > 1)
			sendUniformInfo(ok.getChannel());
		
		try {
			ok.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	/**
	 * New view.
	 * @param view
	 */
	private void handleNewView(View view) {
		isBlocked = false;
		ls=view.ls;
		vs=view.vs;

		// resets sequence numbers and information about delays
		reset();

		try {
			view.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		
		if (!utSet && unifInfoPeriod > 0) {
			try {
				new UniformTimer(unifInfoPeriod,view.getChannel(),Direction.DOWN,this,EventQualifier.ON).go();
				utSet = true;
			} catch (AppiaEventException e) {
				e.printStackTrace();
			} catch (AppiaException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * @param event
	 */
	private void handleGroupSendable(GroupSendableEvent event) {
		final Message msg = event.getMessage();
		if(event.getDir() == Direction.DOWN) {
			msg.pushLong(sn);
			for (int i = 0; i < snInfoList[ls.my_rank].length; i++)
				msg.pushLong(snInfoList[ls.my_rank][i]);
			try {
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
			timeLastMsgSent = timeProvider.currentTimeMillis();
		}
		else{
			final long[] uniformInfo = new long[vs.view.length];
			for (int i = uniformInfo.length; i > 0; i--)
				uniformInfo[i-1] = msg.popLong();
			mergeUniformInfo(uniformInfo, event.orig);
			final long msgSN = msg.popLong();
			receivedMessages.add(new MessageContainer(msgSN,event));
			snInfoList[ls.my_rank][event.orig] = msgSN;
			try {
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
			if(unifInfoPeriod == 0)
			    sendUniformInfo(event.getChannel());
		}	
	}
	
	private void handleUniformTimer(UniformTimer timer) {
	    if (!isBlocked && timeProvider.currentTimeMillis() - timeLastMsgSent >= unifInfoPeriod) {
	        sendUniformInfo(timer.getChannel());
	    }
	}
	
	private void sendUniformInfo(Channel channel) {
	    if (!isBlocked) {
	        try {
	            final UniformInfoEvent event = new UniformInfoEvent(channel,Direction.DOWN,this,vs.group,vs.id);

	            final Message msg = event.getMessage();
	            for (int i = 0; i < snInfoList[ls.my_rank].length; i++)
	                msg.pushLong(snInfoList[ls.my_rank][i]);

	            event.go();
	        } catch (AppiaEventException e) {
	            e.printStackTrace();
	        }
	    }
	}

	private void handleUniformInfo(UniformInfoEvent event) {
	    // If I do not have a view, I should not receive this message...
	    // FIXME: for now, I'm ignoring the message, but this should work without this... fix later
	    if(vs == null)
	        return;
		final Message msg = event.getMessage();
		final long[] uniformInfo = new long[vs.view.length];
		for (int i = uniformInfo.length; i > 0; i--)
			uniformInfo[i-1] = msg.popLong();
		mergeUniformInfo(uniformInfo, event.orig);
		deliverUniform(event.getChannel());
	}
	
	private void mergeUniformInfo(long[] table, int orig) {
		for (int i = 0; i < table.length; i++)
			if (table[i] > snInfoList[orig][i])
				snInfoList[orig][i] = table[i];
	}
	
	/**
	 * Tries to deliver Uniform messages.
	 */
	private void deliverUniform(Channel channel) {
		final ListIterator<MessageContainer> it = receivedMessages.listIterator();
		while (it.hasNext()) {
			final MessageContainer nextMsg = it.next();
			if (isUniform(nextMsg)) {
				try {
					// deliver uniform notification
					new UniformServiceEvent(channel, Direction.UP, this, nextMsg.getSendableEvent().getMessage()).go();
				} catch (AppiaEventException e) {
					e.printStackTrace();
				}
				it.remove();
			}
		}
	}
	
	/**
	 * Checks if the message is uniform.
	 * 
	 * @param header the header of the message.
	 * @return <tt>true</tt> if the message is uniform, <tt>false</tt> otherwise.
	 */
	private boolean isUniform(MessageContainer cont) {
		int seenCount = 0;
		for (int i = 0; i < snInfoList.length; i++)
			if (snInfoList[i][cont.getOrig()] >= cont.getSn())
				seenCount++;
		if (seenCount >= vs.view.length/2 + 1)			
			return true;
		return false;
	}
	
	/**
	 * Resets all sequence numbers and auxiliary variables
	 */
	private void reset(){
		sn = 0;
		snInfoList = new long[vs.view.length][vs.view.length];
		for (int i = 0; i < snInfoList.length; i++)
			Arrays.fill(snInfoList[i], 0);
	}
}
