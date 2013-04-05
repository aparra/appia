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
 * <i>Endpt</i> represents a group member, <i>endpoint</i>.
 * <br>
 * Each member of a group, or groups, has a unique identifier.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.ViewState
 */
public class Endpt implements Externalizable {

  private static final long serialVersionUID = -3355596169573334939L;

  /**
   * The endpoint identifier.
   */
  public String id;

  /**
   * Constructs an anonimous endpoint.
   * <br>
   * The probability of two endpoints created using this constructor being equal,
   * is very small. The endpoint is created by combining the <i>localhost</i>,
   * the current time, and {@link java.lang.Object#hashCode Object.hashCode()}.
   */
  public Endpt() {
	  // FIXME: can System.currentTimeMillis be replaced by other thing?
	  // is this important (under simulation)?
    try {
      id= "Endpt:" +
          InetAddress.getLocalHost().getHostAddress() +
          ":" +
          System.currentTimeMillis() +
          ":" +
          super.hashCode();
    } catch (UnknownHostException e) {
      id= "Endpt:" +
          System.currentTimeMillis() +
          ":" +
          super.hashCode();
    }
  }

  /**
   * Constructs an endpoint using the given {@link java.lang.String String}.
   * @param name The name to be used as endpoint identifier.
   */
  public Endpt(String name) {
    id=name;
  }

  /**
   * Tests if the endpoint represented by the given <i>Endpt</i> is the same one
   * represented by this <i>Endpt</i>.
   *
   * @param e the other <i>Endpt</i>
   * @return true if this <i>Endpt</i> and the given <i>Endpt</i> represent the
   * same <i>endpoint</i>, false otherwise
   */
  public boolean equals(Endpt e) {
    return id.equals(e.id);
  }


  /**
   * Redefines {@link java.lang.Object#equals Object.equals()}.
   *
   * @param o The object to compare.
   * @return Returns true if this and the given object represent the same endpoint.
   */
  public boolean equals(Object o) {
    return (o instanceof Endpt) && id.equals(((Endpt)o).id);
  }

  /**
   * Converts the endpoint to a {@link java.lang.String String}.
   *
   * @return a {@link java.lang.String String} of the <i>endpoint</i>.
   */
  public String toString() {
    return "["+id+"]";
  }

  /**
   * Redefines {@link java.lang.Object#hashCode Object.hashCode()}.
   * <br>
   * The hashcode is equal to the identifier hashcode
   * @return hashcode for this Endpt
   */
  public int hashCode() {
    return id.hashCode();
  }


  public static void push(Endpt endpt, Message message)
  {

    message.pushString(endpt.id);
  }

    public static Endpt pop(Message message)
    {
        return new Endpt(message.popString());
    }

    public static Endpt peek(Message message)
    {
        return new Endpt(message.peekString());
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