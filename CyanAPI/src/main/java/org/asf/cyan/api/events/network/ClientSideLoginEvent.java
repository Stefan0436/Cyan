package org.asf.cyan.api.events.network;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;

/**
 * 
 * Client-side Login Event -- Called after login
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ClientSideLoginEvent extends AbstractExtendedEvent<ClientConnectionEventObject> {

	private static ClientSideLoginEvent implementation;

	@Override
	public String channelName() {
		return "modkit.network.client.login";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ClientSideLoginEvent getInstance() {
		return implementation;
	}
}
