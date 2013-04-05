package net.sf.appia.protocols.total.hybrid;

import java.io.*;

/**
 * This class stores information to identify the messages sent.
 * It stores information about the sender and sequence number of the message.
 */
public class MsgId implements Serializable{
	
	private static final long serialVersionUID = -98581331739233914L;
	private int source;
	private int sequence;
	
	/**
	 * Constructs a new MsgId.
	 * @param source the source of the message.
	 * @param sequence the sequence number of the message.
	 */
	public MsgId(int source, int sequence){
		this.source=source;
		this.sequence=sequence;
	}
	
	/**
	 * @return returns the source of the message.
	 */
	public int getSource(){
		return source;
	}
	
	/**
	 * @return returns the sequence number of the message.
	 */
	public int getSequence(){
		return sequence;
	}
	
	/**
	 * Verifies of two MsgId are equal.
	 * @param msg MsgId to be compared.
	 * @return true if the MsgId are equal and false otherwise.
	 */
	public boolean equals(Object msg){
		if(msg instanceof MsgId)
			return (((MsgId)msg).getSource()==source && 
					((MsgId)msg).getSequence()==sequence);
		else 
			return false;
	}
	
	public Object clone(){
		return new MsgId(source,sequence);
	}
}
