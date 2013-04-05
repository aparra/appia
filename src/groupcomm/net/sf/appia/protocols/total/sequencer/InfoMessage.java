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
 package net.sf.appia.protocols.total.sequencer;

import net.sf.appia.protocols.group.events.*;


/**
 *   Class that defines the information
 *   to be stored in the buffer.  
*/
public class InfoMessage {
    private int ordem;
    private int emissor;
    private int seqIndividual;
    private GroupSendableEvent evento;

    /**
     *   Basic constructor
     *   @param o total order of the message
     *   @param e sender of the message
     *   @param s individual sequence number
     *   @param ev the event to be stored
     */
    public InfoMessage(int o,int e,int s, GroupSendableEvent ev){
        ordem=o;
        emissor=e;
        seqIndividual=s;
        evento=ev;
    }

    /**
     *   Sets the total order of the message
     *   @param o total order of the message
     */
    public void setOrder(int o){
        ordem=o;
    }

    /**
     *    Sets the event of the message
     *    @param e the event to be stored
     */
    public void setEvent(GroupSendableEvent e){
        evento=e;
    }

    /**
     *   Verifies if the sender and sequence
     *   number of the event are equal
     *   @param e sender
     *   @param s individual sequence number   
     */
    public boolean isEqual(int e,int s){
        return (emissor==e && seqIndividual==s);
    }

    /**
     * Returns the order
     *    @return total order
     */
    public int getOrder(){
        return ordem;
    }

    /**
     * returns the event.
     *    @return event
     */
    public GroupSendableEvent getEvent(){
        return evento;
    }

    /**
     * Returns the individual sequence number.
     *    @return the individual sequence number
     */
    public int getNSeq(){
        return seqIndividual;
    }

    /**
     * Returns the sender of the message.
     *    @return the sender of the message
     */
    public int getSender(){
        return emissor;
    }

    /**
     *    Verifies if the message is ready 
     *     to be delivered to the layers above
     *    @return true if is ready and false otherwise
     */
    public boolean isValid(){
        return (ordem!=-1 && evento!=null);
    }
}
