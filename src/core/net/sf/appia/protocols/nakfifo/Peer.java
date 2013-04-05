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
 * Peer.java
 *
 * Created on 11 de Julho de 2003, 13:08
 */

package net.sf.appia.protocols.nakfifo;

import java.util.LinkedList;

import net.sf.appia.core.Channel;
import net.sf.appia.core.TimeProvider;

/**
 *
 * @author  Alexandre Pinto
 */
public class Peer {
  
  public Object addr;
  
  public long first_msg_sent;
  
  public long last_msg_sent;
  public long last_msg_confirmed;
  
  public long last_msg_delivered=0;
  // Used only by NakFifoMulticast
  public long last_confirm_sent=0;
  
  public LinkedList unconfirmed_msgs=new LinkedList();
  public LinkedList undelivered_msgs=new LinkedList();
  
  // TODO: 
  // Option 1: add pending list for events waiting to be sent when unconfirmed grows to much
  // Option 2: add a global pending list for such events (not in here)
  
  public Nacked nacked=null;
  
  public int rounds_msg_sent=0;
  public int rounds_msg_recv=0;
  public int rounds_appl_msg=0;
  
  public Channel last_channel=null;
  
  /** Creates a new instance of Peer */
  public Peer(Object addr, TimeProvider time) {
    this.addr=addr;
    
    // TODO: more random
    last_msg_sent=time.currentTimeMillis() & MessageUtils.INIT_MASK;
    if (last_msg_sent == MessageUtils.INIT_MASK)
      last_msg_sent--;
    last_msg_confirmed=last_msg_sent;
    first_msg_sent=last_msg_sent+1;
  }
  
  public Peer(Object addr, long init) {
    this.addr=addr;
    
    last_msg_sent=last_msg_confirmed=init;
    first_msg_sent=init+1;
  }
}