package net.sf.appia.protocols.total.hybrid;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.message.Message;
import net.sf.appia.protocols.group.Group;
import net.sf.appia.protocols.group.ViewID;
import net.sf.appia.protocols.group.events.GroupSendableEvent;


public class UniformInfoEvent extends GroupSendableEvent {

    public UniformInfoEvent() {
        super();
    }

    public UniformInfoEvent(Channel channel, int dir, Session source,
            Group group, ViewID view_id, Message omsg)
            throws AppiaEventException {
        super(channel, dir, source, group, view_id, omsg);
    }

    public UniformInfoEvent(Channel channel, int dir, Session source,
            Group group, ViewID view_id) throws AppiaEventException {
        super(channel, dir, source, group, view_id);
    }

    public UniformInfoEvent(Message omsg) {
        super(omsg);
    }
    
}
