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
 package net.sf.appia.protocols.utils;

import java.util.ListIterator;

/**
 *
 */
public interface Buffer<T> {
    
    /**
     * inserts a message at the end of the buffer
     */
    public void insertTail(T o);

    /**
     * gets the most recent message of the buffer
     */
    public T getTail();

    /**
     * removes a message from the head of the buffer.
     * return null if there are no messages.
     */
    public T removeHead();

    /**
     * removes a specified object from the buffer.
     */
    public boolean remove(T o);

    /**
     * gets a iterator of the buffer
     */
    public ListIterator<T> getIterator(int index);

    /**
     * gets the size of the buffer (number of messages)
     */
    public int size();

    /**
     * gets all elements of the buffer in a array format.
     */
    public Object[] toArray();
}
