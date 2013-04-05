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
 * Created on 1/Abr/2004
 *  
 */
package net.sf.appia.test.xml;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;

import javax.swing.JButton;
import javax.swing.JPanel;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;


/**
 * @author Jose Mocito
 */
public class WhiteBoard extends JPanel    
implements ActionListener, KeyListener, MouseListener, MouseMotionListener {
	
	private static final long serialVersionUID = -4266110746283114975L;

	private BufferedImage offscreenImage;
	private Graphics2D offscreenImageG;
	private Graphics2D drawPanelG; 
	private int offscreeImage_width, offscreeImage_height;
	private boolean isButtonPressed;
	private int pre_x, pre_y; //the coordinates at the beginning of mouse dragged
	private int x, y; //the coordinates in the end of mouse dragged
	private int top_left_corner_x, top_left_corner_y; //the x and y coordonate
	// of top-left corner
	//that a image is drawn with in
	// the graphic context
	private String s; //to hold string input from keyboard
	
	private Channel drawChannel;
	
	//declare variables for building window
	private Panel topPanel; //to hold "clear" button
	private Panel drawPanel; // draw area
	private JButton clearButton;
	
	public WhiteBoard(Channel drawChannel) {
		super();
		this.drawChannel = drawChannel;
		buildWindow ( );
	}
	
	public void init() {
		isButtonPressed = false;
		top_left_corner_x = 0;
		top_left_corner_y = 0;
		Dimension dpSize = drawPanel.getSize();
		offscreeImage_width = dpSize.width;
		offscreeImage_height = dpSize.height;
		drawPanelG = (Graphics2D)drawPanel.getGraphics ();
		offscreenImage = (BufferedImage) drawPanel.createImage(offscreeImage_width,offscreeImage_height);
		offscreenImageG = (Graphics2D)offscreenImage.getGraphics();
		offscreenImageG.setColor(Color.white);
		offscreenImageG.fillRect(0,0,offscreeImage_width, offscreeImage_height);
		offscreenImageG.setColor(Color.black);
	}
	
	public void paintComponent( Graphics g ) {
		super.paintComponent(g);
		if (drawPanelG != null)
			drawPanelG.drawImage ( offscreenImage,0,0, drawPanel );
	}
	
	private void buildWindow ( ) {
		topPanel= new Panel();
		drawPanel = new Panel ( );
		
		Button clearButton=new Button("Clear");
		clearButton.setBackground(new Color(193, 211, 245));
		clearButton.addActionListener(this);
		
		// build the top panel
		topPanel.setSize(600, 30);
		topPanel.add(clearButton);
		
		// register envent on the draw panel(draw area)
		drawPanel.addKeyListener(this);
		drawPanel.addMouseListener(this);
		drawPanel.addMouseMotionListener(this);
		
		// Set colors
		topPanel.setBackground(Color.gray);
		drawPanel.setBackground (Color.white);      
		
		// build the window
		setLayout (new BorderLayout ( ) );
		add ("Center", drawPanel);
		add("South",topPanel);
		setSize(100,200);
		setBackground (Color.white);
		setVisible ( true );
	} 
	
    public void actionPerformed ( ActionEvent e ) {
    	ClearWhiteBoardEvent event = new ClearWhiteBoardEvent();
    	try {
			event.asyncGo(drawChannel,Direction.DOWN);
		} catch (AppiaEventException e1) {
			e1.printStackTrace();
		}
		clear(); 
    }
  
    
    public void clear(){
    
    	//fills the offScreenImage completely white         
       offscreenImageG.setColor( Color.white );
       offscreenImageG.fillRect( 0, 0, offscreeImage_width, offscreeImage_height); 
       offscreenImageG.setColor( Color.black );
       repaint(); 
    }
	
	/*
	 * KeyListener methods
	 */
	public void keyTyped( KeyEvent e ) {
		char c = e.getKeyChar();
		if ( c != KeyEvent.CHAR_UNDEFINED ) {
			s = s + c;
			offscreenImageG.drawString( s, x, y);
			repaint();
			e.consume();
		}
	}
	
	public void keyPressed(KeyEvent e){}
	
	public void keyReleased(KeyEvent e){}
	
	
	/*
	 * MouseListener methods
	 * 
	 * This method is called after a press and release of a mouse button
	 * with no motion in between
	 */
	public void mouseClicked(MouseEvent e){
		x = e.getX();
		y = e.getY();
		s = "";
		offscreenImageG.drawString( s, x, y);
		repaint();
		e.consume();
	} 
	/*
	 * This method is called after a mouse button is pressesd;
	 */
	public void mousePressed(MouseEvent e){
		
		isButtonPressed=true;
		MouseButtonEvent event = new MouseButtonEvent(true);
		try {
			event.asyncGo(drawChannel,Direction.DOWN);
		} catch (AppiaEventException e1) {
			e1.printStackTrace();
		}
	}
	
	public void mousePressed() {
		isButtonPressed=true;
	}
	
	public void mouseReleased(MouseEvent e){
		isButtonPressed=false;
		MouseButtonEvent event = new MouseButtonEvent(true);
		try {
			event.asyncGo(drawChannel,Direction.DOWN);
		} catch (AppiaEventException e1) {
			e1.printStackTrace();
		}
	}
	
	public void mouseReleased() {
		isButtonPressed=false;
	}
	
	public void mouseEntered(MouseEvent e){ }
	
	public void mouseExited(MouseEvent e){ }
	
	public void mouseDragged(MouseEvent e){
		
		pre_x=e.getX();
		pre_y=e.getY();
		x=e.getX();
		y=e.getY();
		
		offscreenImageG.drawLine( pre_x, pre_y, x, y);
		Point point = e.getPoint();
		DrawEvent event = new DrawEvent(point);
		try {
			event.asyncGo(drawChannel,Direction.DOWN);
		} catch (AppiaEventException ex) {
			ex.printStackTrace();
		}
		pre_x=x;
		pre_y=y;
		repaint();
		e.consume();
	} 
	
	public void mouseDragged(Point p) {
		pre_x = p.x;
		pre_y = p.y;
		
		x = p.x;
		y = p.y;
		offscreenImageG.drawLine( pre_x, pre_y, x, y);
		pre_x=x;
		pre_y=y;
		repaint();
	}
	
	public void mouseMoved(MouseEvent e) {
	}
	
	public SerializableImage getImage() {
		return new SerializableImage(offscreenImage);
	}
	
	public void setImage(SerializableImage si) {
		BufferedImage img = si.getImage();
		offscreenImageG.drawImage(img,0,0,drawPanel);
		repaint();
	}
}