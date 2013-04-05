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
 * Initial developer(s): Alexandre Pinto and Hugo Miranda and Nuno Carvalho.
 * Contributor(s): See Appia web page for a list of contributors.
 */

package net.sf.appia.protocols.group.suspect;



import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeChangeNotification;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.Notification;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.TimeProvider;
import net.sf.appia.core.events.AppiaMulticast;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.management.AbstractSensorSession;
import net.sf.appia.management.AppiaManagementException;
import net.sf.appia.management.ManagedSession;
import net.sf.appia.protocols.common.FIFOUndeliveredEvent;
import net.sf.appia.protocols.group.ArrayOptimized;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.events.Send;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.tcpcomplete.TcpUndeliveredEvent;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import org.apache.log4j.Logger;

/** The <I>Appia</I> failure detector.
 * @see net.sf.appia.protocols.group.suspect.SuspectLayer
 * @author Alexandre Pinto
 */
public class SuspectSession extends AbstractSensorSession implements InitializableSession, ManagedSession {
    private static Logger log = Logger.getLogger(SuspectSession.class);

    /** Default duration of a round.
     */
    public static final long DEFAULT_SUSPECT_SWEEP=2000; //in milliseconds
    /** Default time to suspect a member.
     * <br>
     * This value is converted in number of rounds
     */
    public static final long DEFAULT_SUSPECT_TIME=5000; //in milliseconds
    
    /** 
     * Major debug mode.
     */
    public static final boolean debugFull=false;
    
    private ViewState vs;
    private LocalState ls;

    private long suspect_sweep=DEFAULT_SUSPECT_SWEEP;
    private long rounds_idle=calcRoundsIdle(DEFAULT_SUSPECT_SWEEP, DEFAULT_SUSPECT_TIME);
    private long round=0;
    private long[] last_recv=new long[0];
    private TimeProvider time = null;
    
    private static final String GET_TIME="get_suspect_time";
    private static final String GET_SWEEP="get_suspect_sweep";
    private static final String SET_TIME="set_suspect_time";
    private static final String SET_SWEEP="set_suspect_sweep";
    MBeanOperationInfo[] mboi = null;
    private Map<String,String> operationsMap=new Hashtable<String,String>();

    /** Creates a new Suspect session.
     */  
    public SuspectSession(Layer layer) {
        super(layer);
    }

    /**
     * Initializes the session using the parameters given in the XML configuration.
     * Possible parameters:
     * <ul>
     * <li><b>suspect_sweep</b> duration of a round in milliseconds.
     * <li><b>suspect_time</b> time to suspect a member, in milliseconds. 
     * This value is converted in number of rounds and added 1 
     * (because rounds may not be synchronized)
     * </ul>
     * 
     * @param params The parameters given in the XML configuration.
     * @see net.sf.appia.xml.interfaces.InitializableSession#init(SessionProperties)
     */
    public void init(SessionProperties params) {
        if (params.containsKey("suspect_sweep"))
            suspect_sweep=params.getLong("suspect_sweep");
        if (params.containsKey("suspect_time"))
            rounds_idle=calcRoundsIdle(suspect_sweep, params.getLong("suspect_time"));
    }

    /**
     * Change a parameter of a session.
     * Possible parameters:
     * <ul>
     * <li><b>suspect_sweep</b> duration of a round in milliseconds.
     * <li><b>suspect_time</b> time to suspect a member, in milliseconds. 
     * This value is converted in number of rounds and added 1 
     * (because rounds may not be synchronized)
     * </ul>
     * 
     * @see net.sf.appia.management.ManagedSession#setParameter(java.lang.String, java.lang.String)
     */
    public void setParameter(String parameter, Long value) throws AppiaManagementException {
        Notification notif = null;
        if (parameter.equals(SET_SWEEP)){
            final Long oldValue = new Long(suspect_sweep);
            suspect_sweep=value.longValue();
            notif = new AttributeChangeNotification(this,1,time.currentTimeMillis(),"Suspect sweep Changed",
                    "suspect_sweep",Long.class.getName(),oldValue,value);
        }
        else if (parameter.equals(SET_TIME)){
            final Long oldValue = new Long(rounds_idle);
            rounds_idle=(value.longValue()/suspect_sweep)+1;
            notif = new AttributeChangeNotification(this,1,time.currentTimeMillis(),"Rounds idle Changed",
                    "rounds_idle",Long.class.getName(),oldValue,new Long(rounds_idle));
        }
        else 
            throw new AppiaManagementException("The session "+SuspectSession.class.getName()
                    +" do not accept the parameter '"+parameter+"'.");
        if(notif != null)
            notifySensorListeners(notif);
    }

