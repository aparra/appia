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

import net.sf.appia.core.AppiaException;

/**
 * Superclass of all exceptions thrown by <i>Appias Group Communication protocols</i>.
 *
 * @author Alexandre Pinto
 * @version 0.1
 */
public class AppiaGroupException extends AppiaException {

  private static final long serialVersionUID = -8462418939349278926L;

  /**
   * Creates an <i>AppiaGroupException</i> with the given details message.
   * <br>
   * The details message is preceeded by "group:".
   *
   * @param s the details message
   */
  public AppiaGroupException(String s) {
    super("group:"+s);
  }
  
  public AppiaGroupException(String s, Throwable t) {
	    super("group:"+s, t);
  }

}