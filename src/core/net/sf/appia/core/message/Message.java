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
 */

/* Change Log:
 * Nuno Carvalho - added a MemoryManager to implement "quota" of a Channel
 *               - added some code to deal with the memory manager
 * (9-Jul-2001)
 * Nuno Carvalho - bug removed from clone method. size of message has to be 0
 *                 before doing setByteArray()
 * (18-Jan-2002)
 * Nuno Carvalho - functions setMemoryManager(), bind(), unBind() and finalize() changed
 *                 for better performance.
 * (25-Jan-2002)
 *
 * Alexandre Pinto - Changed the code of all methods to support copy-on-write semantics
 * (7-Oct-2003)
 */

package net.sf.appia.core.message;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.UTFDataFormatException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.sf.appia.core.*;
import net.sf.appia.core.memoryManager.*;
import net.sf.appia.core.message.MsgBuffer;


/**
 * This class is part of the SendableEvent and contains the information that 
 * is going to be sent through the network. It works as a stack, and people can push and pop
 * headers into/from the message.
 * When the message is cloned, it uses the copy-on-write method to improve performance, but
 * this is opaque for the user.
 * @author Alexandre Pinto
 * @version 1.0
 */
public class Message implements Cloneable {
	
	public static final int INCREASE = 512;
	
	public static final boolean CHECK = false;
	private static boolean checked = false;
	
	private static final int SHORTSIZE = 2;
	private static final int INTSIZE = 4;
	private static final int LONGSIZE = 8;
	private static final int BOOLSIZE = 1;
	
	private static final byte GENERIC_OBJECT = 0;
	private static final byte INET_SOCKET_ADDR = 1;
	
	private AuxOutputStream aos = null;
	private AuxInputStream ais = null;
	
	private MsgBuffer mbuf;
	
	
	/**
     * This class defines a Block
     * 
     * @author <a href="mailto:apinto@di.fc.ul.pt">Alexandre Pinto</a>
     * @version 1.0
	 */
	public class Block {
		public byte[] buf;
		public int offset;
		public int length;
		public int off;
		public int len;
		public Block next = null;
		public int refs = 1;
		
		public Block() {}
		
		public Block(byte[] buf, int off, int len, int pos) {
			this.buf = buf;
			this.offset = off;
			this.length = len;
			this.off = pos;
			this.len = length - (pos - off);
		}
	}
	
	protected Block first = null;
	protected int size = 0;
	
	protected boolean ro_mode = false;
	protected int ro_off = 0;
	protected int ro_len = 0;
	
	/* Memory Manager to bind and unbind */
	private MemoryManager memoryManager=null;
	private Object mmLock = new Object();
	protected boolean canBind = true;
	
	/**
	 * Builds a new empty message.
	 */
	public Message() {
		init();
	}
	
	/**
	 * Builds a new message with initial data.
	 * @param data the data to add into the message.
	 * @param offset the offset of important data
	 * @param length the length of the data
	 */
	public Message(byte[] data, int offset, int length) {
		
		// added on 7-Oct-2003
		if (AppiaConfig.QUOTA_ON)
			bind(length);
		
		Block b = new Block(data, offset, length, offset);
		first = b;
		size = length;
		init();
	}
	
	private void init(){
		mbuf = new MsgBuffer();
		if(CHECK)
		    synchronized (getClass()) {
		        if (!checked)
		            check();
		    }
	}
	
	/**
	 * Sets the data of the message.
	 * If the message contained some data, it is discarded.
	 * @param data the data to add into the message.
	 * @param offset the offset of important data
	 * @param length the length of the data
	 */
	public void setByteArray(byte[] data, int offset, int length) {
		
		// added on 7-Oct-2003
		if (AppiaConfig.QUOTA_ON) {
			if (size > 0)
				unBind(size);
			bind(length);
		}
		
		while (first != null) {
			first.refs--;
			first = first.next;
		}
		
		Block b = new Block(data, offset, length, offset);
		first = b;
		size = length;
		
		ro_mode = false;
		ro_len = 0;
		ro_off = 0;
	}
	
	/**
	 * Gets the length (in bytes) of the message.
	 * @return the length (in bytes) of the message.
	 */
	public int length() {
		return size;
	}
	
	/**
	 * Peeks bytes from the message without removing it from the message.
	 * @param mbuf the buffer that will contain the requested data. 
	 */
	public void peek(MsgBuffer mbuf) {
		if (first == null) {
			mbuf.data = null;
			mbuf.off = 0;
			mbuf.len = 0;
			return;
		}
		
		if ((ro_mode && (mbuf.len <= ro_len)) || (!ro_mode && (mbuf.len <= first.len))) {
			if (first.refs > 1) {
				if (ro_mode)
					clearReadOnly();
				else
					first=copyBlock(first,first.off,first.len);
			}
			
			mbuf.data = first.buf;
			mbuf.off = first.off;
		} else {
			pop(mbuf);
			// this is done because pop was called.
			if (AppiaConfig.QUOTA_ON)
				bind(mbuf.len);
			Block b = new Block(mbuf.data, mbuf.off, mbuf.len, mbuf.off);
			b.next = first;
			first = b;
			size += b.len;
		}
	}
	
	/**
	 * Discards the first length bytes of the message.
	 * @param length the number of bytes to discard.
	 * @return the remaining number of bytes in the message.
	 */
	public int discard(int length) {
		// added on 7-Oct-2003
		if (AppiaConfig.QUOTA_ON && ((size-length)>0))
			unBind(Math.min(length,size));
		
		int r = length > size ? size : length;
		int newsize = size - r;
		
		while ((size > newsize) && (first != null)) {
			if (!ro_mode && (first.refs > 1)) {
				ro_mode = true;
				ro_off = first.off;
				ro_len = first.len;
			}
			
			if (ro_mode) {
				if ((size - newsize) >= ro_len) {
					size -= ro_len;
					first.refs--;
					first = first.next;
					
					if (first != null) {
						ro_off = first.off;
						ro_len = first.len;
					} else {
						ro_mode = false;
						ro_off = 0;
						ro_len = 0;
					}
				} else {
					int remove = size - newsize;
					
					ro_off += remove;
					ro_len -= remove;
					size -= remove;
				}
			} else {
				if ((size - newsize) >= first.len) {
					size -= first.len;
					first = first.next;
				} else {
					int remove = size - newsize;
					
					first.off += remove;
					first.len -= remove;
					size -= remove;
				}
			}
		}
		
		return r;
	}
	
	/**
	 * Discards all message contents.<br>
	 * Although equals to "discard(length())" it is faster. Notice that if a {@link MemoryManager MemoryManager}
	 * is being used in a Channel, all stored messages <b>must</b> be empty when they are
	 * no longer needed.
	 */
	public void discardAll(){
		if (AppiaConfig.QUOTA_ON)
			unBind(size);	  
		size = 0;
		while(first != null){
			first.refs--;
			first = first.next;
		}
		ro_mode = false;
	}
	
