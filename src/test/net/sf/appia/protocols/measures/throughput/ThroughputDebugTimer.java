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

package net.sf.appia.protocols.measures.throughput;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.PeriodicTimer;

/**
 * This class defines a ThroughputDebugTimer
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class ThroughputDebugTimer extends PeriodicTimer {

    private static final long PERIOD=1000;
    private static final String ID = "ThroughputDebugTimer";
    
    /**
     * Creates a new ThroughputDebugTimer.
     */
    public ThroughputDebugTimer() {
    }

    /**
     * Creates a new ThroughputDebugTimer.
     * @param channel
     * @param source
     * @param qualifier
     * @throws AppiaEventException
     * @throws AppiaException
     */
    public ThroughputDebugTimer(Channel channel, Session source, int qualifier) 
    throws AppiaEventException,AppiaException {
        super(ID, PERIOD, channel, Direction.DOWN, source, qualifier);
    }

}
