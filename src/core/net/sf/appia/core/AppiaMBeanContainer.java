package net.sf.appia.core;

import java.util.Hashtable;

import net.sf.appia.management.jmx.ChannelManager;

public class AppiaMBeanContainer {
    private Hashtable<String, ChannelManager> beans = new Hashtable<String, ChannelManager>();
    private static AppiaMBeanContainer instance = null;
    
    private AppiaMBeanContainer(){}
    
    public static AppiaMBeanContainer getInstance(){
        if(instance == null)
            instance = new AppiaMBeanContainer();
        return instance;
    }
    
    public void registerBean(String id, ChannelManager bean){
        beans.put(id, bean);
    }
    
    public ChannelManager getBean(String id){
        return beans.get(id);
    }
    
    public ChannelManager unregisterBean(String id){
        return beans.remove(id);
    }

}
