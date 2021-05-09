package org.asf.cyan.api.events.ingame.level;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.ingame.level.ServerLevelLoadEventObject;

/**
 * 
 * Server Level Load Event -- Called on server world load
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ServerLevelLoadEvent extends AbstractExtendedEvent<ServerLevelLoadEventObject> {

	private static ServerLevelLoadEvent implementation;

	@Override
	public String channelName() {
		return "modkit.load.server.level";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ServerLevelLoadEvent getInstance() {
		return implementation;
	}

}
