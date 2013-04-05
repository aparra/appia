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
 * Created on Mar 23, 2004
 */
package net.sf.appia.demo.xml;

import java.io.File;
import java.io.IOException;

import net.sf.appia.core.*;
import net.sf.appia.test.xml.ConnectorInterface;
import net.sf.appia.xml.AppiaXML;

import org.xml.sax.SAXException;

/**
 * @author Liliana Rosa & Nuno Almeida
 *
 */
public class Messenger {
	
    private static final int NUM_ARGS_WITHOUT_SECRET = 4;
    private static final int NUM_ARGS_WITH_SECRET = 5;
    
    private static final int ARG_USERNAME = 0;
    private static final int ARG_GOSSIP_HOST = 1;
    private static final int ARG_GOSSIP_PORT = 2;
    private static final int ARG_FILENAME = 3;
    private static final int ARG_SECRET = 4;
    
    private Messenger() {}
    
	public static void main(String args[]) {
		
		/*
		 * Number of arguments is 4 if no secret is given
		 * otherwise it's 5 args
		 */
		if (args.length != NUM_ARGS_WITH_SECRET && args.length != NUM_ARGS_WITHOUT_SECRET) {
			System.out.println("Invalid number of arguments!");
			System.out.println("Usage: java demo.xml.Messenger <username> <gossip_host> <gossip_port> <filexml> [<secret>]");
			System.exit(0);
		}
		
		String secret = null;
		final String username = args[ARG_USERNAME];
		final String gossipHost = args[ARG_GOSSIP_HOST];
		final int gossipPort = Integer.parseInt(args[ARG_GOSSIP_PORT]);
		final String filename = args[ARG_FILENAME];

		final File file = new File(filename);
		try {
			AppiaXML.load(file);
		} catch (SAXException e) {
			final Exception we = e.getException();
			if (we != null )
				we.printStackTrace();
			else
				e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(args.length == NUM_ARGS_WITH_SECRET){
			secret = args[ARG_SECRET];
			new ConnectorInterface(username,gossipHost,gossipPort,secret);
		} else
			new ConnectorInterface(username,gossipHost,gossipPort);
		
		Appia.run();
    }	
}
