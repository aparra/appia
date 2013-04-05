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

import java.util.Hashtable;
import java.util.concurrent.ThreadFactory;

import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.events.channel.Debug;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.core.events.channel.PeriodicTimer;
import net.sf.appia.core.events.channel.Timer;
import net.sf.appia.core.memoryManager.MemoryManager;
import net.sf.appia.core.message.DefaultMessageFactory;
import net.sf.appia.core.message.MessageFactory;
import net.sf.appia.management.ManagedSession;
import net.sf.appia.management.SensorSession;
import net.sf.appia.management.jmx.ChannelManager;
import net.sf.appia.management.jmx.JMXConfiguration;
import net.sf.appia.protocols.common.AppiaThreadFactory;

import org.apache.log4j.Logger;



// Change log:
// Nuno Carvalho - added the atribute memoryManager,
//                 a contrutor method for this new atribute and
//                 get & set for the Memorymanager
// 
// (9-Jul-2001)
// Nuno Carvalho - changed this to support flow control
//					on channels of events
// (28-Feb-2003)


/**
 * A <b>Channel</b> is an <i>instance</i> of a {@link net.sf.appia.core.QoS QoS}.
 * <br>
 * It is composed of a stack of {@link net.sf.appia.core.Session Sessions}, and it offers
 * {@link net.sf.appia.core.Event Events} and other functionality.
 * <br>
 * It also serves as access to other <i>Appia</i> functionality.
 *
 * @author Alexandre Pinto
 * 	   Modified by Bruno Simoes & Fernando Vicente & Paulo Sousa (5/2001)
 *	   Modifications:
 *		on getEventRoute(Event e):
 * 			before: only the event received was searched on the
 *				hashtable
 *			now: the event received and all its superclasses are
 *			     searched on the hashtable
 * @version 0.1.1
 * @see net.sf.appia.core.QoS
 */
public class Channel {
  
    private static Logger log = Logger.getLogger(Channel.class);
    
  String channelID;
  private QoS qos;
  private Hashtable eventsRoutes=null;
  
  private EventScheduler eventScheduler;
  private TimerManager timerManager;
  
  private boolean alive=false;
  private boolean started=false;
  
  private MemoryManager memoryManager=null;
  
  private JMXConfiguration jmxConfiguration = null;

  // Factories
  private MessageFactory messageFactory = new DefaultMessageFactory();
  private ThreadFactory threadFactory = new AppiaThreadFactory();
  
  /**
   * The {@link net.sf.appia.core.Session Sessions} stack.
   * <br>
   * <b>It should be READ-ONLY</b>
   */
  protected Session[] sessions=null;
  
  /**
   * Creates an uninitialized <i>Channel</i>.
   *
   * @param channelID the ID of the Channel
   * @param qos the {@link net.sf.appia.core.QoS QoS} that <i>models</i> the Channel
   * @param eventScheduler the {@link net.sf.appia.core.EventScheduler EventScheduler} used
   */
  public Channel(String channelID, QoS qos, EventScheduler eventScheduler, JMXConfiguration jmxConfig) {
    
    this.channelID=channelID;
    this.qos=qos;
    this.eventScheduler=eventScheduler;
    sessions=new Session[qos.getLayers().length];
    timerManager=(eventScheduler.getAppiaInstance()).instanceGetTimerManager();
    threadFactory = eventScheduler.getAppiaInstance().getThreadFactory();
    this.jmxConfiguration = jmxConfig;
  }
  
    /* new methods added */
  
  /**
   * Creates an uninitialized <i>Channel</i> with a Memory Manager.
   *
   * @param channelID the ID of the Channel
   * @param qos the {@link net.sf.appia.core.QoS QoS} that <i>models</i> the Channel
   * @param eventScheduler the {@link net.sf.appia.core.EventScheduler EventScheduler} used
   */
  public Channel(String channelID, QoS qos, EventScheduler eventScheduler,
  MemoryManager memoryManager, JMXConfiguration jmxConfig) {
    
    this.channelID=channelID;
    this.qos=qos;
    this.eventScheduler=eventScheduler;

    sessions=new Session[qos.getLayers().length];
    timerManager=eventScheduler.getAppiaInstance().instanceGetTimerManager();
    threadFactory = eventScheduler.getAppiaInstance().getThreadFactory();
    this.memoryManager = memoryManager;
    this.jmxConfiguration = jmxConfig;
  }
  
