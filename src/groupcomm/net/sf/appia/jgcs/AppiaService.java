/**
 * APPIA implementation of JGCS - Group Communication Service
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
 * Initial developer(s): Nuno Carvalho.
 * 
 *  * Contact
 * 	Address:
 * 		LASIGE, Departamento de Informatica, Bloco C6
 * 		Faculdade de Ciencias, Universidade de Lisboa
 * 		Campo Grande, 1749-016 Lisboa
 * 		Portugal
 * 	Email:
 * 		jgcs@lasige.di.fc.ul.pt
 */
 
package net.sf.appia.jgcs;

import org.apache.log4j.Logger;

import net.sf.jgcs.JGCSException;

import net.sf.jgcs.Service;
import net.sf.jgcs.UnsupportedServiceException;

/**
 * This class defines a AppiaService and implements the Service interface of jGCS.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public class AppiaService implements Service {
    
    private static Logger log = Logger.getLogger(AppiaService.class);
	
	private String service;
	private Integer value;
	
	public AppiaService(String serviceName){
		service = serviceName;
		try {
			value = AppiaServiceList.getValueForService(service);
		} catch (JGCSException e) {
            log.debug("Error getting the value for the service: "+service);
			e.printStackTrace();
		}
        if(log.isDebugEnabled())
            log.debug("SERVICE:: "+service+" :: "+value);
	}
	
	public String getService() {
		return service;
	}

	public int compare(Service serviceObject) throws UnsupportedServiceException {
		if(! (serviceObject instanceof AppiaService))
			throw new UnsupportedServiceException("Service not valid: "+service.getClass().getName());
		final AppiaService other = (AppiaService) serviceObject;
		return value.compareTo(other.value);
	}
	
	@Override
	public int hashCode(){
		return service.hashCode();
	}

	@Override
	public boolean equals(Object o){
		if (o instanceof AppiaService) {
			final AppiaService as = (AppiaService) o;
			return as.service.equals(this.service);
		}
		else
			return false;
	}
	
	@Override
	public String toString(){
		return "Appia service: "+service;
	}

}
