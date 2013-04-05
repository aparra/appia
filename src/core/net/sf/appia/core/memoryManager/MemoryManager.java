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
package net.sf.appia.core.memoryManager;

import java.io.*;
import java.security.InvalidParameterException;

import net.sf.appia.core.*;

/**
 * This class is used by the events and a {@link net.sf.appia.core.Channel} to stabilish a
 * maximum number of bytes used by {@link net.sf.appia.core.message.Message messages} that a channel can hold.
 *
 * @see Channel
 * @see net.sf.appia.core.message.Message
 * @author Nuno Carvalho
 */
public class MemoryManager {
	
    private static final float NOTIFY_THRESHOLD = (float) 0.9;
    
	private PrintStream debugOutput = System.out;
	private String mmID;
	/*
	 * contains the maximum size in bytes that this memory manager can hold
	 */
	private int maxSize;
	/*
	 * contains the size of allocated memory
	 */
	private int currentSize;
	/*
	 * contains a tolerance margin
	 */
	private int upthreshold;
	private int downthreshold;
	
	private Object downMutex = new Object(), upMutex = new Object();
	
	/**
	 * Constructor of the class.
	 *
	 * @param id Identifier of the memory manager.
	 * @param size Max number of bytes. It must be a positive integer.
	 * @param upth Tolerance margin of the Memory Manager (in bytes) for events going UP.
	 * @param downth Tolerance margin of the Memory Manager (in bytes) for events going DOWN.
	 */
	public MemoryManager(String id, int size, int upth, int downth) {
		mmID = id;
		if (size <= 0) 
			maxSize = 0;
		else
			maxSize = size;
		currentSize = 0;
		setThreshold(upth, Direction.UP);
		setThreshold(downth,Direction.DOWN);
		if (AppiaConfig.MM_DEBUG_ON && debugOutput!=null)
			debugOutput.println("MemoryManager: new memory manager created! (size = "+maxSize+") "+
					"ID = "+mmID);
	} // end of constructor
	
	/**
	 * Verifies if the channel is above the water mark specified 
	 * by the user in the constructor.
	 * @return true if the amount of memory reached the threshold.
	 */
	public boolean aboveThreshold(int direction) throws InvalidParameterException {
		if(direction == Direction.UP){
			return currentSize >= upthreshold;
		}
		else if(direction == Direction.DOWN){
			return currentSize >= downthreshold;
		}
		else
			throw new InvalidParameterException("Direction must be UP or DOWN in aboveThreshold");
	}
	
	/**
	 * Block while the used bytes in this memory manager is above the threshold.
	 * @return true if it is above the threshold, false otherwise. 
	 * This should return always true, but is an open window for future modifications.
	 * @throws InterruptedException
	 * @see Object#wait()
	 */
	public boolean synchronizedAboveThreshold(int direction) 
	throws InterruptedException, InvalidParameterException{
		// the aboveThreshold() already verifies is the direction parameter is valid.
		boolean above = aboveThreshold(direction);
		if(above){
			Object sync = null;
			if(direction == Direction.UP)
				sync = upMutex;
			else
				sync = downMutex;
			synchronized (sync) {
				while(above = aboveThreshold(direction)){
					sync.wait();
				}
			}
		}
		return above;
	}
	
	/**
	 * Gets the value of the specified threshold.
	 * @return the value of the specified threshold.
	 */
	public int getThreshold(int direction) throws InvalidParameterException {
		if(direction == Direction.UP){
			return upthreshold;
		}
		else if(direction == Direction.DOWN){
			return downthreshold;
		}
		else
			throw new InvalidParameterException("Direction must be UP or DOWN in getThreshold.");
	}
	
	/**
	 * Sets the threshold for this memory manager.
	 *
	 * newThreshold must be between 0 and getMaxSize()
	 * @param newThreshold the new threshold.
	 */
	public void setThreshold(int newThreshold, int direction) throws InvalidParameterException {
		if (newThreshold <= 0 || newThreshold > maxSize)
			throw new InvalidParameterException("Invalid threshold on setThreshold.");
		
		else{
			if(direction == Direction.UP){
				upthreshold = newThreshold;
			}
			else if(direction == Direction.DOWN){
				downthreshold = newThreshold;
			}
			else
				throw new InvalidParameterException("Direction must be UP or DOWN in setThreshold.");
			
		}
	}
	
	/**
	 * Gets Memory Manager ID.
	 * @return the Memory Manager ID.
	 */
	public String getMemoryManagerID() {
		return mmID;
	}
	
	/**
	 * Sets the Maximum size of memory that the channel of this memory manager can hold.
	 *
	 * If the new specified value is lower than 0 or lower than the current used size, throws
	 * a {@link AppiaWrongSizeException}.
	 * @param newSize the new size of the memory (in bytes).
	 */
	public void setMaxSize(int newSize) throws AppiaWrongSizeException {
		if ((newSize <= 0) || (newSize < currentSize)) 
			throw new AppiaWrongSizeException("Could not set size of "+mmID+" to "+newSize);
		maxSize = newSize;
	} // end of method setMaxSize
	
	/**
	 * Gets the actual size of the memory (in bytes)
	 */
	public int getMaxSize() {
		return maxSize;
	}
	
	/**
	 * Gets the amount of memory (in bytes) occupied in the memory manager.
	 */
	public int used() {
		return currentSize;
	}
	
	/**
	 * Reserve an amount of memory.
	 * @param nBytes number of bytes to reserve.
	 * @return True on sucess, False if the memory manager do not have this amount of memory.
	 */
	public boolean malloc(int nBytes) {
		if (nBytes <= 0)
			return true;
		if ((currentSize + nBytes) > maxSize) {
			if (AppiaConfig.MM_DEBUG_ON && debugOutput!=null)
				debugOutput.println("MemoryManager: "+mmID+": malloc of "+nBytes+
						" bytes FAILED! current size = "+currentSize);
			return false;
		}
		currentSize += nBytes;
		
		if (AppiaConfig.MM_DEBUG_ON && debugOutput!=null)
			debugOutput.println("MemoryManager: "+mmID+": malloc of "+nBytes+
					" bytes done! current size = "+currentSize);
		return true;
	} // end of method malloc
	
	/**
	 * Free an amount of memory in the memory manager.
	 * @param nBytes the amount of memory to free.
	 */
	public void free(int nBytes) {
		if (nBytes == 0)
			return;
		
		if (nBytes < 0 || currentSize == 0) { 
			if(AppiaConfig.MM_DEBUG_ON && debugOutput!=null)
				debugOutput.println("MemoryManager: "+mmID+": free error: state is nBytes="+nBytes+" and "+
						"currentSize="+currentSize);
			return;
		}
		
		currentSize -= Math.min(nBytes,currentSize);
		
		if(currentSize <= (downthreshold*NOTIFY_THRESHOLD))
			synchronized (downMutex) { downMutex.notifyAll(); }
		if(currentSize <= (upthreshold*NOTIFY_THRESHOLD))
			synchronized (upMutex) { upMutex.notifyAll(); }
		
		if(AppiaConfig.MM_DEBUG_ON && debugOutput!=null)
			debugOutput.println("MemoryManager: "+mmID+
					": free done (nBytes = "+nBytes+"). current size = "+currentSize);
	} // end of method free
	
} // end of class
