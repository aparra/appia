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
 
package net.sf.appia.protocols.group;

import net.sf.appia.protocols.group.events.GroupSendableEvent;

/**
 * An Event buffer.
 * <br>
 * The implementation uses <i>arrays</i> and is optimized to scenarios where
 * events are buffered during some period of time, without limitations, and
 * at some point the events are all removed in FIFO order.
 * <br>
 * The array will grow when necessary, but will only shrink when there are no
 * more elements. It then reduces to a pre-defined minimum size.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.Event
 */
public class EventBuffer {
  
  private GroupSendableEvent[] buf;
  private int min_size;
  /**
   * The number of events in the buffer.
   */
  public int length;
  private int next;
  
  /**
   * Creates an Event buffer with the given minimum size.
   *
   * @param size the minimum size
   */
  public EventBuffer(int size) {
    buf=new GroupSendableEvent[size];
    min_size=size;
    length=0;
    next=0;
  }
  
  /**
   * Puts an Event in the buffer.
   *
   * @param ev the {link appia.Event Event} to put.
   */
  public void put(GroupSendableEvent ev) {
    if ((next+length) == buf.length) {
      GroupSendableEvent[] aux=new GroupSendableEvent[2*buf.length];
      System.arraycopy(buf,0,aux,0,length);
      buf=aux;
    }
    
    buf[next+length]=ev;
    length++;
  }
  
  /**
   * Gets an Event from the buffer in FIFO order.
   *
   * @return the next Event in buffer
   */
  public GroupSendableEvent get() {
    if (length == 0)
      return null;
    
    GroupSendableEvent ev=buf[next];
    buf[next]=null;
    next++;
    length--;
    
    if (length == 0) {
      buf=new GroupSendableEvent[min_size];
      next=0;
    }
    
    return ev;
  }
}