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
 * This class defines an Appia Timer
 * 
 * @author <a href="mailto:apinto@di.fc.ul.pt">Alexandre Pinto</a>
 * @version 1.0
 */
public class Timer extends ChannelEvent {

    /**
     * Defines the default timer priority. The default for simple events is 127, so these kind
     * of events have more priority and are dispatched before others.
     */
    protected static final int DEFAULT_TIMER_PRIORITY=200;
    
  /**
   * The timer unique Identification.
   */
  public String timerID;
  /**
   * Non negative expiration time in milliseconds. The timer expires after this time.
   * <b>ATENTION: Changed sematic. 
   * It now stands for the time untill reception of the returning event</b>
   */
  protected long when;

  /**
   * Creates a uninitialized Timer Event.
   */
  public Timer() {
	  this.setPriority(DEFAULT_TIMER_PRIORITY);
  }

  /**
   * Creates a initialized Timer Event.
   * Use {@linkplain Timer#Timer(long, String, Channel, int, Session, int)} instead.
   * 
   * @deprecated
   * @see Timer#Timer(long, String, Channel, int, Session, int)
   */
  public Timer(String timerID, long when,
               Channel channel, int dir, Session source,
               int qualifier)
    throws AppiaEventException, AppiaException {

    super(channel,dir,source,qualifier);

    this.timerID=timerID;
    this.setPriority(DEFAULT_TIMER_PRIORITY);

    if ( when < 0 )
       throw new AppiaException("Timer: when is negative");

    this.when = when - channel.getTimeProvider().currentTimeMillis();
  }

  /**
   * Creates a initialized Timer Event.
   * 
   * @param when delta between now and the time that the timer will expire, in milliseconds.
   * @param timerID ID of the timer
   * @param channel channel of the timer
   * @param dir Direction of the timer
   * @param source Session that created the timer
   * @param qualifier Qualifier of the timer
   * @throws AppiaEventException
   * @throws AppiaException
   */
  public Timer(long when, String timerID,
               Channel channel, int dir, Session source,
               int qualifier)
    throws AppiaEventException, AppiaException {

    super(channel,dir,source,qualifier);

    this.timerID=timerID;
    this.setPriority(DEFAULT_TIMER_PRIORITY);
    
    if ( when < 0 )
       throw new AppiaException("Timer: when is negative");

    this.when=when;
  }

  /**
   * Sets the time when the Timer event will be returned. The timer
   * will expire after <code>time</code>milliseconds.
   *
   * @param time The time in milliseconds.
   */
  public void setTimeout(long time) throws AppiaException {
    if ( when < 0 )
       throw new AppiaException("Timer: Parameter is negative");

    this.when=time;
  }

  /**
   * Gets the time when the Timer event will be returned.
   *
   * @return The time in milliseconds.
   */
  public long getTimeout() {
    return when;
  }
  
  /**
   * Redefenition of Event.cloneEvent().
   */
  public Event cloneEvent() throws CloneNotSupportedException {
    return super.cloneEvent();
  }

}