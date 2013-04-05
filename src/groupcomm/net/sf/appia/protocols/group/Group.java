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
 
package net.sf.appia.protocols.group;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.InetAddress;
import java.net.UnknownHostException;

import net.sf.appia.core.message.Message;


/**
 * <i>Group</i> is a group identifier.
 * <br>
 * Every group has its unique identifier.
 *
 * @author Alexandre Pinto
 * @version 0.1
 */
public class Group implements Externalizable {

  private static final long serialVersionUID = -379704347167008367L;

  /**
   * The group identifier.
   */
  public String id;

  /**
   * Constructs an anonymous group identifier.
   * <br>
   * The probability of two group identifiers created using this constructor,
   * being equal, is very small.The group identifier is created by combining
   * the <i>localhost</i>, the current time, and
   * {@link java.lang.Object#hashCode Object.hashCode()}.
   */
  public Group() {
	  // FIXME: can System.currentTimeMillis be replaced by other thing?
	  // is this important (under simulation)?
    try {
      id= "Group:" +
          InetAddress.getLocalHost().getHostAddress() +
          ":" +
          System.currentTimeMillis() +
          ":" +
          super.hashCode();
    } catch (UnknownHostException e) {
      id= "Group:" +
          System.currentTimeMillis() +
          ":" +
          super.hashCode();
    }
  }

  /**
   * Constructs a group identifier using the given
   * {@link java.lang.String String}.
   * @param name The name to be used as Group identifier.
   */
  public Group(String name) {
    id=name;
  }

  /**
   * Tests if this group identifier represents the same group as the given
   * group identifier.
   *
   * @param g the other group identifier
   * @return true if this group identifier and the given group identifier are
   * equal.
   */
  public boolean equals(Group g) {
    return id.equals(g.id);
  }

  /**
   * Redefines {@link java.lang.Object#equals Object.equals()}.
   * @return true if this group identifier and the given group identifier are
   * equal.
   * @param o The object to compare.
   */
  public boolean equals(Object o) {
    return (o instanceof Group) && id.equals(((Group)o).id);
  }

  /**
   * Converts the group identifier into a {@link java.lang.String String}.
   *
   * @return a {@link java.lang.String String} of the group identifier
   */
  public String toString() {
    return "[Group:"+id+"]";
  }

  /**
   * Redefines {@link java.lang.Object#hashCode Object.hashCode()}.
   * <br>
   * The hashcode is equal to the identifier hashcode
   * @return hashcode for this Group
   */
  public int hashCode() {
    return id.hashCode();
  }


  public static void push(Group group, Message message)
  {
    message.pushString(group.id);
  }

  public static Group pop(Message message)
  {
    return new Group(message.popString());
  }

  public static Group peek(Message message)
  {
    return new Group(message.peekString());
  }

	public void writeExternal(ObjectOutput out) throws IOException {
		byte[] bytes = id.getBytes();
		out.writeInt(bytes.length);
		out.write(bytes);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		int len = in.readInt();
		byte[] bytes = new byte [len];
		in.read(bytes);
		id = new String(bytes);
	}

}