  /**
   * gets the memory manager of this Channel
   */
  public MemoryManager getMemoryManager() {
    return this.memoryManager;
  }
  
  /**
   * sets the memory manager of this Channel.
   * If the paramater is null, the channel doesn't have a Memory Manager
   * @param newMemoryManager the new MemoryManager
   */
  public void setMemoryManager(MemoryManager newMemoryManager) {
    this.memoryManager = newMemoryManager;
  }
  
  /**
   * Get this channel {@linkplain TimeProvider}.
   * 
   * @return associated {@linkplain TimeProvider}.
   */
  public TimeProvider getTimeProvider(){
	  return timerManager;
  }

  public ThreadFactory getThreadFactory(){
      return threadFactory;
  }

  public MessageFactory getMessageFactory(){
      return messageFactory;
  }

  public void setMessageFactory(MessageFactory messageFactory) {
      this.messageFactory = messageFactory;
  }

  /**
   * Get the Channel ID
   * @return the Channel ID
   */
  public String getChannelID() {
    return channelID;
  }
  
  /**
   * Get the {@link net.sf.appia.core.EventScheduler EventScheduler} used.
   * @return the {@link net.sf.appia.core.EventScheduler EventScheduler} used
   */
  public EventScheduler getEventScheduler() {
    return eventScheduler;
  }
  
  /**
   * Get the {@link net.sf.appia.core.ChannelEventRoute route} of an {@link net.sf.appia.core.Event Event}.
   * @param event the {@link net.sf.appia.core.Event Event}
   * @return the route
   * @throws AppiaEventException with two possible
   * {@link net.sf.appia.core.AppiaEventException#type types}:
   * {@link net.sf.appia.core.AppiaEventException#CLOSEDCHANNEL CLOSEDCHANNEL},
   * {@link net.sf.appia.core.AppiaEventException#UNWANTEDEVENT UNWANTEDEVENT}
   */
  public ChannelEventRoute getEventRoute(Event event) throws AppiaEventException {
    if (eventsRoutes == null)
      throw new AppiaEventException(AppiaEventException.CLOSEDCHANNEL,"Channel Not Started");
    
    ChannelEventRoute channelRoute=(ChannelEventRoute)eventsRoutes.get(event.getClass());
    
    if (channelRoute==null) {
      
      //verifies if there is a route for a superclass of
      //the event received
      for ( Class c = event.getClass().getSuperclass() ;
      (c != null) && (channelRoute == null) ;
      c = c.getSuperclass() ) {
        channelRoute = (ChannelEventRoute)eventsRoutes.get(c);
      }
      
      if ( channelRoute != null ) {
        eventsRoutes.put(event.getClass(),channelRoute);
      } else {
        throw new AppiaEventException(AppiaEventException.UNWANTEDEVENT,"Unwanted Event '"+event.getClass().getName()
                +"' on Channel '"+this.channelID+"'.");
      }
    }
    return channelRoute;
  }
  
