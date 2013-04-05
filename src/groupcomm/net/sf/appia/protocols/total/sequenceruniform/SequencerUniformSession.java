
/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2010 University of Lisbon
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
package net.sf.appia.protocols.total.sequenceruniform;

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
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.LeaveEvent;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.protocols.total.common.AckViewEvent;
import net.sf.appia.protocols.total.common.RegularServiceEvent;
import net.sf.appia.protocols.total.common.SeqOrderEvent;
import net.sf.appia.protocols.total.common.UniformInfoEvent;
import net.sf.appia.protocols.total.common.UniformServiceEvent;
import net.sf.appia.protocols.total.common.UniformTimer;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import org.apache.log4j.Logger;

/**
 * Sequencer based total order protocol with optimistic deliveries. 
 * 
 * @author Nuno Carvalho and Jose Mocito
 */
public class SequencerUniformSession extends Session implements InitializableSession {
	
	private static Logger log = Logger.getLogger(SequencerUniformSession.class);
	
	private long globalSN;
	private long localSN;
	private long sendingLocalSN;

	private boolean isBlocked = true;
	
	private LocalState ls = null;
	private ViewState vs, vs_old = null;
//	private Channel channel = null;
	private TimeProvider timeProvider = null;
	
	
	private LinkedList<ListContainer> R = new LinkedList<ListContainer>(); // Received 
	private LinkedList<ListSEQContainer> S = new LinkedList<ListSEQContainer>();  // Sequence
	private LinkedList<ListSEQContainer> G = new LinkedList<ListSEQContainer>();  // Regular
	
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
	public SequencerUniformSession(Layer layer) {
		super(layer);
		reset();
	}

      /**
       * Initializes the session using the parameters given in the XML configuration.
       * Possible parameters:
       * <ul>
       * <li><b>uniform_info_period</b> is used to tune the periodic information exchange about uniformity of messages (ms).
       * </ul>
       * 
       * @param params The parameters given in the XML configuration.
       * @see net.sf.appia.xml.interfaces.InitializableSession#init(SessionProperties)
       */
	public void init(SessionProperties params) {
	    if(params.containsKey("uniform_info_period")){
	        uniformInfoPeriod = params.getLong("uniform_info_period");
	    }
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

    //FIXME: check why this is not used
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

        if(log.isDebugEnabled()){
            log.debug(vs.toString());
            log.debug(ls.toString());
            log.debug("NEW VIEW: My rank: "+ls.my_rank+" My ADDR: "+vs.addresses[ls.my_rank]);
        }
	}
	
