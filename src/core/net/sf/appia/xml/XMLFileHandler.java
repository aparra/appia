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
 * Created on Mar 11, 2004
 *
 */
package net.sf.appia.xml;

import java.lang.reflect.InvocationTargetException;

import net.sf.appia.core.AppiaConfig;
import net.sf.appia.core.AppiaError;
import net.sf.appia.core.memoryManager.MemoryManager;
import net.sf.appia.xml.utils.ChannelProperties;
import net.sf.appia.xml.utils.SessionProperties;
import net.sf.appia.xml.utils.SharingState;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * This is the handler to be passed to the parser in order to parse
 * the XML file contents and retrieve the corresponding configuration.
 * 
 * @author Jose Mocito
 * 
 */
public class XMLFileHandler extends DefaultHandler {
	
	// The global configuration
	private Configuration config;
	// Protocol related attributes
	private boolean settingProtocol;
	// Channel related attributes
	private boolean creatingChannel;
	private String channelName;
	private String channelTemplateName;
	private String channelLabel;
	private String channelInitialized;
    private String channelManaged;
    private String channelMsgFactory;
	// Session related attributes
	private String sessionName;
	private boolean settingParameter;
	private String paramName;
	private ChannelProperties params;
	private SessionProperties currentParams;
	//Memory management related attributes
	private String mmSize, mmUPThreshold, mmDOWNThreshold;
	private MemoryManager memoryManager = null;
    //JMX Management variables
    private String namingHost,namingPort,local;
	// Auxiliary flag used to solve the problem of the characters() method not reading
	// all the text at once
	private boolean charactersUsed;
	// Acumulator of text gathered with the characters() method
	private StringBuffer charactersAcum;
	
	/**
	 * Builds a handler with an associated {@link Configuration}
	 * object where it stores the relevant information parsed from the XML contents.
	 * 
	 * @param 	config 	the {@link Configuration} object where the information will be stored.
	 * @see 			Configuration
	 * 
	 */
	public XMLFileHandler(Configuration config) {
		super();
		//params = new ChannelProperties();
		this.config = config;
	}
	
	/**
	 * @see org.xml.sax.ContentHandler#characters(char[], int, int)
	 */
	public void characters(char[] ch, int start, int length) {
		if (!charactersUsed)
			charactersUsed = true;
		charactersAcum.append(ch,start,length);
	}
	
