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

/**
 * The route an {@link net.sf.appia.core.Event Event} will take through the
 * {@link net.sf.appia.core.Channel Channel}.
 * <br>
 * It contains the stack with only the {@link net.sf.appia.core.Session Sessions} that
 * <i>accept</i> the {@link net.sf.appia.core.Event Event}.
 * <br>
 * It is the {@link net.sf.appia.core.Channel Channel} counterpart of the
 * {@link net.sf.appia.core.QoSEventRoute QoSEventRoute}. In practice the
 * <i>ChannelEventRoute</i> is created from the
 * {@link net.sf.appia.core.QoSEventRoute QoSEventRoute}.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.QoSEventRoute
 */
public class ChannelEventRoute {

  private Channel channel;
  private QoSEventRoute qosRoute;
  private Class eventType;
  private Session[] sessions;

  private boolean[] waypoints;
  private Session[] route;

  /**
   * Creates a <i>ChannelEventRoute</i> for the {@link net.sf.appia.core.Channel Channel},
   * from the given {@link net.sf.appia.core.QoSEventRoute QoSEventRoute}.
   *
   * @param channel the {@link net.sf.appia.core.Channel Channel}
   * @param qosRoute the {@link net.sf.appia.core.QoSEventRoute QoSEventRoute} that models
   * the route
   */
  public ChannelEventRoute(Channel channel, QoSEventRoute qosRoute) {
    this.channel=channel;
    this.qosRoute=qosRoute;
    eventType=this.qosRoute.getEventType();
    sessions=this.channel.sessions;
    waypoints=this.qosRoute.waypoints;

    int size,i,j;

    for (size=0,i=0 ; i < sessions.length ; i++) {
      if (waypoints[i])
         size++;
    }

    route=new Session[size];

    for (i=0,j=0 ; i < sessions.length ; i++) {
      if (waypoints[i]) {
         route[j]=sessions[i];
         j++;
      }
    }
  }

  /**
   * Get the actual route.
   *
   * @return the {@link net.sf.appia.core.Session Sessions} array that constitutes the route.
   * <b>It should be READ-ONLY</i>
   */
  public Session[] getRoute() {
    return route;
  }

  /**
   * Get the {@link java.lang.Class Class} of the routed {@link net.sf.appia.core.Event Event}
   * @return the {@link java.lang.Class Class} of the {@link net.sf.appia.core.Event Event}
   */
  public Class getEventType() {
    return eventType;
  }
}