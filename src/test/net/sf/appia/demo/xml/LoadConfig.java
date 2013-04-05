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
 * Created on Sep 3, 2004
 *
 */
package net.sf.appia.demo.xml;

import java.io.File;
import java.io.IOException;

import net.sf.appia.xml.AppiaXML;

import org.xml.sax.SAXException;


/**
 *
 * Loads a configuration and automatically "starts" Appia
 * putting all the channels in execution.
 * 
 * @author jmocito
 * @deprecated
 */
public class LoadConfig {

    private LoadConfig() {}
    
	public static void main(String[] args) {
		if (args.length == 1) {
			final File xmlfile = new File(args[0]);
			try {
				//AppiaXML.load(xmlfile);
				//Appia.run();
				AppiaXML.loadAndRun(xmlfile);
			} catch (SAXException e) {
				final Exception we = e.getException();
				if (we != null )
					we.printStackTrace();
				else
					e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else {
			System.out.println("Invalid number of arguments!");
			System.out.println(
					"Usage:\tjava LoadConfig <xml_file>");
		}
	}
}
