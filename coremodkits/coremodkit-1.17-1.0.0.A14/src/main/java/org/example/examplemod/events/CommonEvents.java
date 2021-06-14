package org.example.examplemod.events;

import modkit.events.ingame.level.ServerLevelLoadEvent;
import modkit.events.network.ModKitServerHandshakeEvent;
import modkit.events.network.ServerSideConnectedEvent;
import modkit.events.objects.ingame.level.ServerLevelLoadEventObject;
import modkit.events.objects.network.ServerConnectionEventObject;

import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;

public class CommonEvents implements IEventListenerContainer {

	@SimpleEvent(ModKitServerHandshakeEvent.class)
	public void serverHandshake(ServerConnectionEventObject event) {
		// Called when a cyan client successfully handshakes with the server
		// You can use Client objects to get client information send by Cyan
	}

	@SimpleEvent(ServerSideConnectedEvent.class)
	public void clientJoined(ServerConnectionEventObject event) {
		// Called when any client successfully joins,
		// not called if a cyan client fails the handshake.
	}
	
	@SimpleEvent(ServerLevelLoadEvent.class)
	public void loadWorld(ServerLevelLoadEventObject event) {
		// Called on world load,
		// The example transformer is redundant, this event provides it.
	}

}
