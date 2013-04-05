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
 * Created on Mar 24, 2004
 *
 */
package net.sf.appia.xml.utils;

import java.util.Hashtable;

/**
 * This class extends an Hashtable and it is meant to facilitate
 * the definition of parameters to be passed to a channel and more specificaly
 * to the respective sessions.
 * 
 * @author Jose Mocito
 *
 */
public class ChannelProperties extends Hashtable {
	
	private static final long serialVersionUID = -5266359622872825158L;

	/**
	 * Builds a {@link ChannelProperties} object with no parameters.
	 *
	 */
	public ChannelProperties() {
		super();
	}
	
	/**
	 * Returns the parameters associated with a session.
	 * 
	 * @param key the name of the session.
	 * @return the parameters as a {@link SessionProperties} object,
	 * 	associated with the give session name.
	 * @see SessionProperties
	 */
	public SessionProperties getParams(String key) {
		return (SessionProperties) get(key);
	}
	
	/**
	 * Associates the given parameters to a session name.
	 * 
	 * @param key the name of the session.
	 * @param params the parameters as a {@link SessionProperties} object,
	 * 	to be associated with the give session name. 
	 * @see SessionProperties
	 * 
	 */
	public void putParams(String key, SessionProperties params) {
		put(key,params);
	}
}
