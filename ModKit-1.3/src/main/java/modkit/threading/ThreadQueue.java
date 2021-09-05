package modkit.threading;

/**
 * 
 * Mod thread call queue
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class ThreadQueue {

	/**
	 * 
	 * Mod thread call interface
	 * 
	 * @author Sky Swimmer - AerialWorks Software Foundation
	 *
	 */
	public static interface ThreadCall {
		/**
		 * Called to run the thread function (short code only)
		 * 
		 * @param thread Mod thread calling this
		 */
		public void call(ModThread thread);
	}

	/**
	 * Retrieves the top-most thread call
	 * 
	 * @return TreadCall instance or null if none is present
	 */
	public abstract ThreadCall getFirstCall();

	/**
	 * Retrieves an array of all thread calls
	 * 
	 * @return Array of ThreadCall instances
	 */
	public abstract ThreadCall[] getAllCalls();

	/**
	 * Adds a thread call function to the queue
	 * 
	 * @param call Call function to queue
	 */
	public abstract void addCall(ThreadCall call);

	/**
	 * Runs and removes the first upcoming call
	 * 
	 * @param thread Owing thread
	 * @return 
	 */
	protected abstract ThreadCall runFirst(ModThread thread);

}
