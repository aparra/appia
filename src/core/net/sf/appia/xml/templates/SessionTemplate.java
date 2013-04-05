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
 /*
 * Created on Mar 11, 2004
 *
 */
package net.sf.appia.xml.templates;

import java.util.Hashtable;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.xml.utils.SharingState;



/**
 * This class implements a session template. It is used to generate one or more
 * sessions.
 * <p>
 * The number of sessions generated is determined based on the defined scope.
 * 
 * @author Jose Mocito
 *
 */
public class SessionTemplate {
	
	// Session name
	private String name;
	// Session sharing property {@see SharingState}
	private int sharing;
	// Layer instance associated to this template
	private Layer layerInstance;
	
	/**
	 * Builds a session template.
	 * 
	 * @param name the name of the session template.
	 * @param sharing the sharing scope of the session template.
	 * @param layer the {@link Layer} associated with this session template.
	 */	
	public SessionTemplate(String name, int sharing, Layer layer) {
		this.name = name;
		this.sharing = sharing;
		layerInstance = layer;
	}
	
	/**
	 * Returns the name associated to this session template.
	 * 
	 * @return a String containing the name.
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Returns the sharing property associated to this session template.
	 * 
	 * @return an integer representing the sharing state whose values are 
	 * 	obtained from {@link SharingState}.
	 * @see SharingState
	 */
	public int getSharingState() {
		return sharing;
	}
	
	/**
	 * Returns the {@link Layer} instance associated to this session template.
	 * 
	 * @return the {@link Layer} associated with this session template.
	 */
	public Layer layerInstance() {
		return layerInstance;
	}
	
	/**
	 * Returns the {@link Session} built from this template depending on
	 * the sharing property of this session template and, if necessary, the
	 * given label.
	 * 
	 * @return the {@link Session} built from this template.
	 */
	public Session sessionInstance(String label, Hashtable<String,Session> sharedSessions) {
        Session ns = null;
		if (sharing == SharingState.PRIVATE){
		    ns = layerInstance.createSession();
            ns.setId(name);
            return ns;
        }
        else if (sharing == SharingState.GLOBAL) {
            if (sharedSessions.containsKey(name))
                return (Session) sharedSessions.get(name);
            else {
                ns = layerInstance.createSession();
                ns.setId(name);
                sharedSessions.put(name,ns);
                return ns;
            }
        }
        else { // sharing == SharingState.LABEL
            if (sharedSessions.containsKey(name+label))
                return (Session) sharedSessions.get(name+label);
            else {
                ns = layerInstance.createSession();
                ns.setId(name);
                sharedSessions.put(name+label,ns);
                return ns;
            }
        }
	}
	
	/**
	 * <b>TESTING PURPOSES ONLY!</b>
	 */
	public void printSessionTemplate() {
		System.out.println("Session name: "+name+" (scope: "+sharing+")");
		System.out.println("\tLayer: "+layerInstance);
	}
}
