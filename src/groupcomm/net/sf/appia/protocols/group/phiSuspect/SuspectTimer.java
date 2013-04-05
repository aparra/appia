/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006-2009 University of Lisbon / Technical University of Lisbon / INESC-ID
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
package net.sf.appia.protocols.group.phiSuspect;

import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.PeriodicTimer;

/** The Phi failure detector timer.
 * @see net.sf.appia.protocols.group.phiSuspect.PhiSuspectLayer
 * @author Dan Mihai Dumitriu
 */
public class SuspectTimer extends PeriodicTimer {

  public SuspectTimer(String timerID, long period, Channel channel, Session source) throws AppiaException {
    super(timerID,period,channel,Direction.DOWN,source,EventQualifier.ON);
  }
}
