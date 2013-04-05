package net.sf.appia.protocols.total.symmetric;

import net.sf.appia.core.Layer;
import net.sf.appia.core.Session;


/**
 * TotalSymmetricLayer is the layer for the protocol that
 * provides total order using a causal based approach.
 */
public class TotalSymmetricLayer extends Layer {

    public TotalSymmetricLayer() {

        evRequire = new Class[]{
                net.sf.appia.protocols.group.events.GroupSendableEvent.class,
                net.sf.appia.protocols.total.symmetric.events.SymmetricAliveTimer.class,
                net.sf.appia.protocols.total.symmetric.events.SymmetricAlive.class,
                net.sf.appia.protocols.total.symmetric.events.SymmetricChangeTimer.class,
        };

        evAccept = new Class[]{
                evRequire[0],
                net.sf.appia.core.events.channel.ChannelInit.class,
                net.sf.appia.protocols.group.intra.View.class,
                net.sf.appia.protocols.group.sync.BlockOk.class,
                net.sf.appia.protocols.group.events.GroupInit.class,
                evRequire[1],
                evRequire[2],
                evRequire[3],
                net.sf.appia.protocols.group.suspect.Fail.class,
        };


        evProvide = new Class[]{
                evRequire[1],
                evRequire[2],
                evRequire[3],
        };
    }

    /**
     * Creates a session that corresponds to this layer.
     */
    public Session createSession() {
        return new TotalSymmetricSession(this);
    }
}




