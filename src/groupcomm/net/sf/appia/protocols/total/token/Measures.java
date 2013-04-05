/**
 * Appia: Group communication and protocol composition framework library
 * Copyright 2010 University of Lisbon / INESC-ID
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
 * @author Nuno Carvalho
 */

package net.sf.appia.protocols.total.token;

import java.util.Hashtable;
import java.util.Map;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;

import net.sf.appia.management.AppiaManagementException;

public class Measures {

    public static final String I_HAVE_TOKEN = "i_have_token";

    private Map<String,String>jmxFeaturesMap = new Hashtable<String,String>();

    private TotalTokenSession session;
    
    public Measures(TotalTokenSession s){
        session = s;
    }
    
    private Object getParameter(String parameter) throws AppiaManagementException {
        if(parameter.equals(I_HAVE_TOKEN))
            return session.iHaveToken();
        throw new AppiaManagementException("Parameter '"+parameter+"' not defined in session "+this.getClass().getName());
    }

    private void setParameter(String parameter, Object newValue) throws AppiaManagementException {
        throw new AppiaManagementException("Parameter '"+parameter+"' not defined in session "+this.getClass().getName());
    }

    public Object invoke(String action, MBeanOperationInfo info, Object[] params, String[] signature) 
    throws AppiaManagementException {
        throw new AppiaManagementException("The Session "+this.getClass().getName()+" does not accept any parameter to "+
        "set a new value. It is read only.");
    }

    public MBeanAttributeInfo[] getAttributes(String sid) {
        jmxFeaturesMap.put(sid+I_HAVE_TOKEN,I_HAVE_TOKEN);
        return new MBeanAttributeInfo[]{
                new MBeanAttributeInfo(sid+I_HAVE_TOKEN,
                        "boolean","returns true if the process has the token",
                        true,false,false),
        };
    }

    public Object attributeGetter(String attribute, MBeanAttributeInfo info) throws AppiaManagementException {
        return getParameter(jmxFeaturesMap.get(attribute));
    }
    
    public void attributeSetter(Attribute attribute, MBeanAttributeInfo info) throws AppiaManagementException {
        String att = jmxFeaturesMap.get(attribute.getName());
        System.out.println("call: "+attribute.getName()+" att "+att);
        setParameter(att, attribute.getValue());
    }

}
