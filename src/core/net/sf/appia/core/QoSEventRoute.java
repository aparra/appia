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
package net.sf.appia.core;

/**
 * This class defines a QoSEventRoute
 * 
 * @author <a href="mailto:apinto@di.fc.ul.pt">Alexandre Pinto</a>
 * @version 1.0
 */
public class QoSEventRoute {

  private QoS qos;
  private Class eventType;
  //private Layer[] layers;

  protected boolean[] waypoints;

  public QoSEventRoute(QoS qos, Class eventType) {
    this.eventType=eventType;
    this.qos=qos;

    final Class[][] accepted=this.qos.eventsAccepted;
    //layers=this.qos.layers;
    waypoints=new boolean[accepted.length];


    int i,j;
    for (i=0 ; i < accepted.length ; i++) {
      if (accepted[i] != null) {
        for (j=0 ; (j < accepted[i].length) && !(accepted[i][j].isAssignableFrom(eventType)) ; j++);
        waypoints[i]= (j < accepted[i].length);
      } else {
        waypoints[i]=false;
      }
    }
  }

  public Class getEventType() {
    return eventType;
  }

  public ChannelEventRoute makeChannelRoute(Channel channel) {
    return new ChannelEventRoute(channel,this);
  }
}