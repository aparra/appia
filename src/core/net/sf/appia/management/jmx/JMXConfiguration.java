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

/**
 * This class defines a JMXConfiguration.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class JMXConfiguration {

    public static final int DEFAULT_NAMING_PORT = 1099;
    public static final String DEFAULT_NAMING_SERVER = "localhost";
    
    private String namingServer;
    private int namingPort;
    private boolean local;
    private String managementMBeanID = "";
    
    public JMXConfiguration(String mbeanID) {
        namingPort = DEFAULT_NAMING_PORT;
        namingServer = DEFAULT_NAMING_SERVER;
        managementMBeanID = mbeanID;
    }

    public JMXConfiguration(int namingPort, String namingServer, String mbeanID){
        this.namingPort = namingPort;
        this.namingServer = namingServer;
        managementMBeanID = mbeanID;
    }

    public int getNamingPort() {
        return namingPort;
    }

    public void setNamingPort(int namingPort) {
        this.namingPort = namingPort;
    }

    public String getNamingServer() {
        return namingServer;
    }

    public void setNamingServer(String namingServer) {
        this.namingServer = namingServer;
    }

    public boolean equals(Object arg0) {
        if(arg0 instanceof JMXConfiguration){
            final JMXConfiguration c = (JMXConfiguration) arg0;
            return namingPort == c.namingPort && namingServer.equals(c.namingServer)
                && managementMBeanID.equals(c.managementMBeanID);
        }
        else
            return false;
    }

    public int hashCode() {
        if(managementMBeanID == null)
            return namingServer.hashCode() ^ namingPort;
        else
            return namingServer.hashCode() ^ namingPort ^ managementMBeanID.hashCode();
    }

    /**
     * @return Returns the local.
     */
    public boolean isLocal() {
        return local;
    }

    /**
     * @param local The local to set.
     */
    public void setLocal(boolean local) {
        this.local = local;
    }

    /**
     * @return Returns the managementMBeanID.
     */
    public String getManagementMBeanID() {
        return managementMBeanID;
    }
    
    
}
