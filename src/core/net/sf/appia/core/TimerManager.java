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

import java.util.concurrent.ThreadFactory;

import net.sf.appia.core.events.channel.ChannelEvent;
import net.sf.appia.core.events.channel.PeriodicTimer;
import net.sf.appia.core.events.channel.Timer;


/** <I>Appia</I> timers manager.
 * This is an independent <I>thread</I> that gathers all timers
 * requested by layers and resends them back when the timer expires.
 *
 * @author Alexandre Pinto
 * @version 1.0
 * @see net.sf.appia.core.events.channel.Timer
 * @see net.sf.appia.core.events.channel.PeriodicTimer
 */
public class TimerManager implements Runnable, TimeProvider {
  
    private static final long MICROS = 1000;
    
    /**
     * This class defines a MyTimer
     * 
     * @version 1.0
     */
  private class MyTimer {
    /**
     * The ID of the timer.
     */    
    public String id;
    public long time;
    public long period;
    public ChannelEvent event;
    public MyTimer next;
    
    public MyTimer(String timerID, long time, long period, ChannelEvent event) {
      this.id=timerID;
      this.time=time;
      this.period=period;
      this.event=event;
    }
  }
  
  private boolean alive=false;
  private Thread thread;
  private MyTimer next=null;
  
  /**
   * This class defines a MyClock
   * 
   * @version 1.0
   */
  private class MyClock {
    public final long syncTime=1000;
    
    private long lastSync=0;
    private long now=0;
    
    public MyClock() {
      sync();
    }
    
    public synchronized void sync() {
      now=currentTimeMillis();
      lastSync=now;
    }
    
    public synchronized void update(long elapsed) {
      now+=elapsed;
    }
    
    public synchronized long read() {
      return now;
    }
    
    public synchronized void conditionalSync() {
      if (now > lastSync + syncTime) {
        now=currentTimeMillis();
        lastSync=now;
      }
    }
  }
  
  private MyClock clock=new MyClock();
  
  private synchronized void insert(MyTimer timer) {
    MyTimer prev=null;
    MyTimer t=next;
    
    while ( (t != null) && (t.time <= timer.time) ) {
      prev=t;
      t=t.next;
    }
    
    if (prev == null) {
      timer.next=next;
      next=timer;
    } else {
      timer.next=t;
      prev.next=timer;
    }
  }
  
  private synchronized void remove(String timerID) {
    MyTimer prev=null;
    MyTimer t=next;
    
    while ( (t != null) && (!t.id.equals(timerID)) ) {
      prev=t;
      t=t.next;
    }
    
    if (t != null) {
      if (prev == null) {
        next=t.next;
      } else {
        prev.next=t.next;
      }
    }
  }
  
  private synchronized MyTimer getNextTimer(long now) {
    if ( (next != null) && (next.time <= now) ) {
      final MyTimer t=next;
      next=next.next;
      return t;
    } else
      return null;
  }
  
  private synchronized void goToSleep(long now) {
    long sleep;
    if ( next != null ) {
      sleep=next.time-now;
    } else {
      sleep=clock.syncTime;
    }
    
    if ( sleep > 0 ) {
      try {
        this.wait(sleep);
        clock.update(sleep);
      } catch (InterruptedException e) {
        clock.sync();
      }
    }
  }
  
  private synchronized void setAlive(boolean alive) {
    this.alive=alive;
  }
  
  private synchronized boolean isAlive() {
    return alive;
  }
  
  
  /** Creates a new TimerManager
   */  
  public TimerManager(ThreadFactory thf) {
    thread = thf.newThread(this);
    thread.setName("Appia Timer Manager");
    thread.setDaemon(true);
  }
  
  //////////////////////////////////////////////
  
  /** Returns the time interval until the next timer expires.
   * In milliseconds.
   * @return The time until the next timer. In milliseconds.
   */  
  public long nextTimerEvent() { return -1; }
  
  /** Receives a timer to manage.
   * @param timer The timer to manage.
   * @see net.sf.appia.core.events.channel.Timer
   */  
  public void handleTimerRequest(Timer timer) {
    final int q=timer.getQualifierMode();
    
    if ( q != EventQualifier.NOTIFY ) {
      
      if ( q == EventQualifier.ON ) {
        insert(new MyTimer(timer.timerID,currentTimeMillis()+timer.getTimeout(),0,timer));
      } else {
        remove(timer.timerID);
      }
      
      thread.interrupt();
    }
  }
  
  /**
   * Processes the next timer to expire.<BR>
   * <B>Currently it does nothing.</B>
   */  
  public void consumeTimerEvent() {}
  
  /** Receives a periodic timer to manage.
   * @param timer The periodic timer to manage.
   * @see net.sf.appia.core.events.channel.PeriodicTimer
   */  
  public void handlePeriodicTimer(PeriodicTimer timer) {
    final int q=timer.getQualifierMode();
    
    if ( q != EventQualifier.NOTIFY ) {
      
      if ( q == EventQualifier.ON ) {
        final long period=timer.getPeriod();
        clock.sync();
        insert(new MyTimer(timer.timerID,currentTimeMillis()+period,period,timer));
      } else {
        remove(timer.timerID);
      }
      
      thread.interrupt();
    }
  }
  
  /** Start execution of the manager thread.
   * @see java.lang.Thread#start
   */  
  public void start() {
    setAlive(true);
    thread.start();
  }
  
  /** Stops execution of the manager thread.
   * @see java.lang.Thread#stop
   */  
  public void stop() {
    setAlive(false);
    thread.interrupt();
  }
  
  /**
   * Current time in milliseconds. <br>
   * Uses {@linkplain System#currentTimeMillis()}.
   * 
   * @see System#currentTimeMillis()
   */
  public long currentTimeMillis(){
	  	return System.currentTimeMillis();
	}

  /**
   * Current time in microseconds. <br>
   * Uses {@linkplain System#currentTimeMillis()} therefore only has a millisecond precision.
   */
  public long currentTimeMicros() {
  	return System.nanoTime()/MICROS;
  }

  /**
   * Current time in nanoseconds. <br>
   * Uses {@linkplain System#currentTimeMillis()} therefore only has a millisecond precision.
   */
  public long nanoTime() {
  	return System.nanoTime();
  }

  ////////////////////////////////////////////
  
  /** The code executed by the manager thread.
   * @see java.lang.Thread#run
   */  
  public void run() {
    
    while (isAlive()) {
      clock.conditionalSync();
      
      final long now=clock.read();
      MyTimer timer;
      
      while ( (timer=getNextTimer(now)) != null ) {
        
        ChannelEvent event;
        
        try {
          if (timer.period > 0) {
            event=(PeriodicTimer)timer.event.cloneEvent();
          } else {
            event=timer.event;
          }
          
          event.setQualifierMode(EventQualifier.NOTIFY);
          event.asyncGo(event.getChannel(),Direction.invert(event.getDir()));
          
          if (timer.period > 0) {
            timer.time=now+timer.period;
            insert(timer);
          }
        }
        catch (AppiaEventException e) {
          //e.printStackTrace();
        }
        catch (CloneNotSupportedException e) {
          throw new AppiaError("TimerManager: CloneNotSupportedException ");
        }
        
      }
      
      goToSleep(now);
    }
  }

}