    public Object invoke(String action, MBeanOperationInfo info, Object[] params, String[] signature) 
        throws AppiaManagementException{

        if(info.getImpact() == MBeanOperationInfo.ACTION){
            if(params.length == 1 && signature[0].equals("java.lang.Long")){
                setParameter(operationsMap.get(action), (Long)params[0]);
                return null;
            }
            else throw new AppiaManagementException("Action "+action+" called with the wrong parameters");
        }
        else throw new AppiaManagementException("Action "+action+" is not accepted");
    }
    
    public MBeanOperationInfo[] getOperations(String sessionID) {
        createMBeanOperations(sessionID);
        return mboi;
    }
    
    public Object attributeGetter(String parameter, MBeanAttributeInfo info) throws AppiaManagementException {
            return getParameter(operationsMap.get(parameter));        
    }
    
    private void createMBeanOperations(String sessionID){
        if(mboi != null)
            return;
        mboi = new MBeanOperationInfo[2];
        mboi[0] = new MBeanOperationInfo(sessionID+SET_SWEEP,"sets the suspect sweep",
                new MBeanParameterInfo[]{new MBeanParameterInfo("sweep","java.lang.Long","sets the suspect sweep")},
                "void",
                MBeanOperationInfo.ACTION);
        mboi[1] = new MBeanOperationInfo(sessionID+SET_TIME,"sets the suspect time",
                new MBeanParameterInfo[]{new MBeanParameterInfo("time","java.lang.Long","sets the suspect time")},
                "void",
                MBeanOperationInfo.ACTION);        
        operationsMap.put(sessionID+SET_SWEEP, SET_SWEEP);
        operationsMap.put(sessionID+SET_TIME, SET_TIME);
    }

    public MBeanAttributeInfo[] getAttributes(String sessionID){
        MBeanAttributeInfo[] mbai = new MBeanAttributeInfo[2];
        mbai[0] = new MBeanAttributeInfo(sessionID+GET_SWEEP,this.getClass().getName(),
                "gets the suspect sweep",true,false,false);
        mbai[1] = new MBeanAttributeInfo(sessionID+GET_TIME,this.getClass().getName(),
                "gets the suspect time",true,false,false);
        operationsMap.put(sessionID+GET_SWEEP, GET_SWEEP);
        operationsMap.put(sessionID+GET_TIME, GET_TIME);
        return mbai;
    }


    public long getSuspectSweep(){
        return suspect_sweep;
    }
    
    public long getSuspectTime(){
        return rounds_idle*suspect_sweep;
    }
    
    /*
     * Gets the value of a parameter.
     * Possible parameters:
     * <ul>
     * <li><b>suspect_sweep</b> duration of a round in milliseconds.
     * <li><b>suspect_time</b> time to suspect a member, in milliseconds. 
     * This value is converted in number of rounds and added 1 
     * (because rounds may not be synchronized)
     * </ul>
     * 
     * @see net.sf.appia.management.ManagedSession#getParameter(java.lang.String)
     */
    public Object getParameter(String parameter) throws AppiaManagementException{
        if (parameter.equals(GET_SWEEP))
            return getSuspectSweep();
        if (parameter.equals(GET_TIME))
            return getSuspectTime();
        throw new AppiaManagementException("Parameter '"+parameter+"' not defined in session "+SuspectSession.class.getName());
    }

