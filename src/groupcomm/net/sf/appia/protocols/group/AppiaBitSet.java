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

import net.sf.appia.core.message.Message;
import net.sf.appia.core.message.MsgBuffer;

/*
 * AppiaBitSet.java
 *
 * Created on March 19, 2002, 4:03 PM
 */

/**
 *
 * @author Alexandre Pinto
 */
public class AppiaBitSet {
  
  private byte[] bits;
  private int len;
  private int off;
  private int size;
  
  private static byte[] BITS={(byte)0x80,(byte)0x40,(byte)0x20,(byte)0x10,(byte)0x08,(byte)0x04,(byte)0x02,(byte)0x01};
  
  /** Creates new AppiaBitSet */
  public AppiaBitSet(int size) {
    bits=new byte[sizeBytesForBits(size)];
    off=0;
    len=bits.length;
    this.size=size;
  }
  
  public AppiaBitSet(byte[] bits, int off, int len, int size) {
    if (len < sizeBytesForBits(size))
      throw new IllegalArgumentException("\"len\" bytes not sufficient for \"size\" bits");
    
    this.bits=bits;
    this.off=off;
    this.len=len;
    this.size=size;
  }
  
  public void setBits(byte[] bits, int off, int len, int size) {
    this.bits=bits;
    this.off=off;
    this.len=len;
    this.size=size;
  }

  public static final int PUSH=1;
  public static final int POP=2;
  public static final int PEEK=3;
  private MsgBuffer mbuf=new MsgBuffer();
  
  public void setBitsFromMessage(Message msg, int operation, int size) {
    mbuf.len=sizeBytesForBits(size);
    if (operation == PUSH)
      msg.push(mbuf);
    else if (operation == POP)
      msg.pop(mbuf);
    else if (operation == PEEK)
      msg.peek(mbuf);
    else
      throw new IllegalArgumentException("unknown operation");
    
    this.bits=mbuf.data;
    this.off=mbuf.off;
    this.len=mbuf.len;
    this.size=size;
  }
  
  public void zero() {
    int i;
    for (i=0 ; i < len ; i++) {
      bits[off+i]=0;
    }
  }
  
  public void set(int bit) {
    if ((bit < 0) || (bit >= size))
      throw new IllegalArgumentException("\"bit\"="+bit);
    bits[off+(bit / 8)] |= BITS[bit % 8];
  }
  
  public void clear(int bit) {
    if ((bit < 0) || (bit >= size))
      throw new IllegalArgumentException("\"bit\"="+bit);
    bits[off+(bit / 8)] &= (BITS[bit % 8] ^ ((byte)0xff));
  }
  
  public boolean get(int bit) {
    if ((bit < 0) || (bit >= size))
      throw new IllegalArgumentException("\"bit\"="+bit);
    return (bits[off+(bit / 8)] & BITS[bit % 8]) != 0;
  }
  
  public void toBooleanArray(boolean[] ba) {
    if (ba.length > size)
      throw new IllegalArgumentException("array is greater than \"size\"");
    int k,j=0,i=off;
    for (k=0 ; k < ba.length ; k++) {
      if (j == 8) {
        i++;
        j=0;
      }
      
      ba[k]= (bits[i] & BITS[j]) != 0;
      j++;
    }
  }
  
  public void fromBooleanArray(boolean[] ba) {
    if (ba.length > size)
      throw new IllegalArgumentException("array is greater than \"size\"");
    int k,j=0,i=off;
    for (k=0 ; k < ba.length ; k++) {
      if (j == 8) {
        i++;
        j=0;
      }
      
      if (ba[k])
        bits[i] |= BITS[j];
      else
        bits[i] &= (BITS[j] ^ ((byte)0xff));
      j++;
    }
  }
    
  public void copyBitsTo(byte[] bits, int off, int len) {
    if (this.len != len)
      throw new IllegalArgumentException("lengths are different");
    System.arraycopy(this.bits,this.off,bits,off,len);
  }
  
  public int getSize() {
    return size;
  }
  
/* TODO
  public void and(AppiaBitSet abs) {
  }
 
  public void or(AppiaBitSet abs) {
  }
 
  public void xor(AppiaBitSet abs) {
  }
 */
  
  public String toString() {
    String s="";
    int i;
    for (i=0 ; i < len ; i++)
      s+=Integer.toHexString(bits[off+i] & 0xff)+".";
    return s;
  }
  
  public static int sizeBytesForBits(int numbits) {
    return numbits/8+(((numbits % 8) > 0) ? 1 : 0);
  }
  
}
