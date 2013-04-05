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
 package net.sf.appia.protocols.total.sequencer;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.*;
import net.sf.appia.protocols.group.events.*;
import net.sf.appia.protocols.group.intra.*;
import net.sf.appia.protocols.group.sync.*;




/**
 *     Class that implements a total order protocol
 *     using a sequencer based approach
 */
public class TotalSequencerSession extends Session {
    
    private Channel channel;

    /*view of the group*/
    private LocalState localView;
    private ViewState viewState;
    
    private int ordemTotal;
    private int nSeqInd;
    private Buffer list;

    private boolean blocked=false;

    /**
     *   Defaul Constructor 
     *   @param l The layer associated to this section
     */
    public TotalSequencerSession(Layer l) {
        super(l);
        ordemTotal=1;
        nSeqInd=1;
        list=new Buffer();
    }

    /**
     * Handles incoming events.
     * @param e incoming event.
     */
    public void handle(Event e) {
        if(e instanceof BlockOk){
            handleBlockOk((BlockOk)e);
            return;
        }

        if(e instanceof TotalOrderEvent){
            handleTotalOrderEvent((TotalOrderEvent)e);
            return;
        }
        if(e instanceof GroupSendableEvent){
            //does not ensure total order on events that are not sent to all elements of the group
            if(e instanceof Send){
                try{
                    e.go();
                }
                catch(AppiaEventException ex){
                	ex.printStackTrace();
                }
            }
            else
                handleGroupSendableEvent((GroupSendableEvent)e);

            return;
        }
        if (e instanceof View){
            handleView((View)e);
            return;
        }
        if(e instanceof ChannelInit) {
            channel=e.getChannel();
            try {
                e.go();
            }
            catch(AppiaEventException ex) {
                ex.printStackTrace();
            }
        }
    }

    /*
     *  (non-Javadoc)
     * @see appia.Session#boundSessions(appia.Channel)
     */
    public void boundSessions(Channel channel) {
    }

    /*
     * 
     */
    private void handleBlockOk(BlockOk e){        
        try{
            e.go();
        }
        catch(AppiaEventException ex) {
        	ex.printStackTrace();
        }
        blocked = true;
    }
    
     /*
      * 
      */
    private void handleTotalOrderEvent(TotalOrderEvent e) {
        if(TotalSequencerConfig.debugOn)
        	debug("received TotalOrderEvent");
        if(e.getDir()==Direction.UP) {
            TotalSequencerHeader h=getHeader(e);

            if(! amCoordinator()) {
        	if(TotalSequencerConfig.debugOn)
                	debug("inserted the order in the list");
                list.insertOrder(h.getOrder(),h.getSender(),h.getnSeqInd());
                sendEvents();
            }
        }
        e = null;
    }

    /*
     * Gets all events that already can be forwarded up
     */
    private void sendEvents() {
        GroupSendableEvent se;
	
	if(TotalSequencerConfig.debugOn){
        	debug("I want to deliver event with order :: "+ordemTotal);
        	debug("List size :: "+list.size());
	}
        while((se=list.getReadyEvent(ordemTotal))!=null) {
            try {
          	if(TotalSequencerConfig.debugOn)
                	debug("delivering event ::"+ordemTotal);
                se.go();
            }
            catch(AppiaEventException ex) {
            	ex.printStackTrace();
            }
            ordemTotal++;
        }
    }


    /*
     *     removes header from the message event
     */
    private TotalSequencerHeader getHeader(GroupSendableEvent e) {
    	Message om = e.getMessage();
    	
    	int order = om.popInt();
    	int sender = om.popInt();
    	int seq = om.popInt();
    	return new TotalSequencerHeader(order,sender,seq);
    }

    /*
     *  puts header on the message event
     */
    private void setHeader(GroupSendableEvent e,TotalSequencerHeader h) {
    	
    	Message om = e.getMessage();
    	
    	om.pushInt(h.getnSeqInd());
    	om.pushInt(h.getSender());
    	om.pushInt(h.getOrder());
    }


    /*
     *    
     */
    private void handleGroupSendableEvent(GroupSendableEvent e) {
        if(TotalSequencerConfig.debugOn)	
        	debug(""+e+"   "+e.getSourceSession());
        if(e.getDir() == Direction.DOWN)
            handleGroupSendableEventDown(e);
        else
            handleGroupSendableEventUp(e);
    }

