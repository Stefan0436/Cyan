package org.example.examplemod.events;

import modkit.events.network.ClientSideLoginEvent;
import modkit.events.network.ModKitClientHandshakeEvent;
import modkit.events.objects.network.ClientConnectionEventObject;

import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;

public class ClientEvents implements IEventListenerContainer {

	// ModKit uses the SimpleEvent system,
	// Annotate the event handler with @SimpleEvent(EventClass.class) to attach an
	// event handler to these events. The parameters should only contain the
	// compatible EventObject.
	@SimpleEvent(ClientSideLoginEvent.class)
	private void login(ClientConnectionEventObject event) {
		//
		// Called on player login
		// EventObjects contain the event information,
		//
		// Control-Click on the event type views the event code,
		// The extends statement in the class declaration will tell which event object
		// is compatible.
		//
	}

	@SimpleEvent(ModKitClientHandshakeEvent.class)
	public void clientHandshake(ClientConnectionEventObject event) {
		// Called when the client successfully handshakes with the server
		// login is called before this event is is dispatched, it is recommended to use
		// the handshake event instead of the login event.
	}

}
