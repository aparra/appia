package net.sf.appia.protocols.total.symmetric.events;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.events.channel.Timer;
import net.sf.appia.protocols.total.symmetric.TotalSymmetricSession;

public class SymmetricAliveTimer extends Timer {

	public SymmetricAliveTimer(
		String id,
		long when,
		Channel c,
		TotalSymmetricSession gen,
		int eq)
		throws AppiaEventException, AppiaException {
		super(when, id, c, Direction.DOWN, gen, eq);
	}
}
