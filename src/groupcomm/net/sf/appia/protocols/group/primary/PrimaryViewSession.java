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
package net.sf.appia.protocols.group.primary;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.management.AppiaManagementException;
import net.sf.appia.management.ManagedSession;
import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.LeaveEvent;
import net.sf.appia.protocols.group.sync.BlockOk;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import org.apache.log4j.Logger;

/**
 * 
 * This session implements a Primary View protocol. Processes only receive
 * views when in a primary partition. To bootstrap the primary partition construction
 * one process must be defined has <i>primary</i>. From that point forward, new
 * processes can be added to a primary partition. Primary views are defined by a
 * majority of members from the previous <b>primary</b> view.
 * 
 * @author Jose Mocito
 * @version 1.0
 */
public class PrimaryViewSession extends Session implements InitializableSession, ManagedSession {

    private static Logger log = Logger.getLogger(PrimaryViewSession.class);
    
    private static final String SET_PRIMARY="set_primary";
    private static final String GET_BLOCKED="blocked";
    private static final String GET_VIEW="view";
    
    private boolean blocked=true;
    private View view, lastPrimaryView;
    private ViewState vs, vsOld;
    private LocalState ls;
    private boolean primaryProcess;
    private boolean isPrimary;
    private boolean wasPrimary;
    private long ackCount;
    int primaryCounter;
    int[] newMembers;
    boolean newMembersState[];
    private Map<String,String> operationsMap=new Hashtable<String,String>();
    
    List<GroupSendableEvent> pendingMessages=new ArrayList<GroupSendableEvent>();
    private LeaveEvent leaveEvent = null;
    
    public PrimaryViewSession(Layer layer) {
        super(layer);
    }

    public void init(SessionProperties params) {
        if (params.containsKey("primary"))
            primaryProcess = params.getBoolean("primary");
    }
    