    /** 
     * Event handler.
     */  
    public void handle(Event event) {

        // Suspect
        if (event instanceof Suspect) {
            handleSuspect((Suspect)event); return;
            // GroupSendableEvent
        } else if (event instanceof GroupSendableEvent) {
            handleGroupSendableEvent((GroupSendableEvent)event); return;
            // SuspectTimer
        } else if (event instanceof SuspectTimer) {
            handleSuspectTimer((SuspectTimer)event); return;
            // View
        } else if (event instanceof View) {
            handleView((View)event); return;
            // FIFOUndeliveredEvent
        } else if (event instanceof FIFOUndeliveredEvent) {
            handleFIFOUndeliveredEvent((FIFOUndeliveredEvent)event); return;
            // TcpUndeliveredEvent
        } else if (event instanceof TcpUndeliveredEvent) {
            handleTcpUndeliveredEvent((TcpUndeliveredEvent)event); return;
        } else if (event instanceof SuspectedMemberEvent) {
            handleSuspectedMember((SuspectedMemberEvent)event); 
            return;
        } else if (event instanceof ChannelInit){
            handleChannelInit((ChannelInit)event); return;
        }

        log.warn("Unwanted event (\""+event.getClass().getName()+"\") received. Continued...");
        try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    /*
     * handles ChannelInit
     * @param init
     */
    private void handleChannelInit(ChannelInit init) {
        try {
            init.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
        time = init.getChannel().getTimeProvider();
    }

    private void handleView(View ev) {
        vs=ev.vs;
        ls=ev.ls;

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        if (round == 0) {
            try {
                SuspectTimer periodic=new SuspectTimer("Suspect Timer",suspect_sweep,ev.getChannel(),this);
                periodic.go();
            } catch (AppiaException ex) {
                ex.printStackTrace();
                System.err.println("appia:group:SuspectSession: impossible to set SuspectTimer, SuspectSession will be idle");
            }
        }

        if (vs.view.length != last_recv.length) {
            last_recv=new long[vs.view.length];
        }
        round=1;
        Arrays.fill(last_recv,round);
    }

    private void handleGroupSendableEvent(GroupSendableEvent ev) {
        if (ev instanceof Send) {
            try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
            return;
        }

        if (ev.getDir() == Direction.DOWN) {
            last_recv[ls.my_rank]=round;
            if (debugFull)
                log.debug("Sent msg ("+ev+") in round "+round);
        } else {
            last_recv[ev.orig]=round;
            if (debugFull)
                log.debug("Recv msg from "+ev.orig+" in round "+round);
        }

        if (ev instanceof Alive)
            return;

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private void handleSuspect(Suspect ev) {

        log.debug("Received Suspect from "+ev.orig);
        if (ev.getDir() == Direction.UP) {
            if ( ls.failed[ev.orig] ) {
                log.debug("Invalid (failed) message source");
                return;
            }

            ev.failed=ArrayOptimized.popArrayBoolean(ev.getMessage());
            if(log.isDebugEnabled()){
                log.debug("Failed on suspect: ");
                for(int i=0; i<ev.failed.length; i++)
                    log.debug("Member "+i+" failed="+ev.failed[i]);
            }

        }

        if (ev.failed[ls.my_rank]) {
            log.debug("i am not failed !!");
            return;
        }

        int i;
        boolean[] new_failed=null;

        for (i=0 ; i < ev.failed.length ; i++) {
            if (ev.failed[i] && !ls.failed[i]) {
                ls.fail(i);
                if (new_failed == null) {
                    new_failed=new boolean[ls.failed.length];
                    Arrays.fill(new_failed,false);
                }
                new_failed[i]=true;
                if(log.isDebugEnabled())
                    log.debug("Member "+i+" has failed. Setting its flag in the array.");
            }
        }

        if (new_failed != null) {
            if (ev.getDir() == Direction.DOWN) {
                ArrayOptimized.pushArrayBoolean(ls.failed,ev.getMessage());
                try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
            }

            sendFail(new_failed,ev.getChannel());
        }
    }

    private void handleSuspectTimer(SuspectTimer ev) {
        if(ev.getPeriod() != suspect_sweep){
            ev.setDir(Direction.invert(ev.getDir()));
            ev.setQualifierMode(EventQualifier.OFF);
            ev.setSourceSession(this);
            try {
                ev.init();
                ev.go();
                SuspectTimer periodic=new SuspectTimer("Suspect Timer",suspect_sweep,ev.getChannel(),this);
                periodic.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            } catch (AppiaException ex) {
                ex.printStackTrace();
                log.error("appia:group:SuspectSession: impossible to set SuspectTimer, SuspectSession will be idle");
            }
            return;
        }
        
        try { 
            ev.go(); 
        } catch (AppiaEventException ex) {
            ex.printStackTrace(); 
        }
            
        int i;
        boolean[] new_failed=null;

        for (i=0 ; i < last_recv.length ; i++) {
            if (i != ls.my_rank) {
                if ( (round-last_recv[i] >= rounds_idle) && !ls.failed[i] ) {
                    ls.fail(i);
                    if (new_failed == null) {
                        new_failed=new boolean[ls.failed.length];
                        Arrays.fill(new_failed,false);
                    }
                    new_failed[i]=true;

                    log.debug("Suspected "+i+" because it passed "+(round-last_recv[i])+" rounds of "+suspect_sweep+" milliseconds since last reception");
                }
            }
        }

        if (new_failed != null) {
            sendSuspect(new_failed,ev.getChannel());
            sendFail(new_failed,ev.getChannel());

            if (log.isDebugEnabled()) {
                String s="New failed members: ";
                for (int j=0 ; j < new_failed.length ; j++)
                    if (new_failed[j])
                        s=s+j+",";
                log.debug(s);
            }    
        }

        if (round > last_recv[ls.my_rank]) {
            sendAlive(ev.getChannel());
            last_recv[ls.my_rank]=round;
            if(debugFull)
                log.debug("Sent Alive in round "+round);
        }

        if (debugFull)
            log.debug("Ended round "+round+" at "+ev.getChannel().getTimeProvider().currentTimeMillis()+" milliseconds");

        round++;

        // this should be here because after a long time, this value can reach Long.MAX_VALUE
        if (round < 0) {
            round=1;
            for (i=0 ; i < last_recv.length ; i++)
                last_recv[i]=0;
        }    
    }

    private void handleFIFOUndeliveredEvent(FIFOUndeliveredEvent ev) {
        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        if (vs == null)
            return;

        if (!(ev.getEvent() instanceof GroupSendableEvent))
            return;

        final GroupSendableEvent event=(GroupSendableEvent)ev.getEvent();

        if (!vs.group.equals(event.group)) {
            log.debug("Ignored FIFOUndelivered due to wrong group");
            return;
        }

        if (!vs.id.equals(event.view_id)) {
            log.debug("Ignored FIFOUndelivered due to wrong view id");
            return;
        }

        if (event.dest instanceof InetSocketAddress)
            undelivered((InetSocketAddress)event.dest,ev.getChannel());
        else if (event.dest instanceof AppiaMulticast) {
            Object[] dests=((AppiaMulticast)event.dest).getDestinations();
            for (int i=0 ; i < dests.length ; i++) {
                if (dests[i] instanceof InetSocketAddress)
                    undelivered((InetSocketAddress)dests[i],ev.getChannel());
            }
        } else
            log.debug("Received FIFOUndelivered with unknown destination address. Ignoring it.");
    }

    private void handleTcpUndeliveredEvent(TcpUndeliveredEvent ev) {
        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        if (vs == null)
            return;

        undelivered((InetSocketAddress)ev.getFailedAddress(),ev.getChannel());
    }
    
    private void handleSuspectedMember(SuspectedMemberEvent ev){
        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
        if (vs == null)
            return;
        if(!ev.getGroup().equals(vs.group) && !ev.getViewID().equals(vs.id)){
            log.debug("SuspectedMemberEvent from another group or view. Discarding it");
            return;
        }
        if(ev.getSuspectedMember() >= 0)
            processUndelivered(ev.getSuspectedMember(),ev.getChannel());        
        else
            log.debug("SuspectedMemberEvent didn't contain a valid view member.");
    }

    private void undelivered(InetSocketAddress addr, Channel channel) {
        int rank;

        if ((rank=vs.getRankByAddress(addr)) >= 0) {
            processUndelivered(rank, channel);
        } else
            log.debug("Undelivered didn't contain a current view member");
    }

    private void processUndelivered(int rank, Channel channel) {
        if (!ls.failed[rank]) {
            ls.fail(rank);
            boolean[] new_failed=new boolean[vs.view.length];
            for (int i=0 ; i < new_failed.length ; i++) 
                new_failed[i]=(i==rank);            
            sendSuspect(ls.failed,channel);
            sendFail(new_failed,channel);
            log.debug("Suspected member "+rank+" due to Undelivered");
        }
    }

    private void sendSuspect(boolean[] failed, Channel channel) {
        try {
            Suspect ev=new Suspect(failed,channel,Direction.DOWN,this,vs.group,vs.id);
            ArrayOptimized.pushArrayBoolean(ls.failed,ev.getMessage());
            ev.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            log.warn("Impossible to send Suspect");
        }
    }

    private void sendFail(boolean[] failed, Channel channel) {
        try {
            Fail ev=new Fail(failed,vs.group,vs.id);
            EchoEvent echo=new EchoEvent(ev,channel,Direction.DOWN,this);
            echo.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            System.err.println("appia:group:SuspectSession: impossible to inform locally of failure");
        }
    }

    private void sendAlive(Channel channel) {
        if (vs.view.length < 2)
            return;

        try {
            Alive alive=new Alive(channel,Direction.DOWN,this,vs.group,vs.id);
            alive.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            log.warn("Impossible to send alive");
        }
    }

    private long calcRoundsIdle(long suspect_sweep, long suspect_time) {
        long r=suspect_time/suspect_sweep; // number of rounds that corresponds to the time given
        if ((suspect_time % suspect_sweep) != 0)
            r++; // correction if suspect time is not dividable by suspect sweep
        return r;
    }

    public void attributeSetter(Attribute attribute, MBeanAttributeInfo info) throws AppiaManagementException {
    }

}