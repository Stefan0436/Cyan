package modkit.threading;

import org.asf.cyan.api.modloader.information.mods.IBaseMod;

/**
 * 
 * Mod Thread Memory -- Contains the thread call queue, name, and other info
 * needed to run the thread
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 * @since ModKit 1.2
 *
 */
public abstract class ThreadMemory {

	/**
	 * Retrieves the thread name
	 * 
	 * @return Thread name
	 */
	public abstract String getName();

	/**
	 * Re-creates the thread (only works if the thread has been saved to memory)
	 * 
	 * @return New ModThread instance
	 * @throws IllegalStateException If restarting is not possible
	 */
	public ModThread recreate() throws IllegalStateException {
		ModThread th = getManager().recreateThread(this);
		getManager().removeFromMemory(this);
		return th;
	}

	/**
	 * Retrieves this thread's manager
	 * 
	 * @return ThreadManager instances
	 */
	protected abstract ThreadManager getManager();

	/**
	 * Retrieves the thread status
	 * 
	 * @return Thread status
	 */
	public abstract ThreadStatus getStatus();

	/**
	 * Retrieves the thread call queue
	 * 
	 * @return ThreadQueue instance
	 */
	public abstract ThreadQueue getQueue();

	/**
	 * Retrieves the thread owner mod
	 * 
	 * @return IBaseMod instance
	 */
	public abstract IBaseMod getOwner();

}
