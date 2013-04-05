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
 * TcpTimer.java
 *
 * Created on 2 de Outubro de 2003, 17:28
 */

package net.sf.appia.protocols.tcpcomplete;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.PeriodicTimer;


/**
 *
 * @author  Alexandre Pinto
 */
public class TcpTimer extends PeriodicTimer {
  
  /** Creates a new instance of TcpTimer */
  public TcpTimer(long period, Channel channel, Session source, int qualifier) throws AppiaEventException, AppiaException {
    super("TCP "+source+" TIMER",period,channel,Direction.DOWN, source, qualifier);
  }
}
