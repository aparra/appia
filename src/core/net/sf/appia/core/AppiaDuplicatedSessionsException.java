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
 * Thrown when a {@link net.sf.appia.core.Session Session} appears twice (or more) in the same
 * {@link net.sf.appia.core.Channel Channel}.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.Channel#start
 */
public class AppiaDuplicatedSessionsException extends AppiaException {

  private static final long serialVersionUID = -1097941557175137054L;

  /**
   * Constructs an <i>AppiaDuplicatedSessionsException</i>
   * with the default details message.
   */
  public AppiaDuplicatedSessionsException() {
    super("Session is 2x referenced in Channel");
  }
  
  public AppiaDuplicatedSessionsException(String message, Throwable t) {
	    super(message, t);
  }

}