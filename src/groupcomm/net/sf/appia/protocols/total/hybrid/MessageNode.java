package net.sf.appia.protocols.total.hybrid;

import net.sf.appia.protocols.group.events.GroupSendableEvent;

/**
 * This class represents the information stored in the MessageList.
 * It stores the event and the header received.
 */
public class MessageNode {
	private GroupSendableEvent event;
	private TotalHybridHeader header;
	private boolean ticketIssued;
	
	/**
	 * Constructs a MessageNode.
	 * @param e The event.
	 * @param h The header of the message.
	 */
	public MessageNode(GroupSendableEvent e, TotalHybridHeader h){
		event = e;
		header = h;
		ticketIssued = false;
	}
	
	/**
	 * @return the event stored in the MessageNode
	 */
	public GroupSendableEvent getEvent(){
		return event;
	}
	
	/**
	 * @return the header stored in the MessageNode.
	 */
	public TotalHybridHeader getHeader(){
		return header;
	}
	
	public boolean isIssued(){
		return ticketIssued;
	}
	
	public void issued(){
		ticketIssued = true;
	}
	
	/**
	 * Prints the MessageNode.
	 * For debbuging only.
	 */
	public void print(){
		switch(header.getType()){
		case TotalHybridHeader.DATA:
			break;
		case TotalHybridHeader.GOINGACTIVE:
			break;
		case TotalHybridHeader.GOINGPASSIVE:
			break;
		case TotalHybridHeader.REASSING:
			break;
		}
		//System.out.println("Event type-> "+type+"  source -> "+header.getSource()+"  seq-> "+header.getSequence());
	}
	
}
