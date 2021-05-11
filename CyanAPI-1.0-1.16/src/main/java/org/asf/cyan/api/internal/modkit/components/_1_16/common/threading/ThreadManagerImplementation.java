package org.asf.cyan.api.internal.modkit.components._1_16.common.threading;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.threading.ModThread;
import org.asf.cyan.api.threading.ThreadManager;
import org.asf.cyan.api.threading.ThreadMemory;

public class ThreadManagerImplementation extends ThreadManager implements IModKitComponent {

	@Override
	public void initializeComponent() {
		implementation = this;
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
