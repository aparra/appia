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
 * Copyright:    Copyright (c) Nuno Carvalho and Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto and Hugo Miranda
 * @version 1.0
 */
package net.sf.appia.protocols.group.remote;

import net.sf.appia.core.*;
import net.sf.appia.protocols.group.heal.GossipOutLayer;



/**
 * This layer describes the GossipOutExt  facilty provided by GossipOutExt.
 */
public class RemoteGossipOutLayer extends GossipOutLayer {
	
	/**
	 * Standard constructor.
	 */
	public RemoteGossipOutLayer() {
		super();
        
        final Class[] evProvideToReplace = new Class[evProvide.length+1];
        for(int i=0; i<evProvide.length; i++)
            evProvideToReplace[i] = evProvide[i];
        evProvideToReplace[evProvideToReplace.length-1] = net.sf.appia.protocols.group.remote.RemoteViewEvent.class;
        evProvide = evProvideToReplace;
        
        final Class[] evAcceptToReplace = new Class[evAccept.length+1];
        for(int i=0; i<evAccept.length; i++)
            evAcceptToReplace[i] = evAccept[i];
        evAcceptToReplace[evAcceptToReplace.length-1] = net.sf.appia.protocols.group.remote.RemoteViewEvent.class;
        evAccept = evAcceptToReplace;
	}
	
	/**
	 * Creates a {@link RemoteGossipOutSession}
	 *
	 * @return the created session.
	 */
	public Session createSession() {
		return new RemoteGossipOutSession(this);
	}
}










