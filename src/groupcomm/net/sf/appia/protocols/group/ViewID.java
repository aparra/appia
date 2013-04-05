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

import net.sf.appia.core.message.Message;

/**
 * The unique identifier of a {@link net.sf.appia.protocols.group.ViewState ViewState}.
 * <br>
 * It is constructed from the <i>coordinator</i> and <i>logical time</i> of the
 * <i>view</i>.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.ViewState
 */
public class ViewID implements Externalizable {

  // WARNING: these attributes should be READ ONLY

  private static final long serialVersionUID = -7452984900144476889L;

  /**
   * The logical time of the view.
   */
  public long ltime;
  /**
   * The {@link net.sf.appia.protocols.group.Endpt Endpt} of the coordinator of the
   * view.
   */
  public Endpt coord;

  /**
   * Constructs a new empty <i>ViewID</i>. Needed for Externalizable interface.
   *
   */
  public ViewID() {}
  
  /**
   * Constructs a new <i>ViewID</i>.
   *
   * @param coord the coordinator of the <i>view</i>
   * @param ltime the logical time of the <i>view</i>
   */
  public ViewID(long ltime, Endpt coord) {
    this.ltime=ltime;
    this.coord=coord;
    hashcode=coord.hashCode() ^ (int)((ltime * 0x5DEECE66DL + 0xBL) & ((1L << 48)-1));    
  }

  /**
   * Constructs a new <i>ViewID</i> that follows the current <i>ViewID</i>.
   *
   * @return the new <i>ViewID</i>
   */
  public ViewID next() {
    return new ViewID(ltime+1,coord);
  }

  /**
   * Constructs a new <i>ViewID</i> that follows the current <i>ViewID</i>, but
   * there is a different coordinator.
   *
   * @param new_coord the coordinator of the <i>view</i>
   * @return the new <i>ViewID</i>
   */
  public ViewID next(Endpt new_coord) {
    return new ViewID(ltime+1,new_coord);
  }

  /**
   * Constructs a new <i>ViewID</i> that follows the current and the given
   * <i>ViewID</i>s. The coordinator is the given one.
   *
   * @return the new <i>ViewID</i>
   * @param new_coord The new view coordinator
   * @param view_id the other <i>ViewID</i>
   */
  public ViewID merge(ViewID view_id, Endpt new_coord) {
    return new ViewID(Math.max(view_id.ltime,ltime)+1,new_coord);
  }

  /**
   * Compares if the given <i>ViewID</i> is equal to the current one.
   *
   * @param vid the other <i>ViewID</i> to compare to
   * @return true if the <i>ViewID</i>s are equal, false otherwise
   */
  public boolean equals(ViewID vid) {
    return (ltime == vid.ltime) && (coord.equals(vid.coord));
  }

  /**
   * Redefines {@link java.lang.Object#equals Object.equals()}.
   * @param o The object to compare with.
   * @return True if this and the given object represent the same ViewID.
   */
  public boolean equals(Object o) {
    return (o instanceof ViewID) && (ltime == ((ViewID)o).ltime) && (coord.equals(((ViewID)o).coord));
  }

  /**
   * Creates a {@link java.lang.String String} representation of the
   * <i>ViewID</i>.
   *
   * @return the {@link java.lang.String String} representation
   */
  public String toString() {
    return "[ViewID:"+ltime+";"+coord.toString()+"]";
  }

  /**
   * Redefines {@link java.lang.Object#hashCode Object.hashCode()}.
   * <br>
   * The hashcode is equal to 
   * <i>coordinator_hashcode ^ ((logical_time * 0x5DEECE66DL + 0xBL) & ((1L << 48)-1));</i>.
   * This is based on java.math.Random that was based in
   * a linear congruential pseudorandom number generator, as 
   * defined by D. H. Lehmer and described by Donald E. Knuth in <i>The 
   * Art of Computer Programming,</i> Volume 2: <i>Seminumerical 
   * Algorithms</i>, section 3.2.1.
   * 
   * @return hashcode for this ViewID
   */
  public int hashCode() {
    return hashcode;
  }

  private int hashcode=0; // coord.hashCode() ^ ((ltime * 0x5DEECE66DL + 0xBL) & ((1L << 48)-1));
  
  public static void push(ViewID viewid, Message message)
  {
    Endpt.push(viewid.coord,message);
    message.pushLong(viewid.ltime);
  }

  public static ViewID pop(Message message)
  {
    return new ViewID(message.popLong(),Endpt.pop(message));
  }

    public static ViewID peek(Message message)
  {
    long l=message.popLong();
    ViewID view=new ViewID(l,Endpt.peek(message));
    message.pushLong(l);
    return view;
  }

	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeLong(ltime);
		coord.writeExternal(out);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		ltime = in.readLong();
		coord = new Endpt();
		coord.readExternal(in);
	    hashcode=coord.hashCode() ^ (int)((ltime * 0x5DEECE66DL + 0xBL) & ((1L << 48)-1));
	}

}