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
 package net.sf.appia.protocols.sslcomplete;

import net.sf.appia.core.Session;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.tcpcomplete.TcpCompleteLayer;



/**
 * @author Pedro Vicente, Alexandre Pinto
 *
 */
public class SslCompleteLayer extends TcpCompleteLayer {
  
  public SslCompleteLayer(){
    super();
    int i;
    Class[] aux;
    
    // Adding SslUndelivered to Provide
    aux=evProvide;
    evProvide=new Class[aux.length+1];
    System.arraycopy(aux, 0, evProvide, 0, aux.length);
    evProvide[aux.length]=SslUndeliveredEvent.class;
    
    // Adding SslRegisterSocketEvent to evAccept
    aux=evAccept;
    evAccept=new Class[aux.length+1];
    System.arraycopy(aux, 0, evAccept, 0, aux.length);
    evAccept[aux.length]=SslRegisterSocketEvent.class;
  }
  
  /**
   * @see net.sf.appia.core.Layer#createSession()
   */
  public Session createSession() {
    return new SslCompleteSession(this);
  }
  
}
