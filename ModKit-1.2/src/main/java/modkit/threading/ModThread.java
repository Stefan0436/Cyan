package modkit.threading;

import java.util.ArrayList;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;

import modkit.threading.ThreadQueue.ThreadCall;

/**
 * 
 * Mod Thread, call-based threads
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 * @since ModKit 1.2
 *
 */
public abstract class ModThread extends CyanComponent {

	/**
	 * Checks if the thread is repeating tasks
	 * 
	 * @return True if tasks are being repeated, false otherwise
	 */
	public abstract boolean isRepeating();

	/**
	 * Starts the thread
	 */
	protected abstract void startThread();

	/**
	 * Retrieves the thread owner mod
	 * 
	 * @return IBaseMod instance
	 */
	protected abstract IBaseMod getOwner();

	/**
	 * Retrieves the thread name
	 * 
	 * @return Thread name
	 */
	public abstract String getName();

	/**
	 * Retrieves the thread status
	 * 
	 * @return Thread status
	 */
	public abstract ThreadStatus getStatus();

	/**
	 * Suspends the thread
	 * 
	 * @throws IllegalStateException if the thread cannot be suspended
	 */
	public abstract void suspend() throws IllegalStateException;

	/**
	 * Resumes the thread
	 * 
	 * @throws IllegalStateException if the thread cannot be resumed
	 */
	public abstract void resume() throws IllegalStateException;

	/**
	 * Destroys the thread
	 * 
	 * @throws IllegalStateException if the thread cannot be destroyed
	 */
	public abstract void kill() throws IllegalStateException;

	/**
	 * Destroys the thread and saves it to memory so it can be restarted by name
	 * 
	 * @throws IllegalStateException if the thread cannot be destroyed
	 */
	public void store() throws IllegalStateException {
		kill();
		getManager().saveThreadToMemory(getMemory());
		setStored();
	}

	/**
	 * Sets the thread's status to 'SAVED'
	 */
	protected abstract void setStored();

	/**
	 * Retrieves this thread's manager
	 * 
	 * @return ThreadManager instances
	 */
	protected abstract ThreadManager getManager();

	/**
	 * Adds a sleep instruction to the thread call queue, blocks execution until
	 * complete.
	 * 
	 * @param timeout Milliseconds to pause the thread
	 * @throws InterruptedException If the timeout is interrupted (when the thread
	 *                              dies or is suspended)
	 */
	public void sleep(int timeout) throws InterruptedException {
		sleep(timeout, true);
	}

	/**
	 * Adds a sleep instruction to the thread call queue.
	 * 
	 * @param timeout Milliseconds to pause the thread
	 * @param block   True to block code execution, false to add to the queue
	 *                without waiting
	 * @throws InterruptedException If the timeout is interrupted (when the thread
	 *                              dies or is suspended)
	 */
	public abstract void sleep(int timeout, boolean block) throws InterruptedException;

	/**
	 * Retrieves the thread memory
	 * 
	 * @return ThreadMemory instance
	 */
	public abstract ThreadMemory getMemory();

	/**
	 * Retrieves the thread call queue
	 * 
	 * @return ThreadQueue instance
	 */
	public abstract ThreadQueue getQueue();

	/**
	 * Adds a thread call function to the queue (short functions only, after 500
	 * milliseconds, a thread triggers a warning)
	 * 
	 * @param call Thread call to add to the queue
	 * @return ThreadQueue instance
	 */
	public abstract ThreadQueue queueCall(ThreadCall call);

	protected static class ThreadSleepCall implements ThreadCall {

		private int delay;
		private boolean done = false;

		public ThreadSleepCall(int delay) {
			this.delay = delay;
		}

		public boolean isDone() {
			return done;
		}

		@Override
		public void call(ModThread thread) {
			try {
				Thread.sleep(delay);
			} catch (InterruptedException e) {
			}
			done = true;
		}

	}

	public static class LoopCall implements ThreadCall {

		public ArrayList<ThreadCall> calls = new ArrayList<ThreadCall>();

		public LoopCall() {
		}

		@Override
		public void call(ModThread thread) {
			for (ThreadCall call : calls)
				thread.getQueue().addCall(call);
			thread.getQueue().addCall(this);
		}

	}

	/**
	 * Sets the 'repeat' mode, tasks added after turning this on will be looped.
	 */
	public abstract void setRepeating(boolean repeat);

	/**
	 * Executes the first queued thread instruction
	 */
	protected void runQueuedCall() {
		long miliseconds = System.currentTimeMillis();
		ThreadCall call = getQueue().runFirst(this);
		if (call == null)
			return;
		long elapsed = System.currentTimeMillis() - miliseconds;
		if (elapsed >= 500 && !(call instanceof ThreadSleepCall) && !(call instanceof LoopCall)) {
			warn("Mod thread " + getName() + " blocked for " + elapsed + " miliseconds, this is a violation!");
		}
	}

}
