package modkit.events.network;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.network.PlayerLogoutEventObject;

/**
 * 
 * Server-side Logout Event -- Called after a client has disconnected
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class PlayerLogoutEvent extends AbstractExtendedEvent<PlayerLogoutEventObject> {

	private static PlayerLogoutEvent implementation;

	@Override
	public String channelName() {
		return "modkit.network.server.logout";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static PlayerLogoutEvent getInstance() {
		return implementation;
	}
}
