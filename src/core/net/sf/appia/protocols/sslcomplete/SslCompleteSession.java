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
 package net.sf.appia.protocols.sslcomplete;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyStore;
import java.util.Hashtable;
import java.util.Random;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Event;
import net.sf.appia.core.Layer;
import net.sf.appia.core.events.AppiaMulticast;
import net.sf.appia.core.events.SendableEvent;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.tcpcomplete.AcceptReader;
import net.sf.appia.protocols.tcpcomplete.SocketInfoContainer;
import net.sf.appia.protocols.tcpcomplete.TcpCompleteSession;
import net.sf.appia.protocols.utils.HostUtils;
import net.sf.appia.protocols.utils.ParseUtils;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import org.apache.log4j.Logger;

/**
 * This class defines a SslCompleteSession
 * 
 * @author Alexandre Pinto and Pedro Vicente and Nuno Carvalho 
 * @version 1.0
 */
public class SslCompleteSession extends TcpCompleteSession implements InitializableSession {
  
  private SSLServerSocketFactory ssf=null;
  private SSLSocketFactory sf=null;
  
    /*
     * Protocol used in secure communication.
     * Ex: "SSL", "TLS"
     */
    private String protocol = "SSL";
    
    /*
     * Certificates implementation used in SSL authentication.<br>
     * Used to create the KeyManagers and TrustManagers that determine, respectively,
     * the certificate sent in authentication and if the certificate received is accepted.
     */ 
    private String certificateManagers = "SunX509";
    
    /*
     * KeyStore, format  used to store the keys.
     * Ex: "JKS"
     */
    private String keyStore = "JKS";
    
    /*
     * Name of the file were the certificates are stored.
     * The identity certificate sent in authentication, and the recognized/trusted certificates
     * used to authenticate the peer must be in the file.
     * <b>Required for peer authentication</b>.
     */
    private String keystoreFile=null;
    
    /*
     * Passphrase to access the file where the certificate is stored.
     */
    private char[] passphrase=null;
    
    /*
     * Enabled ciphers used in SSL.<br>
     * See JSSE documentation
     */
    private String[] enabledCiphers=null;

  private static Logger log = Logger.getLogger(SslCompleteSession.class);

  /**
   * Constructor for NewTcpSession.
   * @param layer
   */
  public SslCompleteSession(Layer layer) {
    super(layer);
  }

  /**
   * Initializes the session using the parameters given in the XML configuration.
   * Possible parameters:
   * <ul>
   * <li><b>protocol</b> Protocol used in secure communication.
   * Ex: "SSL", "TLS". Default is SSL.
   * <li><b>certificate_managers</b> Certificates implementation used in SSL authentication.<br>
   * Used to create the KeyManagers and TrustManagers that determine, respectively,
   * the certificate sent in authentication and if the certificate received is accepted. Default is "SunX509".
   * <li><b>keystore</b> KeyStore is the format  used to store the keys. Default is "JKS".
   * <li><b>keystore_file</b> Name of the file were the certificates are stored.
   * The identity certificate sent in authentication, and the recognized/trusted certificates
   * used to authenticate the peer must be in the file.
   * <b>Required for peer authentication</b>.
   * <li><b>passphrase</b> Passphrase to access the file where the certificate is stored.
   * <li><b>enabled_ciphers</b> Enabled ciphers used in SSL. See JSSE documentation for more details.
   * </ul>
   * 
   * @param props The parameters given in the XML configuration.
   * @see net.sf.appia.protocols.tcpcomplete.TcpCompleteSession#init(net.sf.appia.xml.utils.SessionProperties)
   */
  public void init(SessionProperties props){
      super.init(props);
      
      if(props.containsKey("protocol"))
          protocol = props.getString("protocol");
      if(props.containsKey("certificate_managers"))
          certificateManagers = props.getString("certificate_managers");
      if(props.containsKey("keystore"))
          keyStore = props.getString("keystore");
      if(props.containsKey("keystore_file"))
          keystoreFile = props.getString("keystore_file");
      if(props.containsKey("passphrase"))
          passphrase = props.getCharArray("passphrase");
      if(props.containsKey("enabled_ciphers"))
          enabledCiphers = props.getString("enabled_ciphers").split(",");

      if(log.isDebugEnabled())
          log.debug("SSL parameters after XML init:\n"+
                  "[protocol="+protocol+" certificate_managers="+certificateManagers+" key_store="+keyStore+
                  " keystore_file="+keystoreFile+" passphrase="+new String(passphrase)+" enabled_ciphers="+enabledCiphers+"]");
  }

