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
 * Thrown when an exception ocurs during {@link net.sf.appia.core.Event Event} manipulation.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.Event
 */
public class AppiaEventException extends AppiaException {

  private static final long serialVersionUID = -8004384508061608720L;

  /**
   * Defauld error code.
   */
  public static final int UNKNOWN=-1;
  
  /**
   * Tried to send an Event not initialized.
   * @see net.sf.appia.core.Event#init
   */
  public static final int NOTINITIALIZED=1;
  /**
   * Tried to initialize an Event without giving all the attributes necessary.
   * @see net.sf.appia.core.Event#init
   */
  public static final int ATTRIBUTEMISSING=2;
  /**
   * Tried to create a
   * {@link net.sf.appia.core.events.channel.ChannelEvent ChannelEvent}
   * without giving an
   * {@link net.sf.appia.core.EventQualifier EventQualifier}.
   * @see net.sf.appia.core.events.channel.ChannelEvent
   * @see net.sf.appia.core.EventQualifier
   */
  public static final int UNKNOWNQUALIFIER=3;
  /**
   * Tried to get an
   * {@link net.sf.appia.core.Event Events}
   * first session with an invalid source
   * {@link net.sf.appia.core.Session Session}.
   * @see net.sf.appia.core.Channel#getFirstSession
   */
  public static final int UNKNOWNSESSION=4;
  /**
   * Tried to initialize an
   * {@link net.sf.appia.core.Event Event}
   * that doesn't have a
   * {@link net.sf.appia.core.ChannelEventRoute ChannelEventRoute}.
   * <br>
   * The usual reason for it, is that no {@link net.sf.appia.core.Layer Layer}
   * has declared the {@link net.sf.appia.core.Event Event} as <i>provided</i>.
   * @see net.sf.appia.core.ChannelEventRoute
   * @see net.sf.appia.core.Layer
   */
  public static final int UNWANTEDEVENT=5;
  /**
   * Tried to send an {@link net.sf.appia.core.Event Event}
   * on a closed {@link net.sf.appia.core.Channel Channel}.
   * @see net.sf.appia.core.events.channel.ChannelInit
   * @see net.sf.appia.core.events.channel.ChannelClose
   */
  public static final int CLOSEDCHANNEL=6;

  /**
   * Method {@link net.sf.appia.core.Event#asyncGo} was called from the Appia Thread.
   * This cannot appen. 
   */
  public static final int COULDNOTBLOCK = 7;

  /**
   * Either:
   * <ul>
   * <li> {@link Event#asyncGo(Channel, int)} was called from within the Appia thread
   * <li> {@link Event#go()} was called from outside the Appia thread.
   * </ul>
   */
  public static final int WRONGTHREAD = 8;
  
  /**
   * The type of the exception.
   * <br>
   * It can take 8 values:
   * <i>UNKNOWN, NOTINITIALIZED, ATTRIBUTEMISSING, UNKNOWNQUALIFIER, UNKNOWNSESSION,
   * UNWANTEDEVENT, CLOSEDCHANNEL, INITIALIZEDASYNC, COULDNOTBLOCK, WRONGTHREAD</i>
   */
  public int type = UNKNOWN;

  /**
   * Constructs an <i>AppiaEventException</i> without a details message.
   *
   * @param type the {@link net.sf.appia.core.AppiaEventException#type type} of the exception
   */
  public AppiaEventException(int type) {
    super("AppiaEventException");

    this.type=type;
  }

  /**
   * Constructs an <i>AppiaEventException</i> with a details message.
   *
   * @param type the {@link net.sf.appia.core.AppiaEventException#type type} of the exception
   * @param s the details message
   */
  public AppiaEventException(int type, String s) {
    super("AppiaEventException: "+s);
    this.type=type;
  }
  
  /**
   * Constructs an <i>AppiaEventException</i> with a message and a throwable cause.
   *
   * @param s the details message
   * @param cause the exception that caused this one.
   */
  public AppiaEventException(String s, Throwable cause) {
    super("AppiaEventException: "+s, cause);
  }

}