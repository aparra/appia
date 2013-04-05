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
 /**
 * Title:        Appia<p>
 * Description:  Protocol development and composition framework<p>
 * Copyright:    Copyright (c) Nuno Carvalho and Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Nuno Carvalho and Luis Rodrigues
 * @version 1.0
 */

package net.sf.appia.management;

import javax.management.Attribute;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanOperationInfo;


/**
 * This class defines a ManagedSession. A managed session is a session that can receive new
 * property values from the respective managed channel.
 * 
 * @author <a href="mailto:nunomrc@di.fc.ul.pt">Nuno Carvalho</a>
 * @version 1.0
 */
public interface ManagedSession{

    public Object invoke(String action, MBeanOperationInfo info, Object[] params, String[] signature) 
        throws AppiaManagementException;
    public Object attributeGetter(String attribute, MBeanAttributeInfo info)
        throws AppiaManagementException;
    public void attributeSetter(Attribute attribute, MBeanAttributeInfo info)
    throws AppiaManagementException;
    
    public MBeanOperationInfo[] getOperations(String sessionID);
    public MBeanAttributeInfo[] getAttributes(String sessionID);

}
