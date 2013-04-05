
package net.sf.appia.protocols.total.symmetric.events;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.EventQualifier;
import net.sf.appia.core.events.channel.Timer;
import net.sf.appia.protocols.total.symmetric.TotalSymmetricSession;

public class SymmetricChangeTimer extends Timer {
    
    public SymmetricChangeTimer(Channel c, TotalSymmetricSession gen)
	throws AppiaEventException, AppiaException {
	super(TotalSymmetricSession.CAUSAL_MUDANCA_TIMEOUT, "causal change timer", 
        c, Direction.DOWN, gen,EventQualifier.ON);
    }
}
