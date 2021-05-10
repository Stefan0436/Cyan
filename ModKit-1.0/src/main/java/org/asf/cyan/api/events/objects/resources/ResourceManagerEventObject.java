package org.asf.cyan.api.events.objects.resources;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.server.packs.resources.ResourceManager;

/**
 * 
 * Resource Manager Event Object -- Event object for all events related to the
 * resource manager.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ResourceManagerEventObject extends EventObject {
	private ResourceManager resourceManager;

	public ResourceManagerEventObject(ResourceManager resourceManager) {
		this.resourceManager = resourceManager;
	}

	/**
	 * Retrieves the resource manager
	 */
	public ResourceManager getResourceManager() {
		return resourceManager;
	}

}
