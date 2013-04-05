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
 /**
 * Title:        Appia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Nuno Carvalho and Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Nuno Carvalho and Luis Rodrigues
 * @version 1.0
 */

package net.sf.appia.management.jmx;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.ListIterator;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanFeatureInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.ReflectionException;

import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.management.AppiaManagementException;
import net.sf.appia.management.ManagedSession;
import net.sf.appia.management.SensorSessionListener;

import org.apache.log4j.Logger;


/**
 * This class defines a ChannelManager.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class ChannelManager extends NotificationBroadcasterSupport 
implements DynamicMBean, SensorSessionListener {
	
    private static Logger log = Logger.getLogger(ChannelManager.class);
    
    private static final String LOCALATT_USED_MEMORY = "usedMemory";
    
    private class Operation<T extends MBeanFeatureInfo>{
        T operation;
        ManagedSession session;
        Operation(T op, ManagedSession s){
            operation = op;
            session = s;
        }
    }
    
	private Channel channel;
    // session name -> session
    private Map<String,Session> managedSessions;
    // exported operation name -> session to call
    private Map<String,Operation<MBeanOperationInfo>> operations;
    private Map<String,Operation<MBeanAttributeInfo>> attributes;
    
    private MBeanInfo mbeanInfo;
    private ArrayList<MBeanOperationInfo>mboi;
    private ArrayList<MBeanAttributeInfo>mbai;

    /**
     * Creates a new ChannelManager.
     * @param ch the managed channel.
     */
    public ChannelManager(Channel ch){
        channel = ch;
        managedSessions = new Hashtable<String,Session>();
        operations = new Hashtable<String,Operation<MBeanOperationInfo>>();
        attributes = new Hashtable<String,Operation<MBeanAttributeInfo>>();
        mboi = new ArrayList<MBeanOperationInfo>();
        mbai = new ArrayList<MBeanAttributeInfo>();
        mbai.add(new MBeanAttributeInfo(LOCALATT_USED_MEMORY,"gets the memory used by this channel",
                this.getClass().getName(),true,false,false));
        updateMBeanInfo();
    }

    /**
     * Adds a session to manage.
     * @param s the session to manage.
     */
    public void addManagedSession(Session s){
        managedSessions.put(s.getId(),s);
        if(s instanceof ManagedSession){
            final ManagedSession ms = (ManagedSession) s;
            final MBeanOperationInfo[] ops = ms.getOperations(s.getId()+":");
            if(ops != null){
                for(int i=0; i<ops.length; i++){
                    operations.put(ops[i].getName(), new Operation<MBeanOperationInfo>(ops[i],ms));
                    mboi.add(ops[i]);
                }
            }
            final MBeanAttributeInfo[] atts = ms.getAttributes(s.getId()+":");
            if(atts != null){
                for(int i=0; i<atts.length;i++){
                    attributes.put(atts[i].getName(), new Operation<MBeanAttributeInfo>(atts[i],ms));
                    mbai.add(atts[i]);
                }
                
            }
            updateMBeanInfo();
        }
    }

    /**
     * Removes a session to manage.
     * @param s the session to manage
     * @return the removed session, or null if no session was removed.
     */
    public Session removeManagedSession(Session s){
        final Session session = managedSessions.remove(s.getId());
        if(session instanceof ManagedSession){
            final ManagedSession ms = (ManagedSession) session;
            final MBeanOperationInfo[] ops = ms.getOperations(s.getId()+":");
            if(ops != null){
                for(int i=0; i<ops.length; i++){
                    operations.remove(ops[i].getName());
                    mboi.remove(ops[i]);
                }
            }
            final MBeanAttributeInfo[] atts = ms.getAttributes(s.getId()+":");
            if(atts != null){
                for(int i=0; i<atts.length;i++){
                    attributes.remove(atts[i].getName());
                    mbai.remove(atts[i]);
                }
            }
            updateMBeanInfo();
        }
        return session;
    }    

    /**
     * Callback that receives a notification from the channel. Received the notification and pushes it
     * to the registered clients.
     * 
     * @param notification the received notification
     * @see net.sf.appia.management.SensorSessionListener#onNotification(javax.management.Notification)
     */
	public void onNotification(Notification notification) {
		notification.setSource(this);
		sendNotification(notification);
	}

    /**
     * Gets the name of the managed channel.
     * 
     * @see net.sf.appia.management.jmx.ChannelManagerMBean#getChannelName()
     */
	public String getChannelName() {
		return channel.getChannelID();
	}

    public boolean getStarted() {
        return channel.isStarted();
    }
    
    public int getUsedMemory(){
        if(channel.getMemoryManager() == null)
            return -1;
        else
            return channel.getMemoryManager().used();
    }

    public Object getAttribute(String att) throws AttributeNotFoundException, MBeanException, ReflectionException {
        if(log.isDebugEnabled())
            log.debug("GET from DynamicMBean: "+att);
        if(att.equals(LOCALATT_USED_MEMORY))
            return getUsedMemory();
        
        final Operation<MBeanAttributeInfo> op = attributes.get(att);
        if(op != null && (op.operation.isIs() || op.operation.isReadable())){
            try {
                return op.session.attributeGetter(att, op.operation); 
            } catch (AppiaManagementException e) {
                throw new MBeanException(e,"unable to invoke operation");
            }
        }
        throw new AttributeNotFoundException("cannot find attribute "+att);
    }

    public AttributeList getAttributes(String[] attrs) {
        final AttributeList attrList = new AttributeList();
        for(String att : attrs){
            if(att != null){
                try {
                    attrList.add(new Attribute(att,getAttribute(att)));
                } catch (AttributeNotFoundException e1) {
                    e1.printStackTrace();
                } catch (MBeanException e1) {
                    e1.printStackTrace();
                } catch (ReflectionException e1) {
                    e1.printStackTrace();
                }
            }
        }
        return attrList;
    }

    public MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }
    
    private void updateMBeanInfo(){
        final MBeanOperationInfo[] opsArray = new MBeanOperationInfo[mboi.size()];
        int i=0;
        for(MBeanOperationInfo inf : mboi)
            opsArray[i++] = inf;
        final MBeanAttributeInfo[] attsArray = new MBeanAttributeInfo[mbai.size()];
        i=0;
        for(MBeanAttributeInfo inf : mbai){
            attsArray[i++] = inf;
        }
        mbeanInfo = new MBeanInfo(this.getClass().getName(),
                "Exported operations and attributes list",
                attsArray, // attributes
                null, // constructors
                opsArray, // operations
                null); // notifications        
    }

    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        if(log.isDebugEnabled())
            log.debug("Invoking: "+actionName+" params "+params.length);
        
        if(actionName.equals("getAttribute")){
            try {
                return getAttribute((String) params[0]);
            } catch (AttributeNotFoundException e1) {
                throw new MBeanException(new AppiaManagementException(e1));
            }
        }
        else if(actionName.equals("setAttribute")){
            System.out.println("WILL CALL SETATTRIBUTE");
            try {
                for(Object obj : params)
                    System.out.println("### "+obj);
                System.out.println("------------");
                for(String str : signature)
                    System.out.println("### "+str);
                setAttribute((Attribute) params[0]);
            } catch (AttributeNotFoundException e) {
                throw new MBeanException(new AppiaManagementException(e));
            } catch (InvalidAttributeValueException e) {
                throw new MBeanException(new AppiaManagementException(e));
            }
        }
        else if (actionName.equals("invoke") && params.length == 3)
            return invoke((String)params[0], (Object[])params[1], (String[])params[2]);
        final Operation<MBeanOperationInfo> op = operations.get(actionName);
        if (op == null)
            throw new MBeanException(new AppiaManagementException("Operation "+actionName+" not found."));
        else{
            try {
                return op.session.invoke(actionName,op.operation,params, signature);
            } catch (AppiaManagementException e) {
                throw new MBeanException(e);
            }
        }
    }

    public void setAttribute(Attribute att) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        if(log.isDebugEnabled())
            log.debug("SET from DynamicMBean: "+att.getValue());
        
        Attribute myAtt = att;
        if(myAtt.getName().equals("Attribute"))
            myAtt = (Attribute) att.getValue();
        
        final Operation<MBeanAttributeInfo> op = attributes.get(myAtt.getName());
        if(log.isDebugEnabled()){
            log.debug("ATT "+myAtt+" Name "+myAtt.getName()+" Value "+myAtt.getValue()+" OP="+op);
        }

        if(op != null && op.operation.isWritable()){
            try {
                op.session.attributeSetter(myAtt, op.operation);
            } catch (AppiaManagementException e) {
                throw new MBeanException(e,"unable to invoke operation");
            }
        }
        else
            throw new AttributeNotFoundException("cannot find attribute "+myAtt);
    }

    public AttributeList setAttributes(AttributeList attList) {
        final ListIterator<Object> it = attList.listIterator();
        Attribute att = null;
        final String[] attrs = new String[attList.size()];
        int i=0;
        while(it.hasNext()){
            att = (Attribute) it.next();
            try {
                setAttribute(att);
                attrs[i] = att.getName();
            } catch (AttributeNotFoundException e) {
                attrs[i] = null;
                e.printStackTrace();
            } catch (InvalidAttributeValueException e) {
                attrs[i] = null;
                e.printStackTrace();
            } catch (MBeanException e) {
                attrs[i] = null;
                e.printStackTrace();
            } catch (ReflectionException e) {
                attrs[i] = null;
                e.printStackTrace();
            }
            finally{
                i++;
            }
        }
        return getAttributes(attrs);
    }

}
