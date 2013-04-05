package net.sf.appia.protocols.total.hybrid;

import java.util.*;

/**
 * List that stores the events to be ordered.
 */
public class MessageList{
	
	private LinkedList<MessageNode> mainList;
	
	/**
	 * Constructs an empty list.
	 */
	public MessageList(){
		mainList = new LinkedList<MessageNode>();
	}
	
	/**
	 * Insert a new Message in the list.
	 * @param mn The message node to be inserted.
	 */
	public void insert(MessageNode mn){
		mainList.addLast(mn);
	}
	
	/**
	 * Returns a ListIterator that can be used to search the list.
	 * @return A ListIterator.
	 */
	public ListIterator<MessageNode> elements(){
		return mainList.listIterator(0);
	}
	
	/**
	 * Given the rank of the sender and the sequence number it returns the MessageNode on the list.
	 * @param source rank of the sender.
	 * @param sequence sequence number of the message.
	 * @return The MessageNode or null if it does not exist in the list.
	 */
	public MessageNode getMessageNode(int source, int sequence){
		MessageNode mn;
		TotalHybridHeader thh;
		ListIterator<MessageNode> li= mainList.listIterator(0);
		
		while(li.hasNext()){
			mn= li.next();
			thh= mn.getHeader();
			if(thh.getSource()==source && thh.getSequence()==sequence)
				return mn;
		}
		return null;
	}
	
	/**
	 * Given a certain MessageNode it removes it from the list.
	 * @param mn MessageNode to be removed.
	 */
	public void removeMessageNode(MessageNode mn){
		mainList.remove(mn);
	}
	
	/**
	 * Given a rank and a sequence number changes the sequencer of the message.
	 * @param pid the sender of the message.
	 * @param seq sequence number of the message.
	 * @param newsequencer the new sequencer of the message.
	 */
	public void changeSequencer(int pid,int seq, int newsequencer){
		ListIterator<MessageNode> li= mainList.listIterator(0);
		
		while(li.hasNext()){
			TotalHybridHeader header=li.next().getHeader();
			if(header.getSource()==pid && header.getSequence()==seq)
				header.setSequencer(newsequencer);
		}
	}
	
	/**
	 * Removes the first MessageNode of the list.
	 * @return The first MessageNode of the list or null if the list is empty.
	 */
	public MessageNode removesFirst(){
		try{
			return mainList.removeFirst();
		}
		catch(NoSuchElementException  e){
			return null;
		}
	}
	
	public MsgId getFirstMsgId(){
		if(mainList.isEmpty())
			return null;
		else{
			TotalHybridHeader thh = (mainList.getFirst()).getHeader();
			return new MsgId(thh.getSource(),thh.getSequence());
		}
	}
	
	/**
	 * @return The size of the list.
	 */
	public int size(){
		return mainList.size();
	}
	
	/**
	 * @return Returns the next message to be delivered or null if 
	 * there isn't a message to be delivered.
	 */
	public MessageNode getNextMessage(){
		ListIterator<MessageNode> li= mainList.listIterator(0);	
		MessageNode mn=null;
		MessageNode minMn=li.next();
		int minSeq= minMn.getHeader().getSequence();
		int minSource= minMn.getHeader().getSource();
		
		while(li.hasNext()){
			mn=li.next();
			if(minSeq > mn.getHeader().getSequence()){
				minMn=mn;
				minSeq=mn.getHeader().getSequence();
				minSource=mn.getHeader().getSource();
			}
			else {
				if(minSeq == mn.getHeader().getSequence()){
					if(minSource > mn.getHeader().getSource()){
						minMn=mn;
						minSource=mn.getHeader().getSource();
					}
				}
			}
		}
		if(minMn!=null)
			mainList.remove(minMn);
		return minMn;
	}
	
	/**
	 * @return Returns true if the list is empty and false otherwise.
	 */
	public boolean isEmpty(){
		return mainList.isEmpty();
	}
	
	/**
	 * Prints the message list.
	 * For debugging purposes
	 */
	public void printList(){
		ListIterator<MessageNode> li = mainList.listIterator(0);
		while(li.hasNext())
			li.next().print();
	}
}
