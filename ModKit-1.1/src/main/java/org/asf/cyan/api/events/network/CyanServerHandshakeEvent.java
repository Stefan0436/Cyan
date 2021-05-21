package org.asf.cyan.api.events.network;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;

/**
 * 
 * Server-side Cyan Handshake Event -- Called after the server verified that both sides are running Cyan.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CyanServerHandshakeEvent extends AbstractExtendedEvent<ServerConnectionEventObject> {

	private static CyanServerHandshakeEvent implementation;

	@Override
	public String channelName() {
		return "modkit.network.server.cyanhandshake";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static CyanServerHandshakeEvent getInstance() {
		return implementation;
	}
}
