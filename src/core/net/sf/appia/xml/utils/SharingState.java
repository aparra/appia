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
 * Created on Mar 13, 2004
 *
 */
package net.sf.appia.xml.utils;

/**
 * This class represents the concept of a sharing scope. It facilitates
 * the correspondence between selected strings and their respective int
 * values representing diferent sharing properties.
 * 
 * @author Jose Mocito
 *
 */
public class SharingState {
	
    private SharingState(){}
    
	/**
	 * Represents the <i>private</i> sharing scope.
	 */
	public static final int PRIVATE = 0;
	/**
	 * Represents the <i>label</i> sharing scope.
	 */
	public static final int LABEL = 1;
	/**
	 * Represents the <i>global</i> sharing scope.
	 */
	public static final int GLOBAL = 2;
	
	/**
	 * Based on a given string this method returns the corresponding
	 * sharing property as an int.
	 * 
	 * @param 	str 	the String to be interpreted.
	 * @return 			the sharing property as an int.
	 */
	public static int value(String str) {
		if (str.equals("private"))
			return PRIVATE;
		else if (str.equals("label"))
			return LABEL;
		else if (str.equals("global"))
			return GLOBAL;
		else
			return PRIVATE;
	}
}
