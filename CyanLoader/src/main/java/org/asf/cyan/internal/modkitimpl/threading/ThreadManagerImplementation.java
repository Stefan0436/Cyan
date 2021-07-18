package org.asf.cyan.internal.modkitimpl.threading;

import java.util.ArrayList;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.TargetModloader;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;

import modkit.threading.ModThread;
import modkit.threading.ThreadManager;
import modkit.threading.ThreadMemory;
import modkit.threading.ThreadStatus;

@TargetModloader(CyanLoader.class)
public class ThreadManagerImplementation extends ThreadManager implements IPostponedComponent {

	private ArrayList<ModThread> threads = new ArrayList<ModThread>();
	private ArrayList<ThreadMemory> memory = new ArrayList<ThreadMemory>();

	@Override
	public void initComponent() {
		implementation = new ThreadManagerImplementation();
	}

	@Override
	public ModThread[] getThreads() {
		return threads.stream().filter(t -> t.getStatus() == ThreadStatus.RUNNING).toArray(t -> new ModThread[t]);
	}

	@Override
	public ModThread[] getSuspendedThreads() {
		return threads.stream().filter(t -> t.getStatus() == ThreadStatus.SUSPENDED).toArray(t -> new ModThread[t]);
	}

	@Override
	public ThreadMemory[] getSavedThreads() {
		return memory.toArray(t -> new ThreadMemory[t]);
	}

	@Override
	protected void removeFromMemory(ThreadMemory th) {
		if (memory.contains(th))
			memory.remove(th);
	}

	@Override
	protected ThreadManager instantiate() {
		return new ThreadManagerImplementation();
	}

	@Override
	protected void saveThreadToMemory(ThreadMemory memory) {
		this.memory.add(memory);
	}

	@Override
	protected ModThread recreateThread(ThreadMemory threadMemory) {
		ModThreadImplementation th = new ModThreadImplementation(this, (ThreadMemoryImplementation) threadMemory);
		threads.add(th);
		return th;
	}

	@Override
	protected ModThread newThread(IBaseMod owner) {
		ThreadQueueImplementation queue = new ThreadQueueImplementation();
		ThreadMemoryImplementation memory = new ThreadMemoryImplementation(this, ThreadStatus.SAVED, queue);

		return recreateThread(memory);
	}

	@Override
	protected void setThreadName(String name, ModThread thread) {
		String modName = name;
		int i = 1;
		while (getThread(modName) != null) {
			modName = name + " #" + i++;
		}
		ThreadMemoryImplementation mem = (ThreadMemoryImplementation) thread.getMemory();
		mem.setName(modName);
	}

	@Override
	public ModThread getThread(String name) {
		for (ModThread th : threads) {
			if (th.getName() == null)
				continue;
			if (th.getName().equals(name)) {
				return th;
			}
		}
		return null;
	}

	public void removeThread(ModThreadImplementation th) {
		threads.remove(th);
	}

}