    /*
     *
     */
    private void handleGroupSendableEventDown(GroupSendableEvent e){
        TotalSequencerHeader h;
        GroupSendableEvent clonedEvent=null;
	
	if(TotalSequencerConfig.debugOn)	
        	debug("received GroupSendableEvent from UP");

        try{
            clonedEvent=(GroupSendableEvent) e.cloneEvent();
        }
        catch(CloneNotSupportedException ex){
            System.err.println("Error cloning event.");
        }
        clonedEvent.setDir(Direction.invert(clonedEvent.getDir()));
        clonedEvent.source=viewState.view[localView.my_rank];
        clonedEvent.setSourceSession(this);
        clonedEvent.orig = localView.my_rank;
        try{
            clonedEvent.init();
        }
        catch(AppiaEventException ex) {
        	ex.printStackTrace();
        }

        //if this member is the sequencer, it can put the sequence of the
        //total order and send the cloned event UP
        if(amCoordinator()){
            if(TotalSequencerConfig.debugOn)	
            	debug("Message ordered. Sending it "+ordemTotal);
            h=new TotalSequencerHeader(ordemTotal++,localView.my_rank,nSeqInd++);
            try {
                clonedEvent.go();
            }
            catch(AppiaEventException ex) {
                ex.printStackTrace();
            }
        }
        // if this member is not the sequencer, inserts the event on the queue 
        // and creates an header without the order
        else{
            if(TotalSequencerConfig.debugOn)
            	debug("Message was not ordered. Sending it.");
            h=new TotalSequencerHeader(-1,localView.my_rank,nSeqInd);

            list.insertEvent(localView.my_rank,nSeqInd, clonedEvent);
            nSeqInd++;
        }
        setHeader(e,h);
        
        try {
            e.go();
        }
        catch(AppiaEventException ex) {
            System.err.println("Excepcao no evento ao fazer go!");
        }
    }

    /*
     * 
     */
    private void handleGroupSendableEventUp(GroupSendableEvent e){
        TotalSequencerHeader h;
        TotalOrderEvent toe=null;
        
        if(TotalSequencerConfig.debugOn)	
        	debug("Received GroupSendableEvent from DOWN");

        h=getHeader(e);
        
        // if this is the sequencer, it creates the event of the order.
        if(amCoordinator()){
            if(blocked){
                if(TotalSequencerConfig.debugOn)
                	debug("I'm blocked. Cannot order. Enqueueing event.");
                list.insertEvent(h.getSender(),h.getnSeqInd(),e);
            }
            else{
                if(TotalSequencerConfig.debugOn)
                	debug("Message was ordered. Sending: "+ordemTotal);
                try{
                    toe = new TotalOrderEvent(channel,Direction.DOWN,this,e.group,e.view_id);
                }
                catch(Exception ex){
                    ex.printStackTrace();
                }
                
                TotalSequencerHeader totalSeq_h=new TotalSequencerHeader(ordemTotal++,h.getSender(),h.getnSeqInd());

                setHeader(toe,totalSeq_h);
                
                try {
                    toe.go();
                    e.go();
                }
                catch(AppiaEventException ex) {
                	ex.printStackTrace();
                }
            }
        }
        else{
            /*Not the sequencer*/
            if(h.getOrder() == -1){
                if(TotalSequencerConfig.debugOn)
                	debug("Did not receive the order yet. Enqueueing event.");
                list.insertEvent(h.getSender(),h.getnSeqInd(),e);
            }
            else{
                if(TotalSequencerConfig.debugOn)
                	debug("Received order. Enqueueing it. " + h.getOrder());
                list.insert(h.getOrder(),h.getSender(),h.getnSeqInd(),e);
            }
            sendEvents();
        }
    }

    /*
     * 
     */
    private void handleView(View e){
        if(TotalSequencerConfig.debugOn)
        	debug("Received new view.");
        blocked = false;
        /*Old View*/
        cleanCounters();

        if(TotalSequencerConfig.debugOn){
        	for(int i=0;i<e.vs.addresses.length;i++)
	            	debug("{"+e.vs.view[i].id+"}");
        	debug((e.ls.am_coord ? "I am" : "I am not") + " the group coordinator");
        }

        /*New View*/
        localView=e.ls;
        viewState=e.vs;
        try {
            e.go();
        }
        catch(AppiaEventException ex) {
        	ex.printStackTrace();
        }

    }

    /*
     *  
     */
    private boolean amCoordinator(){
        return localView.am_coord;
    }

    /*
     *   Runs ordering algorithm and reinitializes counters.
     */
    private void cleanCounters(){
        if(TotalSequencerConfig.debugOn)
        	debug("Cleaning list");
        orderAlgorithm();
        ordemTotal=1;
        nSeqInd=1;
    }


    /*
     *     Order the events that are on the queue
     *     this algo is deterministic, so all members will do the same.
     */
    private void orderAlgorithm(){
        GroupSendableEvent e;

        while(!list.isEmpty()){        	
            if(TotalSequencerConfig.debugOn)	            
            	debug("Sends the event");
            e=list.getMinimum();
            try{
                e.go();
            }
            catch(AppiaEventException ex) {
            	ex.printStackTrace();
            }
        }
    }


    private void debug(String msg){
//	System.out.println("TOTALSEQUENCER:: "+msg);
    }
}


