package org.asf.cyan.api.events.network;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;

/**
 * 
 * Client-side Cyan Handshake Event -- Called after the server verified that
 * both sides are running Cyan, but before checking mods.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EarlyCyanClientHandshakeEvent extends AbstractExtendedEvent<ClientConnectionEventObject> {

	private static EarlyCyanClientHandshakeEvent implementation;

	@Override
	public String channelName() {
		return "modkit.network.client.cyanhandshake.early";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static EarlyCyanClientHandshakeEvent getInstance() {
		return implementation;
	}
}
