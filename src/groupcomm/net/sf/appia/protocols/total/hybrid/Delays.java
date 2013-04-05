
/*
 *
 * APPIA: Protocol composition and execution framework
 * Copyright (C) 2005 Laboratorio de Sistemas Informaticos de Grande Escala (LASIGE)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
 * 
 * Contact
 * 	Address:
 * 		LASIGE, Departamento de Informatica, Bloco C6
 * 		Faculdade de Ciencias, Universidade de Lisboa
 * 		Campo Grande, 1749-016 Lisboa
 * 		Portugal
 * 	Email:
 * 		appia@di.fc.ul.pt
 * 	Web:
 * 		http://appia.di.fc.ul.pt
 * 
 */
 package net.sf.appia.protocols.total.hybrid;

/**
 * Class that stores information about the average delays between 
 * the various nodes of a group.
 */
public class Delays{

    private float average;
    private long times[];
    private int position;
    private int last,count;
    
    private boolean firstTime;

    ///////////////////////////////
    private final static int SIZE=7;
    //////////////////////////////
    private final static int DOWN=1;
    private final static int UP=2;
    
    /**
     * Constructor. Initializes the class.
     */
    public Delays(){
	average=Float.MAX_VALUE;
	times=new long[SIZE];
	position=0;
	last=-1;
	count=0;
	firstTime=true;
    }

    private void calculateAverage(){
	float total=0;

	for(int i=0;i!=times.length;i++)
	    total+=times[i];

	average=total/times.length;

	count=0;	
    }
    
    /**
     * Insert a new time. If needed this method updates
     * the average.
     * @param time The time we want to store. Normally the actual time.
     */
    public void newTime(long time){


	//calculates the first average
	if(firstTime && position==0){
	    average= time;
	}

	//verifies if it filled the array the firts time
	if(firstTime && position==times.length-1)
	    firstTime=false;
	
	
	//insert the time in the array
	times[position]=time;
	
	if(times[position] > average){
	   if(last==UP)
	       count++;
	   else{
	       last=UP;
	       count=0;
	   }
	}
	if(times[position]< average){
	    if(last==DOWN)
		count++;
	    else{
		last=DOWN;
		count=0;
	    }
	}

	//upadtes the next position.
	position= ((position+1) % times.length);

	//if there are 7 times below or above average it updates the average.
	if(count == times.length)
	    calculateAverage();
	
	
    }
    /**
     * Returns the average.
     * @return Average.
     */
    public float getAverage(){
	return average;
    }

}
