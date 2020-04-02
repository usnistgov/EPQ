/**
 * gov.nist.nanoscalemetrology.JMONSELutils.CPUtime Created by: John Villarrubia
 * Date: Jul 22, 2013
 */
package gov.nist.nanoscalemetrology.JMONSELutils;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;

/**
 * <p>
 * A class for determining elapsed CPU time of the calling process.
 * </p>
 * <p>
 * This class accesses the CPU time through Java's management.ManagementFactory
 * class. This is a simple enough task that when needed I originally did it
 * directly within the Jython script, but Jython versions after 2.5.3 seem to
 * have lost access to java.lang.management for some reason, possibly a bug.
 * This CPUtime class was created to provide a workaround.
 * </p>
 * <p>
 * Copyright: Pursuant to title 17 Section 105 of the United States Code this
 * software is not subject to copyright protection and is in the public domain
 * </p>
 * <p>
 * Institution: National Institute of Standards and Technology
 * </p>
 * 
 * @author John Villarrubia
 * @version 1.0
 */
public class CPUtime {

	private final ThreadMXBean bean = ManagementFactory.getThreadMXBean();

	/**
	 * Constructs a CPUtime
	 */
	public CPUtime() {

	}

	/**
	 * Returns elapsed CPU time in seconds.
	 * 
	 * @return
	 */
	public double getCPUtime() {
		return bean.getCurrentThreadCpuTime() / 60.e9;
	}
}
