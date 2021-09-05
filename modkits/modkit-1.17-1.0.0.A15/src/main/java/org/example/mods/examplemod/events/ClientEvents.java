package org.example.mods.examplemod.events;

import modkit.events.network.ModKitClientHandshakeEvent;
import modkit.events.objects.network.ClientConnectionEventObject;

import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;

// Client events are only loaded on the client
public class ClientEvents implements IEventListenerContainer {

	// Called after the client verifies that both sides are running it
	@SimpleEvent(ModKitClientHandshakeEvent.class)
	public void handshake(ClientConnectionEventObject event) {
		// You can manage mod communications with this event.
		// Do not attempt to send mod packets to a non-cyan server and vice-versa.
	}

}
