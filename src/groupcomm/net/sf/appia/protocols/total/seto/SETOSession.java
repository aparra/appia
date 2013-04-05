
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
 * Initial developer(s): Nuno Carvalho and Jose' Mocito.
 * Contributor(s): See Appia web page for a list of contributors.
 */
package net.sf.appia.protocols.total.seto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
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
import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.LeaveEvent;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.total.common.AckViewEvent;
import net.sf.appia.protocols.total.common.RegularServiceEvent;
import net.sf.appia.protocols.total.common.SETOServiceEvent;
import net.sf.appia.protocols.total.common.SeqOrderEvent;
import net.sf.appia.protocols.total.common.UniformInfoEvent;
import net.sf.appia.protocols.total.common.UniformServiceEvent;
import net.sf.appia.protocols.total.common.UniformTimer;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import org.apache.log4j.Logger;

/**
 * Optimistical total order protocol implementing the algorithm described in the paper
 * <i>Optimistic Total Order in Wide Area Networks</i> from A. Sousa, J. Pereira,
 * F. Moura and R. Oliveira.
 * 
 * @author Nuno Carvalho and Jose Mocito
 */
public class SETOSession extends Session implements InitializableSession {
	
	private static Logger log = Logger.getLogger(SETOSession.class);
	
	private int lastsender;
	private long lastfinal;
	private long lastfast;
	private double alfa;
	private long globalSN;
	private long localSN;
	private long sendingLocalSN;

	private boolean isBlocked = true;
	
	private LocalState ls = null;
	private ViewState vs, vs_old = null;
//	private Channel channel = null;
	private TimeProvider timeProvider = null;
	private final int seq = 0;
	
	
	private LinkedList<ListContainer> R = new LinkedList<ListContainer>(); // Received 
	private LinkedList<ListSEQContainer> S = new LinkedList<ListSEQContainer>();  // Sequence
    private LinkedList<ListContainer> O = new LinkedList<ListContainer>();  // Optimistic
	private LinkedList<ListSEQContainer> G = new LinkedList<ListSEQContainer>();  // Regular
	private long [] delay = null, r_delay = null;
	
	private long[] lastOrderList;
	private long timeLastMsgSent;
	private static final long DEFAULT_UNIFORM_INFO_PERIOD = 100;
	private long uniformInfoPeriod=DEFAULT_UNIFORM_INFO_PERIOD;
	private boolean utSet; // Uniform timer is set?
	private boolean newUniformInfo = false;
	
    List<GroupSendableEvent> pendingMessages=new ArrayList<GroupSendableEvent>();
    
	/**
	 * Constructs a new SETOSession.
	 * 
	 * @param layer
	 */
	public SETOSession(Layer layer) {
		super(layer);
		alfa=0.95; // default
		reset();
	}

      /**
       * Initializes the session using the parameters given in the XML configuration.
       * Possible parameters:
       * <ul>
       * <li><b>alfa</b> is used to tune the protocol and is a value between 0 and 1.
       * </ul>
       * 
       * @param params The parameters given in the XML configuration.
       * @see net.sf.appia.xml.interfaces.InitializableSession#init(SessionProperties)
       */
	public void init(SessionProperties params) {
	    if(params.containsKey("alfa")){
	        alfa = params.getDouble("alfa");			
	    }
	    if(params.containsKey("uniform_info_period")){
	        uniformInfoPeriod = params.getLong("uniform_info_period");
	    }

	    log.info("Initializing static parameter alfa. Set to "+alfa);
	}

