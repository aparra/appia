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
 
package net.sf.appia.protocols.group;

import net.sf.appia.core.AppiaError;

/**
 * Superclass of all errors thrown by <i>Appias GroupCommunication protocols</i>.
 *
 * @author Alexandre Pinto
 * @version 0.1
 */
public class AppiaGroupError extends AppiaError {

  private static final long serialVersionUID = -3883916405725066151L;

  /**
   * Creates an <i>AppiaGroupError</i> with the given details message.
   * <br>
   * The details message is preceeded by "group:".
   *
   * @param s the details message
   */
  public AppiaGroupError(String s) {
    super("group:"+s);
  }
  
  public AppiaGroupError(String s, Throwable t) {
	    super("group:"+s, t);
  }

}