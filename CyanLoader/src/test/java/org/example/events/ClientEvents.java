package org.example.events;

import org.asf.cyan.api.events.network.CyanClientHandshakeEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;

// Client events are only loaded on the client
public class ClientEvents implements IEventListenerContainer {

	// Called after the client verifies that both sides are running it
	@SimpleEvent(CyanClientHandshakeEvent.class)
	public void handshake(ClientConnectionEventObject event) {
		event = event;
	}
	
}
