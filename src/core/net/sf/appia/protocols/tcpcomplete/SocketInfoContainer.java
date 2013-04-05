package net.sf.appia.protocols.tcpcomplete;

import net.sf.appia.protocols.tcpcomplete.TcpCompleteSession.TcpSender;

/**
 * This class defines a SocketInfoContainer
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class SocketInfoContainer {
    public TcpReader reader;
    public TcpSender sender;
    public SocketInfoContainer(TcpReader r, TcpSender s){
        reader = r;
        sender = s;
    }
    
    public void close(){
        reader.setRunning(false);
        sender.setRunning(false);
    }
}
