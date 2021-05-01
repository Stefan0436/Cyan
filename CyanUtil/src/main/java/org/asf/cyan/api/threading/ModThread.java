package org.asf.cyan.api.threading;

/**
 * 
 * Mod Thread, task-based threads.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class ModThread {
	private Thread modThread;
	
	protected abstract void startThread();
}
