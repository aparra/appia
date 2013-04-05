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
 
package net.sf.appia.core.message;

/**
 * Exception raised by any of the methods of the Message.
 * <br>
 * To allow a transparent redefenition of the methods of Message, it extends
 * RuntimeException.
 * <br>
 * Only in very strange situations it will be raised.
 * <br>
 * It carries the original exception raised.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.message.Message
 * @see java.lang.RuntimeException
 */
public class MessageException extends RuntimeException {

  private static final long serialVersionUID = 6445053185311031824L;

  /**
   * Constructs a new exception.
   *
   * @param cause the original Exception raised.
   */
  public MessageException(Throwable cause) {
      super(cause);
  }

  /**
   * Creates a new MessageException.
   * @param m the error message
   * @param cause the exception that caused this one.
   */
  public MessageException(String m, Throwable cause){
      super(m,cause);
  }

  /**
   * Creates a new MessageException.
   * @param m the error message.
   */
  public MessageException(String m){
      super(m);
  }
}
