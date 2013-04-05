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
 
/**
 * Title:        Appia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Alexandre Pinto & Hugo Miranda & Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto & Hugo Miranda & Luis Rodrigues
 * @version 1.0
 */

// Change Log:
// NunoCarvalho - added another "createUnboundChannel" method for a
//                channel with a MemoryManager
// (9-Jul-2001)

package net.sf.appia.core;

import java.util.HashSet;
import java.util.Iterator;

import net.sf.appia.core.memoryManager.*;
import net.sf.appia.management.jmx.JMXConfiguration;


/**
 * This class defines a QoS.
 * 
 * @author Alexandre Pinto
 * @version 1.0
 */
public class QoS {
  
  private String qosID;
  
  protected QoSEventRoute[] eventsRoutes;
  protected Layer[] layers;
  
  protected Class[][] eventsAccepted=null;
  private Class[] eventsProvided=null;
  
  public QoS(String id, Layer[] layers) throws AppiaInvalidQoSException {
    this.layers=layers;
    this.qosID=id;
    
    eventsAccepted=new Class[layers.length+1][];
    
    final HashSet provided=new HashSet();
    
    int i,j;
    //gathers provided and accepted events from layers
    for (i=0 ; i < layers.length ; i++) {
      final Class[] provides=layers[i].getProvidedEvents();
      
      if (provides != null) {
        for (j=0 ; j < provides.length ; j++)
          provided.add(provides[j]);
      }
      
      eventsAccepted[i]=layers[i].getAcceptedEvents();
    }
    
    //gathers provided and accepted events of the channel
      provided.add(net.sf.appia.core.events.channel.ChannelInit.class);
      provided.add(net.sf.appia.core.events.channel.ChannelClose.class);
      eventsAccepted[eventsAccepted.length-1]=new Class[1];
      eventsAccepted[eventsAccepted.length-1][0]=net.sf.appia.core.events.channel.ChannelEvent.class;
    
    eventsProvided=(Class[])provided.toArray(new Class[0]);
    
    validateQoS();
    this.layers=(Layer[])layers.clone();
    makeEventsRoutes();
    
    
    // Uncomment to print the event routes to a file
    /*
    java.io.PrintStream f=null;
    try {
      f=new java.io.PrintStream(new java.io.FileOutputStream("/home/alexp/appia/JBproject/routes.ex",true));
    } catch (Exception ex) {
      ex.printStackTrace();
      System.exit(1);
    }
    f.println("{"+qosID+"}");
    for (i=0 ; i < eventsRoutes.length ; i++) {
      f.println("\t["+eventsRoutes[i].getEventType().getName()+"]=");
      boolean[] w=eventsRoutes[i].waypoints;
      k=0;
      for (j=0 ; j < w.length ; j++) {
        if (w[j]) {
          k++;
          if (j < layers.length)
            f.println("\t\t"+layers[j].getClass().getName());
          else
            f.println("\t\tCHANNEL");
        }
      }
      f.println("\t\t"+k);
    }
    f.close();
     */
    
  }
  
  public String getQoSID() {
    return qosID;
  }
  
  public Channel createUnboundChannel(String channelID, EventScheduler eventScheduler) {
    return new Channel(channelID, this, eventScheduler, null);
  }

  public Channel createUnboundChannel(String channelID, EventScheduler eventScheduler, JMXConfiguration config) {
      return new Channel(channelID, this, eventScheduler, config);
  }

  public Channel createUnboundChannel(String channelID) {
    return new Channel(channelID, this, new EventScheduler(), null);
  }

  public Channel createUnboundChannel(String channelID, JMXConfiguration config) {
      return new Channel(channelID, this, new EventScheduler(), config);
  }

  public Channel createUnboundChannel(String channelID, EventScheduler eventScheduler, MemoryManager mm) {
    return new Channel(channelID, this, eventScheduler,mm, null);
  }  

  public Channel createUnboundChannel(String channelID, EventScheduler eventScheduler, MemoryManager mm,
          JMXConfiguration config) {
      return new Channel(channelID, this, eventScheduler,mm, config);
  }  

  public Channel createUnboundChannel(String channelID, MemoryManager mm) {
    return new Channel(channelID, this, new EventScheduler(), mm, null);
  }

  public Channel createUnboundChannel(String channelID, MemoryManager mm, JMXConfiguration config) {
      return new Channel(channelID, this, new EventScheduler(), mm, config);
  }

  public Layer[] getLayers() {
    return (Layer[])layers.clone();
  }
  
  protected void validateQoS() throws AppiaInvalidQoSException {
    int i,j;
    
    //checks if all required events belong to provided
    for (i=0 ; i < layers.length ; i++) {
      final Class[] requires=layers[i].getRequiredEvents();
      
      if (requires != null) {
        for (j=0 ; (requires != null) && (j < requires.length) ; j++) {
          if ( ! hasRequired(requires[j]) )
            throw new AppiaInvalidQoSException(" required Event (\""+requires[j].getName()+"\") not provided");
        }
      }
    }
  }
  
  protected void makeEventsRoutes() {
    // gathers all provided and accepted events
    final HashSet all=new HashSet();
    
    int i,j;
    for (i=0 ; i < eventsProvided.length ; i++) {
      all.add(eventsProvided[i]);
    }
    
    for (i=0 ; i < eventsAccepted.length ; i++) {
      if (eventsAccepted[i] != null) {
        for (j=0 ; j < eventsAccepted[i].length ; j++) 
          all.add(eventsAccepted[i][j]);
      }
    }
    
    //creates QoSEventRoute for all events
    eventsRoutes=new QoSEventRoute[all.size()];
    
    final Iterator iter=all.iterator();
    
    for (i=0 ; iter.hasNext() ; i++) {
      final Class eventType=(Class)iter.next();
      
      eventsRoutes[i]=new QoSEventRoute(this,eventType);
    }
  }
  
  public QoSEventRoute[] getEventsRoutes() {
    return eventsRoutes;
  }
  
  private boolean hasRequired(Class required) {
    int i;
    
    for (i=0 ; i < eventsProvided.length ; i++) {
      if (required.isAssignableFrom(eventsProvided[i]))
        return true;
    }
    
    return false;
  }
  
  /**
   * Redefines Object.equals(Object).
   */
  public boolean equals(Object obj) {
    if (getClass() != obj.getClass())
      return false;
    
    if (layers.length != ((QoS)obj).layers.length)
      return false;
    
    int i;
    for (i=0 ; i < layers.length ; i++) {
      if (layers[i].getClass() != ((QoS)obj).layers[i].getClass())
        return false;
    }
    
    return true;
  }
}