	/**
	 * Pops a header from the message and puts the data into the MessageBuffer.
	 * The number of bytes poped could be different from the nunber of bytes requested.
	 * This method has the same semantics as a receive() when using TCP. 
	 * @param mbuf the MessageBuffer to store the poped data.
	 */
	public void pop(MsgBuffer mbuf) {
		if (first == null) {
			mbuf.data = null;
			mbuf.off = 0;
			mbuf.len = 0;
			return;
		}
		
		if ((ro_mode && (mbuf.len <= ro_len)) || (!ro_mode && (mbuf.len <= first.len))) {
            if (ro_mode)
                clearReadOnly();
            
			if (first.refs > 1)
			    first=copyBlock(first,first.off,first.len);
			
			mbuf.data = first.buf;
			mbuf.off = first.off;
			
			first.off += mbuf.len;
			first.len -= mbuf.len;
			
			if (first.len == 0) {
				first.refs--;
				first = first.next;
			}
			size -= mbuf.len;
		} else {
			
			mbuf.len = mbuf.len > size ? size : mbuf.len;
			mbuf.off = 0;
			mbuf.data = new byte[mbuf.len];
			int newsize = size - mbuf.len;
			int off = 0;
			
			if (ro_mode) {
				System.arraycopy(first.buf, ro_off, mbuf.data, off, ro_len);
				off += ro_len;
				
				first.refs--;
				first=first.next;
				size-=ro_len;
				
				ro_mode=false;
				ro_off=0;
				ro_len=0;
			}
			
			while ((size > newsize) && (first != null)) {
				if ((size - newsize) >= first.len) {
					System.arraycopy(first.buf, first.off, mbuf.data, off, first.len);
					off += first.len;
					
					size -= first.len;
					first.refs--;
					first = first.next;
				} else {
					int remove = size - newsize;
					
					System.arraycopy(first.buf, first.off, mbuf.data, off, remove);
					off += remove;
					
					if (first.refs > 1)
						first=copyBlock(first,first.off+remove,first.len-remove);
					else {
						first.off += remove;
						first.len -= remove;
					}
					
					size -= remove;
				}
			}
		}
		
		// added on 7-Oct-2003
		if (AppiaConfig.QUOTA_ON)
			unBind(mbuf.len);
	}
	
	/**
	 * Push a header into the message.
	 * @param mbuf the data to push into the message.
	 */
	public void push(MsgBuffer mbuf) {
		
		// added on 7-Oct-2003
		if (AppiaConfig.QUOTA_ON && canBind)
			bind(mbuf.len);
		
		if (ro_mode)
			clearReadOnly();
		
		int l = mbuf.len;
		
		if ((first == null) || (l > (first.off - first.offset)) || (first.refs > 1)) {
			byte[] a = new byte[l + INCREASE];
			Block b = new Block(a, 0, a.length, a.length - l);
			
			b.next = first;
			first = b;
		} else {
			first.off -= l;
			first.len += l;
		}
		
		mbuf.data = first.buf;
		mbuf.off = first.off;
		
		size += l;
	}
	
	/**
	 * Truncates the message to newLength bytes, removing the last bytes of the message.
	 * @param newLength the new length of the message.
	 * @return the number of bytes removed from the message.
	 */
	public int truncate(int newLength) {
		int remain = newLength;
		Block b;
		
		if (newLength >= size) {
			return 0;
		}
		
		// added on 7-Oct-2003
		if (AppiaConfig.QUOTA_ON && (size<newLength))
			unBind(size-newLength);
		
		if (ro_mode)
			clearReadOnly();
		
		for (b = first;(remain > 0) && (b != null); b = b.next) {
			if (b.refs > 1) {
				// TODO: improve
				copyRemain();
				first.len = newLength;
				remain = 0;
			} else {
				if (b.len >= remain) {
					b.len = remain;
					b.next = null;
					remain = 0;
					//the following bytes are removed by Garbage Collection
				} else {
					remain -= b.len;
				}
			}
		}
		
		size = newLength;
		return newLength - remain;
	}
	
	/**
	 * Fragments the message. The current message keeps the first length bytes and the remaing bytes 
	 * are moved to the message m.
	 * @param m the message where to add the first length bytes.
	 * @param length the number of bytes to move.
	 */
	public void frag(Message m, int length) {
		int remain = length;
		Block b;
		
		if (size <= length) {
			m.first = null;
			m.size = 0;
			return;
		}
		
		// added on 7-Oct-2003
		if (AppiaConfig.QUOTA_ON) {
			int auxSize = Math.max(size-length,0);
			m.bind(auxSize);
			unBind(auxSize);
		}
		
		if (ro_mode)
			clearReadOnly();
		
		Block copy=null;
		Block prev=null;
		int off=0;
		for (b = first;(remain > 0) && (b != null); b = b.next) {
			if ((copy == null) && (b.refs > 1)) {
				copy=new Block(new byte[remain],0,remain,0);
				off=0;
				if (prev == null)
					first=copy;
				else
					prev.next=copy;
			}
			
			if (b.len == remain) {
				if (copy != null) {
					System.arraycopy(b.buf,b.off,copy.buf,off,remain);
					b.refs--;
				}
				
				m.first = b.next;
				remain=0;
			} else if (b.len > remain) {
				
				if (copy != null) {
					System.arraycopy(b.buf,b.off,copy.buf,off,remain);
					m.first=copyBlock(b,b.off+remain,b.len-remain);
				} else {
					m.first =
						new Block(b.buf,b.off + remain,(b.offset + b.length) - (b.off + remain),b.off + remain);
					//System.out.println("BLOCK: size total: "+m.first.buf.length+" offset: "+m.first.offset+" length: "+m.first.length+" off: "+m.first.off+" len: "+m.first.len);
					m.first.next = b.next;
					
					b.next = null;
					b.length = m.first.offset - first.offset;
					b.len = remain;
				}
				remain=0;
			} else { // b.len < remain
				if (copy != null) {
					System.arraycopy(b.buf,b.off,copy.buf,off,b.len);
					off+=b.len;
					b.refs--;
				}
				remain -= b.len;
			}
			
			prev=b;
		}
		
		m.size = size - length;
		size = length;
	}
	