  /**
   * Get the index of the first {@link net.sf.appia.core.Session Session} to be visited
   * by an {@link net.sf.appia.core.Event Event}.
   *
   * @param channelRoute the {@link ChannelEventRoute route} of the Event
   * @param dir the {@link net.sf.appia.core.Direction direction} the Event will take
   * @param source the {@link net.sf.appia.core.Session Session} from where the Event will come
   * @return the index of the first Session in terms of the Sessions stack
   * @throws AppiaEventException with one possible
   * {@link net.sf.appia.core.AppiaEventException#type type}:
   * {@link net.sf.appia.core.AppiaEventException#UNKNOWNSESSION UNKNOWNSESSION}
   */
  public int getFirstSession(ChannelEventRoute channelRoute, int dir, Session source)
  throws AppiaEventException {
    
    final Session[] route=channelRoute.getRoute();
    
    int i,index;
    
    if (source == null) {
      if (dir == Direction.UP)
        return 0;
      else
        return route.length-1;
    }
    
    for(i=sessions.length-1 ; (i >= 0) && (sessions[i] != source) ; i--);
    
    if (i < 0)
      throw new AppiaEventException(AppiaEventException.UNKNOWNSESSION,"Session not memeber of Channel");
    
    i+=dir;
    index=-1;
    while ( (i >= 0) && (i < sessions.length) && (index < 0) ) {
      for(index=route.length-1 ; (index >= 0) && (route[index] != sessions[i]) ; index--);
      i+=dir;
    }
    
    return ((index < 0) ? route.length : index) ;
  }
  
/*
        public int getFirstSession(ChannelEventRoute channelRoute, Direction dir, Session source)
                throws appiaEventException {
 
    if ( sessions == null )
       throw new appiaEventException(appiaEventException.CLOSEDCHANNEL,"Channel Not Started");
 
                boolean[] waypoints=channelRoute.waypoints;
    Session[] route=channelRoute.getRoute();
 
                int i;
 
    if (source==null) {
      if (dir.direction==Direction.UP)
         return 0;
      else
         return route.length-1;
    }
 
    for (i=0 ; (i < sessions.length) && (sessions[i]!=source) ; i++) ;
 
                if (i >= sessions.length)
                        throw new appiaEventException(appiaEventException.UNKNOWNSESSION,"Session not memeber of Channel");
 
    for ( ; (i < waypoints.length) && (i >= 0) && !waypoints[i] ; i+=dir.direction ) ;
 
    if ( (i >= waypoints.length) || (i < 0) )
       return route.length+1;
 
    if ( i == waypoints.length -1 )
       return route.length;
 
    int j;
    for ( j=0 ; (j < route.length) && (sessions[i] != route[j]) ; j++ ) ;
    return j;
        }
 */
  
  /**
   * Get the {@link net.sf.appia.core.QoS QoS} that <i>models</i> the Channel.
   * @return the {@link net.sf.appia.core.QoS QoS}
   */
  public QoS getQoS() {
    return qos;
  }
  
  /**
   * Checks if two Channels have the same {@link net.sf.appia.core.QoS QoS}.
   * @param channel the Cahnnel to compare to
   * @return are the two {@link net.sf.appia.core.QoS QoSs} equal
   */
  public boolean equalQoS(Channel channel) {
    return qos.equals(channel.qos);
  }
  
  /**
   * Starts, initializes, a Channel.
   * <br>
   * First, it allows all already set {@link net.sf.appia.core.Session Sessions} to set
   * yet unbounded {@link net.sf.appia.core.Session Sessions} in the Channel stack.
   * <br>
   * Second, it sets all yet unbounded {@link net.sf.appia.core.Session Sessions} with a
   * default {@link net.sf.appia.core.Session Session}.
   * <br>
   * Third, it checks to see if some {@link net.sf.appia.core.Session Session} appears
   * twice in the stack.
   * <br>
   * Fourth, it generates all the {@link net.sf.appia.core.ChannelEventRoute ChannelEventRoutes}
   * using as starting point the {@link net.sf.appia.core.QoSEventRoute QoSEventRoutes} of
   * the corresponding {@link net.sf.appia.core.QoS QoS}.
   * <br>
   * Fifth, it registers the given {@link net.sf.appia.core.EventScheduler EventScheduler}
   * with {@link net.sf.appia.core.Appia#insertEventScheduler Appia}
   * <br>
   * Sixth, and finally, it sends the
   * {@link net.sf.appia.core.events.channel.ChannelInit ChannelInit}
   *
   * @throws AppiaDuplicatedSessionsException if a {@link net.sf.appia.core.Session Session}
   * appears twice in the Channel stack
   */
  public void start() throws AppiaDuplicatedSessionsException {
    synchronized (this) {
      if (started || alive)
        return;
      started = true;
    }
    
    int i;
    for (i=sessions.length-1 ; i >= 0 ; i--) {
      if (sessions[i]!=null)
        sessions[i].boundSessions(this);
    }
    
    createUnboundedSessions();
    
    int j;
    for (i=0 ; i < sessions.length ; i++) {
      for (j=i+1 ; j < sessions.length ; j++) {
        if (sessions[i]==sessions[j])
          throw new AppiaDuplicatedSessionsException();
      }
    }
    
    makeEventsRoutes();
    
    try {
      if (Thread.currentThread() == eventScheduler.getAppiaInstance().instanceGetAppiaThread())
    	(new ChannelInit(this)).go();
      else
        (new ChannelInit()).asyncGo(this,Direction.UP);
    } catch (AppiaEventException e) {
      e.printStackTrace();
    }
    
    if(jmxConfiguration != null){
        try {
            this.registerMBean();
        } catch (AppiaException e) {
            e.printStackTrace();
        }
    }
  }
  
