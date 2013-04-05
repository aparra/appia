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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.ListIterator;

import net.sf.appia.core.message.Message;
import net.sf.appia.core.message.MessageException;

/**
 * A <i>view</i>.
 * <br>
 * The class encapsulates all the attributes of a <i>view</i>.
 * <br>
 * It also provides some methods to process the <i>view</i>.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.LocalState
 * @see net.sf.appia.protocols.group.intra.View
 */
public class ViewState implements Externalizable {
	
	private static final long serialVersionUID = 8901541793832881340L;
	
	/**
	 * The version of the <i>view</i>.
	 * <br>
	 * Currently ignored.
	 */
	public String version;
	/**
	 * The {@link net.sf.appia.protocols.group.Group Group} to which the <i>view</i>
	 * belongs.
	 */
	public Group group;
	/**
	 * The {@link net.sf.appia.protocols.group.ViewID ViewID} of the <i>view</i>.
	 */
	public ViewID id;
	/**
	 * The {@link net.sf.appia.protocols.group.ViewID ViewIDs} of the previous
	 * <i>views</i>.
	 */
	public ViewID[] previous;
	/**
	 * The view itself, the {@link net.sf.appia.protocols.group.Endpt Endpts} of the
	 * members.
	 */
	public Endpt[] view;
	/**
	 * The addresses of the members.
	 */
	public SocketAddress[] addresses;
	
	/**
	 * Calculates the rank of the given member
	 *
	 * @param endpt the {@link net.sf.appia.protocols.group.Endpt Endpt} of the member
	 * @return the rank of the member
	 */
	public int getRank(Endpt endpt) {
		int i;
		for(i=view.length-1 ; (i >= 0) && !endpt.equals(view[i]) ; i--);
		return i;
	}
	
	/**
	 * Calculates the rank of the member with the given address.
	 *
	 * @param address the address of the member
	 * @return the rank of the member
	 */
	public int getRankByAddress(InetSocketAddress address) {
		int i;
		for(i=addresses.length-1 ; (i >= 0) && !address.equals(addresses[i]) ; i--);
		return i;
	}
	
	public ViewState() {}
	
    /**
     * Constructs a <i>view</i>.
     *
     * @param version the version of the <i>view</i>
     * @param group the {@link net.sf.appia.protocols.group.Group Group} of the <i>view</i>
     * @param id the {@link net.sf.appia.protocols.group.ViewID ViewID} of the <i>view</i>
     * @param previous the {@link net.sf.appia.protocols.group.ViewID ViewIDs} of the previous
     * <i>views</i>
     * @param view the {@link net.sf.appia.protocols.group.Endpt Endpts} of the members
     * of the <i>view</i>
     * @param addresses the addresses of the members of the <i>view</i>
     * @throws NullPointerException if group or id or view or addresses are
     * <b>null</b>
     * @throws AppiaGroupException if the sizes of view and addresses are different
     */
    public ViewState(
            String version,
            Group group,
            ViewID id,
            ViewID[] previous,
            Endpt[] view,
            SocketAddress[] addresses) throws NullPointerException,AppiaGroupException {
        
        if ( (group==null) || (id==null) || (view==null) || (addresses==null) )
            throw new NullPointerException("appia:group:ViewState: group or view_id or view or addresses");
        
        if ( view.length != addresses.length )
            throw new AppiaGroupException("ViewState: view.length != addresses.length");
        
        this.version=version;
        this.group=group;
        this.id=id;
        this.previous=previous;
        this.view=view;
        this.addresses=addresses;
    }

	/**
	 * Creates a {@link java.lang.String String} representation of the <i>view</i>.
	 *
	 * @return the {@link java.lang.String String} representation
	 */
	public String toString() {
		String s="";
		int i;
		
		s="\nversion: "+version+"\ngroup: "+group.toString()+"\nid: "+id.toString();
		s=s+"\nprevious: [";
		for (i=0 ; i < previous.length ; i++) s=s+previous[i].toString()+",";
		s=s+"]\nview: [";
		for (i=0 ; i < view.length ; i++) s=s+view[i].toString()+",";
		s=s+"]\naddresses: [";
		for (i=0 ; i < addresses.length ; i++)
			s=s+addresses[i].toString()+",";
		s=s+"]\n";
		
		return s;
	}
	
	/**
	 * Creates the next ViewState.
	 * <br>
	 * The new ViewState mantains the same
	 * {@link net.sf.appia.protocols.group.ViewState#view view} and
	 * {@link net.sf.appia.protocols.group.ViewState#addresses addresses}.
	 * <br>
	 * The ViewID is generated using the <i>ViewID.next(...)</i> method.
	 *
	 * @param coord the Endpoint id of the coordinator of the new ViewState
	 * @return the next ViewState
	 */
	public ViewState next(Endpt coord) throws NullPointerException,AppiaGroupException {
		if (coord == null)
			throw new NullPointerException("coord");
		
		ViewID new_id=id.next(coord);
		ViewID[] new_previous={id};
		
		return new ViewState(version,group,new_id,new_previous,view,addresses);
	}
	
