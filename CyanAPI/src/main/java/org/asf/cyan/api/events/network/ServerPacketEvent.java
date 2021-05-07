package org.asf.cyan.api.events.network;

import java.util.ArrayList;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.extended.AsyncFunction;
import org.asf.cyan.api.events.extended.EventObject.EventResult;
import org.asf.cyan.api.events.objects.network.ServerPacketEventObject;

/**
 * 
 * Server Packet Processor -- Processes Cyan packets. (use cancel to accept the
 * packet)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ServerPacketEvent extends AbstractExtendedEvent<ServerPacketEventObject> {

	private static ServerPacketEvent implementation;
	private ArrayList<String> clientLanguage = new ArrayList<String>();

	public void defineLanguageKey(String key) {
		if (!clientLanguage.contains(key))
			clientLanguage.add(key);
	}

	@Override
	public AsyncFunction<EventResult> dispatch(ServerPacketEventObject event) {
		clientLanguage.forEach(str -> event.defineClientLanguageKey(str));
		return super.dispatch(event);
	}

	@Override
	public String channelName() {
		return "modkit.network.server.received";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ServerPacketEvent getInstance() {
		return implementation;
	}
}
