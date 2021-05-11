package org.asf.cyan.internal.modkitimpl.threading;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.threading.ModThread;
import org.asf.cyan.api.threading.ThreadManager;
import org.asf.cyan.api.threading.ThreadMemory;

@CYAN_COMPONENT
public class ThreadManagerImplementation extends ThreadManager {

	protected static void initComponent() {
		implementation = new ThreadManagerImplementation();
	}

	@Override
	public ModThread[] getThreads() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ModThread[] suspendedThreads() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ThreadMemory[] inMemoryThreads() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected ModThread newThread() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void setThreadName(String name, ModThread thread) {
		// TODO Auto-generated method stub

	}

	@Override
	protected ThreadManager instantiate() {
		// TODO Auto-generated method stub
		return null;
	}

}