	/**
	 * Joins two messages, adding message m to the end of the current message.
	 * @param m the message to join with the current message.
	 */
	public void join(Message m) {
		
		// added on 7-Oct-2003
		if (AppiaConfig.QUOTA_ON)
			bind(m.length());
		
		if (first == null) {
			first = m.first;
			size = m.size;
			return;
		}
		
		if (ro_mode)
			clearReadOnly();
		if (m.ro_mode)
			m.clearReadOnly();
		
		Block b;
		Block prev=null;
		
		for (b = first; b != null; b = b.next) {
			if (b.refs > 1) {
				Block aux=copyBlock(b,b.off,b.len);
				if (prev == null)
					first=aux;
				else
					prev.next=aux;
				prev=aux;
			} else 
				prev=b;
		}
		
		prev.next = m.first;
		size += m.size;
		
		//clear message m
		if(AppiaConfig.QUOTA_ON)
			m.unBind(m.size);
		m.first = null;
		m.size = 0;
	}
	
	/**
	 * Gets a byte array version of the message.
	 * @return a byte array containing the message.
	 */
	public byte[] toByteArray() {
		int len = length();
		
		byte[] array = new byte[len];
		int off = 0;
		
		Block b;
		if (ro_mode) {
			System.arraycopy(first.buf, ro_off, array, off, ro_len);
			off += ro_len;
			b = first.next;
		} else {
			b = first;
		}
		
		for (; b != null; b = b.next) {
			System.arraycopy(b.buf, b.off, array, off, b.len);
			off += b.len;
		}
		
		return array;
	}
	
	/**
	 * Gets a MessageWalk object, which is used to travel through the message blocks.
	 * @return a new instance of MessageWalk object. 
	 */
	public MsgWalk getMsgWalk() {
		if (first.refs > 1)
			copyRemain();
		
		return new MsgWalk(first);
	}
	
	/**
	 * Clones the message.
	 * It just adds one more reference to the blocks. The message it self is only cloned
	 * if needed. It is used copy-on-write.
	 */
	public Object clone() throws CloneNotSupportedException {
		// added on 7-Oct-2003
		if (AppiaConfig.QUOTA_ON)
			bind(length());
		
		Message msg = (Message) super.clone();
		
		for (Block b=first ; b != null ; b=b.next)
			b.refs++;
		
		msg.mbuf = new MsgBuffer();
		msg.ais = new AuxInputStream();
		msg.aos = new AuxOutputStream();
		
		return msg;
	}
	
	/**
	 * Pops the requested number of bytes without copying these bytes.
	 * This method should be used only when the message is shared and
	 * the user don't want to modify the requested data. This method was created 
	 * for performance reasons.
	 * @param mbuf the MessageBuffer to store the requested data.
	 */
	public void popReadOnly(MsgBuffer mbuf) {
		if (first == null) {
			mbuf.data = null;
			mbuf.off = 0;
			mbuf.len = 0;
			return;
		}
		
		if (first.refs < 2) {
			if (ro_mode) {
				first.off=ro_off;
				first.len=ro_len;
				
				ro_mode=false;
				ro_off=0;
				ro_len=0;
			}
			pop(mbuf);
			return;
		}
		
		if (ro_mode) {
			if (mbuf.len > ro_len) {
				pop(mbuf);
				return;
			}
			
			mbuf.data = first.buf;
			mbuf.off = ro_off;
			
			ro_off += mbuf.len;
			ro_len -= mbuf.len;
			
			size -= mbuf.len;
			
		} else {
			if (mbuf.len > first.len) {
				pop(mbuf);
				return;
			}
			
			mbuf.data = first.buf;
			mbuf.off = first.off;
			
			ro_off = first.off + mbuf.len;
			ro_len = first.len - mbuf.len;
			size -= mbuf.len;
			
			ro_mode = true;
		}
		
		if (ro_len == 0) {
			first.refs--;
			first = first.next;
			
			ro_mode = false;
			ro_off = 0;
			ro_len = 0;
		}
		
		if (AppiaConfig.QUOTA_ON)
			unBind(mbuf.len);
	}
	
	/**
	 * Peeks the requested number of bytes without copying these bytes.
	 * This method should be used only when the message is shared and
	 * the user don't want to modify the requested data. This method was created 
	 * for performance reasons.
	 * @param mbuf the MessageBuffer to store the requested data.
	 */
	public void peekReadOnly(MsgBuffer mbuf) {
		if (first == null) {
			mbuf.data = null;
			mbuf.off = 0;
			mbuf.len = 0;
			return;
		}
		
		if (first.refs < 2) {
			if (ro_mode) {
				first.off=ro_off;
				first.len=ro_len;
				
				ro_mode=false;
				ro_off=0;
				ro_len=0;
			}
			peek(mbuf);
			return;
		}
		
		if (ro_mode) {
			if (mbuf.len > ro_len) {
				peek(mbuf);
				return;
			}
			
			mbuf.data = first.buf;
			mbuf.off = ro_off;
		} else {
			if (mbuf.len > first.len) {
				peek(mbuf);
				return;
			}
			
			mbuf.data = first.buf;
			mbuf.off = first.off;
			
			ro_off = first.off;
			ro_len = first.len;
			ro_mode = true;
		}
	}
	
    /**
     * Gets a MessageWalk object, which is used to travel through the message blocks.
     * This method should be used only when the message is shared and
     * the user don't want to modify the requested data. This method was created 
     * for performance reasons.
     * @return a new instance of MessageWalk object. 
     */
    public MsgWalk getMsgWalkReadOnly() {
        if (ro_mode)
          return new MsgWalk(first, ro_off, ro_len);
        else
          return new MsgWalk(first);
    }
    
	/*
	 * Clear the Read Only mode, copying the remaining blocks
	 */ 
	private void clearReadOnly() {
        if (first.refs > 1) {
            first=copyBlock(first,ro_off,ro_len);
        } else {
            first.off=ro_off;
            first.len=ro_len;
        }
        
		ro_mode = false;
		ro_off = 0;
		ro_len = 0;
	}
	
	/*
	 * Copies remaining blocks
	 */
	private void copyRemain() {
		if (first == null)
			return;
		
		byte[] a = toByteArray();
		Block b = new Block(a, 0, a.length, 0);
		while (first != null) {
			first.refs--;
			first = first.next;
		}
		first = b;
	}
	
	/*
	 * Copy one message block
	 */
	private Block copyBlock(Block b, int off, int len) {
		//byte[] a=new byte[len > INCREASE ? len+INCREASE : INCREASE];
		byte[] a=new byte[len];
		
		Block aux=new Block(a,0,a.length,a.length-len);
		System.arraycopy(b.buf,off,aux.buf,aux.off,len);
		
		aux.next=b.next;
		aux.refs=1;
		b.refs--;
		
		return aux;
	}
	
	
	
	/* Methods added on 9-Jul-2001 */
	
	/**
	 * Gets the current memory manager associated to this message.
	 * @return the current memory manager, or null if there are no memory manager.
	 */
	public MemoryManager getMemoryManager() {
		synchronized (mmLock) {
			return memoryManager;
		}
	}
	
