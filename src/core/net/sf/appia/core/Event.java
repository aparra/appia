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

/*
//Change log:
//Nuno Carvalho - changed this to support flow control
//				 on channels of events. The asyncGo method
//				is now blocking.
//(28-Feb-2003)
 * Nuno Carvalho on 27/03/2003
 * Added some code to detach memory of sendable events
 * attached to a memory manager. This is used for flow control on Appia Channels.
 */

import java.io.PrintStream;
import java.security.InvalidParameterException;

import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.core.events.channel.ChannelEvent;
import net.sf.appia.core.memoryManager.MemoryManager;


/**
 * The class <i>Event</i> is the superclass of all events manipulated by
 * <i>Appia</i>.
 * <br>
 * The class also performs all the basic functionality, namely that associated
 * with moving through the <i>protocol stack</i>.
 *
 * @author Alexandre Pinto
 * @see net.sf.appia.core.EventScheduler
 */
public class Event {
	
	protected static final int MAX_PRIORITY=255;
	protected static final int DEFAULT_PRIORITY=127;
	protected static final int MIN_PRIORITY=0;
  
  private int currentSession = -1;
  private int firstSession = -1;
  private Channel channel=null;
  private EventScheduler eventScheduler=null;
  private Session src;
  
  private int dir=0;
  private Session[] route=null;
  
  private boolean isInitiated = false;
  private boolean sourceSet = false;
  
  private Thread appiaThread=null;
  
  private int priority = DEFAULT_PRIORITY;
  
  /**
   * Create an uninitialized <i>Event</i>.
   * <br>
   * The event doesnt have information regarding the
   * {@link net.sf.appia.core.Channel Channel} to which it belongs,
   * the {@link net.sf.appia.core.Direction Direction} it will take, or the
   * {@link net.sf.appia.core.Session Session} that generated it.
   * <br>
   * All these attributes must be set, before the event is
   * {@link net.sf.appia.core.Event#init initialized}.
   */
  public Event() {
  }
 
  /**
   * Create an initialized <i>Event</i>.
   * <br>
   * The <i>Event</i> is ready to be sent through the
   * {@link net.sf.appia.core.Channel Channel}.
   *
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @param src the {@link net.sf.appia.core.Session Session} that is generating the Event
   * @throws AppiaEventException as the result of calling
   * {@link net.sf.appia.core.Event#init init()}
   */
  public Event(Channel channel, int dir, Session src)
  throws AppiaEventException {
    
    this.channel = channel;
    this.dir=dir;
    this.src = src;
    sourceSet=true;
    
    init();
  }
  
  /**
   * Set the direction the Event will take through the
   * {@link net.sf.appia.core.Channel Channel}.
   *
   * @param dir the direction of the Event
   */
  public void setDir(int dir) {
    this.dir=dir;
    isInitiated = false;
  }
  
  /**
   * Get the direction the Event will take through the
   * {@link net.sf.appia.core.Channel Channel}.
   *
   * @return the direction of the Event
   */
  public int getDir() {
    return dir;
  }
  
  /**
   * Sets the channel of the event.
   * @param channel
   */
  public void setChannel(Channel channel) {
    this.channel = channel;
    isInitiated = false;
  }
  
  /**
   * Gets the channel of the event.
   */
  public Channel getChannel() {
    return channel;
  }
  
  /**
   * Sets the source Session if the event.
   * The source session is the session that created the event.
   * @param src The source Session.
   */
  public void setSourceSession(Session src) {
    this.src = src;
    sourceSet=true;
    isInitiated = false;
  }
  
  /**
   * Gets the source Session if the event.
   * The source session is the session that created the event.
   */
  public Session getSourceSession() {
    return src;
  }

  /**
   * Sets the source Session if the event.
   * The source session is the session that created the event.
   * @param src The source Session.
   */
  @Deprecated
  public void setSource(Session src) {
      setSourceSession(src);
  }
  
  /**
   * Gets the source Session if the event.
   * The source session is the session that created the event.
   */
  @Deprecated
  public Session getSource() {
    return getSourceSession();
  }

  /**
   * Returns true if the current session accepts the Event.
   * Currently it always returns true.
   */
  public final boolean isAccepted() {
    return true;
  }
  
  /**
   * Returns the next Session to which the event will be delivered.
   * @return The next Session
   */
  public final Session popSession() {
    // first session
    if (currentSession < 0)
      currentSession = firstSession;
    else {
      //if event direction is down and there are no more sessions, then next is
      // the channel itself
      if ((currentSession == 0)
      && (dir == Direction.DOWN))
        currentSession = route.length;
      else {
        //if is still in the route continue
        if (currentSession < route.length)
          currentSession += dir;
        else {
          //if is in the channel itself
          if (currentSession == route.length)
            currentSession = Integer.MAX_VALUE;
        }
      }
    }
    
    if ((currentSession >= 0) && (currentSession < route.length))
      return route[currentSession];
    else {
      if ((currentSession == route.length) && (this instanceof ChannelEvent))
        channel.handle(this);
      // added by Nuno on 7/03/2003
      /* if this event ended the route and is a SendableEvent, we can detach the memory 
       * TODO: maybe this is not needed, but we have to ensure that all sessions 
       * invoke Message.discardAll() when they are discarding a message with contents,
       * in order to free all resources from the memory manager.
       */
      if ((currentSession == route.length)	&& (this instanceof SendableEvent)) {
        ((SendableEvent) this).detachFromMemory();
      }
      return null;
    }
  }
  
  /**
   * Gets the current session of the event.
   * @return Session
   */
  public final Session currentSession() {
    if ((currentSession >= 0) && (currentSession < route.length))
      return route[currentSession];
    else
      return null;
  }
  
