package org.example.examplemod.events;

import org.asf.cyan.api.events.ingame.level.ServerLevelLoadEvent;
import org.asf.cyan.api.events.network.CyanServerHandshakeEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.ingame.level.ServerLevelLoadEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;

public class CommonEvents implements IEventListenerContainer {

	@SimpleEvent(CyanServerHandshakeEvent.class)
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