	/**
	 * Changes the current memory manager of this message.
	 * Changes the quota of the message.
	 * @param newMM the new memory manager
	 * @throws AppiaOutOfMemory runtime exception
	 */
	public void setMemoryManager(MemoryManager newMM) {
		// just for debugging
		if (AppiaConfig.MM_DEBUG_ON && debug != null)
			debug.println(
					"Message: set memory manager From "
					+ ((memoryManager == null) ? "NULL" : memoryManager.getMemoryManagerID())
					+ " to "
					+ ((newMM == null) ? "NULL" : newMM.getMemoryManagerID())
					+ " INIT!!!");
		
		synchronized (mmLock) {
			// from null to not null
			if (memoryManager == null && newMM != null) {
				if (!newMM.malloc(size))
					throw new AppiaOutOfMemory("" + this.getClass().getName() + " : setMemoryManager");
			}
			// from not null to null
			else if (memoryManager != null && newMM == null)
				memoryManager.free(size);
			// from a memory manager to another
			else if (memoryManager != null && newMM != null && memoryManager != newMM) {
				if (!newMM.malloc(size))
					throw new AppiaOutOfMemory("" + this.getClass().getName() + " : setMemoryManager");
				memoryManager.free(size);
			}
			memoryManager = newMM;			
		}
		// just for debugging
		if (AppiaConfig.MM_DEBUG_ON && debug != null)
			debug.println(
					"Message: set memory manager DONE!!! changed to "
					+ ((memoryManager == null) ? "NULL" : memoryManager.getMemoryManagerID()));
	}
	
	/**
	 * Unbinds an amount of memory in the current memory manager.
	 * @param nBytes number of bytes to free.
	 */
	public void unBind(int nBytes) {
		synchronized (mmLock) {
			if (memoryManager != null)
				memoryManager.free(nBytes);
		}
	}
	
	/**
	 * Bind a amount of bytes in a Memory Manager.
	 * @param nBytes number of bytes to reserve.
	 * @throws runtime Exception AppiaOutOfMemory.
	 */
	public void bind(int nBytes) {
		synchronized (mmLock) {
			if ((memoryManager != null) && !memoryManager.malloc(nBytes))
				throw new AppiaOutOfMemory(
						"" + this.getClass().getName() + " : bind - " + memoryManager.getMemoryManagerID());
		}
	}
	
	static final boolean debugFull = false;
	static java.io.PrintStream debug = System.out;
	
	// Auxiliary classes
	
	private class AuxInputStream extends InputStream {
		
		private byte[] buf;
		private int off;
		private int len;
		
		public void setBuffer(byte[] buf, int off, int len) {
			this.buf = buf;
			this.off = off;
			this.len = len;
		}
		
		public int available() {
			return len;
		}
		
		public int read() {
			if (len == 0)
				return (-1);
			
			len--;
			return (buf[off++] & 0xff);
		}
		
		public int read(byte[] b) {
			return read(b, 0, b.length);
		}
		
		public int read(byte[] b, int off, int len) {
			if (len == 0)
				return (-1);
			
			if (len > this.len)
				len = this.len;
			
			System.arraycopy(this.buf, this.off, b, off, len);
			
			this.off += len;
			this.len -= len;
			return len;
		}
		
		public long skip(long n) {
			if (n > len)
				n = len;
			
			off += n;
			len -= n;
			return n;
		}
		
		public void close() {}
		
		public boolean markSupported() {
			return false;
		}
		public void mark(int readlimit) {}
		public void reset() {}
	}
	
	/*
	 * FROM EXTENDED MESSAGE
	 */
	
	/**
	 * Push an {@link java.lang.Object Object} (<i>header</i>) into the head of
	 * the message.
	 * <br>
	 * The Object must implement the
	 * {@link java.io.Serializable Serializable} interface. Otherwise
	 * a MessageException is raised.
	 *
	 * @param obj the {@link java.lang.Object Object} (<i>header</i>) to put in
	 * the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushObject(Object obj) {
		if(obj instanceof InetSocketAddress){
			pushInetSocketAddress((InetSocketAddress) obj);
			pushByte(INET_SOCKET_ADDR);
			return;
		}
		
		if (aos == null)
			aos = new AuxOutputStream();
		
		aos.setInternal();
		try {
			ObjectOutputStream oos = new ObjectOutputStream(aos);
			oos.writeObject(obj);
			oos.close();
		} catch (IOException ex) {
			throw new MessageException("Error writing object to message.",ex);
		}
		
		mbuf.len = aos.length();
		push(mbuf);
		aos.copyInternalTo(mbuf.data, mbuf.off, mbuf.len);
		pushInt(mbuf.len);
		pushByte(GENERIC_OBJECT);
	}
	
	/**
	 * Push an <i>long</i> into the head of the message.
	 *
	 * @param l the value to put in the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushLong(long l) {
		mbuf.len = LONGSIZE;
		push(mbuf);
		
		mbuf.data[mbuf.off + 0] = (byte) ((int) (l >>> 56) & 0xFF);
		mbuf.data[mbuf.off + 1] = (byte) ((int) (l >>> 48) & 0xFF);
		mbuf.data[mbuf.off + 2] = (byte) ((int) (l >>> 40) & 0xFF);
		mbuf.data[mbuf.off + 3] = (byte) ((int) (l >>> 32) & 0xFF);
		mbuf.data[mbuf.off + 4] = (byte) ((int) (l >>> 24) & 0xFF);
		mbuf.data[mbuf.off + 5] = (byte) ((int) (l >>> 16) & 0xFF);
		mbuf.data[mbuf.off + 6] = (byte) ((int) (l >>> 8) & 0xFF);
		mbuf.data[mbuf.off + 7] = (byte) ((int) (l >>> 0) & 0xFF);
	}
	
	/**
	 * Push an <i>int</i> into the head of the message.
	 *
	 * @param i the value to put in the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushInt(int i) {
		mbuf.len = INTSIZE;
		push(mbuf);
		
		mbuf.data[mbuf.off + 0] = (byte) ((i >>> 24) & 0xFF);
		mbuf.data[mbuf.off + 1] = (byte) ((i >>> 16) & 0xFF);
		mbuf.data[mbuf.off + 2] = (byte) ((i >>> 8) & 0xFF);
		mbuf.data[mbuf.off + 3] = (byte) ((i >>> 0) & 0xFF);
	}
	
	/**
	 * Push an <i>short</i> into the head of the message.
	 *
	 * @param s the value to put in the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushShort(short s) {
		mbuf.len = SHORTSIZE;
		push(mbuf);
		
		mbuf.data[mbuf.off + 0] = (byte) ((s >>> 8) & 0xFF);
		mbuf.data[mbuf.off + 1] = (byte) ((s >>> 0) & 0xFF);
	}
	
	/**
	 * Push an <i>boolean</i> into the head of the message.
	 *
	 * @param b the value to put in the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushBoolean(boolean b) {
		mbuf.len = BOOLSIZE;
		push(mbuf);
		
		mbuf.data[mbuf.off + 0] = (byte) (b ? 1 : 0);
	}
	
	/**
	 * Push an <i>double</i> into the head of the message.
	 *
	 * @param d the value to put in the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushDouble(double d) {
		pushLong(Double.doubleToLongBits(d));
	}
	
	/**
	 * Push an <i>float</i> into the head of the message.
	 *
	 * @param f the value to put in the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushFloat(float f) {
		pushInt(Float.floatToIntBits(f));
	}
	
	/**
	 * Push an <i>unsigned int</i> into the head of the message.
	 *
	 * @param ui the value to put in the message, only the lowest 32 bits are considered.
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushUnsignedInt(long ui) {
		mbuf.len = INTSIZE;
		push(mbuf);
		
		mbuf.data[mbuf.off + 0] = (byte) ((ui >>> 24) & 0xFF);
		mbuf.data[mbuf.off + 1] = (byte) ((ui >>> 16) & 0xFF);
		mbuf.data[mbuf.off + 2] = (byte) ((ui >>> 8) & 0xFF);
		mbuf.data[mbuf.off + 3] = (byte) ((ui >>> 0) & 0xFF);
	}
	
	/**
	 * Push an <i>unsigned short</i> into the head of the message.
	 *
	 * @param us the value to put in the message, only the lowest 16 bits are considered
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushUnsignedShort(int us) {
		mbuf.len = SHORTSIZE;
		push(mbuf);
		
		mbuf.data[mbuf.off + 0] = (byte) ((us >>> 8) & 0xFF);
		mbuf.data[mbuf.off + 1] = (byte) ((us >>> 0) & 0xFF);
	}
	
	/**
	 * Push a <i>byte</i> into the head of the message.
	 *
	 * @param b the value to put in the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushByte(byte b) {
		mbuf.len = 1;
		push(mbuf);
		
		mbuf.data[mbuf.off + 0] = b;
	}
	
	/**
	 * Push a <i>unsigned byte</i> into the head of the message.
	 *
	 * @param ub the value to put in the message, only the lowest 8 bits are considered.
	 * @see net.sf.appia.core.message.MessageException
	 */
	public void pushUnsignedByte(int ub) {
		mbuf.len = 1;
		push(mbuf);
		
		mbuf.data[mbuf.off + 0] = (byte)(ub & 0xFF);
	}
	
