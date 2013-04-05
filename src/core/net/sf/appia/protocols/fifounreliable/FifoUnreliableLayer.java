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
 package net.sf.appia.protocols.fifounreliable;

//////////////////////////////////////////////////////////////////////
//                                                                  //
// Appia: protocol development and composition framework            //
//                                                                  //
// Class: FifoUnreliableLayer                                       //
//                                                                  //
// Author: Sandra Teixeira, 11/2001                                 //
//                                                                  //
// Change Log:                                                      //
//                                                                  //
//////////////////////////////////////////////////////////////////////

import net.sf.appia.core.*;

/**
 * FifoUnreliableLayer is the layer for the protocol providing unreliable ordered 
 * delivery of messages.
 *
 * The protocol doesn't provide events:
 *
 * The protocol accepts the following events:
 * <ul>
 * <li>SendableEvent (Accept)
 * </ul>
 * @see Layer
 * @see FifoUnreliableSession
 * @see net.sf.appia.core.events.SendableEvent
 * @author Sandra Teixeira
 */
public class FifoUnreliableLayer extends Layer {

    private java.util.Vector sessions;

    /**
     * Usual Layer empty constructor 
     */
    public FifoUnreliableLayer() {
	super();
	
	/* Events enumeration */
	    evProvide=new Class[0];

	    evRequire=new Class[0];

	    evAccept=new Class[1];
	    evAccept[0]=net.sf.appia.core.events.SendableEvent.class;
    }

    /**
     * Standard session instantiation
     */
    public Session createSession() {
	return (new FifoUnreliableSession(this));
    }
}



