/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2009 INESC-ID/IST
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

package net.sf.appia.management.jmx;

import java.io.IOException;
import java.net.MalformedURLException;

import javax.management.Attribute;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.apache.log4j.Logger;

import net.sf.appia.core.AppiaMBeanContainer;
import net.sf.appia.core.Channel;
import net.sf.appia.management.AppiaManagementException;

/**
 * This class defines a GenericActuator. This class is a wrapper to invoke JMX methods on Appia channels.
 * 
 * @author <a href="mailto:nonius@gsd.inesc-id.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class GenericActuator {
    
    private DynamicMBean bean=null;
    
    private static Logger log = Logger.getLogger(GenericActuator.class);
    
    /**
     * This method must be called before any invocations are made and it connects
     * to an Appia process via JMX.
     * @param namingHost the host where Appia is running.
     * @param namingPort the JMX port inside the Appia process.
     * @param channelName the Channel ID that we want to manage
     * @throws AppiaManagementException
     */
    public void connect(String namingHost, int namingPort, String channelName)
        throws AppiaManagementException {
        // The RMI server's host: this is actually ignored by JSR 160
        // since this information is stored in the RMI stub.
        // final String serverHost = "host";
        // The host, port and path where the rmiregistry runs.
        // final String namingHost = "localhost";
        // final int namingPort = 1099;
        Object proxy=null;
        try {
            final String strURL = "service:jmx:rmi:///jndi/rmi://"+namingHost+":"+namingPort+"/jmxrmi";
            final JMXServiceURL url = new JMXServiceURL(strURL);
            log.debug("Connecting to URL: "+strURL);
            // Connect a JSR 160 JMXConnector to the server side
            final JMXConnector connector = JMXConnectorFactory.connect(url);            
            log.debug("Retrieving MBean server connection...");
            // Retrieve an MBeanServerConnection that represent the MBeanServer the remote
            // connector server is bound to
            final MBeanServerConnection connection = connector.getMBeanServerConnection();
            log.debug("Getting instance of MBean for channel: "+channelName);
            final ObjectName delegateName = ObjectName.getInstance(Channel.class.getName()+":"+"name="+channelName);
            proxy = MBeanServerInvocationHandler.newProxyInstance(connection, delegateName, 
                            DynamicMBean.class,false);
        } catch (MalformedObjectNameException e) {
            throw new AppiaManagementException(e);
        } catch (MalformedURLException e) {
            throw new AppiaManagementException(e);
        } catch (NullPointerException e) {
            throw new AppiaManagementException(e);
        } catch (IOException e) {
            throw new AppiaManagementException(e);
        }
        bean = (DynamicMBean) proxy;
        log.debug("JMX Client connected.");
    }
    
    public void getLocalMBean(String managementMBeanID, String channelName){
        String beanID = managementMBeanID+":"+channelName;
        bean = AppiaMBeanContainer.getInstance().getBean(beanID);
        if(bean != null)
            log.debug("Retrieved instance of MBean for ID: "+beanID);
        else
            log.warn("MBean for ID "+beanID+" does not exist.");
    }

    
    /**
     * Invokes a method inside an Appia Layer.
     * @param action the method to invoke in the form layerID:methodName
     * @param params the parameters to pass to the method we want to invoke
     * @param signature the signature of the method.
     * @return the object returned by the invoked method.
     * @throws AppiaManagementException
     */
    public Object invoke(String action, Object[] params, String[] signature) throws AppiaManagementException {
        try {
            return bean.invoke(action, params, signature);
        } catch (MBeanException e) {
            throw new AppiaManagementException(e);
        } catch (ReflectionException e) {
            throw new AppiaManagementException(e);
        }
    }
    
    /**
     * Gets the value of an attribute inside an Appia Layer.
     * @param attribute the attribute name in the form layerID:attributeName
     * @return the value of the attribute
     * @throws AppiaManagementException
     */
    public Object getAttribute(String attribute) throws AppiaManagementException {
        try {
            return bean.getAttribute(attribute);
        } catch (AttributeNotFoundException e) {
            throw new AppiaManagementException(e);
        } catch (MBeanException e) {
            throw new AppiaManagementException(e);
        } catch (ReflectionException e) {
            throw new AppiaManagementException(e);
        }
    }
    
    /**
     * Sets a new value for a specified attribute in an Appia layer.
     * @param attribute the attribute name and value that we want to set.
     * The name must be in the form layerID:attributeName
     * @throws AppiaManagementException
     */
    public void setAttribute(Attribute attribute) throws AppiaManagementException {
        try {
            bean.setAttribute(attribute);
        } catch (AttributeNotFoundException e) {
            throw new AppiaManagementException(e);
        } catch (InvalidAttributeValueException e) {
            throw new AppiaManagementException(e);
        } catch (MBeanException e) {
            throw new AppiaManagementException(e);
        } catch (ReflectionException e) {
            throw new AppiaManagementException(e);
        }
    }
    
}