	/** Pushes the given string into the message.
	 * <br>
	 * The string is coded with <I>UTF-8</I>. The implementation is based on {@link java.io.DataOutputStream DataOutputStream} class.
	 * @param str The string to push into the message.
	 * @see java.io.DataOutputStream
	 */
	public void pushString(String str) {
		int strlen = str.length();
		int utflen = 0;
		char[] charr = new char[strlen];
		int c, count = 0;
		
		str.getChars(0, strlen, charr, 0);
		
		for (int i = 0; i < strlen; i++) {
			c = charr[i];
			if ((c >= 0x0001) && (c <= 0x007F)) {
				utflen++;
			} else if (c > 0x07FF) {
				utflen += 3;
			} else {
				utflen += 2;
			}
		}
		
		mbuf.len = utflen + 2;
		this.push(mbuf);
		
		if (utflen > 65535)
			throw new MessageException("Error writing string to message.",new UTFDataFormatException());
		
		mbuf.data[mbuf.off + count++] = (byte) ((utflen >>> 8) & 0xFF);
		mbuf.data[mbuf.off + count++] = (byte) ((utflen >>> 0) & 0xFF);
		for (int i = 0; i < strlen; i++) {
			c = charr[i];
			if ((c >= 0x0001) && (c <= 0x007F)) {
				mbuf.data[mbuf.off + count++] = (byte) c;
			} else if (c > 0x07FF) {
				mbuf.data[mbuf.off + count++] = (byte) (0xE0 | ((c >> 12) & 0x0F));
				mbuf.data[mbuf.off + count++] = (byte) (0x80 | ((c >> 6) & 0x3F));
				mbuf.data[mbuf.off + count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			} else {
				mbuf.data[mbuf.off + count++] = (byte) (0xC0 | ((c >> 6) & 0x1F));
				mbuf.data[mbuf.off + count++] = (byte) (0x80 | ((c >> 0) & 0x3F));
			}
		}
	}
	
	/**
	 * Pushes the given address into the message.
	 * @param address the address to push
	 */
	private void pushInetSocketAddress(InetSocketAddress address){
		MsgBuffer mbuf = new MsgBuffer();
		mbuf.len = 6;
		push(mbuf);
		mbuf.data[mbuf.off + 0] = (byte) ((address.getPort() >>> 8) & 0xFF);
		mbuf.data[mbuf.off + 1] = (byte) ((address.getPort() >>> 0) & 0xFF);
		System.arraycopy(address.getAddress().getAddress(), 0, mbuf.data, mbuf.off + 2, 4);
	}
	
	/**
	 * Pops a {@link java.lang.Object Object} from the message.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * an Object at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public Object popObject() {
		byte objectType = popByte();
		if(objectType == INET_SOCKET_ADDR){
			return popInetSocketAddress();
		}
		// else, is the generic object
		if (ais == null)
			ais=new AuxInputStream();
		
		mbuf.len = popInt();
		popReadOnly(mbuf);
        
		ais.setBuffer(mbuf.data, mbuf.off, mbuf.len);
		try {
		    ObjectInputStream ois = new ObjectInputStream(ais);
		    return ois.readObject();
		} catch (IOException e) {
            throw new MessageException("IO error reading object from message.",e);
		} catch (ClassNotFoundException e) {
            throw new MessageException("Trying to read an unknown object from message.",e);
		} catch (Exception ex) {
		    throw new MessageException("Error reading object from message.",ex);
		}
	}
	
	/**
	 * Pops a <i>long</i> from the message.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a long at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public long popLong() {
		mbuf.len = LONGSIZE;
		popReadOnly(mbuf);
		
		long ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		long ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		long ch3 = mbuf.data[mbuf.off + 2] & 0xFF;
		long ch4 = mbuf.data[mbuf.off + 3] & 0xFF;
		long ch5 = mbuf.data[mbuf.off + 4] & 0xFF;
		long ch6 = mbuf.data[mbuf.off + 5] & 0xFF;
		long ch7 = mbuf.data[mbuf.off + 6] & 0xFF;
		long ch8 = mbuf.data[mbuf.off + 7] & 0xFF;
		if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0)
			throw new MessageException("Error reading long value.", new EOFException());
		return (
				(ch1 << 56)
				+ (ch2 << 48)
				+ (ch3 << 40)
				+ (ch4 << 32)
				+ (ch5 << 24)
				+ (ch6 << 16)
				+ (ch7 << 8)
				+ (ch8 << 0));
		//return ((long)(popInt()) << 32) + (popInt() & 0xFFFFFFFFL);
	}
	
	/**
	 * Pops a <i>int</i> from the message.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a int at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public int popInt() {
		mbuf.len = INTSIZE;
		popReadOnly(mbuf);
		
		int ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		int ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		int ch3 = mbuf.data[mbuf.off + 2] & 0xFF;
		int ch4 = mbuf.data[mbuf.off + 3] & 0xFF;
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new MessageException("Error reading integer from message.",new EOFException());
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}
	
	/**
	 * Pops a <i>short</i> from the message.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a short at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public short popShort() {
		mbuf.len = SHORTSIZE;
		popReadOnly(mbuf);
		
		int ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		int ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		if ((ch1 | ch2) < 0)
			throw new MessageException("Error reading short value.",new EOFException());
		return (short) ((ch1 << 8) + (ch2 << 0));
	}
	
	/**
	 * Pops a <i>boolean</i> from the message.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a boolean at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public boolean popBoolean() {
		mbuf.len = BOOLSIZE;
		popReadOnly(mbuf);
		
		int ch = mbuf.data[mbuf.off + 0] & 0xFF;
		if (ch < 0)
			throw new MessageException("Error reading boolean value.",new EOFException());
		return (ch != 0);
	}
	
	/**
	 * Pops a <i>double</i> from the message.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a double at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public double popDouble() {
		return Double.longBitsToDouble(popLong());
	}
	
	/**
	 * Pops a <i>float</i> from the message.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a float at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public float popFloat() {
		return Float.intBitsToFloat(popInt());
	}
	
	/**
	 * Pops a <i>unsigned int</i> from the message.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a unsigned int at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public long popUnsignedInt() {
		mbuf.len = INTSIZE;
		popReadOnly(mbuf);
		
		long ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		long ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		long ch3 = mbuf.data[mbuf.off + 2] & 0xFF;
		long ch4 = mbuf.data[mbuf.off + 3] & 0xFF;
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new MessageException("Error reading Unsigned int value.",new EOFException());
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));      
	}
	
	/**
	 * Pops a <i>unsigned short</i> from the message.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a unsigned short at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public int popUnsignedShort() {
		mbuf.len = SHORTSIZE;
		popReadOnly(mbuf);
		
		int ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		int ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		if ((ch1 | ch2 ) < 0)
			throw new MessageException("Error reading Unsigned short value.",new EOFException());
		return ((ch1 << 8) + (ch2 << 0));      
	}
	
	/**
	 * Pops a <i>byte</i> from the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public byte popByte() {
		mbuf.len = 1;
		popReadOnly(mbuf);
		
		return mbuf.data[mbuf.off + 0];
	}
	
	/**
	 * Pops a <i>unsigned byte</i> from the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public int popUnsignedByte() {
		mbuf.len = 1;
		popReadOnly(mbuf);
		
		return (mbuf.data[mbuf.off + 0] & 0xFF);
	}
	
	/** Pops a string from the message.
	 * <br>
	 * The string is coded with <I>UTF-8</I>. The implementation is based on {@link java.io.DataInputStream DataInputStream} class.
	 * @return the string removed from the message
	 */
	public String popString() {
		mbuf.len = 2;
		popReadOnly(mbuf);
		
		int ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		int ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		if ((ch1 | ch2) < 0)
			throw new MessageException("Error reading string from message.",new EOFException());
		int utflen = (ch1 << 8) + (ch2 << 0);
		
		mbuf.len = utflen;
		char str[] = new char[utflen];
		popReadOnly(mbuf);
		int c, char2, char3;
		int count = 0;
		int strlen = 0;
		
		while (count < utflen) {
			c = (int) mbuf.data[mbuf.off + count] & 0xff;
			switch (c >> 4) {
			case 0 :
			case 1 :
			case 2 :
			case 3 :
			case 4 :
			case 5 :
			case 6 :
			case 7 :
				/* 0xxxxxxx*/
				count++;
				str[strlen++] = (char) c;
				break;
			case 12 :
			case 13 :
				/* 110x xxxx   10xx xxxx*/
				count += 2;
				char2 = (int) mbuf.data[mbuf.off + count - 1];
				str[strlen++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
				break;
			case 14 :
				/* 1110 xxxx  10xx xxxx  10xx xxxx */
				count += 3;
				char2 = (int) mbuf.data[mbuf.off + count - 2];
				char3 = (int) mbuf.data[mbuf.off + count - 1];
				str[strlen++] =
					(char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));
				break;
			default:
	            throw new MessageException("Error reading string from message (Wrong string size).",
	                    new EOFException());
			}
		}
		return new String(str, 0, strlen);
	}
	
	private InetSocketAddress popInetSocketAddress() {
		MsgBuffer mbuf = new MsgBuffer();
		mbuf.len = 6;
		pop(mbuf);
		StringBuilder strBuilder = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			strBuilder.append((int)(mbuf.data[mbuf.off + i + 2] & 0xff));
			strBuilder.append(".");
		}
		strBuilder.append((int)(mbuf.data[mbuf.off + 3 + 2] & 0xff));
		InetAddress inet = null;
		try {
			inet = InetAddress.getByName(strBuilder.toString());
		} catch (UnknownHostException ex) {
            throw new MessageException("Unable to retrieve the IP address \""+
                    strBuilder.toString()+"\" correctly.",ex);
        }
		int port = (((int) mbuf.data[mbuf.off]) & 0xFF) << 8;
		port |= (((int) mbuf.data[mbuf.off + 1]) & 0xFF) << 0;
		return new InetSocketAddress(inet,port);
	}
	