  /**
   * Sends the event to the next Layer. If the next layer doesn't exist,
   * the event is discarded.
   * <br>
   * In a protocol Session, after the event is handled, this method should
   * allways be called, unless you have a reason to stop it. This is important
   * for modularity purposes.
   * @throws AppiaEventException
   */
  public final void go() throws AppiaEventException {
    if (!isInitiated)
      throw new AppiaEventException(
      AppiaEventException.NOTINITIALIZED,
      "Event not initialized");
    
    // TODO: does this have any effect on performance ???
    // If so it can be commented.
    if (appiaThread == null)
      appiaThread=eventScheduler.getAppiaInstance().instanceGetAppiaThread();
    if (Thread.currentThread() != appiaThread)
      throw new AppiaEventException(AppiaEventException.WRONGTHREAD,"Method \"go\" called from outside the Appia thread");
    
    eventScheduler.insert(this);
  }
  
  /**
   * Initializes the event. This should be done before sending the
   * event to the next Layer.
   * @throws AppiaEventException
   */
  public final void init() throws AppiaEventException {
    if (channel == null)
      throw new AppiaEventException(
      AppiaEventException.ATTRIBUTEMISSING,
      "Missing Event attribute: Channel");
    
    if ((dir != Direction.UP) && (dir != Direction.DOWN))
      throw new AppiaEventException(
      AppiaEventException.ATTRIBUTEMISSING,
      "Missing, or Incorrect, Event attribute: Direction");
    
    //if (!(this instanceof appia.events.channel.AsyncEvent) && (src==null))
    if (!sourceSet)
      throw new AppiaEventException(AppiaEventException.ATTRIBUTEMISSING,"Missing Event attribute: Source");
    
    final ChannelEventRoute channelRoute = channel.getEventRoute(this);
    route = channelRoute.getRoute();
    
    firstSession = channel.getFirstSession(channelRoute, dir, src);
    currentSession = -1;
    
    eventScheduler = channel.getEventScheduler();
    appiaThread=eventScheduler.getAppiaInstance().instanceGetAppiaThread();
    
    isInitiated = true;
  }
  
  public void debug(PrintStream out) {
    out.println("Event:");
    out.println(this);
  }
  
  /**
   * Clones the Event.
   * @return Event
   * @throws CloneNotSupportedException
   */
  public Event cloneEvent() throws CloneNotSupportedException {
    final Event e = (Event) clone();
    
    /*
     e.firstSession=-1;
     e.currentSession=-1;
     */
    e.isInitiated = false;
    e.src = null;
    e.sourceSet = false;
    
    return e;
  }
  
  /**
   * Inserts the Event in the Channel <i>asynchronously</i>.
   * This method should be used if the Event is to be inserted in the Channel
   * in response to some asynchronous ocurrence, for example the arrival of
   * a network message.
   * <br>
   * <b><u>NOTE</u>: the Event must not be <i>initialized</i>. Any Channel,
   * Direction or source already set are REMOVED.</b>
   * <br>
   * The Channel and Direction of the Event are given as parameters. The
   * <i>source</i> of the Event is the Channel itself. Therefore if the
   * Direction is UP the the Event will start at the bottom of the stack.
   * Otherwise if the Direction is DOWN the Event will start at the top of the
   * stack.
   * <br>
   * <b><u>IMPORTANT</u>:</b> This call could block, if the channel is full
   * of events and a memory manager is being used. This method cannot be
   * called from the Appia thread it self.
   *
   * @param channel the {@link net.sf.appia.core.Channel Channel} of the Event
   * @param dir the {@link net.sf.appia.core.Direction Direction} of the Event
   * @throws AppiaEventException
   * @see net.sf.appia.core.memoryManager.MemoryManager
   */
  public final void asyncGo(Channel channel, int dir)
  throws AppiaEventException {
    
    this.channel=channel;
    this.dir=dir;
    src=null;
    sourceSet=true;
    
    init();
    
    if (Thread.currentThread() == appiaThread)
      throw new AppiaEventException(AppiaEventException.WRONGTHREAD,"Method \"asyncGo\" called from within Appia thread");
    
    // blocks if there are too many events in the channel from the application
    // This is used only if there is a memory manager in the channel
    final MemoryManager mm = channel.getMemoryManager();
    if (AppiaConfig.QUOTA_ON && mm != null) {
    	try {
			mm.synchronizedAboveThreshold(this.dir);
		} catch (InterruptedException e) {
			throw new AppiaEventException("Unable to insert event asynchronously",e);
		}
    }
    
    // insert this event in the channel
    // if the channel was not initialized, it  waits
    channel.insertEvent(this);
  }
  
  private Object schedulerData=null;

  /**
   * Method used by the {@linkplain EventScheduler} to associate some scheduler data to this Event. 
   * This information may be used by the scheduler for its internal agorithms.

   * @param schedulerData The EventScheduler data to set.
   */
  protected void setSchedulerData(Object schedulerData) {
    this.schedulerData = schedulerData;
  }

  /**
   * Method used by the {@linkplain EventScheduler} to obtain the data it has previously placed in this Event. 
   * This information may be used by the scheduler for its internal agorithms.
   * 
   * @return Returns the EventScheduler data.
   */
  protected Object getSchedulerData() {
    return schedulerData;
  }

  public int getPriority() {
	  return priority;
  }
  
  public void setPriority(int priority) throws InvalidParameterException {
	  if(priority < MIN_PRIORITY || priority > MAX_PRIORITY)
		  throw new InvalidParameterException("Priority of event must be defined between "+MIN_PRIORITY+" and "+MAX_PRIORITY);
	  this.priority = priority;
  }
  
}