  public synchronized boolean isStarted() {
	  return started || alive;
  }
  
  /**
   * Ends Channel operation.
   * <br>
   * It sends the
   * {@link net.sf.appia.core.events.channel.ChannelClose ChannelClose}.
   */
  public void end() {
    synchronized (this) {
    	if(jmxConfiguration != null){
    		try {
    			this.unregisterMBean();
    		} catch (AppiaException e) {
    			e.printStackTrace();
    		}
    	}
      if (alive) {
        try {
          if (Thread.currentThread() == eventScheduler.getAppiaInstance().instanceGetAppiaThread())
        	(new ChannelClose(this)).go();
          else
            (new ChannelClose()).asyncGo(this,Direction.DOWN);
          alive=false;
        } catch (AppiaEventException ex) { ex.printStackTrace(); }
      }
    }
  }
  
  /**
   * Get a {@link net.sf.appia.core.ChannelCursor ChannelCursor} for the current Channel
   * {@link net.sf.appia.core.Session Sessions} stack.
   * @return a {@link net.sf.appia.core.ChannelCursor ChannelCursor} for the stack
   */
  public ChannelCursor getCursor() {
    return new ChannelCursor(this);
  }  
  
  /**
   * Inserts an {@link net.sf.appia.core.Event Event} in the Channel.
   * <br>
   * It cheks if the Channel is close and calls
   * {@link net.sf.appia.core.EventScheduler#insert EventScheduler.insert(Event)}.
   *
   * @throws AppiaEventException as the possible result of calling
   * {@link net.sf.appia.core.EventScheduler#insert EventScheduler.insert(Event)}
   * or with type
   * {@link net.sf.appia.core.AppiaEventException#CLOSEDCHANNEL CLOSEDCHANNEL}
   * if the Channel is closed
   */
  protected void insertEvent(Event event) throws AppiaEventException {
    synchronized (this) {
      if (!alive && !(event instanceof ChannelInit)) {
        if (started) {
          while (!alive) {
            try {
              this.wait();
            } catch (InterruptedException e) {
              //e.printStackTrace();
            }
          }
        } else {
          throw new AppiaEventException(AppiaEventException.CLOSEDCHANNEL,"Channel is Closed");
        }
      }
    }
    
    eventScheduler.insert(event);
  }
  
  private void createUnboundedSessions() {
    int i;
    final Layer[] layers=qos.getLayers();
    
    for (i=sessions.length-1 ; i >= 0 ; i--) {
      if (sessions[i]==null)
        sessions[i]=layers[i].createSession();
    }
  }
  
