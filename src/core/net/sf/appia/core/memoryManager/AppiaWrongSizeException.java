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
 package net.sf.appia.core.memoryManager;

/**
 * @author Nuno Carvalho
 */
public class AppiaWrongSizeException extends net.sf.appia.core.AppiaException {

	private static final long serialVersionUID = 8913603650934377697L;

/**
   * Constructs an <i>AppiaWrongSizeException</i> without a details message.
   */
  public AppiaWrongSizeException() {
    super("Wrong size in the memory manager");
  }

  /**
   * Constructs an <i>AppiaWrongSizeException</i> 
   * with the details message given.
   *
   * @param s the details message
   */
  public AppiaWrongSizeException(String s) {
    super("Memory Manager: "+s);
  }
  
  public AppiaWrongSizeException(String s, Throwable t) {
	    super("Memory Manager: "+s, t);
  }

}
