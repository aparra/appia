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

import net.sf.appia.core.message.Message;
import net.sf.appia.core.message.MessageFactory;

/**
 * This class defines a MessageFactoryImpl
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class MessageFactoryImpl implements MessageFactory {

	/* (non-Javadoc)
	 * @see net.sf.appia.core.message.MessageFactory#newMessage()
	 */
	public Message newMessage() {
		return new AppiaMessage();
	}

	/* (non-Javadoc)
	 * @see net.sf.appia.core.message.MessageFactory#newMessage(byte[], int, int)
	 */
	public Message newMessage(byte[] payload, int offset, int length) {
		return new AppiaMessage(payload, offset, length);
	}

}