	/**
	 * Removes the indicated members from the current ViewState.
	 * <br>
	 * The members whose index is at true in the remove array, are removed
	 * from the ViewState.
	 *
	 * @param remove the members to remove
	 */
	public void remove(boolean[] remove) {
		if (remove.length != view.length)
			throw new IllegalArgumentException("different sizes");
		
		int i,j,size;
		
		size=0;
		for(i=0 ; i < remove.length ; i++) {
			if (!remove[i])
				size++;
		}
		
		Endpt[] new_view=new Endpt[size];
		SocketAddress[] new_addrs=new SocketAddress[size];
		
		j=0;
		for(i=0 ; i < remove.length ; i++) {
			if (!remove[i]) {
				new_view[j]=view[i];
				new_addrs[j]=addresses[i];
				j++;
			}
		}
		
		view=new_view;
		addresses=new_addrs;
		if (new_view.length > 0)
			id.coord=new_view[0];
	}
	
	/**
	 * Merges the view states contained in the given list into a single view.
	 * <br>
	 * The view is the concatenation of the different views, according to the order provided by the list itself.
	 * The version is the smallest all the versions.
	 * 
	 * @param l A list containing the view states to merge
	 * @return the merged view state.
	 * @throws AppiaGroupException See {@linkplain ViewState#ViewState(String, Group, ViewID, ViewID[], Endpt[], SocketAddress[])}
	 * @throws NullPointerException See {@linkplain ViewState#ViewState(String, Group, ViewID, ViewID[], Endpt[], SocketAddress[])}
	 */
	public static ViewState merge(List l) throws NullPointerException, AppiaGroupException {
		ListIterator iter=l.listIterator(l.size());
		int viewsize=0;
		int prevsize=0;
		while (iter.hasPrevious()) {
			ViewState aux=(ViewState)iter.previous();
			viewsize+=aux.view.length;
			prevsize+=aux.previous.length;
		}
		
		String v=null;
		Group g=null;
		ViewID vid=null;
		ViewID[] prevs=new ViewID[prevsize];
		Endpt[] endpts=new Endpt[viewsize];
		InetSocketAddress[] addrs=new InetSocketAddress[viewsize];
		int iprevs=0,iendpts=0,iaddrs=0;
		
		while (iter.hasNext()) {
			ViewState aux=(ViewState)iter.next();
			if ((v == null) || (aux.version.compareTo(v) < 0))
				v=aux.version;
			if (g == null)
				g=aux.group;
			if (vid == null)
				vid=aux.id;
			else
				if (aux.id.ltime > vid.ltime)
					vid.ltime=aux.id.ltime;
			System.arraycopy(aux.previous, 0, prevs, iprevs, aux.previous.length);
			iprevs+=aux.previous.length;
			System.arraycopy(aux.view, 0, endpts, iendpts, aux.view.length);
			iendpts+=aux.view.length;
			System.arraycopy(aux.addresses, 0, addrs, iaddrs, aux.addresses.length);
			iaddrs+=aux.addresses.length;
		}
		
		return new ViewState(v,g,vid,prevs,endpts,addrs);
	}
	
	/**
	 * Merges the given ViewState to the current ViewState.
	 *
	 * @param vs the ViewState to merge with the current one
	 */
	public void merge(ViewState vs) {
		int size=view.length+vs.view.length;
		
		Endpt[] new_view=new Endpt[size];
		InetSocketAddress[] new_addrs=new InetSocketAddress[size];
		
		System.arraycopy(view,0,new_view,0,view.length);
		System.arraycopy(addresses,0,new_addrs,0,addresses.length);
		
		System.arraycopy(vs.view,0,new_view,view.length,vs.view.length);
		System.arraycopy(vs.addresses,0,new_addrs,addresses.length,vs.addresses.length);
		
		ViewID[] new_previous=new ViewID[previous.length+vs.previous.length];
		System.arraycopy(previous,0,new_previous,0,previous.length);
		System.arraycopy(vs.previous,0,new_previous,previous.length,vs.previous.length);
		
		view=new_view;
		addresses=new_addrs;
		previous=new_previous;
		id.ltime=Math.max(id.ltime,vs.id.ltime);
	}
	
	
	
	/**
	 * Compares two  ViewState  and returns the
	 * endpoints that were lost during the view change.
	 * @param v The ViewState to be compared.
	 * @return Array of lost EndPt.
	 */
	public Endpt[] getDeadMembers(ViewState v){
		int current=0;
		Endpt[] aux = new Endpt[v.view.length];
		Endpt[] deadEnd=null;
		boolean found;
		
		for(int i=0; i!=v.view.length; i++){
			found = false;
			for(int j=0; j!=view.length ; j++)
				if(v.view[i].equals(view[j]))
					found = true;
			
			if(!found) //doesn't exist in the current view.
				aux[current++] = v.view[i];
		}
		
		deadEnd = new Endpt[current];
		System.arraycopy(aux, 0, deadEnd, 0, deadEnd.length);
		
		return deadEnd;
	}
	
