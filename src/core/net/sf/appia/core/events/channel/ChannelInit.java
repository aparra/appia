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
 * Event that marks the beggining of {@link net.sf.appia.core.Channel Channel} operation
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.events.channel.ChannelClose
 * @see net.sf.appia.core.Channel#start
 */
public class ChannelInit extends ChannelEvent {

  /**
   * Creates a not <b>initialized</b> <i>ChannelInit</i> event
   */
  public ChannelInit() {
	  this.setPriority(MAX_PRIORITY);
  }

  /**
   * Creates a <b>initialized</b> <i>ChannelInit</i> event
   *
   * @see net.sf.appia.core.Event#init
   * @param channel the {@link net.sf.appia.core.Channel Channel} where the event will be put.
   * @throws appiaEventException exception thrown if <i>channel</i> is null.
   */
  public ChannelInit(Channel channel) throws AppiaEventException {
    super(channel,Direction.UP,null,EventQualifier.NOTIFY);
    this.setPriority(MAX_PRIORITY);
  }

//  public ChannelInit(Channel channel, Session src) throws AppiaEventException {
//    super(
//      channel,
//      Direction.UP,
//      src,
//      EventQualifier.NOTIFY
//    );
//  }

}
