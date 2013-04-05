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
 * An <i>EchoEvent</i> is the carrier for an {@link net.sf.appia.core.Event Event} that will
 * be bounced of the edges of the stack.
 * <br>
 * In other words, the payload {@link net.sf.appia.core.Event Event} of an <i>EchoEvent</i>
 * will be sent in the oposite direction of the <i>EchoEvent</i>, when the
 * <i>EchoEvent</i> reaches the boundaries of the stack.
 * <br>
 * The functionality described is performed by the
 * {@link net.sf.appia.core.Channel#handle Channel.handle(Event)} method, which is called
 * when a {@link net.sf.appia.core.events.channel.ChannelEvent ChannelEvent} reaches any of
 * the ends of the {@link net.sf.appia.core.Channel Channel} stack.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.Channel#handle
 */
public class EchoEvent extends ChannelEvent {

  private Event event;

  /**
   * Creates an initialized <i>EchoEvent</i>.
   *
   * @param event the payload {@link net.sf.appia.core.Event Event}
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @param source the {@link net.sf.appia.core.Session Session} that is generating the Event
   * @throws AppiaEventException as the result of calling
   * {@link net.sf.appia.core.events.channel.ChannelEvent#ChannelEvent(Channel,int,Session,int)
   * ChannelEvent(Channel,direction,Session,event qualifier)}
   */
  public EchoEvent(Event event, Channel channel, int dir, Session source)
    throws AppiaEventException {

    super(channel,dir,source,EventQualifier.NOTIFY);

    this.event=event;
  }

  /**
   * Get the payload {@link net.sf.appia.core.Event Event}.
   *
   * @return the payload {@link net.sf.appia.core.Event Event}
   */
  public Event getEvent() {
    return event;
  }
}