  /**
   * 
   * @see net.sf.appia.protocols.tcpcomplete.TcpCompleteSession#handle(net.sf.appia.core.Event)
   */
  public void handle(Event e){
    if(e instanceof SendableEvent)
      handleSendable((SendableEvent)e);
    else if(e instanceof SslRegisterSocketEvent)
      handleSslRegisterSocket((SslRegisterSocketEvent)e);
    else if(e instanceof RegisterSocketEvent)
        handleRegisterSocket((RegisterSocketEvent) e);
    else
      super.handle(e);
  }
  
  private void handleSendable(SendableEvent e){
    Object[] valids=null;
    
    if(log.isDebugEnabled())
        log.debug("Preparing to send event "+e);
    
    if (e.dest instanceof AppiaMulticast) {
      final Object[] dests=((AppiaMulticast)e.dest).getDestinations();
      for (int i=0 ; i < dests.length ; i++) {
        if (dests[i] instanceof InetSocketAddress) {
          if (!validate((InetSocketAddress)dests[i], e.getChannel())) {
            if (valids == null) {
              valids=new Object[dests.length];
              System.arraycopy(dests, 0, valids, 0, i);
            }
            valids[i]=null;
            sendUndelivered(e.getChannel(), (InetSocketAddress) dests[i]);
          } else {
            if (valids != null)
              valids[i]=dests[i];
          }
        } else
          sendUndelivered(e.getChannel(),(InetSocketAddress) dests[i]);
      }
    } else if (e.dest instanceof InetSocketAddress) {
      if (!validate((InetSocketAddress)e.dest, e.getChannel()))
        sendUndelivered(e.getChannel(), (InetSocketAddress) e.dest);
    } else {
      sendUndelivered(e.getChannel(),(InetSocketAddress) e.dest);
    }
    
    if (valids != null) {
      int i,tam=0,j=0;
      for (i=0 ; i < valids.length ; i++)
        if (valids[i] != null)
          tam++;
      final Object[] trimmedDests=new Object[tam];
      for (i=0 ; i < valids.length ; i++) {
        if (valids[i] != null) {
          trimmedDests[j]=valids[i];
          j++;
        }
      }
      e.dest = new AppiaMulticast(((AppiaMulticast)e.dest).getMulticastAddress(), trimmedDests);
    }
    
    super.handle(e);
  }
  
  private boolean validate(InetSocketAddress dest, Channel channel) {
    
    try {
      //check if the socket exist int the opensockets created by us
      if(existsSocket(ourReaders,dest)){
        if(log.isDebugEnabled())
            log.debug("recognized our ssl socket. sending...");
        return true;
      }
      else{//if not
        //check if socket exist in sockets created by the other
        if(existsSocket(otherReaders,dest)){
          if(log.isDebugEnabled())
              log.debug("recognized other ssl socket. sending...");
          return true;
        }
        else{//if not
          //create new socket and put it opensockets created by us
          if(createSSLSocket(ourReaders,dest,channel) != null)
            if(log.isDebugEnabled())
                log.debug("created new ssl socket, sending...");
          return true;
        }
      }
    } catch (IOException ex) {
      if(log.isDebugEnabled()) {
        ex.printStackTrace();
        log.debug("Member "+dest.toString()+" has failed.");
      }
      sendUndelivered(channel,dest);
//      removeSocket(dest);
    }
    return false;
  }

  /**
   * 
   * @see net.sf.appia.protocols.tcpcomplete.TcpCompleteSession#handleRegisterSocket(net.sf.appia.protocols.common.RegisterSocketEvent)
   */
  protected void handleRegisterSocket(RegisterSocketEvent e){
      final int bindedPort = registerWithSSL(e.port, e.getChannel());

      e.port = bindedPort;
      e.localHost = HostUtils.getLocalAddress();
      e.error = (bindedPort < 0);
      
      //        send RegisterSocketEvent
      e.setDir(Direction.invert(e.getDir()));
      e.setSourceSession(this);
      
      try {
        e.init();
        e.go();
      } catch (AppiaEventException ex) {
          if(log.isDebugEnabled())
              ex.printStackTrace();
      }
  }

  /**
   * 
   * @param e
   */
  private void handleSslRegisterSocket(SslRegisterSocketEvent e){
    if(log.isDebugEnabled())
        log.debug("Received SSL register socket event: "+e);
    
    protocol = e.protocol;
    certificateManagers = e.certificateManagers;
    keyStore = e.keyStore;
    keystoreFile = e.keystoreFile;
    passphrase = e.passphrase;
    enabledCiphers = e.enabledCiphers;
    
    final int bindedPort = registerWithSSL(e.port, e.getChannel());
    e.port = bindedPort;
    e.error = (bindedPort < 0);
    
    //		send RegisterSocketEvent
    e.setDir(Direction.invert(e.getDir()));
    e.setSourceSession(this);
    
    try {
      e.init();
      e.go();
    } catch (AppiaEventException ex) {
        if(log.isDebugEnabled())
            ex.printStackTrace();
    }
  }
  
