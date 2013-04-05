package net.sf.appia.protocols.total.hybrid;


import java.io.*;

/**
 * Class that represents a ticket. A ticket is the 
 * information necessary to order a message.
 */
public class Ticket implements Serializable, Comparable {

	private static final long serialVersionUID = 8499895122196740887L;
	private int source;
	private int ticketId;
	private MsgId msgId;
	
	/**
	 * Constructs a new Ticket.
	 * @param s the message sequencer.
	 * @param tId the id of the ticket (sequence number of the sequencer)
	 * @param m the message identifier.
	 */
	public Ticket(int s, int tId, MsgId m){
		source=s;
		ticketId=tId;
		msgId=m;
	}
	
	/**
	 * @return the sequencer of this ticket.
	 */
	public int getSource(){
		return source;
	}
	
	/**
	 * @return the ticket identifier.
	 */
	public int getTicketId(){
		return ticketId;
	}
	
	/**
	 * @return the message id.
	 */
	public MsgId getMsgId(){
		return msgId;
	}
	
	/**
	 * Verifies if two tickets are equal.
	 * @param t The ticket to be compared.
	 * @return true if the tickets are equal and false otherwise.
	 */
	public boolean equals(Object o){
		if(o instanceof Ticket){
			Ticket t = (Ticket) o;
			return (t.getSource()==source && t.getTicketId()==ticketId && t.getMsgId().equals(msgId));
		}
		else
			return false;
	}
	
	public Object clone(){
		return new Ticket(source,ticketId,(MsgId)msgId.clone());
	}
	
	/**
	 * Prints information about a ticket.
	 * Used for debugging.
	 */
	public void print(){
		//System.out.println("SOURCE(T) -> "+ source + "  TID -> "+ticketId +
		//		"  SOURCE(MSG) ->"+ msgId.getSource()+ 
		//		"  SEQUENCE(MSG) ->"+ msgId.getSequence());
	}
	
	public String toString(){
		return "SOURCE(T) -> "+ source + "  TID -> "+ticketId +
		"  SOURCE(MSG) ->"+ msgId.getSource()+ 
		"  SEQUENCE(MSG) ->"+ msgId.getSequence();
		//return "";
	}

	public int compareTo(Object arg0) {
		if (ticketId > ((Ticket)arg0).ticketId)
			return 1;
		else if (ticketId < ((Ticket)arg0).ticketId)
			return -1;
		else {
			if (source > ((Ticket)arg0).source)
				return 1;
			else if (source < ((Ticket)arg0).source)
				return -1;
			else
				return 0;
		}
	}
	
	public boolean isTicketAfter(Ticket t, Ticket safe, boolean[] actives) {
		int nextActive = nextActive(t.source,actives);
		if (source == nextActive) {
			if (nextActive == t.source && ticketId == t.ticketId + 1)
				return true;
			else if (nextActive < t.source && ticketId == t.ticketId + 1)
				return true;
			else if (ticketId == t.ticketId)
				return true;
			else if (this.compareTo(safe) < 0)
				return true;
		}
		return false;
	}
	
	private int nextActive(int rank, boolean[] actives) {
		for (int i = rank + 1; i < actives.length; i++)
			if (actives[i])
				return i;
		for (int i = 0; i < rank; i++)
			if (actives[i])
				return i;
		return rank;
	}

	public boolean isFirstTicket(boolean[] actives) {
		for (int i = 0; i < actives.length; i++)
			if (actives[i] && source == i)
				return true;
			else	
				return false;
		return false;
	}
}
