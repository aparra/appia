package net.sf.appia.demo.jgcs.opengroup;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ClientMessage extends ProtocolMessage {

    int id;

    public ClientMessage(byte[] buf) throws IOException{
        super(buf);
    }
    
    public ClientMessage(int id){
        super();
        this.id=id;
    }
    
    @Override
    public void readUserData(ObjectInputStream is) throws IOException,
            ClassNotFoundException {
        id=is.readInt();
    }

    @Override
    public void writeUserData(ObjectOutputStream os) throws IOException {
        os.writeInt(id);
    }

}
