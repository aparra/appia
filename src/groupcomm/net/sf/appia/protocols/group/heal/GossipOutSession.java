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

package net.sf.appia.protocols.group.heal;


import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;

import net.sf.appia.core.*;
import net.sf.appia.core.events.channel.ChannelClose;
import net.sf.appia.core.events.channel.ChannelInit;
import net.sf.appia.protocols.common.FIFOUndeliveredEvent;
import net.sf.appia.protocols.common.RegisterSocketEvent;
import net.sf.appia.protocols.fifo.FIFOConfigEvent;
import net.sf.appia.protocols.fifo.FifoLayer;
import net.sf.appia.protocols.group.LocalState;
import net.sf.appia.protocols.group.ViewState;
import net.sf.appia.protocols.group.events.GroupInit;
import net.sf.appia.protocols.group.intra.View;
import net.sf.appia.protocols.udpsimple.MulticastInitEvent;
import net.sf.appia.xml.interfaces.InitializableSession;
import net.sf.appia.xml.utils.SessionProperties;

import org.apache.log4j.Logger;


/**
 * The GossipOutSession, creates a parallel channel to be used to communicate
 * with the Gossip Server.
 * <br>
 * This channel, called "Gossip Channel", is composed of a FifoLayer and a
 * UdpSimpleLayer. If the main group communication channel uses a Session
 * of any of these layers, the Sessions are reused.
 * <br>
 * Its operation is simple. Any GossipOutEvent received in the main group
 * communication channel is switched to the Gossip Channel. The
 * destination field is filled with the
 * Gossip Server address.
 * <br>
 * Similary any GossipOutEvent received in the Gossip Channel is switched to
 * the main group communication channel. The destination and source fields are
 * not changed.
 *
 * @author Alexandre Pinto
 * @version 0.1
 * @see net.sf.appia.protocols.group.heal.GossipOutLayer
 * @see net.sf.appia.protocols.group.heal.GossipOutEvent
 * @see net.sf.appia.protocols.fifo.FifoLayer
 * @see net.sf.appia.protocols.udpsimple.UdpSimpleLayer
 * @see net.sf.appia.gossip.GossipServer
 * @see net.sf.appia.core.events.SendableEvent#dest
 * @see net.sf.appia.core.events.SendableEvent#source
 */
public class GossipOutSession extends Session implements InitializableSession {
    private static Logger log = Logger.getLogger(GossipOutSession.class);

    /**
     * Default gossip channel name.
     */
    public static final String DEFAULT_CHANNEL_NAME="Gossip Channel";
    /**
     * Number of retries before giving up on the current used server, and starts using the next supplied server.
     */
    public static final int DEFAULT_FIFO_RETRIES=0;
    /**
     * Layer to use for UDP transport.
     */
    public static final String DEFAULT_UDP_LAYER="net.sf.appia.protocols.udpsimple.UdpSimpleLayer";
    /**
     * Whether it should use, ie, share, the UDP transport layer from the main channel, if compatible.  
     */
    public static final boolean DEFAULT_SHARE_UDP=true;

    private String channel_name=DEFAULT_CHANNEL_NAME;
    private int fifo_retries=DEFAULT_FIFO_RETRIES;
    private String udp_layer_name=DEFAULT_UDP_LAYER;
    private boolean share_udp=DEFAULT_SHARE_UDP;

    /**
     * Creates a GossipOutSession.
     */
    public GossipOutSession(Layer layer) {
        super(layer);
    }

    /**
     * Initializes the session using the parameters given in the XML configuration.
     * Possible parameters:
     * <ul>
     * <li><b>CHANNEL_NAME</b> the name of the channel.
     * <li><b>FIFO_RETRIES</b> the number of retries before giving up on the current used server, and starts using the next supplied server.
     * <li><b>UDP_LAYER</b> String representation of the class that should be used as UDP layer.
     * <li><b>SHARE_UDP</b> true if UDP session should be shared, false otherwise.
     * </ul>
     * 
     * @param params The parameters given in the XML configuration.
     */
    public void init(SessionProperties params) {
        if (params.containsKey("CHANNEL_NAME"))
            channel_name=params.getString("CHANNEL_NAME");
        if (params.containsKey("FIFO_RETRIES"))
            fifo_retries=params.getInt("FIFO_RETRIES");
        if (params.containsKey("UDP_LAYER"))
            udp_layer_name=params.getString("UDP_LAYER");
        if (params.containsKey("SHARE_UDP"))
            share_udp=params.getBoolean("SHARE_UDP");
    }

