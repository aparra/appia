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
 /*
 * Created on 27/04/2004
 *
 */
package net.sf.appia.test.xml;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * 
 * Hash
 * 
 * @author Liliana Rosa
 *
 */
public class Hash implements Serializable {
	
	private static final long serialVersionUID = 6539203254869983019L;
	private byte[] hash;
	
	public Hash() {
		super();
	}
	
	/**
	 * 
	 * 
	 * @param in
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
		hash = (byte[])in.readObject();
	}
	
	/**
	 * 
	 * @param out
	 * @throws IOException
	 */
	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(hash);
		out.flush();        
	}
	
	/**
	 * 
	 * Sets an hash
	 * 
	 * @param md hash
	 */
	public void setHash(byte[] md){
		hash = md;
	}
	
	/**
	 * 
	 * Returns an hash
	 * 
	 * @return an hash
	 */
	public byte[] getHash(){
		return hash;
	}
}