    public void handle(Event event) {
        if (event instanceof View)
            handleView((View) event);
        else if (event instanceof BlockOk)
            handleBlockOk((BlockOk) event);
        else if (event instanceof EchoEvent)
            handleEchoEvent((EchoEvent)event);
        else if (event instanceof ProbeEvent)
            handleProbeEvent((ProbeEvent) event);
        else if (event instanceof DeliverViewEvent)
            handleDeliverViewEvent((DeliverViewEvent) event);
        else if (event instanceof KickEvent)
            handleKickEvent((KickEvent) event);
        else if (event instanceof LeaveEvent)
            handleLeaveEvent((LeaveEvent)event);
        else if (event instanceof GroupSendableEvent)
            handleGroupSendable((GroupSendableEvent)event);
        else if(event instanceof EchoProbeEvent)
            handleEchoProbe((EchoProbeEvent)event);
        else {
            if (log.isDebugEnabled())
                log.error("Received unexpected event: "+event);
            
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleLeaveEvent(LeaveEvent event) {
        if(event.getDir() == Direction.DOWN){
            if(canSendMessages()){
                try {
                    event.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            }
            else{
                leaveEvent = event;
            }
        } else
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
    }

    private void handleGroupSendable(GroupSendableEvent event) {
        if (blocked && event.getDir() == Direction.UP && this.view != null && event.view_id.equals(vs.id)){
            log.debug("I'm blocked and received message to be delivered in the current view");
            log.debug("Adding message to the pending buffer");
            pendingMessages.add(event);
        } else
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
    }

    private void handleEchoEvent(EchoEvent event) {
        if(!blocked){
            // if I'm not blocked, release the echo event.
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
        }
        else{
            // if I'm blocked and I received another block (going up)
            // this means that there is a second block going to be released
            if(event.getEvent() instanceof BlockOk){
                try {
                    // release it here
                    final BlockOk block = (BlockOk) event.getEvent();
                    block.setChannel(event.getChannel());
                    block.setDir(event.getDir() == Direction.UP ? Direction.DOWN : Direction.UP);
                    block.setSourceSession(this);
                    block.init();
                    block.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }                
            }
            else{
                // if this is not a blockOk event, release the echo...
                try {
                    event.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            }
        }
        
    }

    private void handleView(View view) {
        this.view = view;
        if (vs == null) {
            vs = view.vs;
            ls = view.ls;

            if (log.isDebugEnabled()) {
                String viewStr = "Received first View:\n";
                for (int i = 0; i < view.vs.view.length; i++)
                    viewStr += view.vs.view[i] + "\n";
                log.debug(viewStr);
            }

            if (primaryProcess) {
               deliverView();
            }
        }
        else {
            ackCount = 0;
            
            vsOld = vs;
            vs = view.vs;
            ls = view.ls;

            if (log.isDebugEnabled()) {
                String viewStr = "Received new View:\n";
                for (int i = 0; i < view.vs.view.length; i++)
                    viewStr += view.vs.view[i] + "\n";
                log.debug(viewStr);
            }

            if (isPrimary) {
                log.debug("My last view was primary");
                final Endpt[] survivingMembers = vs.getSurvivingMembers(vsOld);
                if (survivingMembers.length >= vsOld.view.length / 2 + 1) {
                    // Is primary view = Has majority of members from previous view
                    log.debug("I'm still on a primary view");
                    if (survivingMembers.length < vs.view.length) {
                        // There are new members, hold view
                        final Endpt[] newMembersEndpts = vs.getNewMembers(vsOld);
                        newMembers = new int[newMembersEndpts.length];
                        for (int i = 0; i < newMembers.length; i++)
                            newMembers[i] = vs.getRank(newMembersEndpts[i]);
                        newMembersState = new boolean[newMembers.length];
                        log.info("There are "+newMembers.length+" new members");
                    }
                    else {
                        // Deliver view
                        deliverView();
                    }
                }
                else {
                    // Left the primary partition...
                    log.debug("Left the primary partition");
                    isPrimary = false;
                    wasPrimary = true;
                    // leaves as soon as it knows that left the primary partition.
                    // TODO fix this!!!
                    // leave(view.getChannel());
                }
            }
            else {
                // Hold view and send Probe
                try {
                    final ProbeEvent event = new ProbeEvent(view.getChannel(), Direction.DOWN, this, vs.group, vs.id);
                    event.getMessage().pushInt(primaryCounter);
                    event.getMessage().pushBoolean(wasPrimary);
                    event.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            }
        }
        if(leaveEvent != null){
            leave(view.getChannel());
            leaveEvent = null;
        }
    }
    
    private void handleBlockOk(BlockOk ok) {
        log.debug("Received BlockOk");
        blocked = true;
        try {
            ok.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }
    
    private void handleProbeEvent(ProbeEvent event) {
        log.debug("Received ProbeEvent from "+event.orig);
        if (isPrimary) {
            // Last view was primary
            if (event.getMessage().popBoolean()) {
                // Peer was in a primary partition at some point
                if (ls.my_rank == vs.getRank(vs.getSurvivingMembers(vsOld)[0]) && canSendMessages()) {
                    // Process has the lowest rank from the surviving members. Kick peer! 
                    kick(event.getChannel(), event.orig);
                }
                
            }
            else if (newMembers != null && ++ackCount == newMembers.length) {
                // New peers were never in a primary partition and all new members probed
                if (ls.my_rank == vs.getRank(vs.getSurvivingMembers(vsOld)[0]) && canSendMessages()) {
                    // Process has the lowest rank from the surviving members. Order view delivery!
                    try {
                        final DeliverViewEvent deliver = new DeliverViewEvent(event.getChannel(), Direction.DOWN, this, vs.group, vs.id);
                        deliver.dest = newMembers;
                        deliver.getMessage().pushInt(primaryCounter);
                        deliver.go();
                    } catch (AppiaEventException e) {
                        e.printStackTrace();
                    }
                }
                // All primary processes can deliver their views
                deliverView();
            }
        }
        else if (wasPrimary) {
            // Process was in a primary partition at some point
            if (event.getMessage().popBoolean()) {
                // Peer was also in a primary partition at some point
                final int peerPrimaryCounter = event.getMessage().popInt();
                // Check primary view counter
                if (peerPrimaryCounter > primaryCounter)
                    leave(event.getChannel());
                else if (peerPrimaryCounter < primaryCounter)
                    ackCount--;
            }
            else {
                // Peer was never in a primary partition. New peers can only join
                // primary partitions. KICK!
                kick(event.getChannel(), event.orig);
            }
            if (++ackCount == vs.view.length && hasMajority(view, lastPrimaryView))
                deliverView();
        }
    }

    private void handleKickEvent(KickEvent kick) {
        log.debug("Received KickEvent");
        leave(kick.getChannel());
    }
    
    private void handleDeliverViewEvent(DeliverViewEvent deliver) {
        log.debug("Received DeliverViewEvent");
        primaryCounter = deliver.getMessage().popInt();
        if(view != null)
            deliverView();
    }
    
    private boolean hasMajority(View v1, View v2) {
        if (v1.vs.getSurvivingMembers(v2.vs).length >= v2.vs.view.length / 2 + 1)
            return true;
        else
            return false;
    }

    private void deliverView() {
        log.debug("Delivering Primary View");
        lastPrimaryView = this.view;
        isPrimary = true;
        wasPrimary = false;
        blocked = false;
        try {
            this.view.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
        this.view = null;
        primaryCounter++;
        
        if(!pendingMessages.isEmpty()){
            log.debug("Delivering "+pendingMessages.size()+" pending messages");
            for(GroupSendableEvent ev : pendingMessages){
                try {
                    ev.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            }
            pendingMessages.clear();
        }
    }
    
    private void kick(Channel ch, int dest) {
        log.debug("Kicking process "+dest+"...");
        try {
            final KickEvent kick = new KickEvent(ch, Direction.DOWN, this, vs.group, vs.id);
            kick.dest = new int[] {dest};
            kick.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }
    
    private void leave(Channel ch) {
        //FIXME: try later, not return!!!
        if(!canSendMessages())
            return;
        
        log.debug("Leaving group...");
        try {
            final LeaveEvent leave = new LeaveEvent(ch, Direction.DOWN, this, vs.group, vs.id);
            leave.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
        if(!pendingMessages.isEmpty())
            pendingMessages.clear();
    }
    
    public Object getParameter(String parameter) throws AppiaManagementException {
        if(parameter.equals(GET_BLOCKED))
            return blocked;
        if(parameter.equals(GET_VIEW)){
            String viewStr = "Membership: ";
            if(vs != null){
                viewStr += ("\nNumber of members: "+vs.view.length+"\nMembers:\n");
                for (int i = 0; i < vs.view.length; i++)
                    viewStr += vs.view[i] + "\n";
            }
            else 
                viewStr += "NULL";
            return viewStr;
        }
        throw new AppiaManagementException("Parameter '"+parameter+"' not defined in session "+this.getClass().getName());
    }

    public void setParameter(String parameter) throws AppiaManagementException {
        if(parameter.equals(SET_PRIMARY)){
            if(blocked && view == null){
                log.warn("Process set to Primary by Management.");
                primaryProcess = true;
            }
            else if(blocked && view != null){
                primaryProcess = true;
                try {
                    new EchoProbeEvent().asyncGo(view.getChannel(), Direction.DOWN);
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            }
            else{
                log.warn("Management instruction "+parameter+" ignored.");
            }
        }
        else 
            throw new AppiaManagementException("The session "+this.getClass().getName()
                    +" do not accept the parameter '"+parameter+"'.");
    }
    
    private void handleEchoProbe(EchoProbeEvent ev) {
        if(blocked && view != null){
            // All primary processes can deliver their views
            if(view.vs.view.length > 1){
                try {
                    final int[] dests = new int[view.vs.view.length-1];
                    int viewLen=0, destsLen=0, rank = -1;
                    while(viewLen<view.vs.view.length){
                        rank = view.vs.getRank(view.vs.view[viewLen]);
                        if (rank != view.ls.my_rank){
                            dests[destsLen++] = rank;
                        }
                        viewLen++;
                    }
                    
                    final DeliverViewEvent deliver = new DeliverViewEvent(ev.getChannel(), Direction.DOWN, this, view.vs.group, view.vs.id);
                    deliver.dest = dests;
                    deliver.getMessage().pushInt(primaryCounter);
                    deliver.go();
//                    System.out.println("sent deliver to processes: ");
//                    for(int dest : dests)
//                        System.out.println("->"+dest);
                    
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
            }
            deliverView();
        }
        else
            log.warn("Management instruction "+SET_PRIMARY+" ignored.");

    }
    
    private boolean canSendMessages(){
        return !blocked || (blocked && view != null);
    }

    public MBeanOperationInfo[] getOperations(String sessionID) {
        MBeanOperationInfo[] mboi = new MBeanOperationInfo[3];
        mboi[0] = new MBeanOperationInfo(sessionID+SET_PRIMARY,"sets the primary process",
                new MBeanParameterInfo[]{},
                "void",
                MBeanOperationInfo.ACTION);
        mboi[1] = new MBeanOperationInfo(sessionID+GET_BLOCKED,"verifies if is blocked",
                new MBeanParameterInfo[]{},
                "java.lang.Boolean",
                MBeanOperationInfo.INFO);
        mboi[2] = new MBeanOperationInfo(sessionID+GET_VIEW,"gets the view",
                new MBeanParameterInfo[]{},
                "java.lang.String",
                MBeanOperationInfo.INFO);
        operationsMap.put(sessionID+SET_PRIMARY, SET_PRIMARY);
        operationsMap.put(sessionID+GET_BLOCKED, GET_BLOCKED);
        operationsMap.put(sessionID+GET_VIEW, GET_VIEW);
        return mboi;
    }
    
    public MBeanAttributeInfo[] getAttributes(String sessionID){
        return null;
    }
    
    public Object invoke(String action, MBeanOperationInfo info, Object[] params, String[] signature) 
        throws AppiaManagementException {
        log.debug("Calling action: "+action);
        if(info.getImpact() == MBeanOperationInfo.ACTION){
            if(params.length == 0){
                setParameter(operationsMap.get(action));
                return null;
            }
            else throw new AppiaManagementException("Action "+action+" called with the wrong parameters");
        }
        else if(info.getImpact() == MBeanOperationInfo.INFO){
            return getParameter(operationsMap.get(action));
        }
        else throw new AppiaManagementException("Action "+action+" is not accepted");
    }

    public Object attributeGetter(String attribute, MBeanAttributeInfo info) throws AppiaManagementException {
        return null;
    }

    public void attributeSetter(Attribute attribute, MBeanAttributeInfo info) throws AppiaManagementException {
        throw new AppiaManagementException("Attribute "+attribute+" does not have a setter");
    }

}