	/** 
	 * Main handler of events.
	 * @see net.sf.appia.core.Session#handle(Event)
	 */
	public void handle(Event event){
		if(event instanceof ChannelInit)
			handleChannelInit((ChannelInit) event);
		else if(event instanceof ChannelClose)
			handleChannelClose((ChannelClose)event);
		else if(event instanceof BlockOk)
			handleBlockOk((BlockOk)event);
		else if(event instanceof View)
			handleNewView((View)event);
        else if(event instanceof AckViewEvent)
            handleAckViewEvent((AckViewEvent) event);
		else if(event instanceof SETOTimer)
			handleTimer((SETOTimer)event);
		else if(event instanceof SeqOrderEvent)
			handleSequencerMessage((SeqOrderEvent)event);
		else if (event instanceof UniformInfoEvent)
			handleUniformInfo((UniformInfoEvent) event);
		else if (event instanceof UniformTimer)
			handleUniformTimer((UniformTimer) event);
        else if (event instanceof LeaveEvent)
            handleLeaveEvent((LeaveEvent) event);
		else if(event instanceof GroupSendableEvent)
			handleGroupSendable((GroupSendableEvent)event);
		else{
			log.warn("Got unexpected event in handle: "+event+". Forwarding it.");
			try {
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
		}
	}

    private void handleLeaveEvent(LeaveEvent ev){
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
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
		log.warn("Channel is closing!");
		try {
			close.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * The group os blocked. It is going to change view.
	 * @param ok
	 */
	private void handleBlockOk(BlockOk ok) {
		log.debug("The group is blocked.");
		log.debug("Impossible to send messages. Waiting for a new View");
		isBlocked = true;
		
		try {
			ok.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

    private void convertUniformInfo() {
        long[] oldLOList = lastOrderList;
        lastOrderList = new long[vs.view.length];
        Arrays.fill(lastOrderList,0);
        Endpt[] survivors = vs.getSurvivingMembers(vs_old);
        for (int i = 0; i < survivors.length; i++) {
            int oldRank = vs_old.getRank(survivors[i]);
            int newRank = vs.getRank(survivors[i]);
            lastOrderList[newRank] = oldLOList[oldRank];
        }
    }

    private Endpt[] survivors;
    private View pendingView;
    
	/**
	 * New view.
	 * @param view
	 */
	private void handleNewView(View view) {
        isBlocked = false;
        
        vs_old = vs;
        
        ls=view.ls;
        vs=view.vs;
        
        pendingView = view;
        
//        System.out.println("SETO: received view "+vs.id);
        
        if (vs_old != null) {
            survivors = vs.getSurvivingMembers(vs_old);
            convertUniformInfo();
            dumpPendingMessages();
            ackView(view.getChannel());
        }
        else {
            lastOrderList = new long[vs.addresses.length];
            Arrays.fill(lastOrderList,0);
            ackView(view.getChannel());
//            deliverPendingView();
        }

        reset();
        delay = new long[vs.addresses.length];
        Arrays.fill(delay,0);
        r_delay = new long[vs.addresses.length];
        Arrays.fill(r_delay,0);
        
		log.debug(vs.toString());
		log.debug(ls.toString());
		log.debug("NEW VIEW: My rank: "+ls.my_rank+" My ADDR: "+vs.addresses[ls.my_rank]);
	}
	
    private void ackView(Channel ch) {
        try {
//            System.out.println("SETO: sending ack for view "+vs.id);
            AckViewEvent ack = new AckViewEvent(ch, Direction.DOWN, this, vs.group, vs.id);
//            int dest[] = new int[survivors.length];
//            for (int i = 0; i < dest.length; i++)
//                dest[i] = vs.getRank(survivors[i]);
            //ack.dest = dest;
            ack.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
        ackCounter = 0;
    }
    
    private int ackCounter;
    
    private void handleAckViewEvent(AckViewEvent ack) {
        if (ack.view_id.equals(vs.id)) {
            // Due to view synchrony sender and receiver have seen the same messages
            lastOrderList[ack.orig] = lastOrderList[ls.my_rank];
            ackCounter++;
            if (ackCounter == vs.view.length) {
                deliverUniform();
                deliverPendingView();
            }
        }
//        System.out.println("SETO: received Ack for view "+ack.view_id+" Num "+ackCounter+" from "+ack.orig+" I am "+ls.my_rank);
    }
    
    private void deliverPendingView() {
//        System.out.println("SETO: delivering view "+pendingView.vs.id);
        try {
            pendingView.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
        
        if(!pendingMessages.isEmpty()){
            log.debug("Delivering "+pendingMessages.size()+" pending messages");
            for(GroupSendableEvent event : pendingMessages){
                reliableDATADeliver(event);
            }
            pendingMessages.clear();
        }
        
        if (!utSet && uniformInfoPeriod > 0) {
            try {
                UniformTimer ut = new UniformTimer(uniformInfoPeriod,pendingView.getChannel(),Direction.DOWN,this,EventQualifier.ON);
                ut.go();
                utSet = true;
            } catch (AppiaEventException e) {
                e.printStackTrace();
            } catch (AppiaException e) {
                e.printStackTrace();
            }
        }
        
        pendingView = null;
    }
	/**
	 * @param event
	 */
	private void handleGroupSendable(GroupSendableEvent event) {
		//log.debug("------------> " + vs.addresses[ls.my_rank] + " Received from "+vs.addresses[event.orig]);
		// events from the application
		if(event.getDir() == Direction.DOWN){
			if(isBlocked){
			    log.warn("Received event while blocked:"+event.getClass().getName()+" from session: "+
                        event.getSourceSession()+". Ignoring it.");
                return;
			}
			long msgDelay = max(delay) - delay[seq];
			reliableDATAMulticast(event, msgDelay);
		}
		// events from the network
		else {
            if (pendingView != null) {
                log.debug("Received GroupSendableEvent but still haven't delivered pending view");
                log.debug("Buffering event for future delivery");
                pendingMessages.add(event);
            }
            else {
                reliableDATADeliver(event);
            }
		}		
	}
	
	/**
	 * Multicast a DATA event to the group.
	 * 
	 * @param event the event to be multicast.
	 * @param msgDelay the message delay associated with the event.
	 */
	private void reliableDATAMulticast(GroupSendableEvent event, long msgDelay) {
		DATAHeader header = new DATAHeader(ls.my_rank, sendingLocalSN++, msgDelay);
		DATAHeader.push(header,event.getMessage());
		Message msg = event.getMessage();
		for (int i = 0; i < lastOrderList.length; i++)
			msg.pushLong(lastOrderList[i]);
		log.debug("Sending DATA message from appl. Rank="+ls.my_rank+" SN="+sendingLocalSN+" Delay="+msgDelay);
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		timeLastMsgSent = timeProvider.currentTimeMillis();
	}
	
	/**
	 * Deliver a DATA event received from the network.
	 * 
	 * @param event the event received from the network.
	 */
	private void reliableDATADeliver(GroupSendableEvent event){
		Message msg = event.getMessage();
		long[] uniformInfo = new long[vs.view.length];
		for (int i = uniformInfo.length; i > 0; i--)
			uniformInfo[i-1] = msg.popLong();
		mergeUniformInfo(uniformInfo);
		DATAHeader header = DATAHeader.pop(event.getMessage());
		log.debug("Received DATA message: "+header.id+":"+header.sn+" timestpamp is "+timeProvider.currentTimeMillis());
		header.setTime(delay[header.id]+timeProvider.currentTimeMillis());
		ListContainer container = new ListContainer(event, header);
		// add the event to the RECEIVED list...
		R.addLast(container);
		// ... and set a timer to be delivered later, according to the delay that came with the message
		setTimer(container,delay[header.id],vs.id);
		
		// Deliver event to the upper layer (spontaneous order)
		try {
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
		if(uniformInfoPeriod == 0)
		    sendUniformInfo(event.getChannel());
	}
	
	/**
	 * Received a message delayed by a timer.
	 * @param timer
	 */
	private void handleTimer(SETOTimer timer) {
		if (timer.vid.equals(vs.id)) {
			long now = timeProvider.currentTimeMillis();
			log.debug(ls.my_rank+": received timer on "+now);
			deliverOptimistic(timer.container);
		}
		else
			log.debug(ls.my_rank+": received SETOTimer from a previous view... discarding!");
	}
	
	private void deliverOptimistic(ListContainer container) {
		if (!O.contains(container)) {
		    if(log.isDebugEnabled())
		        log.debug("Delivering optimistic message.");
			try {
				SETOServiceEvent sse = new SETOServiceEvent(container.event.getChannel(), Direction.UP, this, container.event.getMessage());
				sse.go();
			} catch (AppiaEventException e1) {
				e1.printStackTrace();
			}
			O.add(container);
			if(coordinator() && !isBlocked) {
				log.debug("I'm the coordinator. Sending message to order");
				globalSN++;
				reliableSEQMulticast(container);
				r_delay[container.header.id] = container.header.get_delay();
				delay[ls.my_rank] = max(r_delay);
			}
		}
        else
            O.remove(container);
	}
	
	/**
	 * Multicast a SEQUENCER message to the group.
	 * 
	 * @param container the container of the message to be sequenced.
	 */
	private void reliableSEQMulticast(ListContainer container) {
		SEQHeader header = new SEQHeader(container.header.sender(), container.header.sn(), globalSN);
		SeqOrderEvent event;
		try {
			event = new SeqOrderEvent(container.event.getChannel(),Direction.DOWN,this,vs.group,vs.id);
			SEQHeader.push(header,event.getMessage());
			Message msg = event.getMessage();
			for (int i = 0; i < lastOrderList.length; i++)
				msg.pushLong(lastOrderList[i]);
			log.debug("Sending SEQ message. Rank="+ls.my_rank+" Header: "+header);
			event.go();
		} catch (AppiaEventException e2) {
			e2.printStackTrace();
		}
	}
	
	/**
	 * Received Sequencer message
	 * @param message
	 */
	private void handleSequencerMessage(SeqOrderEvent message) {
		log.debug("Received SEQ message from "+message.orig+" timestamp is "+timeProvider.currentTimeMillis());
		if(message.getDir() == Direction.DOWN)
			log.error("Wrong direction (DOWN) in event "+message.getClass().getName());
		else
			reliableSEQDeliver(message);	
	}
	
	/**
	 * Deliver a SEQUENCER message received from the network.
	 */
	private void reliableSEQDeliver(SeqOrderEvent event) {
		Message msg = event.getMessage();
		long[] uniformInfo = new long[vs.view.length];
		for (int i = uniformInfo.length; i > 0; i--)
			uniformInfo[i-1] = msg.popLong();
		mergeUniformInfo(uniformInfo);
		SEQHeader header = SEQHeader.pop(event.getMessage());
		log.debug("["+ls.my_rank+"] Received SEQ message "+header.id+":"+header.sn+" timestamp is "+timeProvider.currentTimeMillis());
		lastOrderList[ls.my_rank] = header.order;
		newUniformInfo = true;
		// add it to the sequencer list
		S.add(new ListSEQContainer(header,timeProvider.currentTimeMillis()));
		log.debug("Received SEQ from "+event.orig+" at time "+timeProvider.currentTimeMillis());
		// and tries to deliver messages that already have the order
		deliverRegular();
		deliverUniform();
	}
	
	/**
	 * Tries to deliver REGULAR message.
	 */
	private void deliverRegular() {
	    for (ListIterator<ListSEQContainer> li = S.listIterator(); li.hasNext(); ) {
	        ListSEQContainer orderedMsg = li.next();
	        if (log.isDebugEnabled()) {
	            log.debug("Message in order with SN="+(localSN+1)+" -> "+orderedMsg);
	            log.debug("Messages in S {");
	            listOrderedMessage();
	            log.debug("}");
	        }

	        ListContainer msgContainer = getMessage(orderedMsg.header,R);

	        if (msgContainer != null && !hasMessage(orderedMsg,G)) {
	            if(log.isDebugEnabled())
	                log.debug("["+ls.my_rank+"] Delivering regular "+msgContainer.header.id+":"+msgContainer.header.sn+" timestamp "+timeProvider.currentTimeMillis());
	            try {
	                RegularServiceEvent rse = new RegularServiceEvent(msgContainer.event.getChannel(), Direction.UP, this, msgContainer.event.getMessage());
	                rse.go();
	            } catch (AppiaEventException e1) {
	                e1.printStackTrace();
	            }
	            G.addLast(orderedMsg);

	            // Avoid delivery of optimistic service after the regular service
	            if (O.contains(msgContainer))
	                O.remove(msgContainer);
	            else
	                O.add(msgContainer);

	            if (pendingView == null) {
	                // ADJUSTING DELAYS
	                log.debug(ls.my_rank+": Adjusting delays...");
	                long _final = orderedMsg.time;
	                long _fast = msgContainer.header.getTime();
	                int _sender = msgContainer.header.id;
	                if(lastsender != -1){
	                    log.debug("continuing adjusting the delays!");
	                    log.debug("_final:"+_final+" | lastfinal:"+lastfinal+" | _fast:"+_fast+" | lastfast:"+lastfast);
	                    long delta = (_final - lastfinal) - (_fast - lastfast);
	                    log.debug("DELTA: "+delta);
	                    if(delta > 0) {
	                        log.debug("adjust("+lastsender+","+_sender+","+delta+")");
	                        adjust(lastsender,_sender,delta);
	                    }
	                    else if (delta < 0) {
	                        log.debug("adjust("+_sender+","+lastsender+","+delta+")");
	                        adjust(_sender,lastsender,-delta);
	                    }
	                }
	                lastsender = _sender;
	                lastfast = _fast;
	                lastfinal = _final;
	                localSN++;
	            }
	        }
	        li.remove();
	    }
	    if(log.isDebugEnabled())
	        log.debug("DeliverRegular finished.");
	}

	private void handleUniformTimer(UniformTimer timer) {
	    //log.debug("Uniform timer expired. Now is: "+timeProvider.currentTimeMillis());
		if (!isBlocked && newUniformInfo && timeProvider.currentTimeMillis() - timeLastMsgSent >= uniformInfoPeriod) {
			//log.debug("Last message sent was at time "+timeLastMsgSent+". Will send Uniform info!");
			sendUniformInfo(timer.getChannel());
			newUniformInfo = false;
		}
	}
	
	private void sendUniformInfo(Channel channel) {
	    if (!isBlocked) {
	        try {
	            UniformInfoEvent event = new UniformInfoEvent(channel,Direction.DOWN,this,vs.group,vs.id);
	            Message msg = event.getMessage();
	            for (int i = 0; i < lastOrderList.length; i++)
	                msg.pushLong(lastOrderList[i]);

	            event.go();
	        } catch (AppiaEventException e) {
	            e.printStackTrace();
	        }
	    }
	}
	
	private void handleUniformInfo(UniformInfoEvent event) {
		log.debug("Received UniformInfo from "+event.orig+". Uniformity information table now is: ");
		Message msg = event.getMessage();
		long[] uniformInfo = new long[vs.view.length];
		for (int i = uniformInfo.length; i > 0; i--)
			uniformInfo[i-1] = msg.popLong();
		mergeUniformInfo(uniformInfo);
		if (log.isDebugEnabled())
			for (int i = 0; i < lastOrderList.length; i++)
				log.debug("RANK :"+i+" | LAST_ORDER: "+lastOrderList[i]);
		deliverUniform();
	}
	
	private void mergeUniformInfo(long[] table) {
		for (int i = 0; i < table.length; i++)
			if (table[i] > lastOrderList[i])
				lastOrderList[i] = table[i];
	}
	
	/**
	 * Tries to deliver Uniform messages.
	 */
	private void deliverUniform() {
		log.debug("Trying to deliver FINAL messages!");
		ListIterator<ListSEQContainer> it = G.listIterator();
		while (it.hasNext()) {
			ListSEQContainer nextMsg = it.next();
			if (isUniform(nextMsg.header)) {
				ListContainer msgContainer = getRemoveMessage(nextMsg.header,R);
				log.debug("Delivering message: "+msgContainer.event);
				log.debug("["+ls.my_rank+"] Delivering final "+msgContainer.header.id+":"+msgContainer.header.sn+" timestamp "+timeProvider.currentTimeMillis());
				try {
					// deliver uniform notification
					UniformServiceEvent use = new UniformServiceEvent(msgContainer.event.getChannel(), Direction.UP, this, msgContainer.event.getMessage());
					use.go();
				} catch (AppiaEventException e) {
					e.printStackTrace();
				}
				it.remove();
			}
            else
                return;
		}
	}
	
	/**
	 * Checks if the message is uniform.
	 * 
	 * @param header the header of the message.
	 * @return <tt>true</tt> if the message is uniform, <tt>false</tt> otherwise.
	 */
	private boolean isUniform(SEQHeader header) {
		int seenCount = 0;
		for (int i = 0; i < lastOrderList.length; i++)
			if (lastOrderList[i] >= header.order)
				seenCount++;
		if (seenCount >= lastOrderList.length/2 + 1)			
			return true;
		return false;
	}
	
	/**
	 * Resets all sequence numbers and auxiliary variables
	 */
	private void reset(){
		globalSN = 0;
		localSN = 0;
		sendingLocalSN = 0;
		lastfinal=-1;
		lastfast=-1;
		lastsender=-1;
	}
	
	/**
	 * In a view change, if there are messages that were not delivered, deliver them
	 * in a deterministic order. This can be done because VSync ensures that when a new View
	 * arrives, all members have the same set of messages.
	 */
	private void dumpPendingMessages() {
		ListContainer container = null;
        nextDeterministicCounter = 0;
		while((container = getNextDeterministic()) != null){
			if(log.isDebugEnabled()){
				log.debug("Message in deterministic order with SN="+(localSN+1)+" -> "+container);
			}
			SEQHeader header = new SEQHeader(container.header.sender(), container.header.sn(), ++lastOrderList[ls.my_rank]);
            lastOrderList[ls.my_rank] = header.order;
			S.add(new ListSEQContainer(header,timeProvider.currentTimeMillis()));
			log.debug("Resending message to Appl: "+container.event);
			//getRemoveMessage(container.header,R);
		}
        deliverRegular();
	}

	private boolean hasMajority() {
		if (vs_old != null) {
			int count = 0;
			for (int i = 0; i < vs_old.view.length; i++)
				for (int j = 0; j < vs.view.length; j++)
					if (vs_old.view[i].equals(vs.view[j]))
						count++;
			if (count >= vs.view.length / 2 + 1)
				return true;
		}
		return false;
	}
	
    private int nextDeterministicCounter;
    /**
     * Removes and returns an event from the buffer in a deterministic way.
     * Used when there are view changes in the group
     */
    private ListContainer getNextDeterministic(){
    	if (nextDeterministicCounter == R.size()) {
            return null;
        }
        
        ListContainer first = R.getFirst();
    	
        long nSeqMin=first.header.sn;
        int emissor=first.header.id;
        int pos=0;

        for(int i=1; i<R.size(); i++){
            ListContainer current = R.get(i);
            if (!S.contains(current)) {
                if(nSeqMin > current.header.sn){
                    pos=i;
                    nSeqMin=current.header.sn;
                    emissor=current.header.id;
                }
                else if(nSeqMin == current.header.sn){
                    if(emissor > current.header.id){
                        pos=i;
                        emissor=current.header.id;
                    }
                }
            }
        }

        nextDeterministicCounter++;
        return R.get(pos);
	}

    /**
     * Get and remove a message from a list
     */
	private ListContainer getRemoveMessage(Header header, LinkedList<ListContainer> list){
		ListIterator<ListContainer> it = list.listIterator();
		while(it.hasNext()){
			ListContainer cont = it.next();
			if(cont.header.equals(header)){
				it.remove();
				return cont;
			}
		}
		return null;
	}
	
	/**
     * Get a message from a list
     */
	private ListContainer getMessage(Header header, LinkedList<ListContainer> list){
		ListIterator<ListContainer> it = list.listIterator();
		while(it.hasNext()){
			ListContainer cont = it.next();
			if(cont.header.equals(header))
				return cont;
		}
		return null;
	}
	
	/**
	 * Check if the list has the given message.
	 */
	private boolean hasMessage(ListSEQContainer msg, LinkedList<ListSEQContainer> list) {
		ListIterator<ListSEQContainer> it = list.listIterator();
		while(it.hasNext()){
			ListSEQContainer cont = it.next();
			if(cont.header.equals(msg.header)) {
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Get the next ordered message.
	 */
	private ListSEQContainer getOrderedMessage(long ord){
		for (ListIterator<ListSEQContainer> li=S.listIterator(); li.hasNext();){
			ListSEQContainer cont = li.next();
			if(cont.header.order == ord){
				li.remove();
				return cont;
			}
		}
		return null;
	}

	/**
	 * List the order.<br>
	 * <b>FOR DEBUGGING PURPOSES ONLY!</b>
	 */
	private void listOrderedMessage(){
	    for (ListSEQContainer cont : S){
            log.debug("Element: "+cont.header);
	    }
	}

	
	/**
	 * Delivers a message to the layer above.
	 */
	private void delivery(GroupSendableEvent event){
		try {
			event.setSourceSession(this);
			event.init();
			event.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Sets a timer to delay a message that came from the network.
	 */
	private void setTimer(ListContainer container, long timeout, ViewID vid) {
		try {
			log.debug("TIME Container: "+container.header.getTime());
			SETOTimer timer = new SETOTimer(timeout/1000, container.event.getChannel(), 
					Direction.DOWN, this, EventQualifier.ON, container, vid);
				timer.go();
				if(log.isDebugEnabled())
					log.debug("Setting new timer. NOW is "+
							timeProvider.currentTimeMillis()+" timer to "+timer.getTimeout());
		} catch (AppiaEventException e) {
			e.printStackTrace();
		} catch (AppiaException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * MAX of a list of numbers.
	 */
	private long max(long[] a){
		long m=a[0];
		for(int i=1; i< a.length; i++)
			if(a[i]>m)
				m=a[i];
		return m;
	}
	
	/**
	 * Checks if the current process is the coordinator.<br>
	 * The coordinator is also the sequencer. and is the member that has the rank 0
	 */
	private boolean coordinator(){
		return ls != null && ls.my_rank == 0;
	}
	
	/**
	 * Adjust the delays.
	 */
	private void adjust(int i, int j, long d){
		double v = ((delay[i] * alfa) + (delay[i] - d) * (1 - alfa));
		if(v >= 0)
			delay[i] = Math.round(v);
		else{
			delay[i] = 0;
			delay[j] = delay[j] - Math.round(v);
		}
	}
}


/*
 * #######################################################
 *  support classes
 * #######################################################
 */
/**
 * Class container to help putting all information into LinkedLists
 * 
 * @author Nuno Carvalho
 */
class ListContainer {
	GroupSendableEvent event;
	DATAHeader header;

	public ListContainer(GroupSendableEvent e, DATAHeader h) {//, long t){
		event = e;
		header = h;
	}
}

/**
 * Class container to help putting all information into LinkedLists
 * 
 * @author Nuno Carvalho
 */
class ListSEQContainer {
	SEQHeader header;
	long time;
	
	public ListSEQContainer(SEQHeader h, long t){
		header = h;
		time = t;
	}
}

/**
 * Header of messages.
 * 
 * @author Nuno Carvalho
 */
class Header {
	
	int id;
	long sn;
	
	public boolean equals(Object o){
		if(o instanceof Header){
			Header h = (Header) o;
			return h.id == this.id && h.sn == this.sn;
		}
		return false;
	}
	
	public String toString(){
		return "Header ID="+id+" SN="+sn; 
	}
}

/**
 * Header of SEQUENCE messages.
 * 
 * @author Nuno Carvalho
 */
class SEQHeader extends Header {
	
	long order;
	
	public SEQHeader(){
		id = -1;
		sn = order = -1;
	}
	
	public SEQHeader(int id, long sn, long order){
		this.id = id;
		this.sn = sn;
		this.order = order;
	}
	
	public String toString(){
		return super.toString()+" ORDER="+order;
	}
	
	/**
	 * Push all parameters of a Header into a Appia Message.
	 * @param header header to push into the message
	 * @param message message to put the header
	 */
	public static void push(SEQHeader header, Message message){
		message.pushInt(header.id);
		message.pushLong(header.sn);
		message.pushLong(header.order);
	}
	
	
	/**
	 * Pops a header from a message. Creates a new Header from the values contained by the message.
	 * @param message message that contains the info to build the header
	 * @return a header builted from the values of contained by the message
	 */
	public static SEQHeader pop(Message message){
		SEQHeader header = new SEQHeader();
		header.order = message.popLong();
		header.sn = message.popLong();
		header.id = message.popInt();
		return header;
	}

}

/**
 * Header of DATA messages.
 * 
 * @author Nuno Carvalho
 */
class DATAHeader extends Header {
	/**
	 * delay of the message
	 */
	private long delay;
	/**
	 * Time when the message was sent.
	 */
	private long time;

	private int stable_id;
	private long stable_seqno;
	
	public DATAHeader(int sender, long sn, long d, long t){
		this.id=sender;
		this.sn=sn;
		delay=d;
		time=t;
	}
	
	public DATAHeader(int sender, long sn, long d){
		this.id=sender;
		this.sn=sn;
		delay=d;
	}

	public DATAHeader(int sender, long sn){
		this.id=sender;
		this.sn=sn;
		delay=0;
	}

	public DATAHeader(DATAHeader obj){
		this.id=obj.sender();
		this.sn=obj.sn();
		delay=obj.get_delay();
	}
	
	/**
	 * gets the sender od the message.
	 * @return sender of the message
	 */
	public int sender(){
		return id;
	}

	/**
	 * Gets the Serial Number of the message.
	 * @return serial number of the message
	 */
	public long sn(){
		return sn;
	}

	/**
	 * sets the delay of the message.
	 * @param i delay of the message
	 */
	public void set_delay(int i){
		delay=i;
	}

	/**
	 * Gets the delay of the message.
	 * @return delay of the message
	 */
	public long get_delay(){
		return delay;
	}

	public void setStableId(int stable_id) {
		this.stable_id = stable_id;
	}
	
	public int getStableId() {
		return stable_id;
	}
	
	public void setStableSeqNo(long seqno) {
		this.stable_seqno = seqno;
	}
	
	public long getStableSeqNo() {
		return stable_seqno;
	}
	
	/**
	 * Push all parameters of a Header into a Appia Message.
	 * @param header header to push into the message
	 * @param message message to put the header
	 */
	public static void push(DATAHeader header, Message message){
		message.pushInt(header.id);
		message.pushLong(header.sn);
		message.pushLong(header.delay);
	}
	
	
	/**
	 * Pops a header from a message. Creates a new Header from the values contained by the message.
	 * @param message message that contains the info to build the header
	 * @return a header builted from the values of contained by the message
	 */
	public static DATAHeader pop(Message message){
		DATAHeader header = new DATAHeader(-1,-1);
		header.delay = message.popLong();
		header.sn = message.popLong();
		header.id = message.popInt();
		return header;
	}
	
	/**
	 * @return Returns the time.
	 */
	public long getTime() {
		return time;
	}
	
	/**
	 * @param time The time to set.
	 */
	public void setTime(long time) {
		this.time = time;
	}
	
}
