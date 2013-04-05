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
 * Created on Mar 16, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package net.sf.appia.test.xml.ecco;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.message.Message;

/**
 * This class defines a MyEccoEvent
 * 
 * @author Jose Mocito
 * @version 1.0
 */
public class MyEccoEvent extends SendableEvent {
	
	private String text;

    /**
     * Creates a new MyEccoEvent.
     */
    public MyEccoEvent() {
        super();
    }

    /**
     * Creates a new MyEccoEvent.
     * @param channel
     * @param dir
     * @param source
     * @param msg
     * @throws AppiaEventException
     */
    public MyEccoEvent(Channel channel, int dir, Session source, Message msg) throws AppiaEventException {
        super(channel, dir, source, msg);
    }

    /**
     * Creates a new MyEccoEvent.
     * @param channel
     * @param dir
     * @param source
     * @throws AppiaEventException
     */
    public MyEccoEvent(Channel channel, int dir, Session source) throws AppiaEventException {
        super(channel, dir, source);
    }

    /**
     * Creates a new MyEccoEvent.
     * @param msg
     */
    public MyEccoEvent(Message msg) {
        super(msg);
    }

    /**
     * Gets the message text.
     * @return the message text.
     */
	public String getText() {
		return text;
	}
	
    /**
     * Sets the message text.
     * @param text the message text
     */
	public void setText(String text) {
		this.text = text;
	}
}
