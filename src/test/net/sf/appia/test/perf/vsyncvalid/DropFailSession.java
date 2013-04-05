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
 * 
 */
package net.sf.appia.test.perf.vsyncvalid;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.utils.ParseUtils;
import net.sf.appia.test.perf.PerfCastEvent;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;



/**
 * @author Alexandre Pinto
 *
 */
public class DropFailSession extends Session implements InitializableSession {

  /**
   * Default number of messages before failing.
   */
  public static final int DEFAULT_MSGS_TO_FAIL=50;
  
  /**
   * Default number of messages after failing after which it exit.
   */
  public static final int DEFAULT_MSGS_TO_EXIT=5;
    
  /**
   * @param layer
   */
  public DropFailSession(Layer layer) {
    super(layer);
  }
  
  private int msgsToFail=DEFAULT_MSGS_TO_FAIL;
  private int msgsToExit=DEFAULT_MSGS_TO_EXIT;
  private int countMsgs=0;
  private InetSocketAddress destination=null;

  /**
   * Initializes the session using the parameters given in the XML configuration.
   * Possible parameters:
   * <ul>
   * <li><b>fail</b> number of messages before failing.
   * <li><b>exit</b> number of messages after failing after which it exit.
   * <li><b>destination</b> The destination address in the form [IP:port].
   * </ul>
   * 
   * @param params The parameters given in the XML configuration.
   */
  public void init(SessionProperties params) {
    if (params.containsKey("fail"))
      msgsToFail=params.getInt("fail");
    if (params.containsKey("exit"))
      msgsToExit=params.getInt("exit");
    if (params.containsKey("destination")) {
      try {
        destination=ParseUtils.parseSocketAddress(params.getString("destination"), null, -1);
      } catch (UnknownHostException e) {
        e.printStackTrace();
        System.exit(1);
      } catch (ParseException e) {
        e.printStackTrace();
        System.exit(1);
      }
    }
    
    if (destination == null) {
      System.err.println("Require destination address of messages to drop.");
      System.exit(1);
    }
  }

  public void handle(Event event) {
    if (event instanceof PerfCastEvent)
      handlePerfCastEvent((PerfCastEvent)event);
    else
      try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
  }

  private void handlePerfCastEvent(PerfCastEvent event) {
    if (event.getDir() == Direction.UP) {
      try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
      return;
    }
    
    if ((destination != null) && event.dest.equals(destination)) {
      ++countMsgs;
      System.out.println("Messages to "+event.dest+" = "+countMsgs+"("+msgsToFail+")");
      if (countMsgs >= msgsToFail) {
        System.out.println("Dropping Message");
        if (countMsgs >= msgsToFail+msgsToExit) {
          System.out.println("Exiting");
          System.exit(0);
        }
        return;      
      }
    }
    try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
  }

}
