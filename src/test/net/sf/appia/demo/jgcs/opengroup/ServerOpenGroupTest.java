/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2007 University of Lisbon
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
 * Developer(s): Nuno Carvalho.
 */
package net.sf.appia.demo.jgcs.opengroup;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.Hashtable;

import net.sf.appia.demo.jgcs.opengroup.Constants.MessageType;
import net.sf.appia.jgcs.AppiaGroup;
import net.sf.appia.jgcs.AppiaProtocolFactory;
import net.sf.appia.jgcs.AppiaService;
import net.sf.jgcs.ClosedSessionException;
import net.sf.jgcs.ControlListener;
import net.sf.jgcs.ControlSession;
import net.sf.jgcs.DataSession;
import net.sf.jgcs.ExceptionListener;
import net.sf.jgcs.JGCSException;
import net.sf.jgcs.Message;
import net.sf.jgcs.MessageListener;
import net.sf.jgcs.NotJoinedException;
import net.sf.jgcs.Protocol;
import net.sf.jgcs.ProtocolFactory;
import net.sf.jgcs.Service;
import net.sf.jgcs.ServiceListener;
import net.sf.jgcs.UnsupportedServiceException;
import net.sf.jgcs.membership.BlockListener;
import net.sf.jgcs.membership.BlockSession;
import net.sf.jgcs.membership.MembershipListener;
import net.sf.jgcs.membership.MembershipSession;