	/**
	 * Returns the <i>Object</i> from the head of the message, without removing it.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a Object at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public Object peekObject() {
		if (size <= 0)
			return null;
		
		byte objectType = popByte();
		if(objectType == INET_SOCKET_ADDR){
			InetSocketAddress addr = peekInetSocketAddress();
			pushByte(objectType);
			return addr;
		}
		
		if (ais == null)
			ais=new AuxInputStream();
		
		int size = popInt();
		mbuf.len = size;
		peekReadOnly(mbuf);
		ais.setBuffer(mbuf.data, mbuf.off, mbuf.len);
		try {
			ObjectInputStream ois = new ObjectInputStream(ais);
			Object obj = ois.readObject();
			pushInt(size);
			pushByte(objectType);
			return obj;
		} catch (Exception ex) {
			pushInt(size);
			pushByte(objectType);
			throw new MessageException("Error peeking object.",ex);
		}
	}
	
	/**
	 * Returns the <i>long</i> from the head of the message, without removing it.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a long at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public long peekLong() {
		mbuf.len = LONGSIZE;
		peekReadOnly(mbuf);
		
		int ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		int ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		int ch3 = mbuf.data[mbuf.off + 2] & 0xFF;
		int ch4 = mbuf.data[mbuf.off + 3] & 0xFF;
		int ch5 = mbuf.data[mbuf.off + 4] & 0xFF;
		int ch6 = mbuf.data[mbuf.off + 5] & 0xFF;
		int ch7 = mbuf.data[mbuf.off + 6] & 0xFF;
		int ch8 = mbuf.data[mbuf.off + 7] & 0xFF;
		if ((ch1 | ch2 | ch3 | ch4 | ch5 | ch6 | ch7 | ch8) < 0)
			throw new MessageException("Error peeking value.",new EOFException());
		return (
				(ch1 << 56)
				+ (ch2 << 48)
				+ (ch3 << 40)
				+ (ch4 << 32)
				+ (ch5 << 24)
				+ (ch6 << 16)
				+ (ch7 << 8)
				+ (ch8 << 0));
		//return ((long)(peekInt()) << 32) + (peekInt() & 0xFFFFFFFFL);
	}
	
	/**
	 * Returns the <i>int</i> from the head of the message, without removing it.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a int at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public int peekInt() {
		mbuf.len = INTSIZE;
		peekReadOnly(mbuf);
		
		int ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		int ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		int ch3 = mbuf.data[mbuf.off + 2] & 0xFF;
		int ch4 = mbuf.data[mbuf.off + 3] & 0xFF;
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new MessageException("Error peeking int value.",new EOFException());
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
	}
	
	/**
	 * Returns the <i>short</i> from the head of the message, without removing it.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a short at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public short peekShort() {
		mbuf.len = SHORTSIZE;
		peekReadOnly(mbuf);
		
		int ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		int ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		if ((ch1 | ch2) < 0)
			throw new MessageException("Error reading short value.", new EOFException());
		return (short) ((ch1 << 8) + (ch2 << 0));
	}
	
	/**
	 * Returns the <i>boolean</i> from the head of the message, without removing it.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a boolean at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public boolean peekBoolean() {
		mbuf.len = BOOLSIZE;
		peekReadOnly(mbuf);
		
		int ch = mbuf.data[mbuf.off + 0] & 0xFF;
		if (ch < 0)
			throw new MessageException("Error peeking boolean value.", new EOFException());
		return (ch != 0);
	}
	
	/**
	 * Returns the <i>double</i> from the head of the message, without removing it.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a double at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public double peekDouble() {
		return Double.longBitsToDouble(peekLong());
	}
	
	/**
	 * Returns the <i>float</i> from the head of the message, without removing it.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a float at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public float peekFloat() {
		return Float.intBitsToFloat(peekInt());
	}
	
	/**
	 * Returns the <i>unsigned int</i> from the head of the message, without removing it.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a unsigned int at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public long peekUnsignedInt() {
		mbuf.len = INTSIZE;
		peekReadOnly(mbuf);
		
		long ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		long ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		long ch3 = mbuf.data[mbuf.off + 2] & 0xFF;
		long ch4 = mbuf.data[mbuf.off + 3] & 0xFF;
		if ((ch1 | ch2 | ch3 | ch4) < 0)
			throw new MessageException("Error peeking unsigned integer value.",new EOFException());
		return ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));      
	}
	
	/**
	 * Returns the <i>unsigned short</i> from the head of the message, without removing it.
	 * <br>
	 * Throws MessageException if the message is empty or there isn't
	 * a unsigned int at the head of the message.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public int peekUnsignedShort() {
		mbuf.len = SHORTSIZE;
		peekReadOnly(mbuf);
		
		int ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		int ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		if ((ch1 | ch2 ) < 0)
			throw new MessageException("Error peeking unsigned short value.",new EOFException());
		return ((ch1 << 8) + (ch2 << 0));      
	}
	
	/**
	 * Returns the <i>byte</i> from the head of the message, without removing it.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public byte peekByte() {
		mbuf.len = 1;
		peekReadOnly(mbuf);
		
		return mbuf.data[mbuf.off + 0];
	}
	
	/**
	 * Returns the <i>unsigned byte</i> from the head of the message, without removing it.
	 *
	 * @return the value at the head of the message
	 * @see net.sf.appia.core.message.MessageException
	 */
	public int peekUnsignedByte() {
		mbuf.len = 1;
		peekReadOnly(mbuf);
		
		return (mbuf.data[mbuf.off + 0] & 0xFF);
	}
	
