package net.sf.appia.protocols.total.symmetric;

import net.sf.appia.protocols.group.events.GroupSendableEvent;

/**
 * This class represents a message that is ordered in total-causal order.
 */
public class MsgTotalSymmetric extends Object {
	
	public long est;
	public GroupSendableEvent evt;
	
	public MsgTotalSymmetric(GroupSendableEvent e) {
		est = ((Long)e.getMessage().popObject()).longValue();
		evt = e;	
	}
	
	public MsgTotalSymmetric(long est, GroupSendableEvent e) {
		this.est = est;
		evt = e;
	}
	
	public void colocaEstNoEvent() {
		evt.getMessage().pushObject(new Long(est));
	}
}
