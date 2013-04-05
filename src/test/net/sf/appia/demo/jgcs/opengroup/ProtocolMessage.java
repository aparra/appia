/**
 * 
 */
package net.sf.appia.demo.jgcs.opengroup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;

/**
 * @author nonius
 *
 */
public abstract class ProtocolMessage {
	private byte[] msgBuffer;
	private boolean marshaled;
	private SocketAddress senderAddress;
	
	public ProtocolMessage(byte[] buf) throws IOException{
		msgBuffer = buf;
	}

	public ProtocolMessage() {
		marshaled = false;
	}

	public byte[] getByteArray() throws IOException{
		if(!marshaled)
			throw new IOException("Message is not marshaled!");
		return msgBuffer;
	}
	
	protected void setMarshaled(boolean m){
		marshaled = m;
	}
	
	protected boolean isMarshaled(){
		return marshaled;
	}
	
	public void marshal() throws IOException{
		MessageOutputStream mos = new MessageOutputStream();
		ObjectOutputStream os = mos.getOutputStream();
		writeUserData(os);
		mos.close();
		msgBuffer = mos.getByteArray();
		setMarshaled(true);
	}
	
	public void unmarshal() throws IOException, ClassNotFoundException{
		if(msgBuffer == null || msgBuffer.length == 0)
			throw new IOException("Nothing to read in the buffer");
		MessageInputStream mis = new MessageInputStream(msgBuffer);
		ObjectInputStream is = mis.getInputStream();
		readUserData(is);
		mis.close();
		setMarshaled(false);
	}

	public abstract void writeUserData(ObjectOutputStream os) throws IOException;
	public abstract void readUserData(ObjectInputStream is) throws IOException,ClassNotFoundException;

	public SocketAddress getSenderAddress() {
		return senderAddress;
	}

	public void setSenderAddress(SocketAddress sender) {
		this.senderAddress = sender;
	}
}

class MessageOutputStream {
	private ByteArrayOutputStream baos;
	private ObjectOutputStream dos;
	
	public MessageOutputStream() throws IOException {
		baos = new ByteArrayOutputStream();
		dos = new ObjectOutputStream(baos);
	}

	ObjectOutputStream getOutputStream(){
		return dos;
	}
	
	void close() throws IOException{
		dos.close();
		baos.close();
	}
	
	byte[] getByteArray(){
		return baos.toByteArray();
	}
}

class MessageInputStream {
	private ByteArrayInputStream bais;
	private ObjectInputStream dis;
	
	public MessageInputStream(byte[] buffer) throws IOException {
		bais = new ByteArrayInputStream(buffer);
		dis = new ObjectInputStream(bais);
	}
	
	ObjectInputStream getInputStream(){
		return dis;
	}
	
	void close() throws IOException{
		dis.close();
		bais.close();
	}

}