  private void makeEventsRoutes() {
    final QoSEventRoute[] qosRoutes=qos.getEventsRoutes();
    eventsRoutes=new Hashtable();
    
    for (int i=0 ; i < qosRoutes.length ; i++) {
      eventsRoutes.put(qosRoutes[i].getEventType(),qosRoutes[i].makeChannelRoute(this));
    }
  }
  
  
  /**
   * Method that handles all {@link net.sf.appia.core.events.channel channel events} when
   * they reach the top or the bottom of the {@link net.sf.appia.core.Session Sessions} stack.
   * <br>
   * If the {@link net.sf.appia.core.Event Event} is a
   * {@link net.sf.appia.core.events.channel.Timer Timer} or
   * {@link net.sf.appia.core.events.channel.PeriodicTimer PeriodicTimer} event it forwards
   * the {@link net.sf.appia.core.Event Event} to the {@link net.sf.appia.core.TimerManager TimerManager}.
   * <br>
   * If the {@link net.sf.appia.core.Event Event} is a
   * {@link net.sf.appia.core.events.channel.EchoEvent EchoEvent} it gets the payload
   * {@link net.sf.appia.core.Event Event}, inverts the {@link net.sf.appia.core.Direction direction}
   * and sends that {@link net.sf.appia.core.Event Event}.
   */
  public void handle(Event event) {
    
    // ChannelInit
    if ( event instanceof ChannelInit ) {
      synchronized (this) {
        alive=true;
        started=false;
        // notify threads that are blocked in asyncGo of event
		this.notifyAll();
      }
      return;
    }
    
    // ChannelClose
    if ( event instanceof ChannelClose ) {
      eventsRoutes.clear();
      return;
    }
    
    // Timer
    if ( event instanceof Timer ) {
      timerManager.handleTimerRequest((Timer)event);
      return;
    }
    
    // PeriodicTimer
    if ( event instanceof PeriodicTimer ) {
      timerManager.handlePeriodicTimer((PeriodicTimer)event);
      return;
    }
    
    // AsyncEvent
    /*
    if ( event instanceof AsyncEvent ) {
      return;
    }
    */
    
    // Debug
    if ( event instanceof Debug ) {
      return;
    }
    
    // EchoEvent
    if ( event instanceof EchoEvent ) {
      final Event e=((EchoEvent)event).getEvent();
      
      e.setChannel(this);
      e.setDir(event.getDir() == Direction.UP ? Direction.DOWN : Direction.UP);
      e.setSourceSession(null);
      
      try {
        e.init();
        e.go();
      } catch (AppiaEventException ex) {
        System.err.println(
        "appia.Channel.handle(Event): exception (\""+
        ex.getMessage()+
        "\") while processing EchoEvent in \""+channelID+"\" channel."
        );
      }
      
      return;
    }
  }

    private void registerMBean() throws AppiaException{
        log.info("Registering MBean for channel "+channelID);
        final ChannelManager manager = new ChannelManager(this);
        Session currentSession = null;
        int numSensorSessions = 0, numManagedSessions = 0;
        
        final ChannelCursor cc = getCursor();
        cc.top();
        while(cc.isPositioned()){
            currentSession = cc.getSession();
            if(currentSession instanceof SensorSession){
                ((SensorSession)currentSession).addSensorListener(manager);
                numSensorSessions++;
            }
            if(currentSession instanceof ManagedSession){
                manager.addManagedSession(currentSession);
                numManagedSessions++;
            }
            cc.down();
        }
        
        if(jmxConfiguration.isLocal()){
            String beanID = jmxConfiguration.getManagementMBeanID()+":"+this.getChannelID();
            AppiaMBeanContainer.getInstance().registerBean(beanID, manager);
        }
        else
            ManagementServerFactory.getInstance(jmxConfiguration).registerMBean(this,manager);

        log.info("MBean registered on channel "+channelID+". Listening on "+numSensorSessions+" SensorSession(s) and "+
                numManagedSessions+" ManagedSession(s).");
    }

    private void unregisterMBean() throws AppiaException{
        log.info("Unregistering MBean for channel "+channelID);
        Session currentSession = null;
        ChannelManager manager = null;
        if(jmxConfiguration.isLocal()){
            String beanID = jmxConfiguration.getManagementMBeanID()+":"+this.getChannelID();
            AppiaMBeanContainer.getInstance().unregisterBean(beanID);
        }
        else
            manager = (ChannelManager) ManagementServerFactory.getInstance(jmxConfiguration).unregisterMBean(this);
        
        final ChannelCursor cc = getCursor();
        cc.top();
        while(cc.isPositioned()){
            currentSession = cc.getSession();
            if(currentSession instanceof SensorSession){
                ((SensorSession)currentSession).removeSensorListener(manager);
                manager.removeManagedSession(currentSession);
            }
            cc.down();
        }
        log.info("MBean unregistered from channel "+channelID+".");
    }

    /**
     * Gets the JMX configuration.
     * @return Returns the JMX configuration.
     */
    public JMXConfiguration getJMXConfiguration() {
        return jmxConfiguration;
    }
  
}
