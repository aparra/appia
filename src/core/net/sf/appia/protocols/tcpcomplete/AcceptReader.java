/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Initial developer(s): Alexandre Pinto and Hugo Miranda.
 * Contributor(s): See Appia web page for a list of contributors.
 */
 package net.sf.appia.protocols.tcpcomplete;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

import org.apache.log4j.Logger;

import net.sf.appia.core.Channel;
import net.sf.appia.protocols.utils.ParseUtils;



/**
 * @author Pedro Vicente
 *
 */
public class AcceptReader implements Runnable {
  
    private static Logger log = Logger.getLogger(AcceptReader.class);
    private static final int INT_SIZE = 4;
    
  private ServerSocket socket;
  private TcpCompleteSession session;
  private Channel channel;
  private Object lock;
  
  private boolean running;
  
  /**
   * Constructor for AcceptReader.
   */
  public AcceptReader(ServerSocket ss, TcpCompleteSession s, Channel channel, Object lock) {
    super();
    socket = ss;
    try {
		socket.setSoTimeout(s.param_SOTIMEOUT);
	} catch (SocketException e) {
		e.printStackTrace();
	}
    session = s;
    this.channel = channel;
    this.lock = lock;
    setRunning(true);
  }
  
  public void run(){
    Socket newSocket;
    int remotePort;
    //Benchmark bench = Benchmark.getInstance();
    
    while(isRunning()){
      newSocket=null;
      
      if(log.isDebugEnabled())
        log.debug("accepting connections");
      
      try {
        newSocket = socket.accept();
        if(log.isDebugEnabled())
          log.debug("new connection");
      } catch(SocketTimeoutException ste){
      } catch (IOException ex) {
          if(isRunning())
              ex.printStackTrace();
      }
      //check if there is a connection and
      //put new socket in socket list of connected sockets.
      
      if (newSocket != null) {
        try {
          remotePort = initProto(newSocket);
          
          final InetSocketAddress iwp = new InetSocketAddress(newSocket.getInetAddress(),remotePort);
          
          synchronized(lock){
            if(session.existsSocket(session.ourReaders,iwp))
              session.addSocket(session.otherReaders,iwp,newSocket,channel);
            else
              session.addSocket(session.ourReaders,iwp,newSocket,channel);
          }
          if(log.isDebugEnabled())
            log.debug("created socket");
        } catch (IOException ex) {
          if(log.isDebugEnabled())
            log.debug("error initiating connection. closing connection.");
          try {
            newSocket.close();
          } catch (IOException ex1) {}
        }
      }
    }
    try {
        if(!socket.isClosed())
            socket.close();
    } catch (IOException e) {
        e.printStackTrace();
    }
  }
  
  private int initProto(Socket socket) throws IOException{
    int port;
    final byte[] bufferPort = new byte[INT_SIZE];
    
    receiveNBytes(socket.getInputStream(),bufferPort, INT_SIZE);
    
    port = ParseUtils.byteArrayToInt(bufferPort,0);
    
    if(log.isDebugEnabled())
      log.debug("received remote port:: "+port);
    
    return port;
  }
  
  private int receiveNBytes(InputStream is,byte[] b,int length) throws IOException {
    int n=0,i=0;
    while(n!=length && i!=-1) {
      i=is.read(b,n,length-n);
      n+=i;
    }
    if(i==-1)
      throw new IOException();
    return n;
  }
  
	public synchronized void setRunning(boolean r){
		running = r;
        if(!running)
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
	}
	
	private synchronized boolean isRunning(){
		return running;
	}

    public int getPort(){
        return socket.getLocalPort();
    }
    
}
