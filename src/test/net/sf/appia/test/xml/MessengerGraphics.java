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

import java.awt.*;

import javax.swing.BoxLayout;
import javax.swing.JLabel;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;


/**
 * @author Nuno Almeida 
 */
public class MessengerGraphics extends javax.swing.JFrame {
	
	private static final long serialVersionUID = -9001479356849904877L;
	private Channel ctext;
	private Channel cdraw;
	
	private String username;
	private boolean start;

	private javax.swing.JButton sendButton;
	private TextArea chat;
	private TextArea text;
	private TextArea users;
	
	private WhiteBoard whiteBoard;
	
	public MessengerGraphics(Channel ctext, Channel cdraw, String usr) {
		this.ctext = ctext;
		this.cdraw = cdraw;
		username = usr;
		start = false;
		initComponents(); 
		setResizable(false);
		setVisible(true);
		whiteBoard.init();
	}

	private void initComponents() {
		setTitle(username + " on Messenger");
		
		getContentPane().setLayout(new BorderLayout());
		
		Panel ctdPanel = new Panel();
		ctdPanel.setLayout(new GridLayout(1,2));
		Panel ctPanel = new Panel();
		Panel chatPanel = new Panel();
		Panel textPanel = new Panel();
		Panel drawingPanel = new Panel();
		Panel usersPanel = new Panel();
		
	   	chat = new TextArea("",10,20,TextArea.SCROLLBARS_VERTICAL_ONLY);
	   	chat.setEditable(false);
	   	chatPanel.setLayout(new BorderLayout());
		chatPanel.add(chat,BorderLayout.CENTER);
			
		text = new TextArea("",2,20,TextArea.SCROLLBARS_VERTICAL_ONLY);
		sendButton = new javax.swing.JButton();
		sendButton.setText("Send");
		textPanel.setLayout(new BorderLayout());
		textPanel.add(text,BorderLayout.CENTER);
		textPanel.add(sendButton,BorderLayout.EAST);
		
		ctPanel.setLayout(new BorderLayout());
		ctPanel.add(chatPanel,BorderLayout.CENTER);
		ctPanel.add(textPanel,BorderLayout.SOUTH);
		ctdPanel.add(ctPanel);
		
		whiteBoard = new WhiteBoard(cdraw);
		ctdPanel.add(whiteBoard);
		
		getContentPane().add(ctdPanel,BorderLayout.CENTER);
		
		users = new TextArea("",6,20,TextArea.SCROLLBARS_BOTH);
		users.setEditable(false);
		JLabel usersLabel = new JLabel("users");
		
		usersPanel.setLayout(new BoxLayout(usersPanel,BoxLayout.Y_AXIS));
		usersPanel.add(usersLabel);
		usersPanel.add(users);
		
		getContentPane().add(usersPanel,BorderLayout.EAST);
		
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent evt) {
				exitForm(evt);
			}
		});

		sendButton.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent evt) {
				sendButtonClicked(evt);
			}
		});
		
		pack();
	}


	/*
	 * Triggers text event 
	 */
	private void sendButtonClicked(java.awt.event.MouseEvent evt) {
			
		refreshText();
		TextEvent ttEvent = new TextEvent();
		ttEvent.setChannel(ctext);
		ttEvent.setUsername(username);
		ttEvent.setUserMessage(text.getText());
		try {
			ttEvent.asyncGo(ctext, Direction.DOWN);
		} catch (AppiaEventException e) {
			e.printStackTrace();
		}
	}


	
	public void refreshText() {
		if (!start) {
			chat.setText(username + " : " + text.getText());
			start = true;
		}
		else	
			chat.append("\n" + username + " : " + text.getText());
	}
	
	public void setChatText(String usr, String str){
		if (!start) {
			chat.setText(usr + " : " + str);
			start = true;
		}
		else
			chat.append("\n" + usr + " : " + str);
	}
	
	public void clearText() {
		text.setText(" ");
	}
	
	public void setUsers(String usrlist) {
		users.setText(usrlist);
	}

	private void exitForm(java.awt.event.WindowEvent evt) {
		System.exit(0);
	}
	
	
	public void drawPoint(Point point) {
		whiteBoard.mouseDragged(point);
	}
	
	public void mousePressed() {
		whiteBoard.mousePressed();
	}
	
	public void mouseReleased() {
		whiteBoard.mouseReleased();
	}
	
	public void clearWhiteBoard() {
		whiteBoard.clear();
	}
	
	public SerializableImage getImage() {
		return whiteBoard.getImage();
	}
	
	public void setImage(SerializableImage si) {
		whiteBoard.setImage(si);
	}
}
