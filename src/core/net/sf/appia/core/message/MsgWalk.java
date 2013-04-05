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
 * Title:        <p>
 * Description:  <p>
 * Copyright:    Copyright (c) <p>
 * Company:      <p>
 * @author
 * @version 1.0
 */
package net.sf.appia.core.message;

import net.sf.appia.core.message.Message;

public class MsgWalk {

  private Message.Block block;
  private int first_off,first_len;

  public MsgWalk(Message.Block first) {
    block=first;
    this.first_off=-1;
    this.first_len=-1;
  }
  
  public MsgWalk(Message.Block first, int first_off, int first_len) {
    block=first;
    this.first_off=first_off;
    this.first_len=first_len;
  }

  public void next(MsgBuffer mbuf) {
    if (block==null) {
      mbuf.data=null;
      mbuf.off=0;
      mbuf.len=0;
    } else {

      Message.Block b=block;
      block=block.next;

      mbuf.data=b.buf;
      if ((first_off >= 0) && (first_len >= 0)) {
        mbuf.off=first_off;
        mbuf.len=first_len;
        first_off=-1;
        first_len=-1;
      } else {
        mbuf.off=b.off;
        mbuf.len=b.len;
      }
    }
  }
}