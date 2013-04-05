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

import java.util.*;

import net.sf.appia.protocols.group.events.*;


/**
 * Class that implements a buffer to store 
 * events while waiting for the total order.
 */
public class Buffer{
    Vector lista;

    /**
     *   Constructs a new Buffer object.
     */
    public Buffer(){
        lista=new Vector(5,3);
    }


    /**
     *   Insert a new InfoMessage
     *   @param o total order of the message
     *   @param e sender of the message
     *   @param seq individual sequence number
     *   @param ev the event to be stored
     */
    public void insert(int o, int e, int seq, GroupSendableEvent ev){
        lista.addElement(new InfoMessage(o,e,seq,ev));
    }


    /**
     *     Finds and returns if available
     *     a InfoMessage object.
     *  @param emissor sender of the message that wanted
     *  @param seq sequence number of the message wanted
     */
    public InfoMessage find(int emissor, int seq){
        for(int i=0;i!=lista.size();i++)
            if(((InfoMessage)lista.elementAt(i)).isEqual(emissor,seq))
                return (InfoMessage)lista.elementAt(i);

        return null;
    }

    /**
     * Insert a new order
     * @param o the order to be given to the message
     * @param emissor the sender of the message to be ordered
     * @param seq the individual sequence number of the message to be ordered
     */
    public void insertOrder(int o, int emissor, int seq){
        InfoMessage info=find(emissor,seq);

        if( info!=null)
            info.setOrder(o);
        else
            insert(o,emissor,seq,null);
    }

    /**
     * Insert a new event
     * @param emissor the sender of the message to be ordered
     * @param seq the individual sequence number of the message to be ordered
     * @param ev the event to be ordered
     */
    public void insertEvent(int emissor, int seq, GroupSendableEvent ev){
        InfoMessage info=find(emissor,seq);

        if( info!=null){
            info.setEvent(ev);
        }
        else{
            insert(-1,emissor,seq,ev);
        }
    }

    /**
     * Gives an event ready to be delivered
     * @return The event to be delivered or null if none is available
     */
    public GroupSendableEvent getReadyEvent(int ordem){
        GroupSendableEvent evento;

        int tam=lista.size();
        
        for(int i=0; i!=tam; i++){
        	// if it finds an event with the correct order and ready to send it up, returns the event
          // System.out.println("evento order -> "+ ((InfoMessage)lista.elementAt(i)).getOrder() + "isValid ->"+((InfoMessage)lista.elementAt(i)).isValid() );
            if(((InfoMessage)lista.elementAt(i)).getOrder()==ordem && ((InfoMessage)lista.elementAt(i)).isValid()){
                evento=((InfoMessage)lista.elementAt(i)).getEvent();
                lista.removeElementAt(i);
                return evento;
            }
            // if there is only the order, the protocol cannot send it up yet, so it returns null
            if(((InfoMessage)lista.elementAt(i)).getOrder()==ordem){
                return null;
            }
        }
        return null;
    }


    /**
     *    @return true if the buffer is empty and false otherwise
     */
    public boolean isEmpty(){
        return lista.isEmpty();
    }

    public int size(){
    	return lista.size();
    }


    /**
     * Returns an event from the buffer in a deterministic way.
     * Used when there are view changes in the group
     * @return The event
     */
    public GroupSendableEvent getMinimum(){
        int nSeqMin=((InfoMessage)lista.elementAt(0)).getNSeq();
        int emissor=((InfoMessage)lista.elementAt(0)).getSender();
        int pos=0;
        GroupSendableEvent e;

        for(int i=1; i<lista.size(); i++){
            if(nSeqMin > ((InfoMessage)lista.elementAt(i)).getNSeq()){
                pos=i;
                nSeqMin=((InfoMessage)lista.elementAt(i)).getNSeq();
                emissor=((InfoMessage)lista.elementAt(i)).getSender();
            }
            //in the case that is equal, chooses the one with the greater sender.
            else if(nSeqMin == ((InfoMessage)lista.elementAt(i)).getNSeq()){
                if(emissor > ((InfoMessage)lista.elementAt(i)).getSender()){
                    pos=i;
                    emissor=((InfoMessage)lista.elementAt(i)).getSender();
                }
            }
        }/*end of for*/
        
        e=((InfoMessage)lista.elementAt(pos)).getEvent();
        lista.removeElementAt(pos);
        return e;
    }
    
}
