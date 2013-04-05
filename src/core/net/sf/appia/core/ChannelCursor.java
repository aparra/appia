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
 * A <i>ChannelCursor</i> is the way of accessing the
 * {@link net.sf.appia.core.Channel Channel} {@link net.sf.appia.core.Session Sessions} stack.
 * <br>
 * With the <i>ChannelCursor</i> we can access any position in the stack, and
 * if desired we can set the {@link net.sf.appia.core.Session Session} for that position.
 * <br>
 * It is mainly used to create a stack with specific
 * {@link net.sf.appia.core.Session Sessions}, in particular to allow a
 * {@link net.sf.appia.core.Session Session} to belong to more than one
 * {@link net.sf.appia.core.Channel Channel}.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.core.AppiaCursorException
 */
public class ChannelCursor {

  private static final int NOTSET=-2;

  Session[] sessions;
  Layer[] layers;
  Channel channel;
  int pos=NOTSET;

  /**
   * Constructs a <i>ChannelCursor</i> for the specified
   * {@link net.sf.appia.core.Channel Channel}
   *
   * @param channel the cursors {@link net.sf.appia.core.Channel Channel}
   */
  public ChannelCursor(Channel channel) {
    this.channel=channel;
    this.sessions=channel.sessions;
    this.layers=channel.getQoS().layers;
  }

  /**
   * Go to the {@link net.sf.appia.core.Session Session} imediatly below in the
   * {@link net.sf.appia.core.Channel Channel} stack.
   *
   * @throws AppiaCursorException with two possible
   * {@link net.sf.appia.core.AppiaCursorException#type types}:
   * {@link net.sf.appia.core.AppiaCursorException#CURSORNOTSET CURSORNOTSET},
   * {@link net.sf.appia.core.AppiaCursorException#CURSORONBOTTOM CURSORONBOTTOM}
   */
  public void down() throws AppiaCursorException {
    if (pos == NOTSET)
       throw new AppiaCursorException(AppiaCursorException.CURSORNOTSET);

    if (pos < 0)
       throw new AppiaCursorException(AppiaCursorException.CURSORONBOTTOM);

    --pos;
  }

  /**
   * Go to the {@link net.sf.appia.core.Session Session} imediatly above in the
   * {@link net.sf.appia.core.Channel Channel} stack.
   *
   * @throws AppiaCursorException with two possible
   * {@link net.sf.appia.core.AppiaCursorException#type types}:
   * {@link net.sf.appia.core.AppiaCursorException#CURSORNOTSET CURSORNOTSET},
   * {@link net.sf.appia.core.AppiaCursorException#CURSORONTOP CURSORONTOP}
   */
  public void up() throws AppiaCursorException {
    if (pos == NOTSET)
       throw new AppiaCursorException(AppiaCursorException.CURSORNOTSET);

    if (pos >= sessions.length)
       throw new AppiaCursorException(AppiaCursorException.CURSORONTOP);

    ++pos;
  }

  /**
   * Go to the highest {@link net.sf.appia.core.Session Session} in the
   * {@link net.sf.appia.core.Channel Channel} stack.
   */
  public void top() {
    pos=sessions.length-1;
  }

  /**
   * Go to the lowest {@link net.sf.appia.core.Session Session} in the
   * {@link net.sf.appia.core.Channel Channel} stack.
   */
  public void bottom() {
    pos=0;
  }

  /**
   * Is the cursor in a valid position, is it set ?
   *
   * @return <i>true</i> if the cursor is within the satck, <i>false</i> otherwise
   */
  public boolean isPositioned() {
    return (pos >= 0) && (pos < sessions.length);
  }

