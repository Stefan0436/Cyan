package org.asf.cyan.internal.modkitimpl.threading;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.TargetModloader;

import modkit.threading.ModThread;
import modkit.threading.ThreadManager;
import modkit.threading.ThreadMemory;

@TargetModloader(CyanLoader.class)
public class ThreadManagerImplementation extends ThreadManager implements IPostponedComponent {

	@Override
	public void initComponent() {
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
