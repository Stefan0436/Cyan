package modkit.threading;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;
import org.asf.cyan.api.modloader.information.mods.IModManifest;

/**
 * 
 * Mod Thread Managers
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 * @since ModKit 1.2
 *
 */
public abstract class ThreadManager {
	protected static ThreadManager implementation;
	private static HashMap<String, ArrayList<ThreadManager>> loaders = new HashMap<String, ArrayList<ThreadManager>>();

	/**
	 * Instantiates a new Thread Manager
	 * 
	 * @param owner Mod owning this thread manager
	 */
	public static ThreadManager createManager(IBaseMod owner) {
		if (owner == null)
			throw new NullPointerException("Owner mod cannot be null");
		ThreadManager mg = implementation.instantiate();
		Modloader owningLoader = Modloader.getModOwner(owner.getManifest().id());
		if (owningLoader == null)
			throw new NullPointerException("Owner mod is not owned by any modloader");

		if (!loaders.containsKey(owningLoader.getClass().getTypeName())) {
			loaders.put(owningLoader.getClass().getTypeName(), new ArrayList<ThreadManager>());
		}
		loaders.get(owningLoader.getClass().getTypeName()).add(mg);

		mg.owner = owner;
		return mg;
	}

	/**
	 * Retrieves all thread managers for the given modloader
	 * 
	 * @param loader Modloader instance
	 * @return Array of ThreadManager instances
	 */
	public static ThreadManager[] getThreadManagers(Modloader loader) {
		return loaders.getOrDefault(loader.getClass().getTypeName(), new ArrayList<ThreadManager>())
				.toArray(t -> new ThreadManager[t]);
	}

	/**
	 * Retrieves all thread managers for the given mod
	 * 
	 * @param mod Mod instance
	 * @return Array of ThreadManager instances
	 */
	public static ThreadManager[] getThreadManagers(IBaseMod mod) {
		Modloader loader = Modloader.getModOwner(mod);
		if (loader != null) {
			ArrayList<ThreadManager> managers = new ArrayList<ThreadManager>();

			for (ThreadManager manager : getThreadManagers(loader)) {
				if (manager.getOwner().getManifest().id().equals(mod.getManifest().id()))
					managers.add(manager);
			}

			return managers.toArray(t -> new ThreadManager[t]);
		} else
			return new ThreadManager[0];
	}

	/**
	 * Retrieves all thread managers for the given mod
	 * 
	 * @param mod Mod manifest instance
	 * @return Array of ThreadManager instances
	 */
	public static ThreadManager[] getThreadManagers(IModManifest mod) {
		Modloader loader = Modloader.getModOwner(mod);
		if (loader != null) {
			ArrayList<ThreadManager> managers = new ArrayList<ThreadManager>();

			for (ThreadManager manager : getThreadManagers(loader)) {
				if (manager.getOwner().getManifest().id().equals(mod.id()))
					managers.add(manager);
			}

			return managers.toArray(t -> new ThreadManager[t]);
		} else
			return new ThreadManager[0];
	}

	private BigInteger threadCount = BigInteger.ZERO;
	private IBaseMod owner;

	/**
	 * Retrieves the owning mod
	 * 
	 * @return Thread manager owner mod instance
	 */
	public IBaseMod getOwner() {
		return owner;
	}

	/**
	 * Creates a new mod thread
	 */
	public ModThread createThread() {
		ModThread th = newThread(getOwner());
		threadCount = threadCount.add(BigInteger.ONE);
		setThreadName("Thread #" + threadCount, th);
		th.startThread();
		return th;
	}

	/**
	 * Creates a new mod thread
	 * 
	 * @param name Thread name
	 */
	public ModThread createThread(String name) {
		ModThread th = newThread(getOwner());
		threadCount = threadCount.add(BigInteger.ONE);
		setThreadName(name, th);
		th.startThread();
		return th;
	}

	/**
	 * Retrieves an array of all running threads
	 * 
	 * @return Array of ModThread instances
	 */
	public abstract ModThread[] getThreads();

	/**
	 * Retrieves an array of all suspended threads
	 * 
	 * @return Array of suspended ModThread instances
	 */
	public abstract ModThread[] getSuspendedThreads();

	/**
	 * Retrieves an array of saved threads
	 * 
	 * @return Array of ThreadMemory instances that can be restarted
	 */
	public abstract ThreadMemory[] getSavedThreads();

	/**
	 * Retrieves and restarts a saved mod thread
	 * 
	 * @param threadName Mod thread name
	 * @return ModThread instance or null
	 */
	public ModThread restartSavedThread(String threadName) {
		for (ThreadMemory th : getSavedThreads()) {
			if (th.getName().equals(threadName)) {
				return th.recreate();
			}
		}
		return null;
	}

	/**
	 * Removes the given thread from memory
	 * 
	 * @param th Thread to remove from memory
	 */
	protected abstract void removeFromMemory(ThreadMemory th);

	/**
	 * Instantiates a new thread
	 * 
	 * @param owner Thread owner mod
	 */
	protected abstract ModThread newThread(IBaseMod owner);

	/**
	 * Sets the name of a thread
	 * 
	 * @param name   Thread name
	 * @param thread Thread instance
	 */
	protected abstract void setThreadName(String name, ModThread thread);

	/**
	 * Creates a new thread manager
	 */
	protected abstract ThreadManager instantiate();

	/**
	 * Saves the given thread to memory
	 * 
	 * @param memory Thread to save
	 */
	protected abstract void saveThreadToMemory(ThreadMemory memory);

	/**
	 * Recreates the thread
	 * 
	 * @param threadMemory Thread memory
	 * @return New ModThread instance
	 * @throws IllegalStateException if the thread cannot be re-created
	 */
	protected abstract ModThread recreateThread(ThreadMemory threadMemory) throws IllegalStateException;

	/**
	 * Retrieves an existing mod thread
	 * 
	 * @param name Thread name
	 * @return ModThread instance or null
	 */
	public abstract ModThread getThread(String name);

	/**
	 * Retrieves a list of mod threads
	 * 
	 * @return List of mod threads
	 */
	public List<ModThread> getThreadList() {
		ArrayList<ModThread> threads = new ArrayList<ModThread>();
		for (ModThread th : getThreads())
			threads.add(th);
		return threads;
	}

	/**
	 * Retrieves a list of suspended mod threads
	 * 
	 * @return List of mod threads
	 */
	public List<ModThread> getSuspendedThreadList() {
		ArrayList<ModThread> threads = new ArrayList<ModThread>();
		for (ModThread th : getSuspendedThreads())
			threads.add(th);
		return threads;
	}
}
