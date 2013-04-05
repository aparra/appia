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
 * Created on 16/Mar/2004
 */
package net.sf.appia.test.xml;

import java.awt.Point;

import net.sf.appia.core.Channel;


/**
 * @author Nuno Almeida
 *
 */
public class MessengerInterface {
	
	private MessengerGraphics mess;
	
	/*
	 * Channels
	 */
	private Channel ctext;
	private Channel cdraw;
	
	public MessengerInterface(Channel ctext, Channel cdraw, String username){
		this.ctext = ctext;
		this.cdraw = cdraw;
		mess = new MessengerGraphics(ctext,cdraw,username);
	}
	
	public void setWindow(String usr, String str){
		 mess.setChatText(usr, str);
	}
	
	public void drawPoint(Point p) {
		mess.drawPoint(p);
	}
	
	public void mousePressed() {
		mess.mousePressed();
	}
	
	public void mouseReleased() {
		mess.mouseReleased();
	}
	
	public void clearText(){
		mess.clearText();
	}
	
	public void setUsers(String usrlist) {
		mess.setUsers(usrlist);
	}
	
	public void clearWhiteBoard() {
		mess.clearWhiteBoard();
	}
	
	public SerializableImage getImage() {
		return mess.getImage();
	}
	
	public void setImage(SerializableImage si) {
		mess.setImage(si);
	}
}
