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
 * Created on Mar 30, 2004
 */
package net.sf.appia.test.xml;

import java.awt.Point;

import net.sf.appia.protocols.group.events.GroupSendableEvent;

/**
 * Event that carries a point.
 * 
 * @author Jose Mocito
 */
public class DrawEvent extends GroupSendableEvent {
	
	private Point point;
	
	public DrawEvent() {
		super();
	}
	
	public DrawEvent(Point point) {
		super();
		this.point = point;
	}
	
	public Point getPoint() {
		return point;
	}
	
	public void setPoint(Point p) {
		point = p;
	}
}
