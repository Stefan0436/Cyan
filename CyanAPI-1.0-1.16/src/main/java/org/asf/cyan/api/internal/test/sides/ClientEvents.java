package org.asf.cyan.api.internal.test.sides;

import org.asf.cyan.api.events.core.ReloadEvent;
import org.asf.cyan.api.events.entities.EntityRendererRegistryEvent;
import org.asf.cyan.api.events.network.ClientSideLoginEvent;
import org.asf.cyan.api.events.network.CyanClientHandshakeEvent;
import org.asf.cyan.api.events.resources.manager.LanguageManagerStartupEvent;
import org.asf.cyan.api.events.resources.manager.ResourceManagerStartupEvent;
import org.asf.cyan.api.events.resources.manager.TextureManagerStartupEvent;
import org.asf.cyan.api.events.objects.core.ReloadEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRendererRegistryEventObject;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.objects.resources.LanguageManagerEventObject;
import org.asf.cyan.api.events.objects.resources.ResourceManagerEventObject;
import org.asf.cyan.api.events.objects.resources.TextureManagerEventObject;
import org.asf.cyan.api.internal.test.TestRenderer;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;

public class ClientEvents implements IEventListenerContainer {
	private static boolean first = true;

	@SimpleEvent(value = ReloadEvent.class, synchronize = true)
	public void reload(ReloadEventObject event) {
//		if (first) {
//			HandshakeRule.registerRule(new HandshakeRule(GameSide.CLIENT, "test", "1.16.5"));
//			HandshakeRule.registerRule(new HandshakeRule(GameSide.SERVER, "test", "1.16.5"));
//		}
		if (first && System.getProperty("serverIP") != null) {
			int port = 25565;
			if (System.getProperty("serverPort") != null)
				port = Integer.valueOf(System.getProperty("serverPort"));
			Minecraft.getInstance().setScreen(new ConnectScreen(new TitleScreen(), Minecraft.getInstance(),
					System.getProperty("serverIP"), port));
		}
		first = false;
	}

	@SimpleEvent(value = ResourceManagerStartupEvent.class)
	public void startResourceManager(ResourceManagerEventObject event) {
		event = event;
	}

	@SimpleEvent(value = TextureManagerStartupEvent.class)
	public void startTextureManager(TextureManagerEventObject event) {
		event = event;
	}

	@SimpleEvent(value = LanguageManagerStartupEvent.class)
	public void startTextureManager(LanguageManagerEventObject event) {
		event = event;
	}

	@SimpleEvent(CyanClientHandshakeEvent.class)
	private void successfulCyanHandshakeClient(ClientConnectionEventObject event) {
		event = event;
	}

	@SimpleEvent(EntityRendererRegistryEvent.class)
	public void test(EntityRendererRegistryEventObject event) { // OK
		event.addEntity(ServerEvents.TEST_ENTITY, new TestRenderer(event.getDispatcher()));
	}

	@SimpleEvent(ClientSideLoginEvent.class)
	private void login(ClientConnectionEventObject event) {
		event = event;
//		TestPacketChannel ch = PacketChannel.getChannel(TestPacketChannel.class, event);
//		ch.sendPacket("test", "test",
//				new PacketWriter.RawWriter(ch.newPacket().writeString("Hello 123")).writeString("123").getWriter());
	}

}