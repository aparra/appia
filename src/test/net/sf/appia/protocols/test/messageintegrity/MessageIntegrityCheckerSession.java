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

package net.sf.appia.protocols.test.messageintegrity;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.message.Message;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import org.apache.log4j.Logger;
//import org.continuent.appia.core.message.MessageException;

/**
 * This class defines a MessageIntegrityCheckerSession
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class MessageIntegrityCheckerSession extends Session implements InitializableSession {

    private static final int HEADER_SIZE=4;
    
    private static Logger log = Logger.getLogger(MessageIntegrityCheckerSession.class);
    private boolean active = true;
    private String tag = "";

    /**
     * Creates a new MessageIntegrityCheckerSession.
     * @param layer
     */
    public MessageIntegrityCheckerSession(Layer layer) {
        super(layer);
    }

    /**
     * Main event handler.
     * @see net.sf.appia.core.Session#handle(net.sf.appia.core.Event)
     */
    public void handle(Event event){
        if(log.isDebugEnabled())
            log.debug("Received event on handle: "+event.getClass().getName()+" with direction "+(event.getDir()==Direction.UP? "UP":"DOWN"));
        if(!active){
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
            return;
        }
        
        if(event instanceof SendableEvent){
            final Message message = ((SendableEvent)event).getMessage();
            int size = -1;
            if(event.getDir() == Direction.DOWN){
                size = message.length();
                message.pushInt(size);
            }
            else {
                if(message.length() < HEADER_SIZE)
                    log.warn("Received event of type "+event.getClass().getName()+" with message length = "+message.length());
                else{
                    size = message.popInt();
                    if(message.length() != size){
                        log.error("Message integrity check has failed on tag [ "+tag+" ]. Message contains "
                                +message.length()+" bytes and the value it read was "+size+ "bytes in event of type "+event.getClass().getName());
                    }
                }
            }
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
        }
        else{
            log.warn("Received unexpected event of type "+event.getClass().getName()+". Forwarding it.");
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Initializes the session using the parameters given in the XML configuration.
     * Possible parameters:
     * <ul>
     * <li><b>active</b> it is a boolean value that sets if this protocol should check anything or not.
     * Default is true.
     * <li><b>tag</b> A String that is printed in the error message when integrity check fails. 
     * This should be used when there are several protocols of this type among the stack.
     * </ul>
     * 
     * @param params The parameters given in the XML configuration.
     * @see net.sf.appia.xml.interfaces.InitializableSession#init(SessionProperties)
     */
    public void init(SessionProperties params) {
        if(params.containsKey("active")){
            active = params.getBoolean("active");
        }
        if(params.containsKey("tag")){
            tag = params.getString("tag");
        }
    }
}
