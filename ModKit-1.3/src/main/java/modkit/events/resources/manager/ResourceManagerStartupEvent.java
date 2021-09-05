package modkit.events.resources.manager;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.resources.ResourceManagerEventObject;

/**
 * 
 * Resource Manager Startup Event -- Called on resource manager startup
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ResourceManagerStartupEvent extends AbstractExtendedEvent<ResourceManagerEventObject> {

	private static ResourceManagerStartupEvent implementation;

	@Override
	public String channelName() {
		return "modkit.start.resources.manager";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ResourceManagerStartupEvent getInstance() {
		return implementation;
	}

}
