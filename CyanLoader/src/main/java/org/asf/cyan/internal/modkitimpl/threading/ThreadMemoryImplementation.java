package org.asf.cyan.internal.modkitimpl.threading;

import org.asf.cyan.api.modloader.information.mods.IBaseMod;

import modkit.threading.ThreadManager;
import modkit.threading.ThreadMemory;
import modkit.threading.ThreadQueue;
import modkit.threading.ThreadStatus;

public class ThreadMemoryImplementation extends ThreadMemory {

	private String name;
	private ThreadManager manager;
	private ThreadStatus status;
	public ThreadQueueImplementation queue;

	public ThreadMemoryImplementation(ThreadManager manager, ThreadStatus status, ThreadQueueImplementation queue) {
		this.status = status;
		this.queue = queue;
		this.manager = manager;
	}

	public void setStatus(ThreadStatus status) {
		this.status = status;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	protected ThreadManager getManager() {
		return manager;
	}

	@Override
	public ThreadStatus getStatus() {
		return status;
	}

	@Override
	public ThreadQueue getQueue() {
		return queue;
	}

	@Override
	public IBaseMod getOwner() {
		return manager.getOwner();
	}

	public void setName(String modName) {
		name = modName;
	}

}
