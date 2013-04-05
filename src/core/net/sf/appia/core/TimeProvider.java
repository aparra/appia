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
 * Appia interface, that should be used by all Appia enteties to obtain the current time.
 */
public interface TimeProvider {
  
  /**
   * The current time in milliseconds, ie, the number of milliseconds since January 1st, 1970.
   * 
   * @return current time
   */
  public long currentTimeMillis();
  
  /**
   * The current time in microseconds, ie, the number of microseconds since January 1st, 1970.
   * 
   * @return current time
   */
  public long currentTimeMicros();
  
  /**
   * The current time in nanoseconds, ie, the number of nanoseconds since January 1st, 1970.
   * 
   * @return current time
   */
  public long nanoTime();
}
