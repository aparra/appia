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
 
package net.sf.appia.core;

/**
 * Superclass of all {@link java.lang.Error Errors} thrown by <i>Appia</i>.
 *
 * @author Alexandre Pinto
 * @version 0.1
 */
public class AppiaError extends Error {

  private static final long serialVersionUID = 8465747972493197513L;

  /**
   * Constructs an <i>AppiaError</i> with the details message given.
   * <br>
   * The details message is preceeded by "appia:".
   *
   * @param s the details message
   */
  public AppiaError(String s) {
    super("appia:"+s);
  }
  
  public AppiaError(String s, Throwable t) {
	  super("appia:"+s, t);
  }

}