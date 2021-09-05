package modkit.events.network;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.network.ServerConnectionEventObject;

/**
 * 
 * Server-side Connection Event -- Called after a client has connected
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ServerSideConnectedEvent extends AbstractExtendedEvent<ServerConnectionEventObject> {

	private static ServerSideConnectedEvent implementation;

	@Override
	public String channelName() {
		return "modkit.network.server.login";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ServerSideConnectedEvent getInstance() {
		return implementation;
	}

	@Override
	public boolean requiresSynchronizedListeners() {
		return true;
	}

}
