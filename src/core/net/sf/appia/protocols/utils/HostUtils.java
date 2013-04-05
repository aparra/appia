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
 package net.sf.appia.protocols.utils;

import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Class containing methods useful for Host information retrieval.
 *  
 * @author Alexandre Pinto
 */
public final class HostUtils {
	
	private HostUtils() {}
  
  /**
   * The preferred local address.
   * <br>
   * It chooses from the existing local address the one that satisfies more of the following rules, 
   * applying the rules by the order presented:
   * <nl>
   * <li> Is IPv4 address;
   * <li> Isn't a Local Link address;
   * <li> Isn't a Loopback address;
   * <li> Isn't a Site Local address;
   * <li> Has the lower interface number;
   * <li> Has the lower interface name.
   * </nl>
   * 
   * @return The local address to use.
   */
  public static InetAddress getLocalAddress() {
    NetworkInterface result_intf=null, intf;
    InetAddress result_addr=null, addr;
    Enumeration<InetAddress> eips=null;
    Enumeration<NetworkInterface> eintfs = null;
    
    try {
      eintfs=NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e1) {
      e1.printStackTrace();
    }
    while ((eintfs != null) && eintfs.hasMoreElements()) {
      intf=eintfs.nextElement();
      eips=intf.getInetAddresses();
      while (eips.hasMoreElements()) {
        addr=eips.nextElement();
        debug("INTERFACE "+intf.getName()+"("+intf.getDisplayName()+") -> "+addr+"\n\t"+
            "(Link Local ? "+addr.isLinkLocalAddress()+") "+
            "(Loopback ? "+addr.isLoopbackAddress()+") "+
            "(Site Local ? "+addr.isSiteLocalAddress()+") "+
            "(IPV4 ? "+(addr instanceof Inet4Address)+") ");
        
        if (result_addr == null) {
          result_addr=addr;
          result_intf=intf;
          debug(" CHOSEN: result was null");
        } else {
          if (!(result_addr instanceof Inet4Address) && (addr instanceof Inet4Address)) {
            result_addr=addr;
            result_intf=intf;
            debug(" CHOSEN: address is IPv4");          
          } else if ((result_addr instanceof Inet4Address) == (addr instanceof Inet4Address)) {
            if (result_addr.isLinkLocalAddress() && !addr.isLinkLocalAddress()) {
              result_addr=addr;
              result_intf=intf;
              debug(" CHOSEN: address wasn't Link Local");
            } else if (result_addr.isLinkLocalAddress() == addr.isLinkLocalAddress()) {
              if (result_addr.isLoopbackAddress() && !addr.isLoopbackAddress()) {
                result_addr=addr;
                result_intf=intf;
                debug(" CHOSEN: address wasn't Loopback");
              } else if (result_addr.isLoopbackAddress() == addr.isLoopbackAddress())
                if (result_addr.isSiteLocalAddress() && !addr.isSiteLocalAddress()) {
                  result_addr=addr;
                  result_intf=intf;            
                  debug(" CHOSEN: address wasn't Site Local");
                } else if (result_addr.isSiteLocalAddress() == addr.isSiteLocalAddress()) {
                  String s=intf.getName();
                  String rs=result_intf.getName();
                  int snum,rsnum;
                  int i;
                  for (i=0 ; (i < s.length()) && !Character.isDigit(s.charAt(i)) ; i++);
                  if (i < s.length()) {
                  	int begini=i;
                  	for (; i < s.length() && Character.isDigit(s.charAt(i)) ; i++);
                  	snum=Integer.parseInt(s.substring(begini,i));
                  } else {
                  	snum=Integer.MAX_VALUE;
                  }
                  for (i=0 ; (i < rs.length()) && !Character.isDigit(rs.charAt(i)) ; i++);
                  if (i < rs.length()) {
                  	int begini=i;
                  	for (; i < rs.length() && Character.isDigit(rs.charAt(i)) ; i++);
                  	rsnum=Integer.parseInt(rs.substring(begini,i));
                  } else {
                  	rsnum=Integer.MAX_VALUE;
                  }
                  
                  if (snum < rsnum) {
                    result_addr=addr;
                    result_intf=intf;
                    debug(" CHOSEN: interface number was lower");
                  } else if ((snum == rsnum) && 
                  		((s.length() < rs.length()) || (s.compareTo(rs) < 0))) {
                    result_addr=addr;
                    result_intf=intf;
                    debug(" CHOSEN: interface name was lower");            
                  }
                }
            }
          }
        }
      }
    }
    
    if (result_addr==null) {
      try {
          result_addr = InetAddress.getByName(null);
//        result_addr=InetAddress.getLocalHost();
      } catch (UnknownHostException e) {
        e.printStackTrace();
      }
    }
    
    debug("LOCAL ADDRESS -> "+result_addr);
    return result_addr;
  }
  
  /**
   * Checks if the given address is one of the addresses of the current machine.
   * 
   * @param address The address to check.
   * @return Is it one of the machine addresses.
   */
  public static boolean isLocalAddress(InetAddress address) {
    NetworkInterface intf;
    InetAddress addr;
    Enumeration<InetAddress> eips=null;
    Enumeration<NetworkInterface> eintfs = null;
    
    try {
      eintfs=NetworkInterface.getNetworkInterfaces();
    } catch (SocketException e1) {
      e1.printStackTrace();
    }
    while ((eintfs != null) && eintfs.hasMoreElements()) {
      intf=eintfs.nextElement();
      eips=intf.getInetAddresses();
      while (eips.hasMoreElements()) {
        addr=eips.nextElement();
        if (address.equals(addr))
        	return true;
      }
    }
    return false;
  }
  
  public static final PrintStream debug=null;
  
  private static void debug(String s) {
    if (debug != null) {
      debug.println("[HostUtils.getLocalAddress()] :"+s);
    }
  }
  
  public static void main(String[] args) {
    System.out.println("\nHostUtils.getLocalAddress()="+getLocalAddress());
  }
}
