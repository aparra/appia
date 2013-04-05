package net.sf.appia.protocols.total.hybrid;

import java.io.Serializable;


public class ConfigProps implements Serializable{

	private static final long serialVersionUID = 4673486821511804212L;
	public String[] nodeids;
	public boolean[] active;
	
	public String myNode;
	public String sequencer;
	
	public ConfigProps(String[] ids, boolean[] act, String node, String seq){
		nodeids = ids;
		active = act;
		myNode = node;
		sequencer= seq;
	}
}
