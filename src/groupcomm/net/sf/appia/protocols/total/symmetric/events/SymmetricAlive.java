package net.sf.appia.protocols.total.symmetric.events;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Direction;
import net.sf.appia.core.Session;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.events.GroupSendableEvent;

public class SymmetricAlive extends GroupSendableEvent implements Cloneable {
	
	public SymmetricAlive() throws AppiaEventException {
		super();
	}
	
	public SymmetricAlive(Channel channel, Session source, Group group,
			ViewID view_id, long est) 
	throws AppiaEventException {
		super(channel,Direction.DOWN,source,group,view_id);
		getMessage().pushObject(new Long(est));
		init(); 
	}
}
