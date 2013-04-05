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
 
package net.sf.appia.core.events.channel;

import net.sf.appia.core.*;

/**
 * Superclass of all {@link net.sf.appia.core.Event Events} accepted by the
 * {@link net.sf.appia.core.Channel Channel}.
 * <br>
 * All subclasses of <i>ChannelEvent</i> are handled by the
 * {@link net.sf.appia.core.Channel Channel} through the
 * {@link net.sf.appia.core.Channel#handle handle(Event)} method.
 * <br>
 * A few of the subclasses are also provided by the
 * {@link net.sf.appia.core.Channel Channel}, namely:
 * {@link net.sf.appia.core.events.channel.ChannelInit ChannelInit},
 * {@link net.sf.appia.core.events.channel.ChannelClose ChannelClose}.
 * <br>
 * A <i>ChannelEvent</i> extends an {@link net.sf.appia.core.Event Event} by adding an
 * {@link net.sf.appia.core.EventQualifier EventQualifier}.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.events.channel
 * @see net.sf.appia.core.EventQualifier
 */
public class ChannelEvent extends Event {

  private int mode;

  /**
   * Creates an uninitialized <i>ChannelEvent</i>.
   * <br>
   * The event is created with an {@link net.sf.appia.core.EventQualifier#NOTIFY NOTIFY}
   * {@link net.sf.appia.core.EventQualifier EventQualifier}.
   */
  public ChannelEvent() {
    mode=EventQualifier.NOTIFY;
  }

  /**
   * Creates an initialized <i>ChannelEvent</i>.
   *
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @param source the {@link net.sf.appia.core.Session Session} that is generating the Event
   * @param qualifier the {@link net.sf.appia.core.EventQualifier EventQualifier} of the Event
   * @throws AppiaEventException as the result of calling
   * {@link net.sf.appia.core.Event#Event(Channel,int,Session) Event(Channel,Direction,Session)}
   * or with
   * {@link net.sf.appia.core.AppiaEventException#type type}
   * {@link net.sf.appia.core.AppiaEventException#UNKNOWNQUALIFIER UNKNOWNQUALIFIER}
   */
  public ChannelEvent(Channel channel, int dir, Session source, int qualifier)
    throws AppiaEventException {

    super(channel,dir,source);

    this.mode=qualifier;
  }

  /**
   * Set the EventQualifier mode.
   *
   * @param mode the new EventQualifier mode.
   */
  public void setQualifierMode(int mode) {
    this.mode=mode;
  }

  /**
   * Get the {@link net.sf.appia.core.EventQualifier EventQualifier}.
   *
   * @return the current {@link net.sf.appia.core.EventQualifier EventQualifier}
   */
  public int getQualifierMode() {
    return mode;
  }

  /**
   * Clone the <i>ChannelEvent</i>.
   * <br>
   * It extends the {@link net.sf.appia.core.Event#cloneEvent Event.cloneEvent()} method
   * by cloning the {@link net.sf.appia.core.EventQualifier EventQualifier}.
   *
   * @return the clone
   * @throws CloneNotSupportedException as the possible result of calling
   * {@link net.sf.appia.core.Event#cloneEvent Event.cloneEvent()}
   */
  public Event cloneEvent() throws CloneNotSupportedException {
    return (ChannelEvent)super.cloneEvent();
  }
}