  /**
   * Set the {@link net.sf.appia.core.Session Session} for the current position in the
   * {@link net.sf.appia.core.Channel Channel} stack.
   *
   * @param session the {@link net.sf.appia.core.Session Session} to set
   * @throws AppiaCursorException with three possible
   * {@link net.sf.appia.core.AppiaCursorException#type types}:
   * {@link net.sf.appia.core.AppiaCursorException#CURSORNOTSET CURSORNOTSET},
   * {@link net.sf.appia.core.AppiaCursorException#ALREADYSET ALREADYSET},
   * {@link net.sf.appia.core.AppiaCursorException#WRONGLAYER WRONGLAYER},
   */
  public void setSession(Session session) throws AppiaCursorException {
    if (pos == NOTSET)
       throw new AppiaCursorException(AppiaCursorException.CURSORNOTSET);

    if (sessions[pos] != null)
       throw new AppiaCursorException(AppiaCursorException.ALREADYSET,"Session has already been Set");

    if (layers[pos].getClass() != session.getLayer().getClass())
       throw new AppiaCursorException(AppiaCursorException.WRONGLAYER,"Wrong Session Layer");

    sessions[pos]=session;
  }

  /**
   * Get the current {@link net.sf.appia.core.Session Session} in the
   * {@link net.sf.appia.core.Channel Channel} stack.
   *
   * @return the current {@link net.sf.appia.core.Session Session}, <i>null</i> if no
   * {@link net.sf.appia.core.Session Session} has been set for the current position
   * @throws AppiaCursorException with three possible
   * {@link net.sf.appia.core.AppiaCursorException#type types}:
   * {@link net.sf.appia.core.AppiaCursorException#CURSORNOTSET CURSORNOTSET},
   * {@link net.sf.appia.core.AppiaCursorException#CURSORONBOTTOM CURSORONBOTTOM},
   * {@link net.sf.appia.core.AppiaCursorException#CURSORONTOP CURSORONTOP}
   */
  public Session getSession() throws AppiaCursorException {
    if (pos == NOTSET)
       throw new AppiaCursorException(AppiaCursorException.CURSORNOTSET);

    if (pos < 0)
       throw new AppiaCursorException(AppiaCursorException.CURSORONBOTTOM);

    if (pos >= sessions.length)
       throw new AppiaCursorException(AppiaCursorException.CURSORONTOP);

    return sessions[pos];
  }

  /**
   * Get the {@link net.sf.appia.core.Layer Layer} of the {@link net.sf.appia.core.QoS QoS} stack, that
   * corresponds to the current position in the {@link net.sf.appia.core.Channel Channel}
   * stack.
   *
   * @return layer the {@link net.sf.appia.core.Layer Layer} corresponding to the current
   * position
   * @throws AppiaCursorException with one possible
   * {@link net.sf.appia.core.AppiaCursorException#type type}:
   * {@link net.sf.appia.core.AppiaCursorException#CURSORNOTSET CURSORNOTSET}
   */
  public Layer getLayer() throws AppiaCursorException {
    if (pos<0)
       throw new AppiaCursorException(AppiaCursorException.CURSORNOTSET);

    return layers[pos];
  }

  /**
   * Jumps a number of Layers. If the <i>offset</i> is positive then it will
   * jump upwards. Otherwise it will jump downwards.
   *
   * @throws AppiaCursorException with two possible
   * {@link net.sf.appia.core.AppiaCursorException#type type}:
   * {@link net.sf.appia.core.AppiaCursorException#CURSORNOTSET CURSORNOTSET},
   * {@link net.sf.appia.core.AppiaCursorException#INVALIDPOSITION INVALIDPOSITION}
   */
  public void jump(int offset) throws AppiaCursorException {
    if (pos == NOTSET)
      throw new AppiaCursorException(AppiaCursorException.CURSORNOTSET);

    offset+=pos;
    if ((offset < 0) || (offset >= sessions.length))
      throw new AppiaCursorException(AppiaCursorException.INVALIDPOSITION);

    pos=offset;
  }

  /**
   * Jumps to a specified position in the Channel stack.
   *
   * @throws AppiaCursorException with two possible
   * {@link net.sf.appia.core.AppiaCursorException#type type}:
   * {@link net.sf.appia.core.AppiaCursorException#CURSORNOTSET CURSORNOTSET},
   * {@link net.sf.appia.core.AppiaCursorException#INVALIDPOSITION INVALIDPOSITION}
   */
  public void jumpTo(int position) throws AppiaCursorException {
    if (pos == NOTSET)
      throw new AppiaCursorException(AppiaCursorException.CURSORNOTSET);

    if ((position < 0) || (position >= sessions.length))
      throw new AppiaCursorException(AppiaCursorException.INVALIDPOSITION);

    pos=position;
  }
}