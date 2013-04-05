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
 package net.sf.appia.test.appl;

import net.sf.appia.core.Direction;

/*
 * Change Log:
 * Nuno Carvalho: 7/Aug/2002 - Alterei o protocolo para nao usar
 *                        codigo deprecated (asynEvent)
 */

class ApplReader implements Runnable {

  private ApplSession         parentSession;
  private java.io.BufferedReader keyb;
  private String                 local=null;

  public ApplReader(ApplSession parentSession) {
    super();
	  this.parentSession=parentSession;
	  keyb=new java.io.BufferedReader(new java.io.InputStreamReader(System.in));
  }

  public void run() {
    while(true) {
      try {
        local=keyb.readLine();
		ApplAsyncEvent asyn = new ApplAsyncEvent(local);
		asyn.asyncGo(parentSession.channel,Direction.DOWN);
      }
	    catch(java.io.IOException e) {}
	    catch(net.sf.appia.core.AppiaEventException e) {}
	  }
  }
}
