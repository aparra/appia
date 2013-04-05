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
 * Initial developer(s): Nuno Carvalho.
 * Contributor(s): See Appia web page for a list of contributors.
 */

package net.sf.appia.jgcs;

import net.sf.jgcs.JGCSException;
import net.sf.jgcs.membership.MembershipID;
import net.sf.jgcs.utils.AbstractMarshallableSocketAddress;

public class AppiaMarshallable extends AbstractMarshallableSocketAddress {

	public AppiaMarshallable() {}

	public MembershipID getMembershipID(byte[] buffer)
			throws JGCSException {
		AppiaMembershipID id = new AppiaMembershipID();
		id.fromBytes(buffer);
		return id;
	}

	public byte[] getBytes(MembershipID id)
			throws JGCSException {
		if( ! (id instanceof AppiaMembershipID))
			throw new JGCSException("ID of type "+id.getClass().getName()+" is not supported.");
		AppiaMembershipID appiaID = (AppiaMembershipID) id;
		return appiaID.getBytes();
	}

}