  /**
   * 
   * @param port
   * @param channel
   * @return
   */
  private int registerWithSSL(int port, Channel channel){
      SSLServerSocket ss= null;
      
      try{
        final SSLContext ctx = SSLContext.getInstance(protocol);
        
        if (keystoreFile != null) {
          final KeyStore ks = KeyStore.getInstance(keyStore);
          ks.load(new FileInputStream(keystoreFile),passphrase);
          
          final KeyManagerFactory kmf = KeyManagerFactory.getInstance(certificateManagers);
          kmf.init(ks,passphrase);
          final TrustManagerFactory tmf=TrustManagerFactory.getInstance(certificateManagers);
          tmf.init(ks);
          
          ctx.init(kmf.getKeyManagers(),tmf.getTrustManagers(),null);
          ssf = ctx.getServerSocketFactory();
          sf = ctx.getSocketFactory();
          
        } else {
          
          ctx.init(null,null,null);
          ssf = new CustomSSLServerSocketFactory(ctx,true);
          sf = new CustomSSLSocketFactory(ctx,true);
        }
        
        if (log.isDebugEnabled()) {
          int i;
          String[] suites;
          String output = "Configuration dump:\n";
          output += "--> Server Supported cipher suites\n";
          suites=ssf.getSupportedCipherSuites();
          for (i=0 ; i < suites.length ; i++)
            output += (suites[i]+"\n");
          output += ("--> Server Default cipher suites\n");
          suites=ssf.getDefaultCipherSuites();
          for (i=0 ; i < suites.length ; i++)
            output += (suites[i]+"\n");
          output +=("--> Client Supported cipher suites\n");
          suites=sf.getSupportedCipherSuites();
          for (i=0 ; i < suites.length ; i++)
            output += (suites[i]+"\n");
          output += ("--> Client Default cipher suites\n");
          suites=sf.getDefaultCipherSuites();
          for (i=0 ; i < suites.length ; i++)
            output += (suites[i]+"\n");
          log.debug(output);
        }
        
      }
      catch(Exception ex){
        if(log.isDebugEnabled())
            ex.printStackTrace();
        log.warn("An error ocurred when initializing SSL session though the SSL register socket event: "+ex.getMessage());
        return -1;
      }
      
      if(port == SslRegisterSocketEvent.FIRST_AVAILABLE){
        try {
          ss = (SSLServerSocket)ssf.createServerSocket(0);
          port = ss.getLocalPort();
        } catch (IOException ex) {
            return -1;
        }
      }
      else if(port == SslRegisterSocketEvent.RANDOMLY_AVAILABLE){
        final Random rand = new Random();
        int p;
        boolean done = false;
        
        while(!done){
          p = rand.nextInt(Short.MAX_VALUE);
          
          try {
            ss = (SSLServerSocket)ssf.createServerSocket(p);
            done = true;
            port = ss.getLocalPort();
          } catch(IllegalArgumentException ex){
              if(log.isDebugEnabled())
                  ex.printStackTrace();
          } catch (IOException ex) {
              if(log.isDebugEnabled())
                  ex.printStackTrace();
          }
        }
      } else if (port > 0) {
        try {
          ss = (SSLServerSocket)ssf.createServerSocket(port);
        } catch (IOException ex) {
            if(log.isDebugEnabled())
                ex.printStackTrace();
          port = -1;
        }
      }
      
      if (port > 0) {
        //create accept thread with the requested port.
        // FIXME: this is using class from tcpcomplete
          // comment: it should be Ok because it extends the class anyway...
        acceptThread = new AcceptReader(ss,this,channel,socketLock);
        final Thread t = channel.getThreadFactory().newThread(acceptThread);
        t.setName("TCP SSL accept reader");
        t.start();
        ourPort = ss.getLocalPort();
        if(log.isDebugEnabled())
          log.debug("Local port is "+ourPort);
      }
      else
          ourPort = -1;
      return ourPort;
  }
  
  /**
   * Create the socket, put in hashmap and create thread
   * @param hm
   * @param iwp
   * @param channel
   * @return the new socket or null if an error occurred.
   * @throws IOException
   */
  protected Socket createSSLSocket(Hashtable<InetSocketAddress,SocketInfoContainer> hm,
          InetSocketAddress iwp,Channel channel) throws IOException{
    synchronized(socketLock){
      Socket newSocket = null;
      
      if (sf == null)
        return null;
      
      //Create SslSocket.
      newSocket  = (SSLSocket)sf.createSocket(iwp.getAddress(),iwp.getPort());

      newSocket.setTcpNoDelay(true);
      
      final byte bPort[]= ParseUtils.intToByteArray(ourPort);
      
      newSocket.getOutputStream().write(bPort);
      if(log.isDebugEnabled())
        log.debug("Sending our original port "+ourPort);
      addSocket(hm,iwp,newSocket,channel);
      return newSocket;
    }
  }

}
