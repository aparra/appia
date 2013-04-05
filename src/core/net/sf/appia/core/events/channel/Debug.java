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

import java.io.OutputStream;

import net.sf.appia.core.*;

/**
 * Event for <i>debugging</i> porpuses.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see java.io.OutputStream
 */
public class Debug extends ChannelEvent {

  OutputStream stream;

  /**
   * Creates an uninitialized <i>Debug</i> Event.
   * <br>
   * Debug messages will be sent to the given
   * {@link java.io.OutputStream OutputStream}.
   *
   * @param stream the {@link java.io.OutputStream OutputStream} where debugging
   *               information will be put.
   */
  public Debug(OutputStream stream) {
    this.stream=stream;
  }

  /**
   * Creates an initialized <i>Debug</i> Event.
   * <br>
   * Debug messages will be sent to the given
   * {@link java.io.OutputStream OutputStream}.
   *
   * @param stream the {@link java.io.OutputStream OutputStream} where debugging
   *               information will be put.
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @param source the {@link net.sf.appia.core.Session Session} that is generating the Event
   * @throws AppiaEventException as the result of calling
   * {@link net.sf.appia.core.events.channel.ChannelEvent#ChannelEvent(Channel,int,Session,int)
   * ChannelEvent(Channel,Direction,Session,EventQualifier)}
   */
  public Debug(OutputStream stream, Channel channel, int dir, Session source, int qualifier)
         throws AppiaEventException {
    super(channel,dir,source,qualifier);
    this.stream=stream;
  }

  /**
   * Get the {@link java.io.OutputStream OutputStream} where debugging information
   * will be put.
   * <br>
   *
   * @return the {@link java.io.OutputStream OutputStream}
   */
  public OutputStream getOutput() {
    return stream;
  }
}