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
 package net.sf.appia.core.events;

/**
 * Multicast destination of a SendableEvent.
 * <br>
 * If a SendableEvent is intended to be sent in multicast, an instance of this
 * class should be used as the value of the
 * {@link net.sf.appia.core.events.SendableEvent#dest "dest" field}.
 * <br>
 * It contains a multicast address that will be used as the address to where
 * the message will be sent. It also contains an array of destination addresses.
 * These addresses are intended to be used by those protocols that need to
 * know who is suposed to receive the message, for instance a reliability
 * protocol.
 * <br>
 * If there is no multicast address, then the destination addresses may be used
 * to send the message.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.events.SendableEvent
 */
public class AppiaMulticast {

  private Object multicastAddress;
  private Object[] destinations;

  /**
   * Constructs a new AppiaMulticast destination.
   *
   * @param multicast The multicast address to send the message.
   *                  This may be null.
   * @param destinations The addresses of the intended receivers.
   * @exception NullPointerException If destinations is null.
   */
  public AppiaMulticast(Object multicast, Object[] destinations) {
    if (destinations == null)
      throw new NullPointerException("destinations is null");

    this.multicastAddress=multicast;
    this.destinations=destinations;
  }

  /**
   * Gets the multicast address to be used.
   * <br>
   * It may be null.
   *
   * @return The multicast address.
   */
  public Object getMulticastAddress() {
    return multicastAddress;
  }

  /**
   * Gets the address of the destinations, the expected receivers, of the
   * message.
   *
   * @return The destinations.
   */
  public Object[] getDestinations() {
    return destinations;
  }
}
