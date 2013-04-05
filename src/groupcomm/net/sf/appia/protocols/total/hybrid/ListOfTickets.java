package net.sf.appia.protocols.total.hybrid;

import java.util.*;

/**
 * A list that stores the tickets.
 * It is used for several porposes.
 */
public class ListOfTickets{
	
	private LinkedList mainTicketList;
	
	/**
	 * Constructs an empty list.
	 */
	public ListOfTickets(){
		mainTicketList=new LinkedList();
	}	
	
	/**
	 * Insert a ticket in the list
	 * @param ticket The ticket to be inserted.
	 */
	public void insert(Ticket ticket){
		mainTicketList.addLast((Object)ticket);
		//System.out.println("xxxxxxxxxxxxxxxxxxx\nInseri no ListOfTickets ticket "+ticket+"\nxxxxxxxxxxxxxxxxxxx");
	}
	
//	/**
//	 * Gets the ticket with the larger ticket id.
//	 * @return the largest ticket.
//	 */
//    public int getMaxTicket(){
//	 int max=-1;
//	 ListIterator li=mainTicketList.listIterator(0);
//	 Ticket t;
//	 
//	 while(li.hasNext()){
//	 t=(Ticket)li.next();
//	 if(t.getTicketId() >max)
//	 max=t.getTicketId();
//	 }
//	 
//	 return max;
//	 }
	
//	/**
//	 * Return the next ticket to be delivered.
//	 * @param size The size of the group.
//	 * @param actives the number of acive members in the group.
//	 * @return the position of the next ticket or -1 if a ticket can not be 
//	 * delivered.
//	 */
//	public int getNextTicket(int size,int actives){
//		boolean b[]= new boolean[size];
//		int minTick=-1;
//		int minPid=-1;
//		int c=0;
//		int pos=0;
//		ListIterator li;
//		Ticket t;       
//		
//		for(int i=0;i!=size;i++)
//			b[i]=false;
//		
//		li=mainTicketList.listIterator(0);
//		while(li.hasNext()){
//			t=(Ticket)li.next();
//			//	    System.out.println("O TICKET VEIO DE -> "+ t.getSource());
//			if(!b[t.getSource()]){
//				b[t.getSource()]=true;
//				c++;
//			}
//		}
//		
//		System.out.println("ACTIVOS -> "+actives+ "   MSGS DE PPL DIF -> " + c);
//		
//		//it must have tickets from all the active members of the group
//		if(c < actives){
//			//	    System.out.println("n recebemos msgs de todos");
//			return -1;
//		}
//		
//		int i=0;
//		li=mainTicketList.listIterator(0);
//		while(li.hasNext()){
//			t=(Ticket) li.next();
//			
//			//	    System.out.println("TICKETID-> "+ t.getTicketId());
//			//	    System.out.println("SOURCE-> "+ t.getMsgId().getSource());
//			
//			if(minTick==-1){
//				minTick=t.getTicketId();
//				minPid=t.getMsgId().getSource();
//				pos=i;
//			}
//			else{
//				if(minTick > t.getTicketId()){
//					minTick=t.getTicketId();
//					minPid=t.getMsgId().getSource();
//					pos=i;
//				}
//				else{
//					if(minTick==t.getTicketId() && minPid > t.getMsgId().getSource()){
//						minPid= t.getMsgId().getSource();
//						pos=i;
//					}
//				}
//			}
//			i++;
//		}
//		return pos;		
//	}
	
	/**
	 * Removes the ticket in the given position
	 * @param pos position of the ticket to be removed.
	 */
	public Ticket remove(int pos){
		Ticket t = (Ticket)mainTicketList.get(pos);
		mainTicketList.remove(pos);
		return t;
	}
	
	public Ticket remove(Ticket ticket) {
		ListIterator it = mainTicketList.listIterator();
		while (it.hasNext()) {
			Ticket currTicket = (Ticket) it.next();
			if (currTicket == ticket) {
				it.remove();
				return currTicket;
			}
		}
		return null;
	}
	
	/**
	 * Transforms the list of tickets in an array to be sent.
	 * @return array of tickets.
	 */
	public Ticket[] toArray(){	
		Ticket ts[]=new Ticket[mainTicketList.size()];
		for(int i=0;i!=ts.length;i++)
			ts[i]=remove(0);
		return ts;
	}
	
	/**
	 * Verifies if a message id exists in the list of tickets.
	 * @param msgid The MsgId to be searched.
	 * @return true if the MsgId exists and false otherwise.
	 */
	public boolean exists(MsgId msgid){
		ListIterator li=mainTicketList.listIterator(0);
		Ticket t;
		while(li.hasNext()){
			t=(Ticket)li.next();
			if(t.getMsgId().equals(msgid))
				return true;
		}
		return false;
	}   
	
	/**
	 * Return the MsgId of the first ticket in the list.
	 * @return The firts MsgId or null if the list is empty.
	 */
	public MsgId getFirstMsgID(){
		if(mainTicketList.size() == 0)
			return null;
		return ((Ticket)mainTicketList.getFirst()).getMsgId();
	}
	
	public Ticket getFirstTicket() {
		if(mainTicketList.size() == 0)
			return null;
		return (Ticket)mainTicketList.getFirst();
	}
	/**
	 * Return the current size of the list.
	 * @return size of the list.
	 */
	public int size(){
		return mainTicketList.size();
	}
	
	/**
	 * Prints the list to the screen.
	 * For debug only.
	 */
	public void printList(){
		Ticket t;
		ListIterator li= mainTicketList.listIterator(0);	
		while(li.hasNext()){
			t= (Ticket)li.next();
			t.print();
		}
	}
}
