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
 package net.sf.appia.protocols.causalWaiting;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;

import net.sf.appia.core.AppiaError;
import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.events.Send;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.group.leave.LeaveEvent;

import org.apache.log4j.Logger;


/**
 * Causal order protocol that implements the waiting causal broadcast algorithm
 * as described in the book <i>Introduction to Reliable Distributed Programming</i> by
 * Rachid Guerraoui and Luis Rodrigues. 
 * 
 * @see net.sf.appia.protocols.causalWaiting.CausalWaitingLayer
 * @see EventContainer
 * @author Jose Mocito
 */
public class CausalWaitingSession extends Session {

    private static Logger log = Logger.getLogger(CausalWaitingSession.class);
    private static final boolean debugOn = true;
    
	private LocalState ls;
	private ViewState vs;
	
	/**
	 * Local causality information vector.
	 */
	private long[] VC;
	
	/**
	 * List of received events still to be delivered.
	 */
	private LinkedList<EventContainer> pending = new LinkedList<EventContainer>();
	
	/**
	 * Constructs a new waiting causal order protocol session.
	 * 
	 * @param layer 
	 */
	public CausalWaitingSession(Layer layer) {
		super(layer);
	}
	
	/**
	 * This is the protocol's main event handler.
	 * It accepts the following events:
	 * <ul>
     * <li>net.sf.appia.protocols.group.leave.LeaveEvent
	 * <li>net.sf.appia.protocols.group.events.GroupSendableEvent
	 * <li>net.sf.appia.protocols.group.intra.View
	 * </ul>
	 * 
     * @param event the event to handle.
	 * @see net.sf.appia.core.Session#handle(net.sf.appia.core.Event)
	 */
	public void handle(Event event) {
		if (event instanceof View)
			handleView((View) event);
        else if(event instanceof LeaveEvent)
            handleLeaveEvent((LeaveEvent) event);
		else if (event instanceof GroupSendableEvent)
			handleGroupSendableEvent((GroupSendableEvent) event);
		else
			try {
				event.go();
			} catch (AppiaEventException e) {
				e.printStackTrace();
			}
	}

	private void handleView(View view) {
		ls = view.ls;
		vs = view.vs;
		
		VC = new long[vs.view.length];
		Arrays.fill(VC,0);
		
		// Sanity check
		if (!pending.isEmpty()) {
			log.fatal("Received new view but pending messages still exist! View synchrony properties compromised!");
            throw new AppiaError("Received new view but pending messages still exist! View synchrony properties compromised!");
		}
		
		try {
			view.go();
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}

    private void handleLeaveEvent(LeaveEvent ev){
        try {
            ev.go();
        } catch (AppiaEventException e) {
            e.printStackTrace();
        }
    }
    
	private void handleGroupSendableEvent(GroupSendableEvent event) {
        if(debugOn && log.isDebugEnabled())
            log.debug("CAUSAL Processing event "+event);
        if (!(event instanceof Send)) {
            if (event.getDir() == Direction.DOWN) {
                Message omsg = event.getMessage();
                for (int i = 0; i < VC.length; i++)
                    omsg.pushLong(VC[i]);
                try {
                    event.go();
                } catch (AppiaEventException e) {
                    e.printStackTrace();
                }
                VC[ls.my_rank]++;
            }
            else {
                if (event.orig != ls.my_rank) {
                    long[] VCm = new long[VC.length];
                    extractVCm(event.getMessage(), VCm);
                    pending.add(new EventContainer(event, VCm));
                    deliverPending();
                }
                else {
                    clearVC(event.getMessage());
                    try {
                        event.go();
                    } catch (AppiaEventException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else
            try {
                event.go();
            } catch (AppiaEventException e) {
                e.printStackTrace();
            }
	}
	
	/**
	 * Extracts the VC from the header of a message.
	 * 
	 * @param omsg the message from where the VC will be extracted.
	 * @param VCm array where the values of the VC for the given message will be stored.
	 */
	private void extractVCm(Message omsg, long[] VCm) {
		for (int i = VCm.length - 1; i >= 0; i--)
			VCm[i] = omsg.popLong();
	}
	
	/**
	 * Clears the values of the VC in the header of a message.
	 * 
	 * @param omsg the message whose header will be cleared.
	 */
	private void clearVC(Message omsg) {
		for (int i = 0; i < VC.length; i++)
			omsg.popLong();
	}
	
	/**
	 * Delivers pending messages that satisfy the causality order criteria.
	 */
	private void deliverPending() {
		ListIterator it = pending.listIterator();
		while (it.hasNext()) {
			EventContainer cont = (EventContainer) it.next();
			long[] VCx = cont.getVC();
			if (canDeliver(VCx)) {
				it.remove();
				GroupSendableEvent ev = cont.getEvent();
				try {
					ev.go();
				} catch (AppiaEventException e) {
					e.printStackTrace();
				}
				VC[ev.orig]++;
				if (!pending.isEmpty() && it.nextIndex() != 0)
					it = pending.listIterator();
			}
		}
	}

	/**
	 * Checks if the causality order criteria is met for the message associated
	 * with the VCx vector.
	 * 
	 * @param VCx the causality information vector associated with the message to be checked.
	 * @return <tt>true</tt> if the causality order criteria is met, <tt>false</tt> otherwise.  
	 */
	private boolean canDeliver(long[] VCx) {
		for (int i = 0; i < VC.length; i++)
			if (VC[i] < VCx[i])
				return false;
		return true;
	}
}
