package org.asf.cyan.api.threading;

/**
 * 
 * Mod Thread Manager -- 
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class ThreadManager {
	protected static ThreadManager implementation;

	/**
	 * Instantiates a new Thread Manager
	 */
	public static ThreadManager createManager() {
		return implementation.instantiate();
	}

	/**
	 * Creates a new mod thread
	 */
	public ModThread createThread() {
		ModThread th = newThread();
		return th;
	}

	/**
	 * Creates a new mod thread
	 * 
	 * @param name Thread name
	 */
	public ModThread createThread(String name) {
		ModThread th = createThread();
		setThreadName(name, th);
		return th;
	}
	
	public abstract ModThread[] getThreads();
	public abstract ModThread[] suspendedThreads();
	public abstract ThreadMemory[] inMemoryThreads();

	/**
	 * Instantiates a new thread
	 */
	protected abstract ModThread newThread();

	/**
	 * Sets the name of a thread
	 * 
	 * @param name   Thread name
	 * @param thread Thread instance
	 */
	protected abstract void setThreadName(String name, ModThread thread);

	/**
	 * Creates a new thread manager
	 */
	protected abstract ThreadManager instantiate();
}
