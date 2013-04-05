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
 
/**
 * Title:        Appia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Alexandre Pinto & Hugo Miranda & Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto & Hugo Miranda & Luis Rodrigues
 * @version 1.0
 */
package net.sf.appia.core.events.channel;

import net.sf.appia.core.*;

/**
 * Defines a PeriodicTimer class.
 * @author Alexandre Pinto
 * @version 1.0
 *
 */
public class PeriodicTimer extends ChannelEvent implements Cloneable {

  /**
   * The Timer unique Identification.
   */
  public String timerID;
  
  /**
   * The period between notifications, in milliseconds.
   */
  protected long period=0;

  /**
   * Creates a uninitialized PeriodicTimer Event.
   */
  public PeriodicTimer() {
	  this.setPriority(Timer.DEFAULT_TIMER_PRIORITY);
  }

  /**
   * Creates a initialized PeriodicTimer Event.
   * @param timerID the timer ID
   * @param period the period of the timer, in milliseconds.
   * @param channel the channel
   * @param dir the direction of the timer.
   * @param source the session that created the event
   * @param qualifier the event qualifier
   * @throws AppiaEventException
   * @throws AppiaException if the period is negative.
   */
  public PeriodicTimer(String timerID, long period,
                       Channel channel, int dir, Session source,
                       int qualifier)
    throws AppiaEventException, AppiaException {

    super(channel,dir,source,qualifier);

    this.timerID=timerID;
    this.setPriority(Timer.DEFAULT_TIMER_PRIORITY);
    
    if ( period < 0 )
       throw new AppiaException("PeriodicTimer: period is negative");

    this.period=period;
  }

  /**
   * Sets the time between the periodic notifications, in milliseconds.
   * 
   * @param period The period in milliseconds.
   */
  public void setPeriod(long period) throws AppiaException {
    if ( period < 0 )
       throw new AppiaException("PeriodicTimer: period is negative");

    this.period=period;
  }

  /**
   * Gets the time between the periodic notifications.
   * 
   * @return period The period in milliseconds.
   */
  public long getPeriod() {
    return period;
  }

  /**
   * Redefenition of Event.cloneEvent.
   */
  public Event cloneEvent() throws CloneNotSupportedException {
    return super.cloneEvent();
  }

}