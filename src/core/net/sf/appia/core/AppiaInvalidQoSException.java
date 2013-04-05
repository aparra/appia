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
 * Thrown when the desired {@link net.sf.appia.core.QoS QoS} is invalid.
 * <br>
 * This is normally due to some {@link net.sf.appia.core.Event Event} that is <i>required</i>
 * by some {@link net.sf.appia.core.Layer Layer}, but no other {@link net.sf.appia.core.Layer Layer}
 * <i>provides</i> it.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.QoS
 * @see net.sf.appia.core.Layer
 */
public class AppiaInvalidQoSException extends AppiaException {

  private static final long serialVersionUID = 1631242417136917160L;

  /**
   * Constructs an <i>AppiaInvalidQoSException</i> with the default details
   * message.
   */
  public AppiaInvalidQoSException() {
    super("Invalid QoS");
  }

  /**
   * Constructs an <i>AppiaInvalidQoSException</i> with the details message given.
   * <br>
   * The details message is preceeded by "Invalid QoS:".
   *
   * @param s the details message
   */
  public AppiaInvalidQoSException(String s) {
    super("Invalid QoS: "+s);
  }
  
  public AppiaInvalidQoSException(String s, Throwable t) {
	    super("Invalid QoS: "+s, t);
  }

}