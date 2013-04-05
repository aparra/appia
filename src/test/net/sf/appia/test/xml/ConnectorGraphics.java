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
package net.sf.appia.test.xml;

import java.awt.BorderLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.TextField;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import net.sf.appia.core.AppiaCursorException;
import net.sf.appia.core.AppiaDuplicatedSessionsException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.ChannelCursor;
import net.sf.appia.xml.AppiaXML;



/**
 * Initializes sessions, channels and graphic interface.
 * 
 * @author Liliana Rosa & Nuno Almeida 
 */
public class ConnectorGraphics extends javax.swing.JFrame {
	
	private static final long serialVersionUID = -8334515860857876633L;
	private String username;
//	private int channelCounter;
	private String gossip_host;
	private int gossip_port;
	
	private javax.swing.JButton jButton1;
	private TextField group;
	private Label l1;
//	private Label l2;
	
	/*
	 * To use with IntegrityLayer
	 */
	private String secret = null;
	
	/*
	 * Constructor to use with IntegrityLayer
	 */
	public ConnectorGraphics(String user, String gossip_host, int gossip_port,String secret) {
		username = user;
		this.gossip_host = gossip_host;
		this.gossip_port = gossip_port;
		initComponents();
		setVisible(true); 
		this.secret = secret;
	}
	
	/*
	 * Normal constructor
	 */
	public ConnectorGraphics(String user, String gossip_host, int gossip_port) {
	
		username = user;
		this.gossip_host = gossip_host;
		this.gossip_port = gossip_port;
		initComponents();
		setVisible(true); 
	}
	
	
	private void initComponents() {
		setTitle(username);
		
		Panel p1 = new Panel();
		l1 = new Label("Group:");
		group = new TextField(15);
		Panel p3 = new Panel();
		jButton1 = new javax.swing.JButton("Connect");
		p1.setLayout(new BorderLayout());
		p1.add("West",l1);
		p1.add("East",group);
		getContentPane().add("North",p1);
		p3.setLayout(new BorderLayout());
		p3.add("South",jButton1);
		getContentPane().add("South",p3);
				
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				exitForm(evt);
			}
		});

		jButton1.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				jButtonMouseClicked(evt);
			}
		});
		
		pack();
	}

	
	private void jButtonMouseClicked(java.awt.event.MouseEvent evt) {
		String txtStr = "text" + group.getText();
        String drawStr = "draw" + group.getText();
        
        Channel ctext = null;
    	Channel cdraw = null;
    	try {
			ctext = AppiaXML.createChannel(txtStr,"text",group.getText(),null,false,null,null);
			cdraw = AppiaXML.createChannel(drawStr,"draw",group.getText(),null,false,null,null);
		} catch (AppiaException e) {
			e.printStackTrace();
		}
		System.out.println("Channels created!");
		
		ChannelCursor cc = ctext.getCursor();
		ChannelCursor cd = cdraw.getCursor();
		cc.top();
		cd.top();
		
		MessengerSession currMsgSession = null;
		IntegritySession intSession = null;
		
		try {
			currMsgSession = (MessengerSession) cc.getSession();
	
			/*
			 * Integrity Layer should the before last layer (under Messenger Application)
			 */
			if(secret != null){
				cd.down();
	
				intSession = (IntegritySession) cd.getSession();
			}
		} catch (AppiaCursorException e1) {
			e1.printStackTrace();
		}
				
		currMsgSession.init(username,group.getText(),gossip_host,gossip_port);
		
		InetSocketAddress[] gossips = new InetSocketAddress[1];
		try {
			gossips[0] = new InetSocketAddress(InetAddress.getByName(gossip_host),gossip_port);
		} catch (UnknownHostException e3) {
			e3.printStackTrace();
		}
		
		/*
		 * For usage with Integrity Layer
		 */
		if(secret != null)
			intSession.init(secret);

		System.out.println("Initialized sessions!");
		
		try {
			ctext.start();
			cdraw.start();
		} catch(AppiaDuplicatedSessionsException ex) {
			System.err.println("Sessions binding strangely resulted in "+
			       "one single sessions occurring more than "+
			       "once in a channel");
			System.exit(1);
		}
		System.out.println("Initialized channels!");
	}
		
	private void exitForm(java.awt.event.WindowEvent evt) {
		System.exit(0);
	}
}