    /**
     * The event handler.
     */
    public void handle(Event event) {

        // ChannelInit
        if (event instanceof ChannelInit) {
            handleChannelInit((ChannelInit)event);
            return;
            // ChannelClose
        } else if (event instanceof ChannelClose) {
            handleChannelClose((ChannelClose)event);
            return;
            // GroupInit
        } else if (event instanceof GroupInit) {
            handleGroupInit((GroupInit)event);
            return;
            // GossipOutEvent
        } else if (event instanceof GossipOutEvent) {
            handleGossipOutEvent((GossipOutEvent)event);
            return;
            // FIFOUndeliveredEvent
        } else if (event instanceof FIFOUndeliveredEvent) {
            handleFIFOUndelivered((FIFOUndeliveredEvent)event);
            return;
            // View
        } else if (event instanceof View) {
            handleView((View)event);
            return;
        }

        log.warn("Unwanted event (\""+event.getClass().getName()+"\") received. Continued...");
        try { event.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private ViewState vs;
    private LocalState ls;

    private HashMap in=new HashMap();
    private Channel out;

    private boolean requiresRSE;
    private SocketAddress outAddr;
    private InetSocketAddress[] gossipAddrs;
    private int server=-1;
    private boolean outCreated=false;

    private void handleChannelInit(ChannelInit ev) {

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        Channel c=ev.getChannel();

        if (c.getChannelID().equals(channel_name)) {
            out=c;

            if (vs != null)
                register();

            return;
        }


        in.put(c.getChannelID(),c);
        if ((out == null) && !outCreated) {
            createOutChannel(c);
            return;
        }
    }

    private void handleChannelClose(ChannelClose ev) {

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }

        if (ev.getChannel() == out) {
            out=null;
        } else {
            in.remove(ev.getChannel().getChannelID());
            if (in.size() == 0) {
                if (out != null)
                    out.end();
            }
        }
    }

    private void handleView(View ev) {
        if ((vs == null) && (out != null)) {
            vs=ev.vs;
            ls=ev.ls;
            register();
        } else {
            vs=ev.vs;
            ls=ev.ls;
        }

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private void handleGroupInit(GroupInit ev) {
        gossipAddrs=(InetSocketAddress[]) ev.getGossip();
        if ((gossipAddrs != null) && (gossipAddrs.length > 0))
            server=0;
        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private void handleGossipOutEvent(GossipOutEvent ev) {

        if (vs == null)
            return;

        if ((ev.getChannel() != out) && (ev.getDir() == Direction.UP)) {
            log.debug("Received upward GossipOutEvent. That's very strange but forwarding it !!!!");
            try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
            return;
        }

        if ((out != null) && (ev.getChannel() != out)) {
            if (ev.dest != null)
                log.debug("GossipOut has destination address. Ignoring it and sending to server.");

            if (server < 0) {
                log.warn("No server available, discarding message");
                return;
            }
            ev.dest=gossipAddrs[server];

            if (debugFull)
                log.debug("placing my address "+outAddr.toString()+" on message to "+ev.dest);

            ev.getMessage().pushString(ev.getChannel().getChannelID());
            ev.getMessage().pushObject(outAddr);

            try {
                ev.setChannel(out);
                ev.setDir(Direction.DOWN);
                ev.setSourceSession(this);
                ev.init();
                ev.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
                log.warn("Impossible to send gossip in \"out\" channel");
            }

            return;
        }

        if ((ev.getChannel() == out) && (in != null)) {
            ev.source = ev.getMessage().popObject();
            String channelName=ev.getMessage().popString();

            if (debugFull)
                log.debug("Received message from "+ev.source+" for channel \""+channelName+"\".");

            Channel channel=(Channel)in.get(channelName);
            if (channel == null) {
                log.debug("Received message for unknown channel ("+channelName+") ignoring it.");
                return;
            }

            try {
                ev.setChannel(channel);
                ev.setDir(Direction.UP);
                ev.setSourceSession(this);
                ev.init();
                ev.go();
            } catch (AppiaEventException ex) {
                if (debugFull)
                    ex.printStackTrace();
                log.warn("Impossible to send gossip in \"in\" channel");
            }

            return;
        }
    }

    private void handleFIFOUndelivered(FIFOUndeliveredEvent ev) {
        if (server < 0)
            return;

        if ((ev.getChannel() == out) && gossipAddrs[server].equals(ev.getEvent().dest)) {
            if (server == gossipAddrs.length-1)
                server=0;
            else
                server++;
        }

        try { ev.go(); } catch (AppiaEventException ex) { ex.printStackTrace(); }
    }

    private void createOutChannel(Channel channel) {
        try {
            Layer[] l=new Layer[3];

            ChannelCursor cursor=channel.getCursor();
            cursor.bottom();

            Layer main_bottom_layer=cursor.getLayer();
            Class udp_layer_class=Class.forName(udp_layer_name);
            if ((share_udp) && udp_layer_class.isInstance(main_bottom_layer))
                udp_layer_class=null;

            if (udp_layer_class == null) {
                l[0]=main_bottom_layer;
                requiresRSE=false;
            } else {
                l[0]=(Layer)udp_layer_class.newInstance();
                requiresRSE=true;
            }

            l[1]=new FifoLayer();
            l[2]=this.layer;

            Channel oChannel=(new QoS("Gossip Out QoS",l)).createUnboundChannel(channel_name,channel.getEventScheduler());
            ChannelCursor oCursor=oChannel.getCursor();
            oCursor.bottom();

            if (udp_layer_class == null)
                oCursor.setSession(cursor.getSession());

            oCursor.up();
            // Fifo is created by default
            oCursor.up();
            oCursor.setSession(this);

            oChannel.start();
            outCreated = true;

            if (debugFull) {
                log.debug("Sharing UDP ? "+(udp_layer_class == null)+" (share_udp="+share_udp+" udp_layer="+udp_layer_name+" main_bottom_layer="+main_bottom_layer+")");
            }


        } catch (AppiaCursorException ex) {
            ex.printStackTrace();
            log.warn("Unable to create \"out\" channel, GossipOutEvents will be lost");
        } catch (AppiaInvalidQoSException ex) {
            ex.printStackTrace();
            log.warn("Unable to create \"out\" channel, GossipOutEvents will be lost");
        } catch (AppiaDuplicatedSessionsException ex) {
            ex.printStackTrace();
            log.warn("Unable to create \"out\" channel, GossipOutEvents will be lost");
        } catch (Exception ex) {
            ex.printStackTrace();
            log.warn("Unable to create \"out\" channel, GossipOutEvents will be lost");      
        }
    }

    private void register() {
        outAddr=vs.addresses[ls.my_rank];

        if (requiresRSE) {
            try {
                RegisterSocketEvent rse=new RegisterSocketEvent(out,Direction.DOWN,this,0);
                rse.go();
            } catch (AppiaEventException ex) {
                ex.printStackTrace();
                log.warn("Unable to register Gossip Out socket. Unable to locate concurrent views. Unable to merge.");
            }
        }

        try {
            FIFOConfigEvent fifoconf=new FIFOConfigEvent(out,Direction.DOWN,this);
            fifoconf.setRetries(fifo_retries);
            fifoconf.go();
        } catch (AppiaEventException ex) {
            ex.printStackTrace();
            log.warn("Unable to config Fifo layer. It will work but not optimized");
        }

        if (gossipAddrs != null) {
            for (int i=0 ; i < gossipAddrs.length ; i++) {
                if (gossipAddrs[i].getAddress().isMulticastAddress()) {
                    try {
                        MulticastInitEvent e=new MulticastInitEvent(gossipAddrs[i],false,out,Direction.DOWN,this);
                        e.go();

                        log.debug("Registering gossip multicast address: "+gossipAddrs[i]);
                    } catch (AppiaEventException ex) {
                        ex.printStackTrace();
                        log.warn("Impossible to register multicast address.");
                    }
                }
            }
        }
    }

    /**
     * Gets the outgoing channel.
     * @return Channel
     */
    public Channel getOutChannel() {
        return out;
    }

    /**
     * Gets the outgoing address.
     * @return the outgoing address
     */
    public SocketAddress getOutAddress() {
        return outAddr;
    }

    /**
     * Gets the view state.
     * @return ViewState
     */
    public ViewState getViewState() {
        return vs;
    }

    // DEBUG
    public static final boolean debugFull = true;
}
