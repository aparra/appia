package net.sf.appia.protocols.total.hybrid;

import net.sf.appia.core.AppiaEventException;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.Session;
import net.sf.appia.core.events.channel.PeriodicTimer;

public class TotalHybridTimer extends PeriodicTimer {
	
	public static final long DEFAULT_PERIOD=5;
	
	public TotalHybridTimer(){
		super();
	}
	
	public TotalHybridTimer(Channel channel, int dir, Session source, int qualifier) 
	throws AppiaEventException,AppiaException{
		super("TotalHybridTimer",DEFAULT_PERIOD,channel,dir,source,qualifier);
	}
	
	public TotalHybridTimer(long timer,Channel channel, int dir, Session source, int qualifier)  throws AppiaEventException, AppiaException{
		super("TotalHybridTimer",timer,channel,dir,source,qualifier);
	}
	
}
