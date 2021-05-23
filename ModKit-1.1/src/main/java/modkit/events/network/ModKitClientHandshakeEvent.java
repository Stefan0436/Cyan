package modkit.events.network;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.network.ClientConnectionEventObject;

/**
 * 
 * Client-side Cyan Handshake Event -- Called after the server verified that both sides are running Cyan.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ModKitClientHandshakeEvent extends AbstractExtendedEvent<ClientConnectionEventObject> {

	private static ModKitClientHandshakeEvent implementation;

	@Override
	public String channelName() {
		return "modkit.network.client.cyanhandshake";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ModKitClientHandshakeEvent getInstance() {
		return implementation;
	}
}
