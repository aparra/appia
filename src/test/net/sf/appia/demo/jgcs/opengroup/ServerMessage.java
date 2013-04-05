package net.sf.appia.demo.jgcs.opengroup;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.SocketAddress;

public class ServerMessage extends ProtocolMessage {

    int id;
    SocketAddress addr;
    
    public ServerMessage(byte[] buf) throws IOException{
        super(buf);
    }
    
    public ServerMessage(int id, SocketAddress addr){
        this.id = id;
        this.addr = addr;
    }
    
    @Override
    public void readUserData(ObjectInputStream is) throws IOException,
            ClassNotFoundException {
        id = is.readInt();
        addr = (SocketAddress) is.readObject();
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.writeInt(id);
        os.writeObject(addr);
    }

}
