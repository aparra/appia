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

import java.net.SocketAddress;

import net.sf.appia.core.message.Message;

public class AppiaMessage extends Message implements net.sf.jgcs.Message, Cloneable {

	private SocketAddress senderAddress;
	
	public AppiaMessage() {
		super();
	}

	public AppiaMessage(byte[] payload, int off, int len) {
		super(payload,off,len);
	}
	
	public void setPayload(byte[] buffer) {
		this.setByteArray(buffer,0,buffer.length);
	}

	public byte[] getPayload() {
		return this.toByteArray();
	}

	public SocketAddress getSenderAddress() {
		return senderAddress;
	}
	
	public void setSenderAddress(SocketAddress sender) {
		senderAddress = sender;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
	    AppiaMessage m = (AppiaMessage)super.clone();
	    m.senderAddress = senderAddress;
	    return m;
	}
	
}
