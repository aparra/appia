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
 * Copyright:    Copyright (c) Alexandre Pinto & Hugo Miranda & Luis Rodrigues<p>
 * Company:      F.C.U.L.<p>
 * @author Alexandre Pinto & Hugo Miranda & Luis Rodrigues
 * @version 1.0
 */
package net.sf.appia.core;

/**
 * 
 * This class defines a Session
 * 
 * @author <a href="mailto:apinto@di.fc.ul.pt">Alexandre Pinto</a>
 * @version 1.0
 */
public abstract class Session {

  protected Layer layer;
  private String id="";

  public Session(Layer layer) {
    this.layer=layer;
  }

  public void boundSessions(Channel channel) {}

  public void handle(Event event) {
    try {
      event.go();
    } catch (AppiaEventException e) {
      throw new AppiaError(e.getMessage());
    }
    
    
  }

  public Layer getLayer() {
      return layer;
  }

  public String getId() {
      return id;
  }

  public void setId(String id) {
      this.id = id;
  }
}
