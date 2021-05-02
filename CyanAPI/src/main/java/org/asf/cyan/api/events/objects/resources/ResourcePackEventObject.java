package org.asf.cyan.api.events.objects.resources;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.server.packs.PackResources;

/**
 * 
 * Resource Event Object -- Event object for all events related to the
 * PackResources type.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ResourcePackEventObject extends EventObject {
	private PackResources pack;

	public ResourcePackEventObject(PackResources pack) {
		this.pack = pack;
	}

	/**
	 * Retrieves the Cyan resource pack
	 */
	public PackResources getResourcePack() {
		return pack;
	}
}
