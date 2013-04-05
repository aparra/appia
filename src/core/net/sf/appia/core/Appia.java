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
 
package net.sf.appia.core;

//Change log:
//Nuno Carvalho - changed this to support flow control
//				 on channels
//(28-Feb-2003)

import java.util.Vector;
import java.util.concurrent.ThreadFactory;

import net.sf.appia.core.events.channel.ExternalEvent;
import net.sf.appia.protocols.common.AppiaThreadFactory;


/**
 * <i>Appia</i> main class.
 * <br>
 * This class has the <i>static</i> methods responsible for the execution of all <i>Appia</i>s
 * functionality.
 * <br>
 * It is this class that calls the
 * {@link net.sf.appia.core.EventScheduler EventSchedulers}
 * and therefore makes the entire <i>Appia</i> system run.
 * <br>
 * It also keeps the current {@link net.sf.appia.core.TimerManager TimerManager}.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.EventScheduler
 */
public class Appia {
  
  protected Vector<EventScheduler> eventSchedulers=new Vector<EventScheduler>();
  protected TimerManager timerManager=null;
  protected Thread thread = null;
  protected int nEvents=0;
  private ThreadFactory threadFactory;  
  private boolean running = true;
  private String managementMBeanID;
  
  /**
   * Default constructor.
   * <br>
   * It creates, and starts, the default {@link net.sf.appia.core.TimerManager TimerManager}.
   */
  public Appia() {
      threadFactory = new AppiaThreadFactory();
      timerManager=new TimerManager(threadFactory);
  }

  /**
   * Default constructor.
   * <br>
   * It creates, and starts, the default {@link net.sf.appia.core.TimerManager TimerManager}.
   */
  public Appia(ThreadFactory thf) {
      threadFactory = thf;
      timerManager=new TimerManager(threadFactory);
  }
  
  public TimerManager instanceGetTimerManager() {
    return timerManager;
  }
  
  public void instanceSetTimerManager(TimerManager timerManager) {
    if (this.timerManager!=null) {
      this.timerManager.stop();
    }
    
    this.timerManager=timerManager;
    this.timerManager.start();
  }

  public void instanceInsertEventScheduler(EventScheduler eventScheduler) {
    if ( ! eventSchedulers.contains(eventScheduler) )
      eventSchedulers.addElement(eventScheduler);
  }

  public void instanceRemoveEventScheduler(EventScheduler eventScheduler) {
    eventSchedulers.removeElement(eventScheduler);
  }

  public void instanceInsertListenRequest(ExternalEvent descriptor) {}
  
  public void instanceRemoveListenRequest(ExternalEvent descriptor) {}

  public synchronized void instanceInsertedEvent() {
      notify();
      nEvents++;
  }
  
  public Thread instanceGetAppiaThread() {
    return thread;
  }

  public void instanceRun() {
    // Starting associated TimerManager
    timerManager.start();

    //some final initializations
    int i;
    thread = Thread.currentThread();
    for (i=0 ; i < eventSchedulers.size() ; i++) {
      final EventScheduler es=eventSchedulers.elementAt(i);
      es.start();
    }
    
    i=0;
    boolean consumedEvent;
    EventScheduler es;
    
    while (true) {	
      try {
        es=eventSchedulers.elementAt(i);
      } catch (ArrayIndexOutOfBoundsException e) {
        es=null;
      }
      
      if ( es != null )
        consumedEvent=es.consumeEvent();
      else
        consumedEvent=false;
      
      synchronized (this) {
    	  if ( consumedEvent )
    		  nEvents--;
    	  
    	  while ( running && nEvents == 0 ) {
    		  try {
    			  wait();
    		  } catch (InterruptedException e) {}
    	  }        
        
    	if(!running)
    		break;    		
      }
      
      i++;
      if ( i >= eventSchedulers.size() ) {
        i=0;
        //so other threads can run
        // Thread.yield();
      }
    }

  }