    private void ackView(Channel ch) {
        try {
            if(log.isDebugEnabled())
                log.debug("Sending ack for view "+vs.id);
            AckViewEvent ack = new AckViewEvent(ch, Direction.DOWN, this, vs.group, vs.id);
            //FIXME: why was this removed???
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
        if(log.isDebugEnabled())
            log.debug("Received Ack for view "+ack.view_id+" Num "+ackCounter+" from "+ack.orig+" I am "+ls.my_rank);
    }
    
    private void deliverPendingView() {
        if(log.isDebugEnabled())
            log.debug("Delivering view "+pendingView.vs.id);
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
			reliableDATAMulticast(event);
		}
		// events from the network
		else {
		    if (pendingView != null) {
		        if(log.isDebugEnabled()){
		            log.debug("Received GroupSendableEvent but still haven't delivered pending view");
		            log.debug("Buffering event for future delivery");
		        }
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
	 */
	private void reliableDATAMulticast(GroupSendableEvent event) {
		Header header = new Header(ls.my_rank, sendingLocalSN++);
        Message msg = event.getMessage();
		header.pushMe(msg);
		for (int i = 0; i < lastOrderList.length; i++)
			msg.pushLong(lastOrderList[i]);
		if(log.isDebugEnabled())
		    log.debug("Sending DATA message from appl. Rank="+ls.my_rank+" SN="+(sendingLocalSN-1));
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
		Header header = new Header();
		header.popMe(event.getMessage());
		if(log.isDebugEnabled())
		    log.debug("Received DATA message: "+header.id+":"+header.sn+" timestpamp is "+timeProvider.currentTimeMillis());
		ListContainer container = new ListContainer(event, header);
		// add the event to the RECEIVED list...
		R.addLast(container);
        if(coordinator() && !isBlocked) {
            if(log.isDebugEnabled())
                log.debug("I'm the coordinator. Sending message to order");
            globalSN++;
            reliableSEQMulticast(container);
        }
		
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
	 * Multicast a SEQUENCER message to the group.
	 * 
	 * @param container the container of the message to be sequenced.
	 */
	private void reliableSEQMulticast(ListContainer container) {
		SEQHeader header = new SEQHeader(container.header.getId(), container.header.getSn(), globalSN);
		SeqOrderEvent event;
		try {
			event = new SeqOrderEvent(container.event.getChannel(),Direction.DOWN,this,vs.group,vs.id);
			header.pushMe(event.getMessage());
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
		SEQHeader header = new SEQHeader();
		header.popMe(event.getMessage());
		if(log.isDebugEnabled())
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
	    if(log.isDebugEnabled())
	        log.debug("Trying to deliver FINAL messages!");
		ListIterator<ListSEQContainer> it = G.listIterator();
		while (it.hasNext()) {
			ListSEQContainer nextMsg = it.next();
			if (isUniform(nextMsg.header)) {
			    ListContainer msgContainer = getRemoveMessage(nextMsg.header,R);
			    if(log.isDebugEnabled()){
			        log.debug("Delivering message: "+msgContainer.event);
			        log.debug("["+ls.my_rank+"] Delivering final "+msgContainer.header.id+":"+msgContainer.header.sn+" timestamp "+timeProvider.currentTimeMillis());
			    }
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
	void reset(){
		globalSN = 0;
		localSN = 0;
		sendingLocalSN = 0;
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
			SEQHeader header = new SEQHeader(container.header.getId(), container.header.getSn(), ++lastOrderList[ls.my_rank]);
            lastOrderList[ls.my_rank] = header.order;
			S.add(new ListSEQContainer(header,timeProvider.currentTimeMillis()));
			log.debug("Resending message to Appl: "+container.event);
			//getRemoveMessage(container.header,R);
		}
        deliverRegular();
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
	 * List the order.<br>
	 * <b>FOR DEBUGGING PURPOSES ONLY!</b>
	 */
	private void listOrderedMessage(){
	    for (ListSEQContainer cont : S){
            log.debug("Element: "+cont.header);
	    }
	}
	
	/**
	 * Checks if the current process is the coordinator.<br>
	 * The coordinator is also the sequencer. and is the member that has the rank 0
	 */
	private boolean coordinator(){
		return ls != null && ls.my_rank == 0;
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
	Header header;

	public ListContainer(GroupSendableEvent e, Header h) {//, long t){
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
	
	protected int id = -1;
	protected long sn = -1;
	
	Header(){}
	
	Header(int id, long sn){
	    this.id = id;
	    this.sn = sn;
	}
	
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

    public int getId() {
        return id;
    }

    public long getSn() {
        return sn;
    }
    
    /**
     * Push all parameters of a Header into an Appia Message.
     * @param header header to push into the message
     * @param message message to put the header
     */
    public void pushMe(Message message){
        message.pushInt(this.id);
        message.pushLong(this.sn);
    }
    
    
    /**
     * Pops a header from a message. Creates a new Header from the values contained by the message.
     * @param message message that contains the info to build the header
     * @return a header built from the values of contained by the message
     */
    public void popMe(Message message){
        this.sn = message.popLong();
        this.id = message.popInt();
    }
}

/**
 * Header of SEQUENCE messages.
 * 
 * @author Nuno Carvalho
 */
class SEQHeader extends Header {
	
	protected long order;
	
	public SEQHeader(){
	    super();
	}
	
	public SEQHeader(int id, long sn, long order){
	    super(id,sn);
		this.order = order;
	}
	
    public long getOrder() {
        return order;
    }

	public String toString(){
		return super.toString()+" ORDER="+order;
	}
	
	/**
	 * Push all parameters of a Header into a Appia Message.
	 * @param header header to push into the message
	 * @param message message to put the header
	 */
	public void pushMe(Message message){
	    super.pushMe(message);
		message.pushLong(this.order);
	}
	
	
	/**
	 * Pops a header from a message. Creates a new Header from the values contained by the message.
	 * @param message message that contains the info to build the header
	 * @return a header built from the values of contained by the message
	 */
	public void popMe(Message message){
		this.order = message.popLong();
		super.popMe(message);
	}

}
