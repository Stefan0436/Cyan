package modkit.events.network;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.network.ServerConnectionEventObject;

/**
 * 
 * Server-side Cyan Handshake Event -- Called after the server verified that both sides are running Cyan.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ModKitServerHandshakeEvent extends AbstractExtendedEvent<ServerConnectionEventObject> {

	private static ModKitServerHandshakeEvent implementation;

	@Override
	public String channelName() {
		return "modkit.network.server.cyanhandshake";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ModKitServerHandshakeEvent getInstance() {
		return implementation;
	}
}
