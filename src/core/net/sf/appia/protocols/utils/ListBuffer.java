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

import java.util.*;

/**
 *
 */
public class ListBuffer<T> implements Buffer<T> {

    private LinkedList<T> list;
    
    public ListBuffer(){
	this.list = new LinkedList<T>();
    }

    /**
     * inserts a message at the end of the buffer
     */
    public void insertTail(T o){
	list.addLast(o);
    }

    /**
     * gets the most recent message of the buffer
     */
    public T getTail(){
	return list.getLast();
    }

    /**
     * removes a message from the head of the buffer.
     * return null if there are no messages.
     */
    public T removeHead(){
	return list.removeFirst();
    }

    /**
     * removes a specified object from the buffer.
     */
    public boolean remove(T o){
	return list.remove(o);
    }

    /**
     * gets a iterator of the buffer
     */
    public ListIterator<T> getIterator(int index){
	return list.listIterator(index);
    }

    /**
     * gets the size of the buffer (number of messages)
     */
    public int size(){
	return list.size();
    }

    /**
     * gets all elements of the buffer in a array format
     */
    public Object[] toArray(){
	return list.toArray();
    }

}
