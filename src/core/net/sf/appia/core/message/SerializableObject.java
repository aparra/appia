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


package net.sf.appia.core.message;

/**
 * Interface for read and write objects from/into the Message. These methods should be constructed using the message
 * primitives, in order to optimize the serializability.
 * 
 * @author nuno
 *
 */
public interface SerializableObject {

	/**
	 * Adds it self to the Message. It should get all the parameters needed to rebuild the object and push that into the message.
	 * @param m the message to self push.
	 */
	public void pushMySelf(Message m);
	
	/**
	 * Retrieves all pushed items from a Message and updates the state of the Object.
	 * @param m the Message used to retrieve the state of the object.
	 */
	public void popMySelf(Message m);
	
	/**
	 * Retrieves all pushed items from a Message and updates the state of the Object, without removing the data from the message.
	 * This should be used when the user wants to rebuild the object, but keep the data in the message for future usage.
	 * @param m the Message used to retrieve the state of the object.
	 */
	public void peekMySelf(Message m);
	
}
