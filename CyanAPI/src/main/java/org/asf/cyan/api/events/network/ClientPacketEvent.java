package org.asf.cyan.api.events.network;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.network.ClientPacketEventObject;

/**
 * 
 * Client Packet Processor -- Processes Cyan packets. (use cancel to accept the
 * packet)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ClientPacketEvent extends AbstractExtendedEvent<ClientPacketEventObject> {
 
	private static ClientPacketEvent implementation;

	@Override
	public String channelName() {
		return "modkit.network.client.received";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ClientPacketEvent getInstance() {
		return implementation;
	}
}