/**
 * This class defines a ServerOpenGroupTest.
 * This example shows how to use and configure Appia with jGCS
 * using an open group, where there is a group of servers that accept
 * Messages from external members. This is the server part.
 * 
 * The example only shows how to configure and use, and it only sends
 * dummy messages. It does not intend to implement any algorithm.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class ServerOpenGroupTest implements ControlListener, ExceptionListener,
		MembershipListener, BlockListener {
	
    private long viewChangeTime=0;
    
    /*
     * Class that implements a message listener
     */
	private class GroupMessageListener implements MessageListener,ServiceListener{

	    Service uniform = new AppiaService("uniform_total_order");
	    
	    /*
	     * All messages arrive here. Messages can be sent from
	     * clients or servers. Messages from servers are totally ordered
	     * and messages from clients arrive async. from another 
	     * communication channel.
	     */
		public Object onMessage(Message msg) {
			ProtocolMessage protoMsg=null;
            try {
                protoMsg = Constants.createMessageInstance(msg.getPayload());
            } catch (IOException e) {
                e.printStackTrace();
            }
            
            System.out.println("RECEIVED MESSAGE: "+protoMsg);
            
            if(protoMsg == null)
                return null;
			
			if(protoMsg instanceof ClientMessage){
                handleClientMessage((ClientMessage) protoMsg,msg.getSenderAddress());
                return null;
			}
			else if(protoMsg instanceof ServerMessage){
			    handleServerMessage((ServerMessage)protoMsg,msg.getSenderAddress());
			    return null;
			}
			return null;
		}
		
        public void onServiceEnsured(Object context, Service service) {
//            try {
//                if(service.compare(uniform)>=0){
//                    handleServerMessage((Message) context);
//                }
//            } catch (UnsupportedServiceException e) {
//                e.printStackTrace();
//            }
        }
		
		private void handleClientMessage(ClientMessage msg, SocketAddress addr){
			System.out.println("Received message from Client "+addr);
			try {
                msg.unmarshal();
            } catch (IOException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            } catch (ClassNotFoundException e2) {
                // TODO Auto-generated catch block
                e2.printStackTrace();
            }
			Message groupMsg = null;
			try {
				groupMsg = groupSession.createMessage();
			} catch (ClosedSessionException e) {
				e.printStackTrace();
			}
			ServerMessage serverMsg = new ServerMessage(msg.id,addr);
			try {
                serverMsg.marshal();
                byte[] bytes = Constants.createMessageToSend(MessageType.SERVER, serverMsg.getByteArray());
                groupMsg.setPayload(bytes);
            } catch (IOException e1) {
                e1.printStackTrace();
            }
			try {
			    times.put(msg.id, System.nanoTime());
			    System.out.println("added time for message #"+msg.id);
				// forward message to the servers, using the "group" Service
			    System.out.println("multicasting message to the group");
				groupSession.multicast(groupMsg,group, null);
			} catch (UnsupportedServiceException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}			
		}

		private void handleServerMessage(ServerMessage smsg, SocketAddress addr){
		    try {
		        smsg.unmarshal();
		        if(addr.equals(control.getLocalAddress())){
		            long deltaT = System.nanoTime()-times.remove(smsg.id);
		            System.out.println("TIME for message "+smsg.id+" : "+deltaT);
		        }
		        Message climsg = groupSession.createMessage();
		        ClientMessage myMsg = new ClientMessage(smsg.id);
		        myMsg.marshal();
		        byte[] bytes = Constants.createMessageToSend(MessageType.CLIENT, myMsg.getByteArray());
		        climsg.setPayload(bytes);
		        groupSession.send(climsg, clients, null, smsg.addr);
		    } catch (IOException e) {
		        e.printStackTrace();
		    } catch (ClassNotFoundException e) {
		        e.printStackTrace();
		    }
		}

	} // end of class GroupMessageListener

	private ControlSession control;
	private DataSession groupSession;
	private Service clients, group;

	private Hashtable<Integer, Long> times = new Hashtable<Integer, Long>();
	
	public ServerOpenGroupTest(ControlSession control, DataSession grSession, Service cl, Service gr) 
	throws JGCSException {
		this.control = control;
		this.groupSession = grSession;
		this.clients = cl;
		this.group = gr;

		// set listeners
		GroupMessageListener l = new GroupMessageListener();
		groupSession.setMessageListener(l);
		groupSession.setServiceListener(l);
		control.setControlListener(this);
		control.setExceptionListener(this);
		if (control instanceof MembershipSession)
			((MembershipSession) control).setMembershipListener(this);
		if (control instanceof BlockSession)
			((BlockSession) control).setBlockListener(this);

	}

	public void onJoin(SocketAddress peer) {
		System.out.println("-- JOIN: " + peer);
	}

	public void onLeave(SocketAddress peer) {
		System.out.println("-- LEAVE: " + peer);
	}

	public void onFailed(SocketAddress peer) {
		System.out.println("-- FAILED: " + peer);
	}

	public void onMembershipChange() {
        System.out.println("MEMBERSHIP: "+(System.currentTimeMillis()-viewChangeTime));
		try {
			System.out.println("-- NEW MEMBERSHIP: " + ((MembershipSession) control).getMembership());
		} catch (NotJoinedException e) {
			e.printStackTrace();
			groupSession.close();
		}			
	}

	// this notification is issued before a new view
	// a new view will not appear while the flush is not notified
	// (using the blockOk() method). After this, no message can be sent
	// while waiting for a new view.
	public void onBlock() {
	    viewChangeTime = System.currentTimeMillis();
	    System.out.println("BLOCK: "+viewChangeTime);
		try {
			((BlockSession) control).blockOk();
		} catch (JGCSException e) {
			e.printStackTrace();
		}
	}
	
	public void onExcluded() {
		System.out.println("-- EXCLUDED");
	}

	public void onException(JGCSException arg0) {
		System.out.println("-- EXCEPTION: " + arg0.getMessage());
		arg0.printStackTrace();
	}

	public void run() throws Exception {
	    // joins the group
		control.join();

		// wait forever.
		Thread.sleep(Long.MAX_VALUE);
	}

	public static void main(String[] args) {
	    if(args.length != 1){
	        System.out.println("Must put the xml file name as an argument.");
	        System.exit(1);
	    }
	    
		try {
            ProtocolFactory pf = new AppiaProtocolFactory();
            AppiaGroup g = new AppiaGroup();
            g.setGroupName("group");
            g.setConfigFileName(args[0]);
            Protocol p = pf.createProtocol();
            DataSession session = p.openDataSession(g);
            ControlSession control = p.openControlSession(g);
            Service sc = new AppiaService("rrpc");
            Service sg = new AppiaService("rrpc_group");
			ServerOpenGroupTest test = new ServerOpenGroupTest(control, session, sc, sg);
			test.run();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
