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

package net.sf.appia.protocols.tcpcomplete;

import java.util.Hashtable;
import java.util.Map;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;

import net.sf.appia.core.TimeProvider;
import net.sf.appia.management.AppiaManagementException;

/**
 * This class defines a Measures
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class Measures {

    public static final String THRPUT_MSG_PER_SECOND_UP = "msg_per_second_up";
    public static final String THRPUT_MSG_PER_SECOND_DOWN = "msg_per_second_down";
    public static final String THRPUT_BYTES_PER_SECOND_UP = "bytes_per_second_up";
    public static final String THRPUT_BYTES_PER_SECOND_DOWN = "bytes_per_second_down";
    public static final String REFRESH_INTERVAL = "refresh_interval";
    public static final String QUEUE_SIZE = "queue_size";

    private static final float MINIMUM_VALUE = 0.05F;
    private static final long DEFAULT_REFRESH_INTERVAL = 5000;

    private Throughput msgPerSecondUp, msgPerSecondDown, bytesPerSecondUp, bytesPerSecondDown;
    private Map<String,String>jmxFeaturesMap = new Hashtable<String,String>();

    private long refreshInterval = DEFAULT_REFRESH_INTERVAL;
    
    private TcpCompleteSession session;

    /**
     * This class defines a Throughput.
     * 
     * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
     * @version 1.0
     */
    class Throughput {
        private static final long SECOND_IN_MILLIS=1000;
        private TimeProvider timeProvider = null;
        private long counter=0, lastTime=0;
        private float rate = 0F;
        
        Throughput(TimeProvider time){
            this.timeProvider = time;
            lastTime = getTime();
        }
        
        Throughput(){
            this.timeProvider = null;
            lastTime = getTime();
        }

        
        float get(){
            long currentTime = getTime();
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
        
        void setTimeProvider(TimeProvider tp){
            timeProvider = tp;
        }
        
        long getTime(){
            return (timeProvider == null)? System.currentTimeMillis() : timeProvider.currentTimeMillis();
        }
        
        @Override
        public String toString(){
            return ""+get();
        }
    }
    
    public Measures(TimeProvider tp, TcpCompleteSession s){
        msgPerSecondUp = new Throughput(tp);
        msgPerSecondDown = new Throughput(tp);
        bytesPerSecondUp = new Throughput(tp);
        bytesPerSecondDown = new Throughput(tp);
        session = s;
    }

    public Measures(TcpCompleteSession s){
        msgPerSecondUp = new Throughput();
        msgPerSecondDown = new Throughput();
        bytesPerSecondUp = new Throughput();
        bytesPerSecondDown = new Throughput();
        session = s;
    }

    public void setTimeProvider(TimeProvider tp){
        msgPerSecondUp.setTimeProvider(tp);
        msgPerSecondDown.setTimeProvider(tp);
        bytesPerSecondUp.setTimeProvider(tp);
        bytesPerSecondDown.setTimeProvider(tp);        
    }
    
    public void countMessagesUp(int count){
        msgPerSecondUp.add(count);
    }
    
    public void countMessagesDown(int count){
        msgPerSecondDown.add(count);        
    }

    public void countBytesUp(int count){
        bytesPerSecondUp.add(count);
    }

    public void countBytesDown(int count){
        bytesPerSecondDown.add(count);
    }
    
    private Object getParameter(String parameter) throws AppiaManagementException {
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
        if(parameter.equals(QUEUE_SIZE))
            return session.getGlobalQueueSize();
        throw new AppiaManagementException("Parameter '"+parameter+"' not defined in session "+this.getClass().getName());
    }

    private void setParameter(String parameter, Object newValue) throws AppiaManagementException {
        if(parameter.equals(REFRESH_INTERVAL)){
            refreshInterval = (Long) newValue;
            return;
        }            
        throw new AppiaManagementException("Parameter '"+parameter+"' not defined in session "+this.getClass().getName());
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
        jmxFeaturesMap.put(sid+QUEUE_SIZE,QUEUE_SIZE);
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
                                                        new MBeanAttributeInfo(sid+QUEUE_SIZE,
                                                                "long","gets the queue size of sending messages",
                                                                true,false,false),
        };
    }

    public Object attributeGetter(String attribute, MBeanAttributeInfo info) throws AppiaManagementException {
        return getParameter(jmxFeaturesMap.get(attribute));
    }
    
    public void attributeSetter(Attribute attribute, MBeanAttributeInfo info) throws AppiaManagementException {
        String att = jmxFeaturesMap.get(attribute.getName());
        System.out.println("call: "+attribute.getName()+" att "+att);
        setParameter(att, attribute.getValue());
    }

}
