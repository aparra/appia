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
 package net.sf.appia.protocols.fifo;

//////////////////////////////////////////////////////////////////////
//                                                                  //
// Appia: protocol development and composition framework            //
//                                                                  //
// Version: 1.0/J                                                   //
//                                                                  //
// Copyright, 2000, Universidade de Lisboa                          //
// All rights reserved                                              //
// See license.txt for further information                          //
//                                                                  //
// Class: FIFOConfigEvent: Event for Configuration of FIFO Sessions //
//                                                                  //
// Author: Hugo Miranda, Nuno Carvalho 11/2001                      //
//                                                                  //
// Change Log:                                                      //
//                                                                  //
//////////////////////////////////////////////////////////////////////

import net.sf.appia.core.*;

/**
 * Class FIFOConfigEvent extends Event and is used for the
 * configuration of FIFO sessions. FifoSession is
 * initialized with default values. 
 * The event may redefine all or some of these values.
 *
 * @author Hugo Miranda, Nuno Carvalho
 * @see    FifoLayer
 * @see    Event
 * @see    FifoSession
 */

public class FIFOConfigEvent extends Event {

    private boolean periodDef=false, windowDef=false, numTimersDef=false;
    private boolean nResendsDef = false;
    private int window, timersToResend, nResends;
    private long period;

    /**
     * Constructor initializing basic event attributes. Events created with
     * this constructor do not need to be initialized.
     *
     * @param c The channel where the event will flow
     * @param d The direction the event will flow
     * @param source The session creating the event
     * @see Channel
     * @see Direction
     * @see Session
     */
    public FIFOConfigEvent(Channel c, int d, Session source) throws AppiaEventException {
	/* Calls the main event Constructor Class */
	super(c,d,source);
    }

    /**
     * Defines the minimum number of miliseconds to wait
     * prior to resending a unacknowledged message.
     *
     * @param period The interval (in miliseconds) prior to resending
     * a message
    */
    public void setPeriod(long period) {
       this.period=period;
       this.periodDef=true;
      }

    /**
     * Learns if the period was set
    */
    public boolean isPeriodDef()  {
       return periodDef;
    }

   /**
    * Returns the period. The returned value
    * is valid only if isPeriodDef() returns true
    */
   public long getPeriod() {
     return period;
   }

    /**
     * sets the number of periodic timers for resending 
     * messages
     * @param nTimers number of timers
     */
    public void setTimersToResend(int nTimers) {
	timersToResend = nTimers;
	numTimersDef = true;
    }

    /**
     * checks if the number of timers was set
     */
    public boolean isTimersToResendDef() {
	return numTimersDef;
    }

    /**
     * gets number of timers to resend messages.
     * this is valid only if isTimersToResendDef() returns true
     */
    public int getTimersToResend() {
	return timersToResend;
    }
	
    /**
     * This method just calls setNumResends(). Used for
     * compatibility with other protocols
     * @see #setNumResends(int)
     */
    public void setRetries(int n){
	setNumResends(n);
    }

    /**
     * Defines the number of resends of a not acknoledged message.
     * @param n new number of resends
     */
    public void setNumResends(int n) {
	nResends = n;
	nResendsDef = true;
    }

    /**
     * Learns if the Number of resends was set.
     */
    public boolean isNumResendsDef() {
	return nResendsDef;
    }

    /**
     * Returns the new number of resends.
     * The value is only valid if the isNumResends() method returns true
     * @return number of resends
     */
    public int getNumResends() {
	return nResends;
    }

    /**
     * Defines the maximum number of messages received 
     * out of order kept for any peer.
     *
     * @param window The number of out-of-order messages hold by the session.
     */
    public void setWindow(int window) {
       this.window=window;
       this.windowDef=true;
      }

    /**
     * Learns if the window was set
    */
    public boolean isWindowDef()  {
       return windowDef;
    }

   /**
    * Returns the new window size. The returned value
    * is valid only if isWindowDef() returns true
    */
   public int getWindow() {
     return window;
   }
}
