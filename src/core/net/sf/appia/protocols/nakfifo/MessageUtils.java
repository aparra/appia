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
package net.sf.appia.protocols.nakfifo;

import net.sf.appia.core.message.Message;
import net.sf.appia.core.message.MsgBuffer;

public class MessageUtils {

	/** <b>DON'T CHANGE</b> */
  public static final byte NOFLAGS=0;
  public static final byte IGNORE_FLAG=1;
  
  public static final int SEQ_SIZE = 4;
  public static final long INIT_MASK = 0x7FFFFFFF;
  private static final long MASK = ((long)1) << 31;
  private static final long ADD_MASK = ((long)1) << 32;
  private static final long CLEAR_MASK = (((long)0xFFFF) << 48) | (((long)0xFFFF) << 32);
 
  private MsgBuffer mbuf=new MsgBuffer();
  
  public void pushSeq(Message msg, long seq) {
	  mbuf.len = SEQ_SIZE;
	  msg.push(mbuf);
	  
	  mbuf.data[mbuf.off + 0] = (byte) ((seq >>> 24) & 0xFF);
	  mbuf.data[mbuf.off + 1] = (byte) ((seq >>> 16) & 0xFF);
	  mbuf.data[mbuf.off + 2] = (byte) ((seq >>> 8) & 0xFF);
	  mbuf.data[mbuf.off + 3] = (byte) ((seq >>> 0) & 0xFF);
  }

  public long popSeq(Message msg, long base, boolean keep) {
	  mbuf.len = SEQ_SIZE;
	  if (keep)
	  	msg.peekReadOnly(mbuf);
	  else
	  	msg.popReadOnly(mbuf);
	  
	  long l = 
	  	((long)(mbuf.data[mbuf.off + 0] & 0xFF) << 24) +
	  	((long)(mbuf.data[mbuf.off + 1] & 0xFF) << 16) +
	  	((long)(mbuf.data[mbuf.off + 2] & 0xFF) <<  8) +
	  	((long)(mbuf.data[mbuf.off + 3] & 0xFF));

	  if (((l & MASK) == 0) && ((base & MASK) != 0))
      l = ((base & CLEAR_MASK) + ADD_MASK) | l;
    else
      l = (base & CLEAR_MASK) | l;
    return l;
  }
}
