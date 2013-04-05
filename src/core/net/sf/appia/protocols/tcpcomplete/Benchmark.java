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
 package net.sf.appia.protocols.tcpcomplete;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ThreadFactory;

import net.sf.appia.core.TimeProvider;


/**
 * @author jmartins
 *
 * Benchmarking class.
 * 
 * All of the methods accumulate successive benchmarks, and the total running 
 * time, average running time and number of times executed for each benchmark
 * is printed on standard output when the JVM shuts down.
 * @deprecated
 */
public class Benchmark {
  public static final boolean ON=false;
  
	private static Benchmark inst;

	private Map hash = Collections.synchronizedMap(new HashMap());
	private Runnable shHook;
	
	private TimeProvider timeProvider = null;

	/**
	 * Obtains the singleton instance of the benchmarking class.
	 * @return the benchmark class.
	 */
	public static Benchmark getInstance(TimeProvider tp, ThreadFactory thf) {
          if (!ON)
            return null;
          
		if (inst == null) {
			inst = new Benchmark(tp,thf);
		}
		
		return inst;
	}

	private Benchmark(TimeProvider tp, ThreadFactory threadFactory) {
		shHook = new SHook(hash);
		timeProvider = tp;
		final Thread hook = threadFactory.newThread(shHook);
        hook.setName("Benchmark Shutdown Hook");
		Runtime.getRuntime().addShutdownHook(hook);
	}		

	public void startTagged(String s, String tag) {
		//System.out.println("start tagged st " + s);
		startBench(s+tag);
		//System.out.println("start tagged end " + s);
	}
	
	public void stopTagged(String s, String tag) {
		//System.out.println("stop tagged start  " + s);
		stopBench(s+tag);
		final Measure m = (Measure) hash.get(s+tag);
		indepBench(s, m.totalTime);
		hash.remove(s+tag);
		//System.out.println("stop tagged end  " + s);
	}

	/**
	 * Starts a measure.
	 * @param s measure to start.
	 */
	public void startBench(String s) {
		//System.out.println("start bench start " + s);
		Measure m = (Measure) hash.get(s);
		if (m == null) {
			m = new Measure();
			m.numRuns = 0;
			m.totalTime = 0;
			m.name = s;
			m.sTime = timeProvider.currentTimeMillis();
			hash.put(s, m);
		} else {
			m.sTime = timeProvider.currentTimeMillis();
		}
		//System.out.println("start bench end " + s);
	}

	/**
	 * Stops a measure.
	 * This method assumes the measure was started !! If it was not a 
	 * NullPointerException might occur.
	 * @param s measure to stop.
	 */
	public void stopBench(String s) {
		//System.out.println("stop benh start " +s );
		Measure m = (Measure) hash.get(s);
		long end = timeProvider.currentTimeMillis() - m.sTime;
		m.totalTime += end;
		m.numRuns++;
		//System.out.println("stop bench end " + s+" time:: "+end);
	}
	
	/**
	 * Inserts an independent benchmark time. Useful for accumulating
	 * benchmarks measured outside this class, like message timestamps.
	 * @param s the name of the benchmark.
	 * @param time the time measured for this benchmark.
	 */
	public void indepBench(String s, long time) {
		Measure m = (Measure) hash.get(s);
		if (m == null) {
			m = new Measure();
			m.numRuns = 1;
			m.totalTime = time;
			m.name = s;
			hash.put(s, m);
		} else {
			m.totalTime += time;
			m.numRuns++;
		}
	}
}

/**
 * Class that represents a measure.
 */
class Measure {
	String name;
	long totalTime;
	long numRuns;
	long sTime;
	String tag;
} 


/**
 * This shutdown hook will print the stored measures on node shutdown.
 */
class SHook implements Runnable {
	Map hm;
	
	public SHook(Map hm) {
		this.hm = hm;
	}
	
	public void run() {
		System.out.println("Benchmark measures:");
		for (Iterator i = hm.values().iterator(); i.hasNext(); ) {
			Measure m = (Measure) i.next();
			if (m.numRuns != 0) {
				System.out.println("\t" + m.name + " -- tot: " + m.totalTime 
											+ " runs: " + m.numRuns + " avg: " 
											+ m.totalTime / m.numRuns);
			}
		}
		System.out.println("End of measures");
	}
}
