/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006-2009 University of Lisbon / Technical University of Lisbon / INESC-ID
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

package net.sf.appia.protocols.group.phiSuspect;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import net.sf.appia.protocols.common.FIFOUndeliveredEvent;
import net.sf.appia.protocols.group.ArrayOptimized;
import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.suspect.Alive;
import net.sf.appia.protocols.group.suspect.Fail;
import net.sf.appia.protocols.group.suspect.Suspect;
import net.sf.appia.protocols.tcpcomplete.TcpUndeliveredEvent;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import org.apache.log4j.Logger;

import flanagan.analysis.Stat;

/** The Phi failure detector, by Naohiro Hayashibara.
 * @see net.sf.appia.protocols.group.phiSuspect.PhiSuspectLayer
 * @author Dan Mihai Dumitriu
 */
public class PhiSuspectSession extends AbstractSensorSession implements InitializableSession {
	
    private static Logger log = Logger.getLogger(PhiSuspectSession.class);

    private static long IMMUNITY_THRESHOLD = 4;
    private int sampleWindowSize_ = 100;
    private int phiSuspectThreshold_ = 5;
    private long aliveInterval_ = 100; // very short
    
//    private long lastTimeSent_;
    
    private ViewState vs;
    private LocalState ls;

    private TimeProvider time = null;
    
    private Map<Endpt, PeerState> windows_ = new HashMap<Endpt, PeerState>();
    
    private boolean firstView_ = true;
    
    /** Creates a new Suspect session.
     */  
    public PhiSuspectSession(Layer layer) {
        super(layer);
    }

    /**
     * Initializes the session using the parameters given in the XML configuration.
     * Possible parameters:
     * <ul>
     * 
     * </ul>
     * 
     * @param params The parameters given in the XML configuration.
     * @see org.continuent.appia.xml.interfaces.InitializableSession#init(SessionProperties)
     */
    public void init(SessionProperties params) {
        if (params.containsKey("alive_interval"))
        	aliveInterval_=params.getLong("alive_interval");
        if (params.containsKey("window_size"))
        	sampleWindowSize_=params.getInt("window_size");
        if (params.containsKey("suspect_threshold"))
        	phiSuspectThreshold_=params.getInt("suspect_threshold");
    }

//    /**
//     * Change a parameter of a session.
//     * Possible parameters:
//     * <ul>
//     * <li><b>alive_interval</b> the frequency of sending alive messages.
//     * <li><b>suspect_threshold</b> the phi threshold at which we suspect.
//     * This value is converted in number of rounds and added 1 
//     * (because rounds may not be synchronized)
//     * </ul>
//     * 
//     * @see org.continuent.appia.management.ManagedSession#setParameter(java.lang.String, java.lang.String)
//     */
//    public void setParameter(String parameter, String value) throws AppiaManagementException {
//        Notification notif = null;
//        
//        if (parameter.equals("alive_interval")){
//            final Long oldValue = new Long(aliveInterval_);
//            final Long newValue = new Long(value);
//            aliveInterval_=newValue.longValue();
//            notif = new AttributeChangeNotification(this,1,time.currentTimeMillis(),"Alive Interval Changed",
//                    "alive_interval",Long.class.getName(),oldValue,newValue);
//        } else if (parameter.equals("suspect_threshold")){
//            final Integer oldValue = new Integer(phiSuspectThreshold_);
//            final Integer newValue = new Integer(value);
//            phiSuspectThreshold_=newValue.intValue();
//            notif = new AttributeChangeNotification(this,1,time.currentTimeMillis(),"Suspect Threshold Changed",
//                    "suspect_threshold",Integer.class.getName(),oldValue,newValue);
//        }
//        else 
//            throw new AppiaManagementException("The session "+PhiSuspectSession.class.getName()
//                    +" do not accept the parameter '"+parameter+"'.");
//        
//        if(notif != null)
//            notifySensorListeners(notif);
//    }
//
//    /**
//     * Gets the value of a parameter.
//     * Possible parameters:
//     * <ul>
//     * <li><b>suspect_sweep</b> duration of a round in milliseconds.
//     * <li><b>suspect_time</b> time to suspect a member, in milliseconds. 
//     * This value is converted in number of rounds and added 1 
//     * (because rounds may not be synchronized)
//     * </ul>
//     * 
//     * @see org.continuent.appia.management.ManagedSession#getParameter(java.lang.String)
//     */
//    public String getParameter(String parameter) throws AppiaManagementException{
//        if (parameter.equals("alive_interval"))
//            return ""+aliveInterval_;
//        throw new AppiaManagementException("Parameter '"+parameter+"' not defined in session "+PhiSuspectSession.class.getName());
//    }

