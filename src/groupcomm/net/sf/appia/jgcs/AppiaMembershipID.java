/**
 * APPIA implementation of JGCS - Group Communication Service
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
 * Initial developer(s): Nuno Carvalho.
 * 
 *  * Contact
 * 	Address:
 * 		LASIGE, Departamento de Informatica, Bloco C6
 * 		Faculdade de Ciencias, Universidade de Lisboa
 * 		Campo Grande, 1749-016 Lisboa
 * 		Portugal
 * 	Email:
 * 		jgcs@lasige.di.fc.ul.pt
 */
 
package net.sf.appia.jgcs;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import net.sf.appia.protocols.group.Endpt;
import net.sf.appia.protocols.group.ViewID;
import net.sf.jgcs.JGCSException;
import net.sf.jgcs.membership.MembershipID;


public class AppiaMembershipID extends ViewID implements MembershipID {

	private static final long serialVersionUID = -8971339993160631028L;

	public AppiaMembershipID(long ltime, Endpt coord) {
		super(ltime, coord);
	}

	public AppiaMembershipID() {
		super();
	}

	public int compareTo(Object o) {
		if(o instanceof AppiaMembershipID){
			ViewID id = (ViewID) o;
			if(this.equals(id))
				return 0;
			//FIXME: is this correct?
			if(ltime < id.ltime)
				return -1;
			else return 1;
		}
		else 
			throw new ClassCastException("Could not compare with object "+o);
	}
	
	public byte[] getBytes() throws JGCSException{
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream out = null;
		try {
			out = new ObjectOutputStream(byteStream);
			super.writeExternal(out);
			out.close();
		} catch (IOException e) {
			throw new JGCSException("Could not write to output stream", e);
		}
		return byteStream.toByteArray();
	}

	public void fromBytes(byte[] bytes) throws JGCSException{
		ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
		ObjectInputStream in = null;
		try {
			in = new ObjectInputStream(byteStream);
			super.readExternal(in);
			in.close();
		} catch (IOException e) {
			throw new JGCSException("Could not read from input stream", e);
		} catch (ClassNotFoundException e) {
			throw new JGCSException("Could not read from input stream", e);
		}
	}

}