	/**
	 * Compares the current ViewState with the given ViewState and returns the
	 * endpoints that
	 * are new in this view.
	 * @param v The ViewState to be compared.
	 * @return Array of new EndPt.
	 */
	public Endpt[] getNewMembers(ViewState v){
		int current=0;
		Endpt[] aux = new Endpt[view.length];
		Endpt[] newEnd=null;
		boolean found;
		
		for(int i=0; i!=view.length; i++){
			found = false;
			for(int j=0; j!=v.view.length ; j++)
				if(view[i].equals(v.view[j]))
					found = true;
			
			if(!found) //doesn't exist in the given view.
				aux[current++] = view[i];
		}
		
		newEnd = new Endpt[current];
		System.arraycopy(aux, 0, newEnd, 0, newEnd.length);
		
		return newEnd;
	}
	
	
	/**
	 * Compares the current ViewState with the given ViewState and returns the
	 * endpoints that are in both views.
	 * @param v The ViewState to be compared.
	 * @return Array of the common EndPt.
	 */
	public Endpt[] getSurvivingMembers(ViewState v){
		int current=0;
		Endpt[] aux = new Endpt[v.view.length];
		Endpt[] survivorEnd=null;
		boolean found;
		
		for(int i=0; i!=view.length; i++){
			found = false;
			for(int j=0; j!=v.view.length ; j++)
				if(view[i].equals(v.view[j]))
					found = true;
			
			if(found) //exists in both views
				aux[current++] = view[i];
		}
		
		survivorEnd = new Endpt[current];
		System.arraycopy(aux, 0, survivorEnd, 0, survivorEnd.length);
		
		return survivorEnd;
	}
	
	public static void push( ViewState vs, Message message) {
		ArrayOptimized.pushArrayInetWithPort(vs.addresses,message);
		ArrayOptimized.pushArrayEndpt(vs.view,message);
		ArrayOptimized.pushArrayViewID(vs.previous,message);
		ViewID.push(vs.id,message);
		Group.push(vs.group,message);
		message.pushString(vs.version);
	}
	
	public static ViewState pop(Message message) {
		try {
			return new ViewState(
					message.popString(),
					Group.pop(message),
					ViewID.pop(message),
					ArrayOptimized.popArrayViewID(message),
					ArrayOptimized.popArrayEndpt(message),
					ArrayOptimized.popArrayInetWithPort(message));
		} catch (AppiaGroupException ex) {
			throw new MessageException("Error poping view state.",ex);
		}
	}
	
	public static ViewState peek(Message message) {
	    ViewState vs=ViewState.pop(message);
	    ViewState.push(vs,message);
	    return vs;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
//		public String version;
		byte[] bytes = version.getBytes();
		out.writeInt(bytes.length);
		out.write(bytes);		
//		public Group group;
		out.writeObject(group);
//		public ViewID id;
		out.writeObject(id);
//		public ViewID[] previous;
		out.writeInt(previous.length);
		for(int i=0; i<previous.length; i++)
			out.writeObject(previous[i]);
//		public Endpt[] view;
		out.writeInt(view.length);
		for(int i=0; i<view.length; i++)
			out.writeObject(view[i]);
//		public InetWithPort[] addresses;
		out.writeInt(addresses.length);
		for(int i=0; i<addresses.length; i++){
			bytes = ((InetSocketAddress)addresses[i]).getAddress().getAddress();
			out.writeInt(bytes.length);
			out.write(bytes);
			out.writeInt(((InetSocketAddress)addresses[i]).getPort());
		}
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
//		public String version;
		int len = in.readInt();
		byte[] bytes = new byte[len];
		in.read(bytes);
		version = new String(bytes);
//		public Group group;
		group = (Group) in.readObject();
//		public ViewID id;
		id = (ViewID) in.readObject();
//		public ViewID[] previous;
		len = in.readInt();
		previous = new ViewID[len];
		for(int i=0; i<len; i++)
			previous[i] = (ViewID) in.readObject();
//		public Endpt[] view;
		len = in.readInt();
		view = new Endpt[len];
		for(int i=0; i<len; i++)
			view[i] = (Endpt) in.readObject();
//		public InetWithPort[] addresses;
		len = in.readInt();
		int addrLen = 0;
		addresses = new InetSocketAddress[len];
		for(int i=0; i<len; i++){
			addrLen = in.readInt();
			bytes = new byte[addrLen];
			in.read(bytes);
            addresses[i] = new InetSocketAddress(InetAddress.getByAddress(bytes),in.readInt());
		}
	}
}
