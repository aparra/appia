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
 * Copyright:    Copyright (c) Nuno Carvalho and Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Nuno Carvalho and Luis Rodrigues
 * @version 1.0
 */

package net.sf.appia.management;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import javax.management.Notification;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;

/**
 * 
 * This class defines a AbstractSensorSession.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public abstract class AbstractSensorSession extends Session implements SensorSession {
	
	private List<SensorSessionListener> listeners;
	private long sequenceNumber = 0;
	
	public AbstractSensorSession(Layer layer) {
		super(layer);
		listeners = new ArrayList<SensorSessionListener>();
	}

	/* (non-Javadoc)
	 * @see net.sf.appia.management.SensorSession#addSensorListener(net.sf.appia.management.SensorSessionListener)
	 */
	public void addSensorListener(SensorSessionListener listener) {
		listeners.add(listener);
	}
	
	/* (non-Javadoc)
	 * @see net.sf.appia.management.SensorSession#removeSensorListener(net.sf.appia.management.SensorSessionListener)
	 */
	public void removeSensorListener(SensorSessionListener listener) {
		listeners.remove(listener);
	}
	
	/* (non-Javadoc)
	 * @see net.sf.appia.management.SensorSession#notifySensorListeners(javax.management.Notification)
	 */
	public void notifySensorListeners(Notification notif){
		//FIXME: notify with clones
		final ListIterator it = listeners.listIterator();
		while(it.hasNext()){
			final SensorSessionListener listener = (SensorSessionListener) it.next();
			listener.onNotification(notif);
		}
	}
	
	public long getNotificationSequenceNumber(){
		return sequenceNumber++;
	}

}
