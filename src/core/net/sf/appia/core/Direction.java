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
 * The direction an {@link net.sf.appia.core.Event Event} will take.
 * <br>
 * It can take two possible values,
 * {@link net.sf.appia.core.Direction#UP UP} or {@link net.sf.appia.core.Direction#DOWN DOWN}.
 *
 * @author Alexandre Pinto
 * @version 1.0
 * @see net.sf.appia.core.Event
 */
public final class Direction {

	private Direction(){}
	
  /**
   * The direction value of an ascending
   * {@link net.sf.appia.core.Event Event}.
   */
  public static final int UP = +1;
  /**
   * The direction value of a descending
   * {@link net.sf.appia.core.Event Event}.
   */
  public static final int DOWN = -1;

  /**
   * Gets the reverse direction.
   * @return the reverse direction.
   */
  public static int invert(int dir) {
    if (dir == Direction.UP)
      return Direction.DOWN;
    if (dir == Direction.DOWN)
      return Direction.UP;
    return dir;
  }
}