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
 * Created on Mar 16, 2004
 *
 * To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package net.sf.appia.test.xml.ecco;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;

/**
 * This class defines a MyShell
 * 
 * @author Jose Mocito
 * @version 1.0
 */
public class MyShell implements Runnable {
	
	private Channel channel;
	
    /**
     * Creates a new MyShell.
     * @param ch
     */
	public MyShell(Channel ch) {
		channel = ch;
	}
	
    /**
     * Execution of the thread.
     * @see java.lang.Runnable#run()
     */
	public void run() {
		boolean dontExit = true;
		while(dontExit) {
			System.out.print("> ");
			
			BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
			
			String str = "";
			try {
				str = in.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			MyEccoEvent event = new MyEccoEvent();
			event.setText(str);
			try {
				event.asyncGo(channel,Direction.DOWN);
			} catch (AppiaEventException e1) {
				e1.printStackTrace();
			}
		}
	}
}
