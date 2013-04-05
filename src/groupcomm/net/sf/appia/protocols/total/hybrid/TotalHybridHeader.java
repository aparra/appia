package  net.sf.appia.protocols.total.hybrid;

import java.io.Serializable;

import net.sf.appia.core.message.Message;

/**
 * Message Header to be added to the protocol events.
 */
public class TotalHybridHeader implements Serializable{

	private static final long serialVersionUID = -1164279713630259319L;
	private int type;
	private int source;
	private int sequence;
	private int  sequencer;
	private Ticket tickets[];
	
	//in case of reassing
	private int from;
	private int to;
	
	//constants
	/**
	 * The Event carries a data message.
	 */
	public final static int DATA=0;
	
	/**
	 * The event carries information that a certain node is going to change to active.
	 */
	public final static int GOINGACTIVE=1;
	
	/**
	 * The event carries information that a certain node is going to change to passive.
	 */
	public final static int GOINGPASSIVE=2;
	
	/**
	 * The event carries information that a certain node needs to change the sequencer
	 * to a certain set of messages.
	 */
	public final static int REASSING=3;
	
	/**
	 * 
	 *
	 */
	public final static int PERIODIC=4;
	
	
	/**
	 * 
	 */
	public final static int PENDING=5;
	
	/**
	 * Default constructor
	 */
	public TotalHybridHeader() {}
	
	
	/**
	 * Constructs a header object for normal messages (DATA, GOINGACTIVE, GOINGPASSIVE).
	 * @param type Type of header. Should be DATA, GOINGACTIVE or GOINGPASSIVE.
	 * @param source Sender of the message.
	 * @param seq Sequence number.
	 * @param sequencer the sequencer ID of the message
	 * @param tickets Array of tickets that are piggybanked in the messages.
	 */
	public TotalHybridHeader(int type, int source, int seq, int sequencer, Ticket[] tickets){
		this.type=type;
		this.source=source;
		this.sequence=seq;
		this.sequencer=sequencer;
		this.tickets=tickets;
		this.from=-1;
		this.to=-1;
	}
	
	/**
	 * Construcs an header object for REASSING messages.
	 * @param source Sender of the message.
	 * @param from The new sequencer must order from this message.
	 * @param to The last message that the new sequencer must order.
	 * @param sequencer The new sequencer.
	 */
	public TotalHybridHeader(int source,int from, int to, int sequencer){
		this.type=REASSING;
		this.source=source;
		this.sequencer=sequencer;
		this.from=from;
		this.to=to;
		this.tickets=new Ticket[0];
		this.sequence=-1;
	}
	
	/**
	 * Construct an header for messages only with ticket information
	 * @param t array of tickets
	 */
	public TotalHybridHeader(Ticket[] t){
		this.type= PERIODIC;
		this.tickets=t;
		this.source=-1;
		this.sequencer=-1;
		this.from=-1;
		this.to=-1;
		this.sequence=-1;	
	}
	
	/**
	 * Contructs a PENDING header
	 * @param source source of the message
	 * @param seq sequence of the message
	 */
	public TotalHybridHeader(int source, int seq){
		this.type = PENDING;
		this.source = source;
		this.sequence = seq;
	}
	
	/**
	 * Contructs a header from a Appia Message
	 * @param om Appia Message
	 * @param f source
	 */
	public TotalHybridHeader(Message om, int f){
		type = om.popInt();
		
		source = f;
		sequence = om.popInt();
		sequencer = om.popInt();
		from = om.popInt();
		to = om.popInt();
		
		int tid, idsource, idseq;
		int size = om.popInt();
		
		if(size!=-1){
			tickets = new Ticket[size];
			for(int i=0 ; i!=tickets.length ; i++){
				idseq = om.popInt();
				idsource = om.popInt();
				tid = om.popInt();
				tickets[i] = new Ticket(f,tid,new MsgId(idsource,idseq));
			}
		}
		else
			tickets = new Ticket[0];
	}
	
	
	/**
	 * Return the type of the event.
	 * @return Type of the event.
	 */
	public int getType(){
		return type;
	}
	
	/**
	 * Return the rank of the sender of the event.
	 * @return Sender of the event.
	 */
	public int getSource(){
		return source;
	}
	
	/**
	 * Return the sequence number of the event
	 * @return Sequence number.
	 */
	public int getSequence(){
		return sequence;
	}
	
	/**
	 * Return the new sequencer of a set of messages.
	 * This should only be used in REASSING events.
	 * @return The new Sequencer.
	 */
	public int getSequencer(){
		return sequencer;
	}
	
	/**
	 * Return an array of tickets.
	 * @return Tickets.
	 */
	public Ticket[] getTickets(){
		return tickets;
	}
	
	/**
	 * Return the first message that the sequencer must change.
	 * Only to be used by the REASSING event.
	 * @return The first message.
	 */
	public int getFrom(){
		return from;
	}
	
	/**
	 * Return the last message that the sequencer must change.
	 * Only to be used by the REASSING event.
	 * @return The last message.
	 * 
	 */
	public int getTo(){
		return to;
	}
	
	/**
	 * Change the sequencer of an event.
	 * @param newseq The new sequencer.
	 */
	public void setSequencer(int newseq){
		sequencer=newseq;
	}
	
	/**
	 * Serializes the header into an Appia Message
	 * @param om Appia Message where to put the header information
	 */
	public void writeHeader(Message om){
		
		if(tickets!=null){
			for(int i=tickets.length-1;i>=0;i--){
				om.pushInt(tickets[i].getTicketId());
				om.pushInt(tickets[i].getMsgId().getSource());
				om.pushInt(tickets[i].getMsgId().getSequence());
			}
			om.pushInt(tickets.length);
		}
		else
			om.pushInt(-1);
		
		om.pushInt(to);
		om.pushInt(from);
		om.pushInt(sequencer);
		om.pushInt(sequence);
		om.pushInt(type);
	}
	
}
