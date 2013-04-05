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

package net.sf.appia.core;

import java.io.File;
import java.io.IOException;

import net.sf.appia.xml.AppiaXML;
import net.sf.appia.xml.AppiaXMLException;

import org.xml.sax.SAXException;

/**
 * 
 * This class defines a AbstractAppiaRunnable
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public abstract class AbstractAppiaRunnable implements Runnable {

    private Appia appia;
    private AppiaXML appiaXML;
    
    /**
     * Creates a new AbstractAppiaRunnable.
     * @throws AppiaException 
     */
    public AbstractAppiaRunnable() throws AppiaException {
        super();
        appia = new Appia();
        appiaXML = AppiaXML.getInstance(appia);
    }

    public AbstractAppiaRunnable(Appia appia) throws AppiaException {
        super();
        this.appia = appia;
        appiaXML = AppiaXML.getInstance(appia);
    }

    public AbstractAppiaRunnable(File xmlConfig, String managementID) throws AppiaXMLException {
        super();
        appia = new Appia();
        appia.setManagementMBeanID(managementID);
        appiaXML = AppiaXML.getInstance(appia);
        try {
            appiaXML.instanceLoad(xmlConfig,this.appia);
        } catch (SAXException e) {
            throw new AppiaXMLException("Error loading configuration",e);
        } catch (IOException e) {
            throw new AppiaXMLException("Error loading configuration",e);
        }
    }
    
    public AbstractAppiaRunnable(Appia appia, File xmlConfig) throws AppiaXMLException {
        super();
        this.appia = appia;
        appiaXML = AppiaXML.getInstance(appia);
        try {
            appiaXML.instanceLoad(xmlConfig,this.appia);
        } catch (SAXException e) {
            throw new AppiaXMLException("Error loading configuration",e);
        } catch (IOException e) {
            throw new AppiaXMLException("Error loading configuration",e);
        }
    }

    /**
     * Used to setup and make run time initializations before starting the Appia thread.
     * The programmer must call this method before starting the Thread that will run this Runnable class.
     * @throws AppiaException
     * @see java.lang.Thread
     */
    public abstract void setup() throws AppiaException;
    
    /**
     * Runs the Appia thread.
     * @see java.lang.Runnable#run()
     * @see net.sf.appia.core.Appia#instanceRun()
     */
    public void run() {
        this.appia.instanceRun();
    }

    /**
     * Gets the Appia instance.
     * @return the Appia instance.
     */
    public Appia getAppia() {
        return appia;
    }

    /**
     * Gets the Appia XML instance.
     * @return the Appia XML instance.
     */
    public AppiaXML getAppiaXML() {
        return appiaXML;
    }

}
