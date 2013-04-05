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

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.Timer;


public class ApplTimer extends Timer {

  public  String msg;
  public  int totalResend, thisResend, period;
  public  int[] dest;

  private ApplSession parent;

  public ApplTimer(Channel c,ApplSession gen,String msg, int totalResend,int period, int[] dest) throws AppiaEventException, AppiaException {
    super(0,"",c,Direction.DOWN, gen, EventQualifier.ON);

	  this.parent=gen;
	  this.msg=msg;
	  this.totalResend=totalResend;
	  this.thisResend=1;
	  this.period=period;
	  this.dest=dest;
  }

  public boolean hasMore() {
    return thisResend!=totalResend;
  }

  public void prepareNext() throws AppiaEventException, AppiaException {
    setDir(Direction.invert(getDir()));
	  setQualifierMode(EventQualifier.ON);
	  setTimeout(period);
	  setSourceSession(parent);
	  thisResend++;
	  init();
  }
}
