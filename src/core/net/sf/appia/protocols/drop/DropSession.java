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
// Version: 1.0/J                                                   //
//                                                                  //
// Copyright, 2000, Universidade de Lisboa                          //
// All rights reserved                                              //
// See license.txt for further information                          //
//                                                                  //
// Class: DropSession: Randomly drop sending messages               //
//                                                                  //
// Author: Hugo Miranda, 05/2000                                    //
//                                                                  //
// Change Log:                                                      //
//  11/Jul/2001: The debugOn variable was changed to the            //
//               DropConfig interface                               //
//                                                                  //
//////////////////////////////////////////////////////////////////////


import java.io.PrintStream;

import net.sf.appia.core.*;
import net.sf.appia.core.events.*;
import net.sf.appia.core.events.channel.Debug;

/**
 * Class DropSession randomly drops SendableEvents going down. Messages are
 * droped randomly with the probability stated in the dropRate public attribute.
 *
 * @author Hugo Miranda
 * @see    DropLayer
 * @see    net.sf.appia.core.Session
 * @see    net.sf.appia.core.events.SendableEvent
 */

public class DropSession extends Session {

    private PrintStream   debugOutput=null;

    /**
     * Probability of messages to be dropped. This is a public static
     * value that can be changed any time. 0 prevents messages from
     * being lost.
     */

    public static double  dropRate=0.3;

    /**
     * @see net.sf.appia.core.Session
     */

    public DropSession(DropLayer l) {
	super(l);
    }

    /**
     * This is the protocol's main event handler.
     * It accepts the following events:
     * <ul>
     * <li>net.sf.appia.core.events.SendableEvent
     * <li>net.sf.appia.protocols.group.intra.View
     * </ul>
     * 
     * @param e the event to handle.
     * @see net.sf.appia.core.Session#handle(net.sf.appia.core.Event)
     */
    public void handle(Event e) {
	if(e instanceof SendableEvent) { 
          if(e.getDir()==Direction.UP || Math.random()>=dropRate) {
	    try {
		e.go();
                if(DropConfig.debugOn && debugOutput!=null)
                  debugOutput.println("Drop: Event passed");
	    }
	    catch(AppiaEventException ex) {
		System.err.println("Unexpected exception in Drop Session");
	    }
	  }
          else
            if(DropConfig.debugOn && debugOutput!=null)
              debugOutput.println("Drop: Event dropped");
        }
        else if(e instanceof Debug)
          handleDebug((Debug)e);
    }

    private void handleDebug(Debug e) {
      if(e.getQualifierMode() == EventQualifier.ON) {
        debugOutput=new PrintStream(e.getOutput());
        debugOutput.println("Drop: Debug started");
      }
      else if(e.getQualifierMode() == EventQualifier.OFF) {
        debugOutput=null;
      }
      else {
        PrintStream p=new PrintStream(e.getOutput());
        p.println("Drop state dumping:");
        p.println("Drop rate:"+dropRate);
        p.println("Debug output is currently "+(debugOutput==null? "off." : "on."));
      }
    }
}
