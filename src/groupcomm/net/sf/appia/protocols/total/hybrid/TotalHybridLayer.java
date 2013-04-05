package net.sf.appia.protocols.total.hybrid;

import net.sf.appia.core.Channel;
import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.group.events.GroupSendableEvent;
import net.sf.appia.protocols.group.intra.View;

/**
 * TotalHybridLayer is the layer for the protocol that provides total order
 * using an hybrid protocol.
 */
public class TotalHybridLayer extends Layer {

    /*
     * Basic Constructor.
     */
    public TotalHybridLayer() {
        // Events that the protocol requires
        evRequire = new Class[]{
                View.class,
        };

        //Events that the protocol accepts
        evAccept = new Class[]{
                net.sf.appia.core.events.channel.ChannelInit.class,
                View.class,
                net.sf.appia.protocols.group.sync.BlockOk.class,
                GroupSendableEvent.class,
                TotalHybridTimer.class,
                UniformTimer.class,
                UniformInfoEvent.class,
        };

        //Events that the protocol provides
        evProvide = new Class[]{
//                SpontaneousEvent.class,
//                RegularEvent.class,
        };
    }

    public Session createSession() {
        return new TotalHybridSession(this);
    }

    public void channelDispose(Session session,Channel channel) {
    }
}