	/** Get, without removing, a string from the message.
	 * <br>
	 * The string is coded with <I>UTF-8</I>. The implementation is based on {@link java.io.DataInputStream DataInputStream} class.
	 * @return the string peeked from the message
	 */
	public String peekString() {
		mbuf.len = 2;
		popReadOnly(mbuf);
		
		int ch1 = mbuf.data[mbuf.off + 0] & 0xFF;
		int ch2 = mbuf.data[mbuf.off + 1] & 0xFF;
		if ((ch1 | ch2) < 0)
			throw new MessageException("Error peeking string from message.",new EOFException());
		int utflen = (ch1 << 8) + (ch2 << 0);
		
		mbuf.len = utflen;
		char str[] = new char[utflen];
		peekReadOnly(mbuf);
		int c, char2, char3;
		int count = 0;
		int strlen = 0;
		
		while (count < utflen) {
			c = (int) mbuf.data[mbuf.off + count] & 0xff;
			switch (c >> 4) {
			case 0 :
			case 1 :
			case 2 :
			case 3 :
			case 4 :
			case 5 :
			case 6 :
			case 7 :
				/* 0xxxxxxx*/
				count++;
				str[strlen++] = (char) c;
				break;
			case 12 :
			case 13 :
				/* 110x xxxx   10xx xxxx*/
				count += 2;
				char2 = (int) mbuf.data[mbuf.off + count - 1];
				str[strlen++] = (char) (((c & 0x1F) << 6) | (char2 & 0x3F));
				break;
			case 14 :
				/* 1110 xxxx  10xx xxxx  10xx xxxx */
				count += 3;
				char2 = (int) mbuf.data[mbuf.off + count - 2];
				char3 = (int) mbuf.data[mbuf.off + count - 1];
				str[strlen++] =
					(char) (((c & 0x0F) << 12) | ((char2 & 0x3F) << 6) | ((char3 & 0x3F) << 0));
				break;
			}
		}
		mbuf.len = 2;
		this.push(mbuf);
		mbuf.data[mbuf.off + count++] = (byte) ((utflen >>> 8) & 0xFF);
		mbuf.data[mbuf.off + count++] = (byte) ((utflen >>> 0) & 0xFF);
		return new String(str, 0, strlen);
	}
	
