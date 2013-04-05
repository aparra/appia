/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2006 University of Lisbon
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 *
 * Initial developer(s): Alexandre Pinto and Hugo Miranda.
 * Contributor(s): See Appia web page for a list of contributors.
 */
 /*
 * Created on 25/Jan/2005
 *
 */
package net.sf.appia.xml.utils;

import java.util.LinkedList;

import net.sf.appia.core.EventScheduler;
import net.sf.appia.core.memoryManager.MemoryManager;


/**
 * @author Jose Mocito
 */
public class ChannelInfo {

	private String name;
	private String templateName;
	private String label;
	private ChannelProperties params;
	private boolean initialized;
	private EventScheduler eventScheduler;
	private MemoryManager memoryManager = null;
	private boolean managed;
    private String messageFactory=null;
    
	private LinkedList dependencies;
	
	/**
     * Builds a {@link ChannelInfo} object.
     * 
	 * @param  name            the name of the channel.
	 * @param  templateName    the name of the template.
	 * @param  label           the label of the channel.
	 * @param  params          the parameters of the channel.
	 * @param  initialized     whether or not the channel is to be initialized.
	 */
	public ChannelInfo(String name, String templateName, String label,
			ChannelProperties params, boolean initialized, String msgFact) {
		super();
		this.name = name;
		this.templateName = templateName;
		this.label = label;
		this.params = params;
		this.initialized = initialized;
        this.messageFactory = msgFact;
		
		this.dependencies = new LinkedList();
	}

	/**
     * Builds a {@link ChannelInfo} object.
     * 
	 * @param  name            the name of the channel.
     * @param  templateName    the name of the template.
     * @param  label           the label of the channel.
     * @param  params          the parameters of the channel.
     * @param  initialized     whether or not the channel is to be initialized.
	 * @param  memoryManager    the memory manager associated with the channel.
	 */
	public ChannelInfo(String name, String templateName, String label,
			ChannelProperties params, boolean initialized, MemoryManager memoryManager,
            String msgFact) {
		super();
		this.name = name;
		this.templateName = templateName;
		this.label = label;
		this.params = params;
		this.initialized = initialized;
		this.dependencies = new LinkedList();
		this.memoryManager = memoryManager;
        this.messageFactory = msgFact;
	}

	/**
	 * @return <i>true</i> if the channel is to be initialized.
     * <i>false</i> otherwise.
	 */
	public boolean isInitialized() {
		return initialized;
	}
	
    public void setInitialized(boolean initialized) {
		this.initialized = initialized;
	}
	
    /**
	 * @return the label of the channel.
	 */
	public String getLabel() {
		return label;
	}
	
    public void setLabel(String label) {
		this.label = label;
	}
	
    /**
	 * @return the name of the channel.
	 */
	public String getName() {
		return name;
	}
	
    public void setName(String name) {
		this.name = name;
	}
	
    /**
	 * @return the parameters of the channel.
	 */
	public ChannelProperties getParams() {
		return params;
	}
	
    public void setParams(ChannelProperties params) {
		this.params = params;
	}
	
    /**
	 * @return the template name.
	 */
	public String getTemplateName() {
		return templateName;
	}
	
    public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	
	/**
	 * @return the event scheduler instance associated with the channel.
	 */
	public EventScheduler getEventScheduler() {
		return eventScheduler;
	}
	
    public void setEventScheduler(EventScheduler eventScheduler) {
		this.eventScheduler = eventScheduler;
	}
	
    /**
     * @return the memory manager instance associated with the channel.
     */
	public MemoryManager getMemoryManager(){
		return memoryManager;
	}
	
	/**
	 * @return the dependencies of the channel.
	 */
	public LinkedList getDependencies() {
		return dependencies;
	}
    
    /**
     * Adds a dependency to the channel.
     * 
     * @param channel the channel from which the current channel instance depends.
     */
	public void addDependency(ChannelInfo channel) {
		if (!dependencies.contains(channel))
			dependencies.add(channel);
	}
	
    /**
     * Checks if the current channel depends on another defined channel.
     * 
     * @param channel the channel to be checked against the current instance.
     */
	public boolean depends(ChannelInfo channel) {
		if (dependencies.contains(channel))
			return true;
		else {
			for (int i = 0; i < dependencies.size(); i++) {
				final ChannelInfo cinfo = (ChannelInfo) dependencies.get(i);
				if (cinfo.depends(channel))
					return true;
			}
		}
		return false;
	}
	
    /**
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
	public boolean equals(Object arg) {
	    if(arg instanceof ChannelInfo){
	        final ChannelInfo cinfo = (ChannelInfo)arg;
	        return name.equals(cinfo.name);
	    }
        else
            return false;
	}

    /**
     * Checks the managed state.
     * 
     * @return <i>true</i> if this channel is managed; <i>false</i> otherwise.
     */
    public boolean isManaged() {
        return managed;
    }

    /**
     * @param managed <i>true</i> if the channel is to be managed; <i>false</i> otherwise.
     */
    public void setManaged(boolean managed) {
        this.managed = managed;
    }

    public String getMessageFactory() {
        return messageFactory;
    }

    public void setMessageFactory(String messageFactory) {
        this.messageFactory = messageFactory;
    }

}
