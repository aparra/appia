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
package net.sf.appia.xml.interfaces;

import net.sf.appia.xml.utils.SessionProperties;

/**
 * This interface must be implemented by all the sessions who wish to provide
 * a way for the Appia's XML framework to initialize these sessions with
 * parameters stored in a XML configuration file or string.
 * 
 * @author Jose Mocito
 * 
 */
public interface InitializableSession {
	
	/**
	 * Initialization method.
	 * <p>
	 * This method should implement the necessary actions for retrieval
	 * of the parameters from the {@link SessionProperties} object and used the returned
	 * values to make the initialization of the session.
	 * 
	 * @param  params	the parameters passed to the session.
	 * @see				SessionProperties
	 * 
	 */
	public void init(SessionProperties params);
	
}
