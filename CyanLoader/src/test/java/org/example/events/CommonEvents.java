package org.example.events;

import org.asf.cyan.api.advanced.Client;
import org.asf.cyan.api.events.network.CyanServerHandshakeEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.events.objects.resources.ResourceManagerEventObject;
import org.asf.cyan.api.events.resources.manager.ResourceManagerStartupEvent;
import org.asf.cyan.api.resources.Resource;
import org.asf.cyan.api.resources.Resources;
import org.asf.cyan.api.util.server.language.ClientLanguage;
import org.asf.cyan.mods.AbstractMod;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.example.ExampleMod;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// Common events are loaded on both client and server
public class CommonEvents implements IEventListenerContainer {

	// Called after login
	@SimpleEvent(ServerSideConnectedEvent.class)
	public void join(ServerConnectionEventObject event) {
		event = event;
	}

	// Called after cyan verifies that both sides are running it
	@SimpleEvent(CyanServerHandshakeEvent.class)
	public void handshake(ServerConnectionEventObject event) {
		Client cl = Client.getForConnection(event);
		event = event;
	}

	// Called after resources have been loaded
	@SimpleEvent(ResourceManagerStartupEvent.class)
	public void resourceManagerStartup(ResourceManagerEventObject event) {

		//
		// ModKit has a client language system that tells the server
		// which language keys are present on the client, this way, the server can send
		// fallback messages
		// if the client is missing the language keys.
		//
		// Loads our language into the language marker
		loadLanguage(Resources.getFor(AbstractMod.getInstance(ExampleMod.class)).getResource("lang/en_us.json"));

		//
		// You can use the following to create a chat component:
		// ClientLanguage.createComponent(player, key)
		//
		// It returns either a translatable component or a fallback text component
		// depending on the client language.

	}

	private void loadLanguage(Resource resource) {
		// Load resource into JsonObject
		JsonObject lang = JsonParser.parseString(resource.readAsString()).getAsJsonObject();
		
		// Iterate through all entries
		lang.entrySet().forEach(ent -> {
			
			// Register the key with its value as fallback
			ClientLanguage.registerLanguageKey(ent.getKey(), ent.getValue().getAsString());
			
		});
	}

}
