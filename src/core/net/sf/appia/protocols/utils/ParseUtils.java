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
 * Created on Jun 14, 2005
 */
package net.sf.appia.protocols.utils;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.text.ParseException;

/**
 * Class containing some methods useful for parsing strings.
 * 
 * @author Alexandre Pinto
 */
public final class ParseUtils {
  
  private ParseUtils() {
    super();
  }
  
  /** 
   * Generate a InetSocketAddress[] from a string of the form "[host][:port][,[host][:port] ...]".
   * 
   * @param s
   * @param defaultHost
   * @param defaultPort
   * @return an array with addresses
   * @throws ParseException
   * @throws UnknownHostException
   */
  public static InetSocketAddress[] parseSocketAddressArray(String s, InetAddress defaultHost, int defaultPort) 
  throws ParseException, UnknownHostException {
    //System.err.println("##### Parse string: "+s);
    
    final char separator=',';
    int isep=-1;
    int previsep;
    int j;

    int count=1;
    while (isep < s.length()) {
      isep=s.indexOf(separator,isep+1);
      if (isep < 0)
        break;
      count++;
    }
    final InetSocketAddress[] result=new InetSocketAddress[count];
 
    //System.err.println("##### Parsed array("+result.length+"):{");

    j=0;
    isep=-1;
    while (isep < s.length()) {
      previsep=isep;
      isep=s.indexOf(separator,previsep+1);
      if (isep < 0)
        isep=s.length();
      
      if (isep > previsep+1) {
        try {
          //System.err.print("##### \""+s.substring(previsep+1,isep)+"\"");
          result[j++]=parseSocketAddress(s.substring(previsep+1,isep),defaultHost,defaultPort);
          //System.err.println(" -> "+result[j-1]);
        } catch (ParseException ex) {
          throw new ParseException(ex.getMessage(),previsep+1+ex.getErrorOffset());
        }
      } else {
        throw new ParseException("Missing element in array.",previsep+1);
      }
    }
    
    return result;
  }


  /**
   * Generate a InetSocketAddress from a string of the form "[host][:port]".
   *  
   * @param s the string containing the addresses.
   * @param defaultHost Host to use if the string doesn't contain a host part. If <b>null</b> string must contain a host part.
   * @param defaultPort Port to use if the string doesn't contain a port part. If <b>-1</b> string must contain a host part.
   * @return a parsed address
   * @throws ParseException
   * @throws UnknownHostException
   */
  public static InetSocketAddress parseSocketAddress(String s, InetAddress defaultHost, int defaultPort) 
  throws ParseException, UnknownHostException {
      InetSocketAddress addr = null;
    final int iport=s.indexOf(':');
    if (iport < 0) {
      if (defaultPort < 0)
        throw new ParseException("Missing port in \""+s+"\"",0);
      addr=new InetSocketAddress(InetAddress.getByName(s),defaultPort);
    } else if (iport == 0) {
      if (defaultHost == null)
        throw new ParseException("Missing host in \""+s+"\"",iport);
      addr=new InetSocketAddress(defaultHost,Integer.parseInt(s.substring(1)));
    } else if (iport < s.length()-1) {
        addr=new InetSocketAddress(InetAddress.getByName(s.substring(0,iport)),Integer.parseInt(s.substring(iport+1)));
    } else {
      throw new ParseException("Missing port in \""+s+"\"",iport);
    }
    
    return addr;
  }

  /**
   * converts an int to an array of bytes.
   * @param i the int to convert.
   */
  public static byte[] intToByteArray(int i) {
    byte[] ret = new byte[4];
    
    ret[0] = (byte) ((i & 0xff000000) >> 24);
    ret[1] = (byte) ((i & 0x00ff0000) >> 16);
    ret[2] = (byte) ((i & 0x0000ff00) >> 8);
    ret[3] = (byte) (i & 0x000000ff);
    
    return ret;
  }

  /**
   * Convert an int to a byte array and put the bytes in the given array.
   * @param i the int to convert.
   * @param a the byte array where to place the converted bytes
   * @param o the offset.
   */
  public static void intToByteArray(int i, byte[] a, int o) {
    a[o + 0] = (byte) ((i & 0xff000000) >> 24);
    a[o + 1] = (byte) ((i & 0x00ff0000) >> 16);
    a[o + 2] = (byte) ((i & 0x0000ff00) >> 8);
    a[o + 3] = (byte) (i & 0x000000ff);
  }
  
  /**
   * int deserialization.
   */
  public static int byteArrayToInt(byte[] b, int off) {
    int ret = 0;
    
    ret |= b[off] << 24;
    ret |= (b[off + 1] << 24) >>> 8; // must be done this way because of
    ret |= (b[off + 2] << 24) >>> 16; // java's sign extension of <<
    ret |= (b[off + 3] << 24) >>> 24;
    
    return ret;
  }
  
}

