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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;

import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import net.sf.appia.core.Appia;
import net.sf.appia.core.AppiaException;
import net.sf.appia.core.Channel;
import net.sf.appia.core.memoryManager.MemoryManager;
import net.sf.appia.xml.utils.ChannelProperties;

import org.apache.log4j.Logger;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


/**
 * This class implements methods to create channels using XML
 * definitions in a File or String.
 * <p>
 * To better understand how this class works please refer
 * to <i>AppiaXML: A Brief Tutorial</i> found in the Appia website.
 * 
 * @author Jose Mocito
 *
 */
public class AppiaXML {
	
    private static Logger log = Logger.getLogger(AppiaXML.class);

	// Template groups list
	//private LinkedList templateGroups;
	// Channel templates list
	//private LinkedList channelTemplates;
	// Instantiated channels
	//private LinkedList channels;
	// SAX Parser
	private SAXParser parser;
	// Appia's XML File Handler
	private XMLFileHandler handler;
	// Configuration
	private Configuration config;
	// The current AppiaXML instance
	private static AppiaXML appiaxml = new AppiaXML();
	
	/**
	 * Initializes AppiaXML.
	 *
	 */
	private AppiaXML() {
		//templateGroups = new LinkedList();
		//channelTemplates = new LinkedList();
		//channels = new LinkedList();
		
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			parser = factory.newSAXParser();
		// None of the above exceptions should ever happen if the parser
		// used is the one supplied with Java2
		} catch (ParserConfigurationException e) {
            log.fatal("Exception creating XML parser: "+e);
			e.printStackTrace();
			System.exit(-1);
		} catch (FactoryConfigurationError e) {
            log.fatal("Exception creating XML parser: "+e);
			e.printStackTrace();
			System.exit(-1);
		} catch (SAXException e) {
            log.fatal("Exception creating XML parser: "+e);
			e.printStackTrace();
			System.exit(-1);
		}
		
//		config = new Configuration();
//		handler = new XMLFileHandler(config);
	}
	
	/**
	 * Builds an empty configuration.
	 *
	 */
	private AppiaXML(Appia appia) throws AppiaXMLException{
		//templateGroups = new LinkedList();
		//channelTemplates = new LinkedList();
		//channels = new LinkedList();
		
		try {
			final SAXParserFactory factory = SAXParserFactory.newInstance();
			factory.setValidating(true);
			parser = factory.newSAXParser();
		// None of the above exceptions should ever happen if the parser
		// used is the one supplied with Java2
		} catch (ParserConfigurationException e) {
            throw new AppiaXMLException("Unable to get SAX parser.",e);
		} catch (FactoryConfigurationError e) {
            throw new AppiaXMLException("Unable to get SAX parser.",e);
		} catch (SAXException e) {
            throw new AppiaXMLException("Unable to get SAX parser.",e);
		}
		
		config = new Configuration(appia);
		handler = new XMLFileHandler(config);
	}
	
	/**
	 * Returns a new instance of <tt>AppiaXML</tt> associated with the given
	 * <tt>Appia</tt> instance. All channels created by the returned instance
	 * will belong to the <tt>Appia</tt> instanced given as argument.
	 * 
	 * @param appia the <tt>Appia</tt> instance where channels will be created.
	 * @return a new <tt>AppiaXML</tt> instance.
	 * @throws AppiaXMLException 
	 */
	public static AppiaXML getInstance(Appia appia) throws AppiaXMLException {
		return new AppiaXML(appia);
	}
	
	/**
	 * Loads the configuration in the <i>xmlfile</i> file.
	 *  
	 * @param 	xmlfile 		the File where the configuration resides.
	 * @throws 	SAXException 	if there is an error during the parsing of the file.
	 * 							In that case, the thrown exception is wrapped inside
	 * 							the SAXException.
	 * @throws 	IOException 	if there is an error accessing the file.
	 */
	public static void load(File xmlfile) throws SAXException, IOException {
		appiaxml.instanceLoad(xmlfile, null);
	}
	
	/**
	 * Loads the configuration in the <i>xmlfile</i> file.
	 *  
	 * @param 	xmlfile 		the File where the configuration resides.
	 * @param 	appia			the <tt>Appia</tt> instance where channels are created.
	 * @throws 	SAXException 	if there is an error during the parsing of the file.
	 * 							In that case, the thrown exception is wrapped inside
	 * 							the SAXException.
	 * @throws 	IOException 	if there is an error accessing the file.
	 * 
	 */
	public static void load(File xmlfile, Appia appia) throws SAXException, IOException {
		appiaxml.instanceLoad(xmlfile, appia);
	}

	/**
	 * Auxiliary method.
	 * <p>
	 * <b>INTERNAL USE ONLY!</b>
	 * 
	 * @param 	xmlfile
	 * @throws 	SAXException
	 * @throws 	IOException
	 * 
	 * TODO UPDATE COMMENTS (CHANGED TO SUPPORT OTHER APPIA INSTANCE)
	 */
	public void instanceLoad(File xmlfile, Appia appia) throws SAXException, IOException {
		if (appia == null) {
			if (config == null) {
				config = new Configuration();
				handler = new XMLFileHandler(config);
			}
		}
		else
			if (config == null) {
				try {
                    config = new Configuration(appia);
                } catch (AppiaXMLException e) {
                    throw new SAXException(e);
                }
				handler = new XMLFileHandler(config);
			}
        log.info("Loading XML configuration from file: "+xmlfile);
		parser.parse(xmlfile,handler);
	}
	
	/**
	 * Loads the configuration in the <i>xmlstr</i> string.
	 * 
	 * @param	xmlstr			the String containing the XML description.
	 * @throws 	IOException		if there is an error accessing the string.
	 * @throws 	SAXException	if there is an error during the parsing of the string.
	 * 							In that case, the thrown exception is wrapped inside
	 * 							the SAXException.
	 */
	public static void load(String xmlstr) throws SAXException, IOException {
		appiaxml.instanceLoad(xmlstr,null);
	}
	
	/**
	 * Loads the configuration in the XML that is represented by a String.
	 * 
	 * @param	xmlstr			the String containing the XML description.
	 * @throws 	IOException		if there is an error accessing the string.
	 * @throws 	SAXException	 if there is an error during the parsing of the string.
	 * 							In that case, the thrown exception is wrapped inside
	 * 							the SAXException.
	 * 
	 */
	public static void load(String xmlstr, Appia appia) throws SAXException, IOException {
		appiaxml.instanceLoad(xmlstr,appia);
	}
	
	/**
	 * Auxiliary method.
	 * <p>
	 * <b>INTERNAL USE ONLY!</b>
	 * 
	 * @param	xmlstr
	 * @throws IOException
	 * @throws SAXException
	 */
	public void instanceLoad(String xmlstr, Appia appia) throws SAXException, IOException {
		if (appia == null)
			if (config == null)
				config = new Configuration();
		else
			if (config == null)
                try {
                    config = new Configuration(appia);
                } catch (AppiaXMLException e) {
                    throw new SAXException(e);
                }
		handler = new XMLFileHandler(config);
        log.info("Loading XML configuration from a char stream...");
		parser.parse(new InputSource(new StringReader(xmlstr)),handler);
	}
	
	/**
	 * Loads the configuration in the <i>xmlfile</i> file and
	 * "runs" Appia (calls Appia's run() method), initiating
	 * all channel activities.
	 * <p>
	 * @param 	xmlfile 		the File where the configuration resides.
	 * @throws 	SAXException 	if there is an error during the parsing of the file.
	 * 							In that case, the thrown exception is wrapped inside
	 * 							the SAXException.
	 * @throws 	IOException 	if there is an error accessing the file.
	 */
	public static void loadAndRun(File xmlfile) throws SAXException, IOException {
		appiaxml.instanceLoad(xmlfile,null);
		Appia.run();
	}
	
	/**
	 * Loads the configuration in the <i>xmlstr</i> string and
	 * "runs" Appia (calls Appia's run() method), initiating
	 * all channels activities.
	 * 
	 * @param 	xmlstr	 		the String containing the XML description.
	 * @throws 	SAXException 	if there is an error during the parsing of the string.
	 * 							In that case, the thrown exception is wrapped inside
	 * 							the SAXException.
	 * @throws 	IOException 	if there is an error accessing the string.
	 */
	public static void loadAndRun(String xmlstr) throws SAXException, IOException {
		appiaxml.instanceLoad(xmlstr,null);
		Appia.run();
	}
	
	/**
	 * Returns a string representation of the current configuration.
	 * <p>
	 * <b>TODO method not implemented yet...</b>
	 * 
	 * @return 			String with the current configuration.
	 */
	public static String retrieve() {
		return null;
	}
	
	/**
	 * Creates a channel based on a chosen template with the given parameters.
	 * 
	 * @param	name 			the channel name.
	 * @param	templateName 	the template name.
	 * @param	label 			the String label associated with the channel.
	 * @param	params 			the parameters to be passed to their respective sessions.
	 * @param	initialized 	whether or not the channel is returned initialized.
	 * @param mm the memory manager of the channel
	 * @return					the Channel created.
	 * @throws	AppiaException	when Appia cannot create the channel.
	 * @see						ChannelProperties
	 */
	public static Channel createChannel(
			String name,
			String templateName,
			String label,
			ChannelProperties params,
			boolean initialized,
			MemoryManager mm,
            String messageFactory) throws AppiaException {
		return appiaxml.instanceCreateChannel(name,templateName,label,params,initialized,
                mm,messageFactory);
	}

	/**
	 * Creates a channel based on a chosen template with the given parameters.
	 * 
	 * @param	name 			the channel name.
	 * @param	templateName 	the template name.
	 * @param	label 			the String label associated with the channel.
	 * @param	params 			the parameters to be passed to their respective sessions.
	 * @param	initialized 	whether or not the channel is returned initialized.
	 * @return					the Channel created.
	 * @throws	AppiaException	when Appia cannot create the channel.
	 * @see						ChannelProperties
	 */
	public static Channel createChannel(
			String name,
			String templateName,
			String label,
			ChannelProperties params,
			boolean initialized,
            String messageFactory) throws AppiaException {
		return appiaxml.instanceCreateChannel(name,templateName,label,params,initialized,
                null,messageFactory);
	}

	/**
	 * Auxiliary method.
	 * <p>
	 * <b>INTERNAL USE ONLY!</b>
	 * 
	 * @param name
	 * @param templateName
	 * @param label
	 * @param params
	 * @param initialized
	 * @param mm the memory manager to use in the channel
	 * @return Channel
	 * @throws AppiaException
	 */
	public Channel instanceCreateChannel(String name,
			String templateName,
			String label,
			ChannelProperties params,
			boolean initialized,
			MemoryManager mm,
            String messageFactory) throws AppiaException {
		return config.createChannel(name,templateName,label,params,initialized,mm,false,
                messageFactory);
	}

    /**
     * Auxiliary method.
     * <p>
     * <b>INTERNAL USE ONLY!</b>
     * 
     * @param name
     * @param templateName
     * @param label
     * @param params
     * @param initialized
     * @param mm the memory manager to use in the channel
     * @param managed true if the channel is managed
     * @return Channel
     * @throws AppiaException
     */
    public Channel instanceCreateChannel(String name,
            String templateName,
            String label,
            ChannelProperties params,
            boolean initialized,
            MemoryManager mm,
            boolean managed,
            String messageFactory) throws AppiaException {
        return config.createChannel(name,templateName,label,params,initialized,mm,
                managed,messageFactory);
    }

	/**
	 * Auxiliary method.
	 * <p>
	 * <b>INTERNAL USE ONLY!</b>
	 * 
	 * @param name
	 * @param templateName
	 * @param label
	 * @param params
	 * @param initialized
	 * @return Channel
	 * @throws AppiaException
	 */
	public Channel instanceCreateChannel(String name,
			String templateName,
			String label,
			ChannelProperties params,
			boolean initialized,
            String messageFactory) throws AppiaException {
		return config.createChannel(name,templateName,label,params,initialized,null,false,messageFactory);
	}

    /**
     * Auxiliary method.
     * <p>
     * <b>INTERNAL USE ONLY!</b>
     * 
     * @param name
     * @param templateName
     * @param label
     * @param params
     * @param initialized
     * @return Channel
     * @throws AppiaException
     */
    public Channel instanceCreateChannel(String name,
            String templateName,
            String label,
            ChannelProperties params,
            boolean initialized,
            boolean managed,
            String messageFactory) throws AppiaException {
        return config.createChannel(name,templateName,label,params,initialized,null,
                managed,messageFactory);
    }

	/**
	 * Returns a chosen Channel based on its name.
	 * 
	 * @param 	name 	the channel's name.
	 * @return 			the requested Channel or null if it doesn't exist.
	 */
	public static Channel getChannel(String name) {
		return appiaxml.instanceGetChannel(name);
	}
	
	/**
	 * Returns a chosen Channel based on its name.
	 * <p>
	 * <b>INTERNAL USE ONLY!</b>
	 * 
	 * @param 	name 	the channel's name.
	 * @return 			the requested Channel or null if it doesn't exist.
	 */
	public Channel instanceGetChannel(String name) {
		return config.getChannel(name);
	}
	
	/**
	 * Returns the list of created channels..
	 * 
	 * @return 			array containing all the channels created.
	 */
	public static Channel[] getChannelList() {
		return appiaxml.instanceGetChannelList();
	}
	
	/**
	 * Returns the list of created channels..
	 * <p>
	 * <b>INTERNAL USE ONLY!</b>
	 * 
	 * @return 			array containing all the channels created.
	 */
	public Channel[] instanceGetChannelList() {
		return config.getChannelList();
	}
}
