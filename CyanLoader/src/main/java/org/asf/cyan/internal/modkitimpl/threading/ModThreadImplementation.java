package org.asf.cyan.internal.modkitimpl.threading;

import java.util.stream.Stream;

import org.asf.cyan.api.modloader.information.mods.IBaseMod;

import modkit.threading.ModThread;
import modkit.threading.ThreadManager;
import modkit.threading.ThreadMemory;
import modkit.threading.ThreadQueue;
import modkit.threading.ThreadQueue.ThreadCall;
import modkit.threading.ThreadStatus;

public class ModThreadImplementation extends ModThread {

	private ThreadManagerImplementation manager;
	private ThreadMemoryImplementation memory;

	public ModThreadImplementation(ThreadManagerImplementation manager, ThreadMemoryImplementation memory) {
		this.manager = manager;
		this.memory = memory;
	}

	@Override
	protected IBaseMod getOwner() {
		return getManager().getOwner();
	}

	@Override
	public String getName() {
		return getMemory().getName();
	}

	@Override
	public ThreadStatus getStatus() {
		return getMemory().getStatus();
	}

	@Override
	public ThreadMemory getMemory() {
		return memory;
	}

	@Override
	protected ThreadManager getManager() {
		return manager;
	}

	private Thread th;

	private boolean suspend = false;
	private boolean stop = false;
	private boolean stopped = false;

	@Override
	protected void startThread() {
		stop = false;
		stopped = true;

		th = new Thread(() -> {
			stopped = false;
			memory.setStatus(ThreadStatus.RUNNING);
			while (!stop) {
				while (suspend) {
					stopped = true;
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						break;
					}
				}
				stopped = false;

				runQueuedCall();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					break;
				}
			}
			stopped = true;
		}, getName());
		th.setDaemon(false);
		th.start();

		while (getStatus() != ThreadStatus.RUNNING) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				break;
			}
		}
	}

	@Override
	public void suspend() throws IllegalStateException {
		if (getStatus() != ThreadStatus.RUNNING)
			throw new IllegalStateException("Thread not running!");

		suspend = true;
		while (!stopped) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				break;
			}
		}
		memory.setStatus(ThreadStatus.SUSPENDED);
	}

	@Override
	public void resume() throws IllegalStateException {
		if (getStatus() != ThreadStatus.SUSPENDED)
			throw new IllegalStateException("Thread not suspended!");
		suspend = false;
		while (stopped) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				break;
			}
		}
		memory.setStatus(ThreadStatus.RUNNING);
	}

	@Override
	public void kill() throws IllegalStateException {
		if (getStatus() != ThreadStatus.RUNNING)
			throw new IllegalStateException("Thread not running!");
		stop = true;
		while (!stopped) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				break;
			}
		}
		memory.setStatus(ThreadStatus.KILLED);
		manager.removeThread(this);
	}

	@Override
	public void sleep(int timeout, boolean block) throws InterruptedException {
		if (getStatus() != ThreadStatus.RUNNING)
			throw new IllegalStateException("Thread not running!");

		ThreadSleepCall c = new ThreadSleepCall(timeout);
		this.queueCall(c);
		if (block) {
			while (!c.isDone() && getStatus() == ThreadStatus.RUNNING) {
				Thread.sleep(10);
			}
			if (getStatus() != ThreadStatus.RUNNING)
				throw new InterruptedException("Thread stopped executing its queue!");
		}
	}

	@Override
	public ThreadQueue getQueue() {
		return getMemory().getQueue();
	}

	@Override
	public ThreadQueue queueCall(ThreadCall call) {
		getQueue().addCall(call);
		return getQueue();
	}

	@Override
	protected void setStored() {
		memory.setStatus(ThreadStatus.SAVED);
	}

	@Override
	public boolean isRepeating() {
		return Stream.of(getQueue().getAllCalls()).anyMatch(t -> t instanceof LoopCall);
	}

	@Override
	public void setRepeating(boolean repeat) {
		if (repeat && !isRepeating()) {
			getQueue().addCall(new LoopCall());
		} else if (!repeat && isRepeating()) {
			memory.queue.removeLoop();
		}
	}

}
