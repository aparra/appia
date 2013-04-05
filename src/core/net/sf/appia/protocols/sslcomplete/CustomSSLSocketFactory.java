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
 * CostumSSLSocketFactory.java
 *
 * Created on 9 de Outubro de 2003, 14:21
 */

package net.sf.appia.protocols.sslcomplete;

import javax.net.ssl.*;
/**
 *
 * @author  Alexandre Pinto
 */
public class CustomSSLSocketFactory extends SSLSocketFactory {
  
  private SSLSocketFactory sunFactory;
  private String[] anon_cipher_suites=null;
  private boolean anon=false;
  
  public CustomSSLSocketFactory(SSLContext ctx, boolean anon) {
    this.anon=anon;
    sunFactory=ctx.getSocketFactory();
    
    if (anon) {
      String[] sunSuites=sunFactory.getSupportedCipherSuites();
      String[] aux=new String[sunSuites.length];
      int i,j=0;
      for (i=0 ; i < sunSuites.length ; i++) {
        if (sunSuites[i].indexOf("anon") >= 0)
          aux[j++]=sunSuites[i];
      }
      anon_cipher_suites=new String[j];
      System.arraycopy(aux, 0, anon_cipher_suites,0, j);
    }
  }
  
  public java.net.Socket createSocket() throws java.io.IOException {
    SSLSocket s=(SSLSocket)sunFactory.createSocket();
    if (anon) {
      s.setEnabledCipherSuites(anon_cipher_suites);
      s.setNeedClientAuth(false);
    } else {
      s.setNeedClientAuth(true);
    }
    return s;
  }
  
  public java.net.Socket createSocket(java.net.InetAddress inetAddress, int param) throws java.io.IOException {
    SSLSocket s=(SSLSocket)sunFactory.createSocket(inetAddress,param);
    if (anon) {
      s.setEnabledCipherSuites(anon_cipher_suites);
      s.setNeedClientAuth(false);
    } else {
      s.setNeedClientAuth(true);
    }
    return s;
  }
  
  public java.net.Socket createSocket(String str, int param) throws java.io.IOException, java.net.UnknownHostException {
    SSLSocket s=(SSLSocket)sunFactory.createSocket(str,param);
    if (anon) {
      s.setEnabledCipherSuites(anon_cipher_suites);
      s.setNeedClientAuth(false);
    } else {
      s.setNeedClientAuth(true);
    }
    return s;
  }
  
  public java.net.Socket createSocket(java.net.Socket socket, String str, int param, boolean param3) throws java.io.IOException {
    SSLSocket s=(SSLSocket)sunFactory.createSocket(socket,str,param,param3);
    if (anon) {
      s.setEnabledCipherSuites(anon_cipher_suites);
      s.setNeedClientAuth(false);
    } else {
      s.setNeedClientAuth(true);
    }
    return s;
  }
  
  public java.net.Socket createSocket(java.net.InetAddress inetAddress, int param, java.net.InetAddress inetAddress2, int param3) throws java.io.IOException {
    SSLSocket s=(SSLSocket)sunFactory.createSocket(inetAddress,param,inetAddress2,param3);
    if (anon) {
      s.setEnabledCipherSuites(anon_cipher_suites);
      s.setNeedClientAuth(false);
    } else {
      s.setNeedClientAuth(true);
    }
    return s;
  }
  
  public java.net.Socket createSocket(String str, int param, java.net.InetAddress inetAddress, int param3) throws java.io.IOException, java.net.UnknownHostException {
    SSLSocket s=(SSLSocket)sunFactory.createSocket(str,param,inetAddress,param3);
    if (anon) {
      s.setEnabledCipherSuites(anon_cipher_suites);
      s.setNeedClientAuth(false);
    } else {
      s.setNeedClientAuth(true);
    }
    return s;
  }
  
  public String[] getDefaultCipherSuites() {
    if (anon)
      return anon_cipher_suites;
    else
      return sunFactory.getDefaultCipherSuites();
  }
  
  public String[] getSupportedCipherSuites() {
    return sunFactory.getSupportedCipherSuites();
  }
}

