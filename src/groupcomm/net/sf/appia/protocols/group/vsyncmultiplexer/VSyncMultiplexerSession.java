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
 /*
 * Created on 27-Jan-2005
 *
 */
package net.sf.appia.protocols.group.vsyncmultiplexer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.core.events.channel.EchoEvent;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.sync.BlockOk;

import org.apache.log4j.Logger;


/**
 * @author nunomrc
 *
 */
public class VSyncMultiplexerSession extends Session {
    
    private static Logger log = Logger.getLogger(VSyncMultiplexerSession.class);

	private HashMap<Channel,Object> channels;
	private int blockOkCounter;
	private ViewState vs = null;
	private List<GroupSendableEvent> pendingEvents = null;
	
	/**
	 * @param layer
	 */
	public VSyncMultiplexerSession(Layer layer) {
		super(layer);
		channels = new HashMap<Channel, Object>();
		blockOkCounter = 0;
		pendingEvents = new LinkedList<GroupSendableEvent>();
	}

	public void handle(Event e){
	    if(e instanceof GroupSendableEvent)
	        handleGroupSendable((GroupSendableEvent)e);
	    else if(e instanceof EchoEvent)
			handleEchoEvent((EchoEvent)e);
		else if (e instanceof View)
			handleView((View)e);
		else if(e instanceof BlockOk)
			handleBlockOk((BlockOk)e);
		else if (e instanceof ChannelInit)
			handleChannelInit((ChannelInit)e);
		else if (e instanceof ChannelClose)
			handleChannelClose((ChannelClose)e);
		else
			try {
				e.go();
			} catch (AppiaEventException e1) {
				e1.printStackTrace();
			}
	}

	/**
	 * @param close
	 */
	private void handleChannelClose(ChannelClose close) {
		channels.put(close.getChannel(),null);
		try {
			close.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param init
	 */
	private void handleChannelInit(ChannelInit init) {
		channels.put(init.getChannel(),null);
		try {
			init.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

	private void handleGroupSendable(GroupSendableEvent ev){
//	    if(ev instanceof AckViewEvent && ev.getDir() == Direction.UP){
//	        System.out.println("Multiplexer: received event: "+ev+" FROM "+ev.orig);
//	        System.out.println("ACK: "+ev.getChannel().getChannelID()+" "+ev.view_id);
//	    }
	    
	    if(ev.getDir() == Direction.UP && (vs == null || !vs.id.equals(ev.view_id))){
//	        System.out.println("Multiplexer: holding event "+ev);
	        pendingEvents.add(ev);
	    }
        else
            try {
                ev.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
	}
	
	/**
	 * @param ok
	 */
	private void handleBlockOk(BlockOk ok) {
	    log.debug("Collecting blockok events :: counter = "+blockOkCounter);
	    if((--blockOkCounter) == 0){
	        try {
	            log.debug("Delivering blockok on channel: "+ok.getChannel().getChannelID());
	            ok.go();
	        } catch (AppiaEventException e) {
	            e.printStackTrace();
	        }
	    }
	}

	/**
	 * @param view
	 */
	private void handleView(View view) {
        
	    log.debug("Replicating view to all channels view "+view.view_id+" with size "+view.vs.addresses.length);
//	    System.out.println("Multiplexer: Replicating view to all channels view "+view.view_id+" with size "+view.vs.addresses.length);
        
	  view.vs.version="MULTI";
	  vs = view.vs;
	  
	  Iterator<Channel> it = channels.keySet().iterator();
	  for ( ; it.hasNext() ; ){
	    Channel c = it.next();
	    if( ! c.equals(view.getChannel())){
	      try {
//	          System.out.println("Multiplexer: sending to "+c.getChannelID());
	        View copy = new View(view.vs, view.ls, c, view.getDir(), this);
	        copy.setPriority(copy.getPriority()+1);
	        copy.go();
	      } catch (AppiaEventException e2) {
	        e2.printStackTrace();
	      }
	    }
	  }
	  
	  try {
//          System.out.println("Multiplexer: sending to "+view.getChannel().getChannelID());
	    view.go();
	  } catch (AppiaEventException e1) {
	    e1.printStackTrace();
	  }

	  // dump pending events
	  if(!pendingEvents.isEmpty()){
	      Iterator<GroupSendableEvent> eventIt = pendingEvents.iterator();
	      while(eventIt.hasNext()){
	          GroupSendableEvent ev = eventIt.next();
	          if(ev.view_id.equals(vs.id)){
	              eventIt.remove();
	              try {
	                  ev.go();
	              } catch (AppiaEventException e) {
	                  e.printStackTrace();
	              }
	          }
	          else 
	              break;
	      }
	  }
	}

	/**
	 * @param event
	 */
	private void handleEchoEvent(EchoEvent echo) {
	  if(echo.getEvent() instanceof BlockOk){
          log.debug("Replicating EchoEvent to all channels. Echo received on Channel: "+echo.getChannel().getChannelID());
	    blockOkCounter=0;
	    
	    BlockOk blockok=(BlockOk)echo.getEvent();
	    
	    Iterator<Channel> it = channels.keySet().iterator();
	    for ( ; it.hasNext() ; ){
	      Channel c = it.next();
	      if( ! c.equals(echo.getChannel())){
	        try {
	          EchoEvent copy = new EchoEvent(new BlockOk(blockok.group,blockok.view_id), c, echo.getDir(), this);
	          copy.go();
	          blockOkCounter++;
	        } catch (AppiaEventException e2) {
	          e2.printStackTrace();
	        }
	      }
	    }
	    try {
	      echo.go();
	      blockOkCounter++;
	    } catch (AppiaEventException e1) {
	      e1.printStackTrace();
	    }
	    
	  } else {
	    try {
	      echo.go();
	    } catch (AppiaEventException e) {
	      e.printStackTrace();
	    }
	  }
	}
}
