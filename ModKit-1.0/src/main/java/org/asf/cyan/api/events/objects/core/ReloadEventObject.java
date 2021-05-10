package org.asf.cyan.api.events.objects.core;

import java.util.HashMap;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

/**
 * 
 * Reload Event Object -- Event object for the game reload event
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ReloadEventObject extends EventObject {

	private ResourceManager resourceManager;
	private ProfilerFiller filler;
	private HashMap<String, Object> state = new HashMap<String, Object>();

	public ReloadEventObject(ResourceManager resourceManager, ProfilerFiller filler) {
		this.resourceManager = resourceManager;
		this.filler = filler;
	}

	/**
	 * Sets a state value
	 * 
	 * @param key   State unique key (unique to mod)
	 * @param value State value (for later use)
	 */
	public void setState(String key, Object value) {
		state.put(key, value);
	}

	/**
	 * Retrieves a state value
	 * 
	 * @param key State unique key (unique to mod)
	 */
	public Object getState(String key) {
		return state.get(key);
	}

	/**
	 * Retrieves the resource manager
	 */
	public ResourceManager getResourceManager() {
		return resourceManager;
	}

	/**
	 * Retrieves the profiler filler
	 */
	public ProfilerFiller getProfilerFiller() {
		return filler;
	}

}
