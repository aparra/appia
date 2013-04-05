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
 * CostumSSLServerSocketFactory.java
 *
 * Created on 9 de Outubro de 2003, 14:21
 */

package net.sf.appia.protocols.sslcomplete;

import javax.net.ssl.*;
/**
 *
 * @author  Alexandre Pinto
 */

public class CustomSSLServerSocketFactory extends SSLServerSocketFactory {
  
  private SSLServerSocketFactory sunFactory;
  private String[] anon_cipher_suites=null;
  private boolean anon=false;
  
  public CustomSSLServerSocketFactory(SSLContext ctx, boolean anon) {
    this.anon=anon;
    sunFactory=ctx.getServerSocketFactory();
    
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
  
  public java.net.ServerSocket createServerSocket() throws java.io.IOException {
    SSLServerSocket s=(SSLServerSocket)sunFactory.createServerSocket();
    if (anon) {
      s.setEnabledCipherSuites(anon_cipher_suites);
      s.setNeedClientAuth(false);
    } else {
      s.setNeedClientAuth(true);
    }
    return s;
  }
  
  public java.net.ServerSocket createServerSocket(int param) throws java.io.IOException {
    SSLServerSocket s=(SSLServerSocket)sunFactory.createServerSocket(param);
    if (anon) {
      s.setEnabledCipherSuites(anon_cipher_suites);
      s.setNeedClientAuth(false);
    } else {
      s.setNeedClientAuth(true);
    }
    return s;
  }
  
  public java.net.ServerSocket createServerSocket(int param, int param1) throws java.io.IOException {
    SSLServerSocket s=(SSLServerSocket)sunFactory.createServerSocket(param,param1);
    if (anon) {
      s.setEnabledCipherSuites(anon_cipher_suites);
      s.setNeedClientAuth(false);
    } else {
      s.setNeedClientAuth(true);
    }
    return s;
  }
  
  public java.net.ServerSocket createServerSocket(int param, int param1, java.net.InetAddress inetAddress) throws java.io.IOException {
    SSLServerSocket s=(SSLServerSocket)sunFactory.createServerSocket(param,param1,inetAddress);
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
