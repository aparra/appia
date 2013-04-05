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
 * Created on 24/04/2004
 */
package net.sf.appia.test.xml;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;




/**
 * This session has a pedagogical interest only
 *
 * Verifies integrity of all text and draw messages, based on a secret.
 * <i>Integrity</i> features an integrity property. For each messenger event going DOWN (for the electable ones)
 * an hash of event data concatenated with a secret is generated being sent along with event.
 * Upon event arriving up, received and calculated hashes are compare, deciding if the event should or 
 * shouldn't be forward. 
 * <br>
 * Uses hashing algorithm SHA 
 * <br>
 * XML parameters:
 * 					secret : String
 * 
 * @author Liliana Rosa
 * 
 */
public class IntegritySession extends Session implements InitializableSession {

//	private PrintStream out = System.out;
	private byte[] secret;   
	private MessageDigest md;
//	private byte data[];
	
	/**
	 * Constructor
	 * 
	 * @param layer
	 */
	public IntegritySession(Layer layer) {
		super(layer);
		try {
			md = MessageDigest.getInstance("SHA");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 
	 * Sets secret used to guarantee integrity
	 * 
	 * @param s secret 
	 */
	public void setSecret(String s){
		secret = s.getBytes();
	}
	
	/**
	 * 
	 * Handles all received event
	 * 
	 * @param ev
	 */
	public void handle(Event ev) {
		if (ev instanceof TextEvent || ev instanceof DrawEvent ||
			ev instanceof ImageEvent || ev instanceof ClearWhiteBoardEvent ||
			ev instanceof MouseButtonEvent)
			handleInterestingEvent((GroupSendableEvent)ev);
		else {
			try {
				ev.go();
			} catch (AppiaEventException ex) {
				ex.printStackTrace();
			}
		}
	}
	
	/**
	 * 
	 * Handles all events related to messenger features:
	 * TextEvent, DrawEvent, ImageEvent, ClearWhiteBoardEvent, MouseButtonEvent 
	 *
	 * If an event direction is DOWN an hash is generated
	 * otherwise received and calculated hashes are compared.
	 * <br> 
	 * Different hashes mean that an event is ignored
	 * 
	 * @param ev 
	 */
	private void handleInterestingEvent(GroupSendableEvent ev) {
		
		Message message = ev.getMessage();
		byte[] data;
		Hash h = new Hash();
//		String type;
			
		if(ev.getDir() == Direction.DOWN){
			data = message.toByteArray();
			md.update(data);
			md.update(secret);
			byte[] my = md.digest();
			h.setHash(my);
			message.pushObject(h);
			md.reset();
			//out.println("MD calculated");
			ev.setObjectsMessage(message);
			try {
				ev.go();} 
			catch (AppiaEventException e) {
				e.printStackTrace();}
			
		}
		else{
			//incoming hash
			h = (Hash) message.popObject();
			data = message.toByteArray();
			
			// calculates hash
			md.update(data);
			md.update(secret);
			
			if (MessageDigest.isEqual(md.digest(),h.getHash())){
				//out.println("Valid");
				try {
					ev.go();} 
				catch (AppiaEventException e) {
					e.printStackTrace();}
			}
//			else{
//				out.println("Error! Compromised message received!");
//				out.println("Message ignored!");
//			}
			md.reset();
			
		}
	}
	
	
	/**
	 * In the xml file, on the tag <i>channel</i> Integrity Session must have one 
	 * parameter called <i>secret</i>
	 * 
	 * @param params the params with the secret!
	 */
	public void init(SessionProperties params) {
		secret = params.getString("secret").getBytes();
	}
	
	/**
	 * Uses a string instead of a SessionProperties
	 * 
	 * @param sec secret
	 */
	public void init(String sec){
		secret = sec.getBytes();
	}
}
