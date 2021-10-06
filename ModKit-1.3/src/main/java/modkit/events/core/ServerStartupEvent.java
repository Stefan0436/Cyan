package modkit.events.core;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.core.ServerEventObject;

/**
 * 
 * Startup event -- called on server startup
 * 
 * @since ModKit 1.3
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public class ServerStartupEvent extends AbstractExtendedEvent<ServerEventObject> {

	private static ServerStartupEvent implementation;

	@Override
	public String channelName() {
		return "modkit.start.server";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ServerStartupEvent getInstance() {
		return implementation;
	}

}
