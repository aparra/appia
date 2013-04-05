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

import net.sf.appia.core.AppiaException;

/**
 * Thrown when an exception ocurs in {@link net.sf.appia.core.ChannelCursor ChannelCursor}
 * manipulation.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.Channel
 */
public class AppiaCursorException extends AppiaException {

  private static final long serialVersionUID = -6639734929127173651L;

  /**
   * Defauld error code.
   */
  public static final int UNKNOWN=-1;
  
  /**
   * The cursor hasn't been set.
   */
  public static final int CURSORNOTSET=1;
  /**
   * Tried to get the {@link net.sf.appia.core.Session Session} bellow,
   * but it is already at the bottom of the stack.
   */
  public static final int CURSORONBOTTOM=2;
  /**
   * Tried to get the {@link net.sf.appia.core.Session Session} above,
   * but it is already at the top of the stack.
   */
  public static final int CURSORONTOP=3;
  /**
   * Tried to set an already set {@link net.sf.appia.core.Session Session}
   */
  public static final int ALREADYSET=4;
  /**
   * Tried to set a {@link net.sf.appia.core.Session Session} of the wrong
   * {@link net.sf.appia.core.Layer Layer}
   */
  public static final int WRONGLAYER=5;
  /**
   * Tried to jump to a invalid position in the Sessions stack.
   */
  public static final int INVALIDPOSITION=6;

  /**
   * The type of the exception.
   * <br>
   * There are 5 valid values:
   * <i>CURSORNOTSET, CURSORONBOTTOM, CURSORONTOP, ALREADYSET, WRONGLAYER</i>
   */
  public int type;

  /**
   * Constructs an <i>AppiaCursorException</i> with no detailed message.
   *
   * @param type the {@link net.sf.appia.core.AppiaCursorException#type type} of the exception
   */
  public AppiaCursorException(int type) {
    super("appiaCursorException");

    this.type=type;
  }

  /**
   * Constructs an <i>AppiaCursorException</i> with a details message.
   *
   * @param type the {@link net.sf.appia.core.AppiaCursorException#type type} of the exception
   * @param s the details message
   */
  public AppiaCursorException(int type, String s) {
    super("appiaCursorException:"+s);

    this.type=type;
  }
  
  public AppiaCursorException(String s, Throwable t) {
	    super("appiaCursorException:"+s, t);
	    this.type=UNKNOWN;
  }

}