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
 package net.sf.appia.protocols.drop;

//////////////////////////////////////////////////////////////////////
//                                                                  //
// Appia: protocol development and composition framework            //
//                                                                  //
// Class: DropLayer: randomly drops outgoing sendable events        //
//                                                                  //
// Author: Hugo Miranda, 06/2000                                    //
//                                                                  //
// Change Log:                                                      //
//                                                                  //
//////////////////////////////////////////////////////////////////////

import net.sf.appia.core.*;

/**
 * DropLayer is the layer for the protocol that randomly drops
 * SendableEvents going on the DOWN direction.
 *
 * The Drop protocol requires no events.
 * The following events are handled by this protocol:
 * <ul> 
 * <li>SendableEvent (Accepted): Events that if flowing down will be 
 * randomly dropped.
 * <li>Debug (Accepted): The protocol follows the specified rules for 
 * debugging.
 * </ul>
 * @see Layer
 * @see DropSession
 * @see net.sf.appia.core.events.SendableEvent
 * @see net.sf.appia.core.events.channel.Debug
 * @author Hugo Miranda
 */

public class DropLayer extends Layer {

    /**
     * Standard empty constructor.
     * @see Layer
     */

	public DropLayer() {
		super();
		evProvide=new Class[0];
		evRequire=new Class[0];
		evAccept=new Class[2];
		evAccept[0]=net.sf.appia.core.events.SendableEvent.class;
		evAccept[1]=net.sf.appia.core.events.channel.Debug.class;
	}
	
	/**
	 * Session constructor.
	 * @see Layer
	 */
	
	public Session createSession() {
		return new DropSession(this);
	}
}
