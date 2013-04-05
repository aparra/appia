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
// Nuno Carvalho - added some code to deal with the MemoryManager (9-Jul-2001)

package net.sf.appia.core.events;

import net.sf.appia.core.*;
import net.sf.appia.core.message.Message;

/**
 * This class defines a SendableEvent
 * 
 * @author <a href="mailto:apinto@di.fc.ul.pt">Alexandre Pinto</a>
 * @version 1.0
 */
public class SendableEvent extends Event implements Cloneable {

	private boolean detached;

  public SendableEvent() {
    message=new Message();
    detached = true;
  }

  public SendableEvent(Channel channel, int dir, Session source) throws AppiaEventException {
    super(channel,dir,source);
    message=new Message();
    detached = false;
    // added on 9-Jul-2001
    attachToMemory();		
  }

  public SendableEvent(Message msg) {
    message=msg;
    detached = false;
    // added on 9-Jul-2001
    attachToMemory();
  }

  public SendableEvent(Channel channel, int dir, Session source, Message msg) throws AppiaEventException {
    super(channel,dir,source);
    message=msg;
    detached = false;
    // added on 9-Jul-2001
    attachToMemory();
  }

  public Object dest;
  public Object source;

  protected Message message;

  public Message getMessage() {
    return message;
  }

  public void setMessage(Message message) {
    this.message=message;
    // added on 9-Jul-2001
    attachToMemory();
  }
  
  public void setChannel(Channel channel) {
    super.setChannel(channel);
    /* redefined on 9-Jul-2001 */
	attachToMemory();
  }

	/*
	 * Attach the number of bytes in the Message of this sendable event
	 * to the memory manager.
	 * 
	 * This can only be called when we are sure that the memory
	 * is detached from the memory manager. (detached == true)
	 * 
	 * Added by Nuno on 7/03/2003
	 */
	private void attachToMemory(){
		if (detached)
			return;
		if (AppiaConfig.QUOTA_ON && (this.message!=null) && (this.getChannel()!=null)){
			this.message.setMemoryManager(this.getChannel().getMemoryManager());
			detached = false;
		}
	}

	/**
	 * Detaches the amount of memory occupied by the Message of
	 * This SendableEvent from the MemoryManager.
	 * This is used when we need to use flow control in the channel with
	 * good performance.
	 * <br>
	 * This method is called by the Appia kernel when the event route ends.
	 * This method should be called if we want to destroy the event before 
	 * he reaches the end of his route or if we dont call the go() method in 
	 * the last session. If this is not done by the protocol programmer, it is 
	 * done in the finalizer of the message, but the performance is not so good.
	 * @see net.sf.appia.core.message.Message
	 * @see net.sf.appia.core.memoryManager.MemoryManager
	 */
	public void detachFromMemory(){
		if(AppiaConfig.QUOTA_ON && !detached && (this.message.getMemoryManager() != null)){
			this.message.setMemoryManager(null);
			detached = true;
		}
	}

	/**
 	 * Clones the sendable event.
 	 * @see net.sf.appia.core.Event#cloneEvent()
 	 */
	public Event cloneEvent() throws CloneNotSupportedException {
	    final SendableEvent ev = (SendableEvent) super.cloneEvent();
	    ev.message = (Message) message.clone();
	    return ev;
	}

	/**
	 * Called when the garbage Collector runs and this object is going to
	 * be destroyed.
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
	    super.finalize();
        if(message!=null && message.length() > 0)
	    detachFromMemory();
	}

}
