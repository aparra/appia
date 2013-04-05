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

package net.sf.appia.jgcs.protocols.services;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.jgcs.protocols.top.JGCSGroupEvent;
import net.sf.appia.jgcs.protocols.top.JGCSSendEvent;
import net.sf.appia.jgcs.protocols.top.JGCSSendableEvent;
import net.sf.appia.protocols.common.ServiceEvent;

/**
 * This class defines a DefaultServiceSession
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class DefaultServiceSession extends Session {

    /**
     * Creates a new DefaultServiceSession.
     * @param layer
     */
    public DefaultServiceSession(Layer layer) {
        super(layer);
    }
    
    public void handle(Event event){
        try {
            event.go();
            if(isAccepted(event)){
                new ServiceEvent(event.getChannel(),Direction.UP,this,((SendableEvent)event).getMessage()).go();
            }
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }
    
    private boolean isAccepted(Event e){
        return (e instanceof JGCSSendableEvent || e instanceof JGCSSendEvent || e instanceof JGCSGroupEvent) 
        && (e.getDir() == Direction.UP);
    }

}
