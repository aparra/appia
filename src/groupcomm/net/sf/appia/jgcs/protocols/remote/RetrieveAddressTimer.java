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
 * Initial developer(s): Nuno Carvalho.
 * Contributor(s): See Appia web page for a list of contributors.
 */

package net.sf.appia.jgcs.protocols.remote;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.PeriodicTimer;

/**
 * This class defines a RetrieveAddressTimer
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class RetrieveAddressTimer extends PeriodicTimer {

	/**
	 * Creates a new RetrieveAddressTimer.
	 */
	public RetrieveAddressTimer() {
		super();
	}

	/**
	 * Creates a new RetrieveAddressTimer.
	 * @param period
	 * @param channel
	 * @param dir
	 * @param source
	 * @param qualifier
	 * @throws AppiaEventException
	 * @throws AppiaException
	 */
	public RetrieveAddressTimer(long period, Channel channel,
			int dir, Session source, int qualifier) throws AppiaEventException,
			AppiaException {
		super("jgcs remote address timer", period, channel, dir, source, qualifier);
	}

}