    /** 
     * Event handler.
     */  
    public void handle(Event event) {

        	// Suspect
        if (event instanceof Suspect) {
            handleSuspect((Suspect)event); return;
            // Alive
        } else if (event instanceof Alive) {
            handleAliveEvent((Alive)event); return;
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

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        if (firstView_) {
            try {
                SuspectTimer periodic=new SuspectTimer("Suspect Timer", aliveInterval_, ev.getChannel(), this);
                periodic.go();
            } catch (AppiaException ex) {
                ex.printStackTrace();
                System.err.println("appia:group:SuspectSession: impossible to set SuspectTimer, SuspectSession will be idle");
            }
            
            firstView_ = false;
            
            for (int i=0; i<ev.vs.view.length; i++)
            	if (i != ev.ls.my_rank) // don't add self
            		windows_.put(ev.vs.view[i], new PeerState(time.currentTimeMillis()));
            
        } else {
        	
        	// remove dead members
        	Endpt[] dead = ev.vs.getDeadMembers(vs);
        	for (Endpt e : dead)
        		windows_.remove(e);

        	// create state for new members
        	Endpt[] added = ev.vs.getNewMembers(vs);
        	for (Endpt e : added)
        		windows_.put(e, new PeerState(time.currentTimeMillis()));
            
        }
        
        vs=ev.vs;
        ls=ev.ls;
    }

    private void handleAliveEvent(Alive ev) {

        if (ev.getDir() == Direction.UP) {
            if (debugFull)
                log.debug("Recv msg from "+ev.orig+"@"+time.currentTimeMillis());
            
            PeerState window = windows_.get(vs.view[ev.orig]);
            window.observeArrival(time.currentTimeMillis());
        }

    }

    private void handleSuspect(Suspect ev) {

        if (ev.getDir() == Direction.UP) {
            if ( ls.failed[ev.orig] ) {
                log.debug("Invalid (failed) message source");
                return;
            }

            ev.failed=ArrayOptimized.popArrayBoolean(ev.getMessage());
        }

        if (ev.failed[ls.my_rank]) {
            log.debug("i am not failed, but someone suspected me!!");
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
            }
        }

        if (new_failed != null) {
            if (ev.getDir() == Direction.DOWN) {
                ArrayOptimized.pushArrayBoolean(ls.failed,ev.getMessage());
                //ev.getObjectsMessage().push(ls.failed);
                try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
            }

            sendFail(new_failed,ev.getChannel());
        }
    }

