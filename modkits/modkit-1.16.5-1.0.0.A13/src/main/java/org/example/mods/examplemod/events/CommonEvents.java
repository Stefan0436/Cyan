package org.example.mods.examplemod.events;

import org.asf.cyan.api.events.network.CyanServerHandshakeEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.events.objects.resources.ResourceManagerEventObject;
import org.asf.cyan.api.events.resources.manager.ResourceManagerStartupEvent;
import org.asf.cyan.api.network.channels.PacketChannel;
import org.asf.cyan.api.resources.Resource;
import org.asf.cyan.api.resources.Resources;
import org.asf.cyan.api.util.server.language.ClientLanguage;
import org.asf.cyan.mods.AbstractMod;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.example.mods.examplemod.ExampleMod;
import org.example.mods.examplemod.channels.ExamplePacketChannel;
import org.example.mods.examplemod.channels.packets.examplechannel.HelloWorldPacket;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

// Common events are loaded on both client and server
public class CommonEvents implements IEventListenerContainer {

	// Called after login
	@SimpleEvent(ServerSideConnectedEvent.class)
	public void join(ServerConnectionEventObject event) {
		// This server event is always called unless a cyan client fails to handshake
		// Plugin-like mods should use this event unless they depend on client mods
	}

	// Called after cyan verifies that both sides are running it
	@SimpleEvent(CyanServerHandshakeEvent.class)
	public void handshake(ServerConnectionEventObject event) {
		// Client objects contain information about the remote client
		// Client cl = Client.getForConnection(event);
		
		
		// Lets send our Hello World packet:
		// First, we load our packet channel:
		ExamplePacketChannel channel = PacketChannel.getChannel(ExamplePacketChannel.class, event);
		
		//
		// Then we instantiate a Hello World packet followed by the instruction to send it:
		new HelloWorldPacket().setValues(event, "Hello to You").write(channel); // Sends the hello world packet
		
		
		// They can store modkit-compatible metadata (such as mods)
		// as long as the modloader provides it (cyan does)
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