	private InetSocketAddress peekInetSocketAddress() {
		MsgBuffer mbuf = new MsgBuffer();
		mbuf.len = 6;
		peek(mbuf);
		StringBuilder strBuilder = new StringBuilder();
		for (int i = 0; i < 3; i++) {
			strBuilder.append((int)(mbuf.data[mbuf.off + i + 2] & 0xff));
			strBuilder.append(".");
		}
		strBuilder.append((int)(mbuf.data[mbuf.off + 3 + 2] & 0xff));
		InetAddress inet = null;
		try {
			inet = InetAddress.getByName(strBuilder.toString());
		} catch (UnknownHostException ex) {
            throw new MessageException("Unable to retrieve the IP address \""+
                    strBuilder.toString()+"\" correctly.",ex);
        }
		int port = (((int) mbuf.data[mbuf.off]) & 0xFF) << 8;
		port |= (((int) mbuf.data[mbuf.off + 1]) & 0xFF) << 0;
		return new InetSocketAddress(inet,port);
	}
	
	public class AuxOutputStream extends OutputStream {
		
		private byte[] buf;
		private int off;
		private int len;
		private byte[] internal = new byte[256];
		private boolean useInternal = true;
		private int init_off;
		
		public void setBuffer(byte[] buf, int off, int len) {
			this.buf = buf;
			this.off = off;
			this.len = len;
			useInternal = false;
			init_off = off;
		}
		
		public void setInternal() {
			buf = internal;
			off = 0;
			len = internal.length;
			useInternal = true;
		}
		
		public void close() {}
		public void flush() {}
		
		public void write(byte[] b) {
			write(b, 0, b.length);
		}
		
		public void write(byte[] b, int off, int len) {
			if (len > this.len) {
				if (useInternal)
					increaseCapacity(len - this.len);
				else
					len = this.len;
			}
			
			System.arraycopy(b, off, this.buf, this.off, len);
			
			this.off += len;
			this.len -= len;
		}
		
		public void write(int b) {
			if (len == 0) {
				if (useInternal)
					increaseCapacity(1);
				else
					return;
			}
			
			len--;
			buf[off++] = (byte) b;
		}
		
		public int copyInternalTo(byte[] buf, int off, int len) {
			if (len > internal.length)
				len = internal.length;
			
			System.arraycopy(internal, 0, buf, off, len);
			return len;
		}
		
		public int length() {
			return off - init_off;
		}
		
		private void increaseCapacity(int moreRequired) {
			internal = new byte[(buf.length + moreRequired) * 2];
			System.arraycopy(buf, 0, internal, 0, buf.length);
			len = internal.length - (buf.length - len);
			buf = internal;
		}
	}
	
	private void check() {
		if (aos == null)
			aos=new AuxOutputStream();
		if (ais == null)
			ais=new AuxInputStream();	
		
		byte[] aux = new byte[24];
		aos.setBuffer(aux, 0, aux.length);
		DataOutputStream dos = new DataOutputStream(aos);
		ais.setBuffer(aux, 0, aux.length);
		DataInputStream dis = new DataInputStream(ais);
		
		try {
			aos.setBuffer(aux, 0, aux.length);
			dos.writeShort((short) - 514);
			pushShort((short) - 514);
			mbuf.len = length();
			if (mbuf.len != aos.length())
				throw new Error("Size  mismatch in type Short");
			peek(mbuf);
			if (!equalBytes(mbuf.data, mbuf.off, aux, 0, mbuf.len))
				throw new Error("Raw contents mismatch in type Short");
			if (popShort() != -514)
				throw new Error("Value mismatch in type Short");
			
			aos.setBuffer(aux, 0, aux.length);
			dos.writeInt(-134480386);
			pushInt(-134480386);
			mbuf.len = length();
			if (mbuf.len != aos.length())
				throw new Error("Size  mismatch in type Int");
			peek(mbuf);
			if (!equalBytes(mbuf.data, mbuf.off, aux, 0, mbuf.len))
				throw new Error("Raw contents mismatch in type Int");
			if (popInt() != -134480386)
				throw new Error("Value mismatch in type Int");
			
			aos.setBuffer(aux, 0, aux.length);
			dos.writeLong(-119247870);
			pushLong(-119247870);
			mbuf.len = length();
			if (mbuf.len != aos.length())
				throw new Error("Size  mismatch in type Long");
			peek(mbuf);
			if (!equalBytes(mbuf.data, mbuf.off, aux, 0, mbuf.len))
				throw new Error("Raw contents mismatch in type Long");
			if (popLong() != -119247870)
				throw new Error("Value mismatch in type Long");
			
			aos.setBuffer(aux, 0, aux.length);
			dos.writeFloat(-134480386);
			pushFloat(-134480386);
			mbuf.len = length();
			if (mbuf.len != aos.length())
				throw new Error("Size  mismatch in type Float");
			peek(mbuf);
			if (!equalBytes(mbuf.data, mbuf.off, aux, 0, mbuf.len))
				throw new Error("Raw contents mismatch in type Float");
			if (popFloat() != -134480386)
				throw new Error("Value mismatch in type Float");
			
			aos.setBuffer(aux, 0, aux.length);
			dos.writeDouble(-134480386);
			pushDouble(-134480386);
			mbuf.len = length();
			if (mbuf.len != aos.length())
				throw new Error("Size  mismatch in type Double");
			peek(mbuf);
			if (!equalBytes(mbuf.data, mbuf.off, aux, 0, mbuf.len))
				throw new Error("Raw contents mismatch in type Double");
			if (popDouble() != -134480386)
				throw new Error("Value mismatch in type Double");
			
			aos.setBuffer(aux, 0, aux.length);
			dos.writeBoolean(true);
			pushBoolean(true);
			mbuf.len = length();
			if (mbuf.len != aos.length())
				throw new Error("Size  mismatch in type Boolean");
			if (popBoolean() != true)
				throw new Error("Value mismatch in type Boolean");
			
			// TODO: test String
			
			dos.close();
			dis.close();
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new Error("DHHH");
		}
		
		discard(length());
		checked = true;
	}
	
	private boolean equalBytes(byte[] a1, int a1_off, byte[] a2, int a2_off, int len) {
		int i;
		for (i = 0; i < len; i++)
			if (a1[a1_off + i] != a2[a2_off + i])
				return false;
		return true;
	}
	
	protected void finalize() throws Throwable {
		super.finalize();
		if(size>0)
			discardAll();
	}
	
} // end of class Message

