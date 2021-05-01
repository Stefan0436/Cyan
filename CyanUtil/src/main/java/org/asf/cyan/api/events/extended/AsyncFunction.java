package org.asf.cyan.api.events.extended;

/**
 * 
 * Async functions -- runs code asynchronously
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class AsyncFunction<T> {
	private boolean started = false;
	private boolean done = false;
	private T result = null;

	/**
	 * True to run in own thread, false to implement async running manually
	 */
	protected boolean async() {
		return true;
	}

	/**
	 * Sets the result and unlocks if awaiting
	 * 
	 * @param result Result object
	 */
	protected void setResult(T result) {
		this.result = result;
		done = true;
	}

	/**
	 * Checks if the asynchronous function completed
	 * 
	 * @return True if completed, false otherwise.
	 */
	public boolean hasResult() {
		return done;
	}

	/**
	 * Retrieves the result (waits for result if not present)
	 * 
	 * @return Function result
	 */
	public T getResult() {
		if (!started)
			throw new IllegalStateException("Function not running.");

		while (!hasResult()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				break;
			}
		}
		return result;
	}

	/**
	 * Called to start the function
	 */
	protected abstract void run();

	/**
	 * Starts the async function, can only be called once.
	 */
	public void start() {
		if (started)
			throw new IllegalStateException("Function already started.");
		started = true;
		if (async())
			new Thread(() -> run(), "Async Function " + getClass().getTypeName()).start();
		else
			run();
	}
}
