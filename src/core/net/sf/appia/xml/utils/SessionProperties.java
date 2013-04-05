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
 * Created on Mar 22, 2004
 *
 */
package net.sf.appia.xml.utils;

import java.util.Properties;

/**
 * This class extends Properties and facilitates 
 * the passage of parameters to sessions.
 * 
 * @author Jose Mocito
 */
public class SessionProperties extends Properties {
	
	private static final long serialVersionUID = 1898613189177874509L;

	/**
	 * Builds a SessionProperties object with no parameters associated.
	 *
	 */
	public SessionProperties() {
		super();
	}
	
	/**
	 * Returns the parameter specified by key as a boolean value.
	 * 
	 * @param key the name of the parameter.
	 * @return the parameter specified by key as a boolean value.
	 */
	public boolean getBoolean(String key) {
		return getProperty(key).equals("true");
	}
	
	/**
	 * Returns the parameter specified by key as a byte value.
	 * 
	 * @param key the name of the parameter.
	 * @return the parameter specified by key as a byte value.
	 */
	public byte getByte(String key) {
		return Byte.parseByte(getProperty(key));
	}
	
	/**
	 * Returns the parameter specified by key as a short value.
	 * 
	 * @param key the name of the parameter.
	 * @return the parameter specified by key as a short value.
	 */
	public short getShort(String key) {
		return Short.parseShort(getProperty(key));
	}
	
	/**
	 * Returns the parameter specified by key as an int value.
	 * 
	 * @param key the name of the parameter.
	 * @return the parameter specified by key as an int value.
	 */
	public int getInt(String key) {
		return Integer.parseInt(getProperty(key));
	}
	
	/**
	 * Returns the parameter specified by key as a long value.
	 * 
	 * @param key the name of the parameter.
	 * @return the parameter specified by key as a long value.
	 */
	public long getLong(String key) {
		return Long.parseLong(getProperty(key));
	}
	
	/**
	 * Returns the parameter specified by key as a float value.
	 * 
	 * @param key the name of the parameter.
	 * @return the parameter specified by key as a float value.
	 */
	public float getFloat(String key) {
		return Float.parseFloat(getProperty(key));
	}
	
	/**
	 * Returns the parameter specified by key as a double value.
	 * 
	 * @param key the name of the parameter.
	 * @return the parameter specified by key as a double value.
	 */
	public double getDouble(String key) {
		return Double.parseDouble(getProperty(key));
	}
	
	/**
	 * Returns the parameter specified by key as a String.
	 * 
	 * @param key the name of the parameter.
	 * @return the parameter specified by key as a String.
	 */
	public String getString(String key) {
		return getProperty(key);
	}
	
	/**
	 * Returns the parameter specified by key as a char array.
	 * 
	 * @param key the name of the parameter.
	 * @return the parameter specified by key as a char array.
	 */
	public char[] getCharArray(String key) {
		return getProperty(key).toCharArray();
	}
	
}