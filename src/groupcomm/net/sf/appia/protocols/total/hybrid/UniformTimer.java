package net.sf.appia.protocols.total.hybrid;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.PeriodicTimer;


public class UniformTimer extends PeriodicTimer {
	
	public UniformTimer(){
		super();
	}
	
	public UniformTimer(long timer,Channel channel, int dir, Session source, int qualifier)  
	    throws AppiaEventException, AppiaException{
		    super("UniformTotalHybridTimer",timer,channel,dir,source,qualifier);
	}
	
}
