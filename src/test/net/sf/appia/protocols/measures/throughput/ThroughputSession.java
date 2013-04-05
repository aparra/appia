/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006-2007 University of Lisbon
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

package net.sf.appia.protocols.measures.throughput;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.TimeProvider;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.management.AppiaManagementException;
import net.sf.appia.management.ManagedSession;

import org.apache.log4j.Logger;

/**
 * This class defines a ThroughputSession
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class ThroughputSession extends Session implements ManagedSession {

    private static Logger log = Logger.getLogger(ThroughputSession.class);
    
    public static final String THRPUT_MSG_PER_SECOND_UP = "msg_per_second_up";
    public static final String THRPUT_MSG_PER_SECOND_DOWN = "msg_per_second_down";
    public static final String THRPUT_BYTES_PER_SECOND_UP = "bytes_per_second_up";
    public static final String THRPUT_BYTES_PER_SECOND_DOWN = "bytes_per_second_down";
    public static final String REFRESH_INTERVAL = "refresh_interval";

    private static final float MINIMUM_VALUE = 0.05F;
    private static final long DEFAULT_REFRESH_INTERVAL = 5000;

    private Throughput msgPerSecondUp, msgPerSecondDown, bytesPerSecondUp, bytesPerSecondDown;
    private boolean created = false;
    private List<Channel> channels = new ArrayList<Channel>();
    private Channel timerChannel = null;
    private Map<String,String>jmxFeaturesMap = new Hashtable<String,String>();

    private long refreshInterval = DEFAULT_REFRESH_INTERVAL;
    
    /**
     * This class defines a Throughput.
     * 
     * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
     * @version 1.0
     */
    class Throughput {
        private static final long SECOND_IN_MILLIS=1000;
        private TimeProvider timer = null;
        private long counter=0, lastTime=0;
        private float rate = 0F;
        
        Throughput(TimeProvider time){
            this.timer = time;
            lastTime = time.currentTimeMillis();
        }
        
        float get(){
            long currentTime = timer.currentTimeMillis();
            long diff = currentTime - lastTime;

            if(diff > refreshInterval && diff > 0){
                float myRate = ((float)counter/(float)diff)*SECOND_IN_MILLIS;
                myRate = (myRate<MINIMUM_VALUE)? 0 : myRate;
                rate = myRate;
                counter = 0;
                lastTime = currentTime;
            }
            return rate;
        }
        
        void add(long value){
            counter += value;
        }
        
        @Override
        public String toString(){
            return ""+get();
        }
    }

    /**
     * Creates a new ThroughputSession.
     * @param layer
     */
    public ThroughputSession(Layer layer) {
        super(layer);
    }
    
    /**
     * 
     * 
     * @see net.sf.appia.core.Session#handle(net.sf.appia.core.Event)
     */
    public void handle(Event event){
        if(event instanceof SendableEvent)
            handleSendable((SendableEvent)event);
        else if(event instanceof ChannelInit)
            handleChannelInit((ChannelInit)event);
        else if(event instanceof ChannelClose)
            handleChannelClose((ChannelClose)event);
        else if(event instanceof ThroughputDebugTimer)
            handleDebugTimer();
        else
            try {
                log.debug("Forwarding unwanted event: "+event.getClass().getName());
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
    }

    private void handleDebugTimer() {
        log.debug("Throughput going DOWN: Messages per second = "
                +msgPerSecondDown+" and bytes per second = "+bytesPerSecondDown);
        log.debug("Throughput going UP: Messages per second = "
                +msgPerSecondUp+" and bytes per second = "+bytesPerSecondUp);
    }

    private void handleChannelClose(ChannelClose close) {
        final Channel ch = close.getChannel();
        channels.remove(ch);
        
        if(log.isDebugEnabled()){
            if(timerChannel != null && timerChannel == ch){
                try {
                    new ThroughputDebugTimer(timerChannel,this,EventQualifier.OFF).go();
                    if(channels.size()>0){
                        timerChannel = channels.get(0);
                        new ThroughputDebugTimer(timerChannel,this,EventQualifier.ON).go();
                    }
                    else
                        timerChannel = null;
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                } catch (AppiaException e) {
                    e.printStackTrace();
                }
            }
        }
        
        try {
            close.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    private void handleChannelInit(ChannelInit init) {
        final TimeProvider tp = init.getChannel().getTimeProvider();
        channels.add(init.getChannel());
        if(!created){
            msgPerSecondUp = new Throughput(tp);
            msgPerSecondDown = new Throughput(tp);
            bytesPerSecondUp = new Throughput(tp);
            bytesPerSecondDown = new Throughput(tp);
            if(log.isDebugEnabled()){
                try {
                    new ThroughputDebugTimer(init.getChannel(),this,EventQualifier.ON).go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                } catch (AppiaException e) {
                    e.printStackTrace();
                }
            }
            created = true;
        }

        try {
            init.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }        
    }

    private void handleSendable(SendableEvent event) {
        if(event.getDir() == Direction.DOWN){
            msgPerSecondDown.add(1);
            bytesPerSecondDown.add(event.getMessage().length());
        }
        else{
            msgPerSecondUp.add(1);
            bytesPerSecondUp.add(event.getMessage().length());
        }
        
        try {
            event.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }

    public Object getParameter(String parameter) throws AppiaManagementException {
        if(parameter.equals(THRPUT_MSG_PER_SECOND_UP))
            return msgPerSecondUp.get();
        if(parameter.equals(THRPUT_MSG_PER_SECOND_DOWN))
            return msgPerSecondDown.get();
        if(parameter.equals(THRPUT_BYTES_PER_SECOND_UP))
            return bytesPerSecondUp.get();
        if(parameter.equals(THRPUT_BYTES_PER_SECOND_DOWN))
            return bytesPerSecondDown.get();
        if(parameter.equals(REFRESH_INTERVAL))
            return refreshInterval;
        throw new AppiaManagementException("Parameter '"+parameter+"' not defined in session "+this.getClass().getName());
    }

    public void setParameter(String parameter, Object newValue) throws AppiaManagementException {
        if(parameter.equals(REFRESH_INTERVAL)){
            refreshInterval = (Long) newValue;
            return;
        }            
        throw new AppiaManagementException("Parameter '"+parameter+"' not defined in session "+this.getClass().getName());
    }

    public MBeanOperationInfo[] getOperations(String sid) {
        return null;
    }
    
    public Object invoke(String action, MBeanOperationInfo info, Object[] params, String[] signature) 
    throws AppiaManagementException {
        throw new AppiaManagementException("The Session "+this.getClass().getName()+" does not accept any parameter to "+
        "set a new value. It is read only.");
    }

    public MBeanAttributeInfo[] getAttributes(String sid) {
        jmxFeaturesMap.put(sid+THRPUT_BYTES_PER_SECOND_DOWN,THRPUT_BYTES_PER_SECOND_DOWN);
        jmxFeaturesMap.put(sid+THRPUT_BYTES_PER_SECOND_UP,THRPUT_BYTES_PER_SECOND_UP);
        jmxFeaturesMap.put(sid+THRPUT_MSG_PER_SECOND_DOWN,THRPUT_MSG_PER_SECOND_DOWN);
        jmxFeaturesMap.put(sid+THRPUT_MSG_PER_SECOND_UP,THRPUT_MSG_PER_SECOND_UP);
        jmxFeaturesMap.put(sid+REFRESH_INTERVAL,REFRESH_INTERVAL);
        return new MBeanAttributeInfo[]{
                new MBeanAttributeInfo(sid+THRPUT_BYTES_PER_SECOND_DOWN,
                        "float","gets the throughput value",
                        true,false,false),
                        new MBeanAttributeInfo(sid+THRPUT_BYTES_PER_SECOND_UP,
                                "float","gets the throughput value",
                                true,false,false),
                                new MBeanAttributeInfo(sid+THRPUT_MSG_PER_SECOND_DOWN,
                                        "float","gets the throughput value",
                                        true,false,false),
                                        new MBeanAttributeInfo(sid+THRPUT_MSG_PER_SECOND_UP,
                                                "float","gets the throughput value",
                                                true,false,false),
                new MBeanAttributeInfo(sid+REFRESH_INTERVAL,
                        "long","gets and sets the refresh interval",
                        true,true,false),
        };
    }

    public Object attributeGetter(String attribute, MBeanAttributeInfo info) throws AppiaManagementException {
        return getParameter(jmxFeaturesMap.get(attribute));
    }
    
    public void attributeSetter(Attribute attribute, MBeanAttributeInfo info) throws AppiaManagementException {
        String att = jmxFeaturesMap.get(attribute.getName());
//        System.out.println("call: "+attribute.getName()+" att "+att);
        setParameter(att, attribute.getValue());
    }

}

