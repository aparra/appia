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
 package net.sf.appia.protocols.loopBack;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.group.*;
import net.sf.appia.protocols.group.events.*;
import net.sf.appia.protocols.group.intra.*;

/**
 * Class LoopBackSession provides the dynamic behavior for the
 * LoopBack Protocol
 *
 * @author Hugo Miranda
 * @see    LoopBackLayer
 * @see    Session
 */
public class LoopBackSession extends Session {

    private int myRank;
    private Endpt myEndpt;

    /**
     * Default Constructor.
     */
    public LoopBackSession(Layer l) {
	super(l);
    }

    /**
     * Event Dispatcher.
     * @param e Received event.
     */
    public void handle(Event e) {	
	
	if(e instanceof ChannelInit) {}

	if(e instanceof View){
	    View view = (View) e;
	    myRank = view.ls.my_rank;
	    myEndpt = view.vs.view[myRank];
	}

	if(e instanceof GroupSendableEvent && !(e instanceof Send) && e.getDir() == Direction.DOWN)
	    handleGroupSendableEvent((GroupSendableEvent) e);
	try {
	    e.go();
	}
	catch(AppiaEventException ex) {
	    System.err.println("Error sending event");
	}
    }
    
    private void handleGroupSendableEvent(GroupSendableEvent e){
        GroupSendableEvent cloned=null;
       
        //clones event, invert direction and send it upward.
        try{
            cloned= (GroupSendableEvent)e.cloneEvent();
        }
        catch(CloneNotSupportedException ex){
            System.err.println("Error sending event");
        }
        cloned.setDir(Direction.invert(cloned.getDir()));
        cloned.setSourceSession(this);
        cloned.dest=e.source;
        cloned.source=myEndpt;
        cloned.orig=myRank;
        cloned.setChannel(e.getChannel());
        try{
            cloned.init();
            cloned.go();
        }
        catch(AppiaEventException ex){
            System.err.println("Error Sending event");
        }
    }

    public void boundSessions(Channel channel) {
    }
}
