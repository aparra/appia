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
 * Initial developer(s): Nuno Carvalho.
 * 
 *  * Contact
 * 	Address:
 * 		LASIGE, Departamento de Informatica, Bloco C6
 * 		Faculdade de Ciencias, Universidade de Lisboa
 * 		Campo Grande, 1749-016 Lisboa
 * 		Portugal
 * 	Email:
 * 		jgcs@lasige.di.fc.ul.pt
 */
 
package net.sf.appia.jgcs;

import java.io.File;

import net.sf.jgcs.GroupConfiguration;

/**
 * This class defines a AppiaGroup and implements a GroupConfiguration. 
 * It must be used to create control and data sessions.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class AppiaGroup implements GroupConfiguration {

	private String configFileName;
	private String groupName;
	private String managementMBeanID;
	
	public String getGroupName() {
		return groupName;
	}

	public File getConfigFile() {
		return new File(configFileName);
	}

	public String getConfigFileName() {
		return configFileName;
	}

	public void setConfigFileName(String configFileName) {
		this.configFileName = configFileName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

    /**
     * @return Returns the managementMBeanID.
     */
    public String getManagementMBeanID() {
        return managementMBeanID;
    }

    /**
     * @param managementMBeanID The managementMBeanID to set.
     */
    public void setManagementMBeanID(String managementMBeanID) {
        this.managementMBeanID = managementMBeanID;
    }
    
	@Override
	public int hashCode(){
		return groupName.hashCode();
	}

	@Override
	public boolean equals(Object o){
		if (o instanceof AppiaGroup) {
			AppiaGroup ag = (AppiaGroup) o;
			return ag.groupName.equals(this.groupName) && ag.managementMBeanID.equals(managementMBeanID);
		}
		else return false;
	}
	
	@Override
	public String toString(){
		return groupName;
	}
}