    private void handleSuspectTimer(SuspectTimer ev) {
    	
        if(ev.getPeriod() != aliveInterval_) {
            ev.setDir(Direction.invert(ev.getDir()));
            ev.setQualifierMode(EventQualifier.OFF);
            ev.setSourceSession(this);
            
            try {
                ev.init();
                ev.go();
                SuspectTimer periodic=new SuspectTimer("Suspect Timer",aliveInterval_,ev.getChannel(),this);
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
        
        /*
         * compute time that we cannot detect failure
         * let's wait for IMMUNITY_THRESHOLD% of our window to fill up
         * that's 100 samples by default
         */
        long immunityTime_ = aliveInterval_ * sampleWindowSize_/IMMUNITY_THRESHOLD;
            
        boolean[] new_failed=null;

        long now = time.currentTimeMillis();
        
        for (Map.Entry<Endpt, PeerState> e : windows_.entrySet()) {
        	
        	// check if this peer is still immune from suspicion
        	if (now < e.getValue().getTimeCreated() + immunityTime_)
        		continue;
        	
        	double phi = e.getValue().getPhi(now);
        	
        	int rank = vs.getRank(e.getKey());
        	
			if (phi > phiSuspectThreshold_ && !ls.failed(rank)) {
				ls.fail(rank);
				
				if (new_failed == null) {
					new_failed = new boolean[ls.failed.length];
					Arrays.fill(new_failed, false);
				}
				new_failed[rank] = true;
				
//				double elapsed = now - e.getValue().lastTimeReceived_;
				
				log.debug("Suspected "+e.getKey()+" because its phi is " + phi);
			}
		}
        
        if (new_failed != null) {
            sendSuspect(new_failed,ev.getChannel());
            sendFail(new_failed,ev.getChannel());

            if (debugFull) {
                String s="New failed members: ";
                for (int j=0 ; j < new_failed.length ; j++)
                    if (new_failed[j])
                        s=s+j+",";
                log.debug(s);
            }    
        }

//        lastTimeSent_ = now;
        sendAlive(ev.getChannel());
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

    private void undelivered(InetSocketAddress addr, Channel channel) {
        int rank,i;

        if ((rank=vs.getRankByAddress(addr)) >= 0) {
            if (!ls.failed[rank]) {
                ls.fail(rank);

                boolean[] new_failed=new boolean[vs.view.length];
                for (i=0 ; i < new_failed.length ; i++)
                	new_failed[i]=(i==rank);

                sendSuspect(ls.failed,channel);
                sendFail(new_failed,channel);

                log.debug("Suspected member "+rank+" due to Undelivered");
            }
        } else
            log.debug("Undelivered didn't contain a current view member");
    }

    private void sendSuspect(boolean[] failed, Channel channel) {
        try {
            Suspect ev=new Suspect(failed,channel,Direction.DOWN,this,vs.group,vs.id);
            ArrayOptimized.pushArrayBoolean(ls.failed,ev.getMessage());
            //ev.getObjectsMessage().push(ls.failed);
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

    // DEBUG
    /** Major debug mode.
     */
    public static final boolean debugFull=false;
    
    class PeerState {
    	
    	long timeCreated_;
		long lastTimeReceived_ = 0L;
		List<Double> arrivalIntervals_ = new ArrayList<Double>();
		private int maxWindowSize_;
		private double sum_;

		PeerState(long created) {
			timeCreated_ = created;
			
			maxWindowSize_ = sampleWindowSize_;

			/*
			 * add two dummy samples so that mean is not zero and stddev is not NaN
			 */
			addSample((double) aliveInterval_);
			addSample((double) aliveInterval_);
		}

		long getTimeCreated() {
			return timeCreated_;
		}
		
		void addSample(double sample) {
			arrivalIntervals_.add(sample);
			sum_ += sample;
		}
		
		void observeArrival(long now) {
			
			if (arrivalIntervals_.size() == maxWindowSize_) {
				sum_ -= arrivalIntervals_.remove(0);
			}

			if (lastTimeReceived_ > 0L) {
				double interArrivalTime = now - lastTimeReceived_;
				System.out.println(interArrivalTime+"");
				addSample(interArrivalTime);
			}
			
			lastTimeReceived_ = now;
			
		}

		double mean() {	
			return sum_ / arrivalIntervals_.size();
		}

		double deviation() {
			return Math.sqrt(variance());
		}

		double variance() {
			
			double mean = mean();
			
			double sumOfDeviations = 0.0F;
			for (double l : arrivalIntervals_) {
				sumOfDeviations += (l - mean) * (l - mean);
			}
			
			return sumOfDeviations / arrivalIntervals_.size();
			
		}

		double p(double current) {

			double mean = mean();
			double deviation = deviation();

			// integrate from current position until infinity
			return Stat.gaussianCDF(mean, deviation, current, Double.POSITIVE_INFINITY);
			
		}

		double getPhi(long now) {

			double timeElapsed = now - lastTimeReceived_;
			double probability = p(timeElapsed);
			
			return (-1) * Math.log10(probability);

		}
	}

}
