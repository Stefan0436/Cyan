package org.asf.cyan.api.events.resources.modresources;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.resources.ResourcePackEventObject;

/**
 * 
 * Resource Pack Load Event -- Called after the Cyan Resource Pack has been
 * loaded.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ModResourcePackLoadEvent extends AbstractExtendedEvent<ResourcePackEventObject> {

	private static ModResourcePackLoadEvent implementation;

	@Override
	public String channelName() {
		return "modkit.load.resources";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ModResourcePackLoadEvent getInstance() {
		return implementation;
	}

}
