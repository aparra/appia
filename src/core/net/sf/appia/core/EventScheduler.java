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
 * Change Log: 
 * 
 */

/**
 * Title:        Appia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Alexandre Pinto & Hugo Miranda & Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto & Hugo Miranda & Luis Rodrigues
 * @version 1.0
 */
public class EventScheduler {
  private Appia appia;
  
  private Event mainHead=null;
  private Event mainTail=null;
  private Event mainLast=null;
  
  private Event reverseHead=null;
  private Event reverseTail=null;
  
  private Event waitingHead=null;
  private Event waitingTail=null;
  
  private int currentDirection=0;
  private Channel currentChannel=null;
  private Session currentSession=null;
  
  public EventScheduler() {
    appia=Appia.appia;
    appia.instanceInsertEventScheduler(this);
  }
  
  public EventScheduler(Appia appia) {
    this.appia = appia;
    appia.instanceInsertEventScheduler(this);
  }
  
  public void insert(Event event) {
    if (
        (Thread.currentThread() == appia.instanceGetAppiaThread()) &&
        (currentSession != null) &&
        (event.getChannel() == currentChannel) &&
        ((event.currentSession() == currentSession) || (event.getSourceSession() == currentSession))
    ) {
      if (event.getDir() == currentDirection) {
        
        if (mainHead == null) { // if queue is empty
          event.setSchedulerData(null);
          mainHead=mainTail=event;
        } else {
          
          if (mainTail == mainLast)
            mainTail=event;
          
          if (mainLast == null) {
            event.setSchedulerData(mainHead);
            mainHead=event;
          } else {
            event.setSchedulerData(mainLast.getSchedulerData());
            mainLast.setSchedulerData(event);
          }
        }
        mainLast=event;
        
      } else { // inserting in the opposite direction
        event.setSchedulerData(null);
        if (reverseHead == null)
          reverseHead=event;
        else
          reverseTail.setSchedulerData(event);
        reverseTail=event;
      }
    } else { // inserting on a different channel, etc
    	synchronized (this) {
    		event.setSchedulerData(null);
    		if(waitingTail == null || event.getPriority() <= waitingTail.getPriority()){
    			if (waitingHead == null)
    				waitingHead=event;
    			else {
    				waitingTail.setSchedulerData(event);
    			}
    			waitingTail=event;
    		}
    		else if(event.getPriority() > waitingHead.getPriority()){
    			event.setSchedulerData(waitingHead);
    			waitingHead = event;    			
    		} else {
    			// start in the second
    			Event previous = waitingHead, current = (Event) waitingHead.getSchedulerData();
    			//while(current != null && event.getPriority() <= current.getPriority()){
    			while(event.getPriority() <= current.getPriority()){
    				previous = current;
    				current = (Event) current.getSchedulerData();
    			}
    			event.setSchedulerData(current);
    			previous.setSchedulerData(event);
    		}
    	}
    }
    
    appia.instanceInsertedEvent();
  }
  
  public boolean consumeEvent() {
    boolean consumed=true;
    Event event=null;
    Session session=null;
    
    if (mainHead != null) {
      event=mainHead;
      mainHead=(Event) mainHead.getSchedulerData();
      if (mainHead == null)
        mainTail=null;
    } else {
      if (reverseHead != null) {
        event=reverseHead;
        mainHead=(Event) reverseHead.getSchedulerData();
        if (mainHead == null)
          mainTail=null;
        else
          mainTail=reverseTail;
        reverseHead=null;
        reverseTail=null;
      } else {
        synchronized (this) {
          if (waitingHead != null) {
            event=waitingHead;
            waitingHead=(Event) waitingHead.getSchedulerData();
            if (waitingHead == null)
              waitingTail=null;
          } else {
            consumed=false;
          }
        }
      }
    }
    
    if (consumed) {
      event.setSchedulerData(null);
      session=event.popSession();
      
      if (session != null) {
        currentSession=session;
        currentDirection=event.getDir();
        currentChannel=event.getChannel();
        
        mainLast=null;
        
        try {
            session.handle(event);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.println("--------------------------------"+
                    "Exception report:"+
                    "\nSession: "+session+
                    "\nEvent: "+event+
                    "\nDirection: "+((event.getDir()==Direction.UP)?"UP":"DOWN")+
                    "\nSourceSession: "+event.getSourceSession()+
                    "\nChannel: "+event.getChannel()+
                    "\n--------------------------------");
            throw e;
        }
        currentSession=null;
      }
    }
    return consumed;
  }
    
  public Session getHandelingSession() {
    return currentSession;
  }

  public Appia getAppiaInstance() {
    return appia;
  }

  public void start() {}
  public void stop() {}
  
}