	/**
	 * Processes the text gathered in the characters() method.
	 * 
	 * @throws SAXException
	 */
	private void processCharacters() throws SAXException {
		if (settingProtocol) {
			try {
				config.setProtocol(charactersAcum.toString());
			} catch (ClassNotFoundException e) {
				throw new SAXException(e);
			} catch (InstantiationException e) {
				throw new SAXException(e);
			} catch (IllegalAccessException e) {
				throw new SAXException(e);
			}
		}
		else if (creatingChannel && settingParameter) {
			currentParams.setProperty(paramName,charactersAcum.toString());
		}
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	public void startElement(String uri, String localName, String qName,
			Attributes attributes) throws SAXException {
		charactersAcum = new StringBuffer();
		if (qName.equals("appia")) {
			String att = attributes.getValue("multischedulers");
			if (att != null && att.equals("yes"))
				config.useMultiSchedulers(true);
            att = attributes.getValue("threadFactory");
            if(att != null && !att.equals("")){
                try {
                    config.setThreadFactory(att);
                } catch (ClassNotFoundException e) {
                    throw new SAXException(e);
                } catch (InstantiationException e) {
                    throw new SAXException(e);
                } catch (IllegalAccessException e) {
                    throw new SAXException(e);
                }
            }
			att = attributes.getValue("scheduler");
			if (att != null) {
				try {
					config.setEventScheduler(att);
				} catch (SecurityException e) {
					throw new SAXException(e);
				} catch (IllegalArgumentException e) {
					throw new SAXException(e);
				} catch (ClassNotFoundException e) {
					throw new SAXException(e);
				} catch (NoSuchMethodException e) {
					throw new SAXException(e);
				} catch (InstantiationException e) {
					throw new SAXException(e);
				} catch (IllegalAccessException e) {
					throw new SAXException(e);
				} catch (InvocationTargetException e) {
					throw new SAXException(e);
				}
			}
		}
		else if (qName.equals("template")) {
			config.addTemplate(attributes.getValue("name"));
		}
		else if (qName.equals("session")) {
			config.addSession(
					attributes.getValue("name"),
					SharingState.value(attributes.getValue("sharing")));
		}
		else if (qName.equals("protocol")) {
			settingProtocol = true;
		}
		else if (qName.equals("channel")) {
			params = new ChannelProperties();
			creatingChannel = true;
			memoryManager = null;
			channelName = attributes.getValue("name");
			channelTemplateName = attributes.getValue("template");
			channelInitialized = attributes.getValue("initialized");
             channelManaged = attributes.getValue("managed");
			channelLabel = attributes.getValue("label");
            channelMsgFactory = attributes.getValue("messageFactory");
		}
		else if (qName.equals("chsession")) {
			sessionName = attributes.getValue("name");
			currentParams = new SessionProperties();
		}
		else if (qName.equals("memorymanagement")) {
			if(!AppiaConfig.QUOTA_ON)
				throw new AppiaError("Memory management specified in XML configuration, but gobal static boolean is false.");
			mmSize = attributes.getValue("size");
			mmUPThreshold = attributes.getValue("up_threshold");
			mmDOWNThreshold = attributes.getValue("down_threshold");
		}
        else if (qName.equals("management")) {
            namingHost = attributes.getValue("naming_host");
            namingPort = attributes.getValue("naming_port");
            local = attributes.getValue("local");
            
        }
		else if (qName.equals("parameter")) {
			paramName = attributes.getValue("name");
			settingParameter = true;
		}
	}
	
	/* (non-Javadoc)
	 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	public void endElement(String uri, String localName, String qName)
			throws SAXException {
//		if (qName.equals("appia")) {
//			config.printConfig();
//		}
		
		if (charactersUsed) {
			charactersUsed = false;
			processCharacters();
		}
		
		if (qName.equals("protocol")) {
			settingProtocol = false;
		}
		else if (qName.equals("channel")) {
			boolean init = false, managed = false;
			if (channelInitialized.equals("yes"))
				init = true;
            if (channelManaged != null && channelManaged.equals("yes"))
                managed = true;
			if (config.usesGlobalScheduler())
				try {
					config.createChannel(channelName,channelTemplateName,channelLabel,params,init,
                            memoryManager,managed,channelMsgFactory);
				} catch (AppiaXMLException e) {
					throw new SAXException(e);
				}
			else
				config.storeChannel(channelName,channelTemplateName,channelLabel,params,init,
                        memoryManager,managed,channelMsgFactory);
			creatingChannel = false;
		}
		else if (qName.equals("chsession") && creatingChannel) {
			params.put(sessionName,currentParams);			
		}
		else if (qName.equals("memorymanagement") && creatingChannel) {
			memoryManager = new MemoryManager(channelName+" Memory Manager",
					Integer.parseInt(mmSize),
					Integer.parseInt(mmUPThreshold),
					Integer.parseInt(mmDOWNThreshold));
		}
        else if (qName.equals("management")) {
            if(namingHost != null)
                config.getJMXConfiguration().setNamingServer(namingHost);
            if(namingPort != null)
                config.getJMXConfiguration().setNamingPort(Integer.parseInt(namingPort));
            if(local != null)
                config.getJMXConfiguration().setLocal(local.equals("yes") ? true : false);
        }
		else if (qName.equals("parameter")) {
			settingParameter = false;
		}
		else if (qName.equals("appia")) {
			try {
				if (!config.usesGlobalScheduler())
					config.createChannels();
			} catch (AppiaXMLException e) {
				throw new SAXException(e);
			}
		}
	}
		
	//===========================================================
    // SAX ErrorHandler methods
    //===========================================================

    public void error(SAXParseException e) throws SAXParseException {
    	    throw e;
    }

    // treat warnings as fatal
    public void warning(SAXParseException e) throws SAXParseException {
//        System.out.println("** Warning"
//            + ", line " + err.getLineNumber()
//            + ", uri " + err.getSystemId());
//        System.out.println("   " + err.getMessage());
        throw e;
    }
    
	/**
	 * <p>Returns the current configuration.</p>
	 * 
	 * @return a {@link Configuration} object holding the current configuration.
	 */
	public Configuration configuration() {
		return config;
	}
}
