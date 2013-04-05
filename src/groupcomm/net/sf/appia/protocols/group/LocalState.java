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

import java.util.Arrays;

/**
 * Local values corresponding to the current
 * {@link net.sf.appia.protocols.group.ViewState ViewState}.
 * <br>
 * This class contains some values communly used. The values can be obtained
 * from the {@link net.sf.appia.protocols.group.ViewState ViewState}, but because they
 * are often used, we optimize by providing them in advance.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.ViewState
 */
public class LocalState {

  /**
   * The corresponding {@link net.sf.appia.protocols.group.ViewState ViewState}.
   */
  public ViewState vs;

  // THE FOLLOWING FIELDS ARE SUPPOSED TO BE READ-ONLY

  /**
   * An array of the failed members of the current <i>view</i>.
   */
  public boolean[] failed;
  /**
   * My rank in the current <i>view</i>.
   */
  public int my_rank;
  /**
   * The rank of the current coordinator of the current <i>view</i>.
   */
  public int coord;
  /**
   * Am I the current coordinator of the current <i>view</i>.
   */
  public boolean am_coord;

  /**
   * Constructs a <i>LocalState</i> from the given
   * {@link net.sf.appia.protocols.group.ViewState ViewState}.
   *
   * @param vs the {@link net.sf.appia.protocols.group.ViewState ViewState}
   * @param my_endpt my {@link net.sf.appia.protocols.group.Endpt Endpt}
   */
  public LocalState(ViewState vs, Endpt my_endpt) {
    this.vs=vs;

    failed=new boolean[vs.view.length];
    Arrays.fill(failed,false);

    my_rank=vs.getRank(my_endpt);

    coord=0;

    am_coord= (my_rank == coord);
  }

  /**
   * Marks a member as failed, and if necessary recomputes the
   * {@link net.sf.appia.protocols.group.LocalState#coord coord} and
   * {@link net.sf.appia.protocols.group.LocalState#am_coord am_coord} fields.
   *
   * @param rank the rank of the failed member
   */
  public void fail(int rank) {
    failed[rank]=true;

    if (failed[coord] == true) {
      int i;
      for (i=coord ; (i < failed.length) && failed[i] ; i++) ;
      if (i < failed.length) {
        coord=i;
      } else {
        throw new AppiaGroupError("No Coordinator !!!!!!");
      }

      am_coord= (my_rank == coord);
    }
  }

  /**
   * Creates a {@link java.lang.String String} representation of the
   * <i>LocalState</i>.
   *
   * @return the {@link java.lang.String String} representation
   */
  public String toString() {
    String s;
    int i;

    s="\nam_coord: "+am_coord+"\ncoord: "+coord+"\nmy_rank: "+my_rank+"\nfailed: [";
    for (i=0 ; i < failed.length ; i++)
        s=s+failed[i]+",";
    s=s+"]\n";

    return s;
  }

  // The following methods are available for future multi-thread support.
  // Now they aren't used.

  /**
   * <b>For multi-thread support. NOT USED</b>
   */
  public synchronized boolean failed(int i) {
    return failed[i];
  }

  /**
   * <b>For multi-thread support. NOT USED</b>
   */
  public synchronized void setFailed(int i) {
    failed[i]=true;
  }

  /**
   * <b>For multi-thread support. NOT USED</b>
   */
  public synchronized int coord() {
    return coord;
  }

  /**
   * <b>For multi-thread support. NOT USED</b>
   */
  public synchronized void setCoord(int i) {
    coord=i;
  }

  /**
   * <b>For multi-thread support. NOT USED</b>
   */
  public synchronized boolean am_coord() {
    return (coord == my_rank);
  }
}