  public void instanceStop() {
      synchronized (this) {
      	running = false;
      	timerManager.stop();
//      	instanceGetAppiaThread().interrupt();
  	}
  }
  
  /* the instance of Appia! */
  protected static Appia appia=new Appia();

  /**
   * Get the current {@link net.sf.appia.core.TimerManager TimerManager}.
   *
   * @return the current {@link net.sf.appia.core.TimerManager TimerManager}
   */
  public static TimerManager getTimerManager() {
    return appia.timerManager;
  }
  
  /**
   * Set the {@link net.sf.appia.core.TimerManager TimerManager}.
   * <br>
   * The current {@link net.sf.appia.core.TimerManager TimerManager} will be
   * {@link net.sf.appia.core.TimerManager#stop stoped}. The new
   * {@link net.sf.appia.core.TimerManager TimerManager} will be
   * {@link net.sf.appia.core.TimerManager#start started}.
   *
   * @param timerManager the new {@link net.sf.appia.core.TimerManager TimerManager}
   */
  public static void setTimerManager(TimerManager timerManager) {
    appia.instanceSetTimerManager(timerManager);
  }
  
  
  /**
   * Registers a new {@link net.sf.appia.core.EventScheduler EventScheduler}.
   * <br>
   * <b>This method is called by {@link net.sf.appia.core.Channel#start Channel.start()}</b>
   *
   * @param eventScheduler the new {@link net.sf.appia.core.EventScheduler EventScheduler}
   */
  public static void insertEventScheduler(EventScheduler eventScheduler) {
    appia.instanceInsertEventScheduler(eventScheduler);
  }
  
  /**
   * Unregisters a {@link net.sf.appia.core.EventScheduler EventScheduler}.
   *
   * @param eventScheduler the {@link net.sf.appia.core.EventScheduler EventScheduler} to deregister.
   * It does nothing if the given {@link net.sf.appia.core.EventScheduler EventScheduler} is not registered.
   */
  public static void removeEventScheduler(EventScheduler eventScheduler) {
    appia.instanceRemoveEventScheduler(eventScheduler);
  }
  
  /**
   * <b>In the Java version this method does nothing.</b>
   */
  public static void insertListenRequest(ExternalEvent descriptor) {
    appia.instanceInsertListenRequest(descriptor);
  }
  
  /**
   * <b>In the Java version this method does nothing.</b>
   */
  public static void removeListenRequest(ExternalEvent descriptor) {
    appia.instanceRemoveListenRequest(descriptor);
  }
  
  /**
   * Starts <i>Appia</i> operation.
   * <br>
   * This method implements the infinite loop that calls the
   * {@link net.sf.appia.core.EventScheduler#consumeEvent consumeEvent()} method
   * of the registered {@link net.sf.appia.core.EventScheduler EventSchedulers}.
   * <br>
   * It's this method that makes the <i>Appia</i> run.
   */
  public static void run() {
      appia.instanceRun();
  }
  
  /**
   * Method used by any {@link net.sf.appia.core.EventScheduler EventScheduler} to signal
   * that a new {@link net.sf.appia.core.Event Event} has been inserted.
   * <br>
   * The <i>Appia</i> keeps track of how many events exist in all the
   * {@link net.sf.appia.core.EventScheduler EventSchedulers}, and if none exist it simply
   * waits idle.
   */
  public static void insertedEvent() {
    appia.instanceInsertedEvent();
  }
  
  /**
   * Gets the Thread where Appia is running.
   * @return Thread
   */
  public static Thread getAppiaThread() {
      return appia.thread;
  }

  public synchronized ThreadFactory getThreadFactory() {
      return threadFactory;
  }

  public void setThreadFactory(ThreadFactory thf) throws AppiaException {
      if (this.timerManager!=null)
          this.timerManager.stop();
      this.threadFactory = thf;
      this.timerManager=new TimerManager(thf);
      this.timerManager.start();
  }
  
  public void setManagementMBeanID(String id){
      managementMBeanID = id;
  }
  
  public String getManagementMBeanID(){
      return managementMBeanID;
  }

}
