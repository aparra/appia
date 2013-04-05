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
 /*
 * NakFifoTimer.java
 *
 * Created on 10 de Julho de 2003, 16:00
 */

package net.sf.appia.protocols.nakfifo;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.PeriodicTimer;


/** Timer used to define the rounds.
 * @author Alexandre Pinto
 */
public class NakFifoTimer extends PeriodicTimer {
  
  /** Creates a new instance of NakFifoTimer
   * @param period Round duration in milliseconds.
   * @throws AppiaException If the <I>period</I> is invalid.
   */
  public NakFifoTimer(long period, Channel channel, Session source, int qualifier) throws AppiaEventException, AppiaException {
    super("NakFifoTimer:"+source.toString(),period,channel,Direction.DOWN,source,qualifier);
  }
}
