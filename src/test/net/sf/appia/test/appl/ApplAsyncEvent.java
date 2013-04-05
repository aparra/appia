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
 package net.sf.appia.test.appl;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Event;
import net.sf.appia.core.Session;

/**
 * @author Nuno Carvalho
 * Appia: protocol development and composition framework
 * Version 1.0/J
 * Copyright, 2000, Universidade de Lisboa
 * All rights reserved
 * See licence.txt for further information
 * Class: ApplAsyncEvent.java
 * Created on 07-Aug-2002
 */
public class ApplAsyncEvent extends Event {

	private String comLine;

	/**
	 * Constructor for ApplAsyncEvent.
	 */
	public ApplAsyncEvent(String s) {
		super();
		comLine = s;
	}

	/**
	 * Constructor for ApplAsyncEvent.
	 * @param channel
	 * @param direction
	 * @param src
	 * @throws AppiaEventException
	 */
	public ApplAsyncEvent(Channel channel, int direction, Session src)
		throws AppiaEventException {
		super(channel, direction, src);
	}

	/**
	 * Returns the comLine.
	 * @return String
	 */
	public String getComLine() {
		return comLine;
	}

	/**
	 * Sets the comLine.
	 * @param comLine The comLine to set
	 */
	public void setComLine(String comLine) {
		this.comLine = comLine;
	}

}
