package modkit.events.core;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.core.ServerEventObject;

/**
 * 
 * Shutdown event -- called on server shutdown
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ServerShutdownEvent extends AbstractExtendedEvent<ServerEventObject> {

	private static ServerShutdownEvent implementation;

	@Override
	public String channelName() {
		return "modkit.stop.server";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ServerShutdownEvent getInstance() {
		return implementation;
	}

}
