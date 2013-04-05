package net.sf.appia.protocols.total.hybrid;

import java.util.LinkedList;
import java.util.ListIterator;

public class UnorderedTicketList{
	
	private LinkedList[] list;
	//private int[] lastRcv;
	//private int lastTicketId = -1;
	//private int safeId;
	private Ticket[] lastRcv;
	private Ticket lastTicket;
	private Ticket safeTicket;
	
	private int size;
	private int numberTickets;
	
	
	public UnorderedTicketList(int size){
		list=new LinkedList[size];
		
		this.size=size;
		
		for(int i=0;i!=size;i++)
			list[i] = new LinkedList();
		
		//lastRcv = new int[size];
		//for (int i = 0; i < lastRcv.length; i++)
		//	lastRcv[i] = -1;
		lastRcv = new Ticket[size];
		//for (int i = 0; i < lastRcv.length; i++)
		//	lastRcv[i] = new Ticket(-1,-1,new MsgId(-1,-1));
		
		//lastTicket = new Ticket(-1,-1,new MsgId(-1,-1));
		//safeTicket = new Ticket(-1,-1,new MsgId(-1,-1));
	}	
	
	/**
	 * Insert a ticket in the list
	 * @param t The ticket to be inserted.
	 */
	public void insert(Ticket t){
		list[t.getSource()].addLast(t);
		//lastRcv[t.getSource()]++;
		//safeId = min(lastRcv);
		lastRcv[t.getSource()] = t;
		safeTicket = min(lastRcv);
		numberTickets++;
		//System.out.println("xxxxxxxxxxxxxxxxxxx\nInseri ticket "+t+"\nxxxxxxxxxxxxxxxxxxx");
	}
	
//	private int min(int[] array) {
//		int min = array[0];
//		for (int i = 1; i < array.length; i++)
//			if ((array[i] != -1 && min == -1) ||
//				(array[i] < min))
//				min = array[i];
//		return min;
//	}
	
	private Ticket min(Ticket[] array) {
		Ticket min = array[0];
		for (int i = 1; i < array.length; i++)
			if (min == null && array[i] != null)
				min = array[i];
			else if (array[i] != null && array[i].compareTo(min) < 0)
				min = array[i];
		return min;
	}
	/**
	 * Return the next ticket to be delivered.
	 * @param actives the number of active members in the group.
	 * @return the next ticket or null if none can be delivered.
	 */
//	public Ticket getNextTicket(boolean[] actives) {//int actives){
	public Ticket getNextTicket(Configuration conf) {//int actives){
		Ticket minTicket=null,current = null;
		int act=0;
		
		//System.out.println("GETTING next TICKET");
		//printList();
		
		for(int i=0;i!=size;i++){
			if(list[i].size()!=0){
				act++;			
				current=(Ticket)list[i].getFirst();
				if(minTicket == null || current.compareTo(minTicket) < 0)
					minTicket = current;
//				else{
//					if(minTicket.getTicketId() > current.getTicketId()){
//						minTicket = current;
//					}
//					else if(minTicket.getTicketId() == current.getTicketId()){
//						if(minTicket.getSource() > current.getSource()){
//							minTicket = current;
//						}
//					}
//				}		    
			}
		}
		
//		if (minTicket != null) {
//			System.out.print("Ticket found: ");
//			minTicket.print();
//		}
//		else
//			System.out.println("THERE IS NO NEXT TICKET!");
		
//		if(act > actives){//sanity check
//			System.err.println("BUG: passive processes sent TICKETS");
//			System.exit(0);
//			return null;
//		}
//		else if(act < actives){
//			System.out.println("++++++++++++++++++++ No ticket to deliver ::"+act+"  ::"+actives);
//			return null;	    
//		}
//		else{
		//if (minTicket != null && (minTicket.getTicketId() == lastTicketId + 1 || minTicket.getTicketId() < safeId)) {
//		if ((minTicket != null && lastTicket == null && minTicket.isFirstTicket(actives)) ||
//			(minTicket != null && lastTicket != null && minTicket.isTicketAfter(lastTicket,safeTicket,actives))) {
		if ((minTicket != null && lastTicket == null && minTicket.isFirstTicket(conf.getActives())) ||
				(minTicket != null && lastTicket != null && minTicket.isTicketAfter(lastTicket,safeTicket,conf.getActives()))) {
			//lastTicketId = minTicket.getTicketId();
			lastTicket = minTicket;
			
			//System.out.println("++++++++++++++++++++ delivering ticket..........: "+minTicket.toString());
			numberTickets--;
			//System.out.println("TICKET RETURNED");
			//((Ticket)list[minTicket.getSource()].getFirst()).print();
			return (Ticket)list[minTicket.getSource()].removeFirst();
		}
		
		return null;
	}
	
	
	/**
	 * Return the current size of the list.
	 * @return size of the list.
	 */
	public int size(){
		return numberTickets;
	}
	
	/**
	 * Prints the list to the screen.
	 * For debug only.
	 */
	protected void printList(){
		Ticket t;
		
		for(int i=0;i!=size;i++){
			ListIterator li= list[i].listIterator(0);
			
			while(li.hasNext()){
				t= (Ticket)li.next();
				t.print();
			}
		}
	}
	
}
