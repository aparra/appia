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
 package net.sf.appia.protocols.fifounreliable;

/**
 * Class that holds the sequence numbers
 *
 *  @author Sandra Teixeira 
 *
 */
public class PeerInfo{

    /*max sequence number*/
    private static final int MAXINT = 0xFFFFFFFF;
    /*masc to evaluate the most significant bit*/
    private static final int MASCBIT = 0x80000000;
    /*masc to evaluate the number without the most significant bit*/
    private static final int MASCNUM = 0x7FFFFFFF;

    private int receiveSeq;	
    private int control;

    /**Constructor
     *@param r received sequence number
     */
    public PeerInfo(int r, int c){
	receiveSeq=r;
	control=c;
    }
    
    /**
     *Returns the control value
     */
    public int getcontrol(){
	return control;
    }

    /**
     *Sets the control value
     *@param c the control value
     */
    public void setcontrol(int c){
	control=c;
    }

    /**
     * Returns true if r is higher than the last received sequence number
     * @param r received sequence number 
     */
    public boolean testRecSeq(int r){
	/*test signal*/
	if ((r & MASCBIT) == (receiveSeq & MASCBIT)){
	    if ((r & MASCNUM) > (receiveSeq & MASCNUM)){
		/*hold the new number*/
		receiveSeq=r;
		return true;
	    }
	    else/*keep the old number*/
		return false;
	}
	else{
	    if((r & MASCNUM) > (receiveSeq & MASCNUM))
		return false;
	    else{
		receiveSeq=r;
		return true;
	    }
	}
    }    
}
