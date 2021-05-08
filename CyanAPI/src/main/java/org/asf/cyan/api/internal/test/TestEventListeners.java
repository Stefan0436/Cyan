package org.asf.cyan.api.internal.test;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.core.ReloadEvent;
import org.asf.cyan.api.events.entities.EntityAttributesEvent;
import org.asf.cyan.api.events.entities.EntityRegistryEvent;
import org.asf.cyan.api.events.entities.EntityRendererRegistryEvent;
import org.asf.cyan.api.events.network.ClientSideLoginEvent;
import org.asf.cyan.api.events.network.CyanClientHandshakeEvent;
import org.asf.cyan.api.events.network.CyanServerHandshakeEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.core.ReloadEventObject;
import org.asf.cyan.api.events.objects.entities.EntityAttributesEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject.EntityRegistryCallback;
import org.asf.cyan.api.events.objects.entities.EntityRendererRegistryEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

@CYAN_COMPONENT
public class TestEventListeners extends CyanComponent implements IEventListenerContainer {
	EntityType<TestEntity> ent;
	private static boolean first = true;

	protected static void initComponent() {
		BaseEventController.addEventContainer(new TestEventListeners());
	}

	@SimpleEvent(value = ReloadEvent.class, synchronize = true)
	public void reload(ReloadEventObject event) {
//		if (first) {
//			HandshakeRule.registerRule(new HandshakeRule(GameSide.CLIENT, "test", "1.16.5"));
//			HandshakeRule.registerRule(new HandshakeRule(GameSide.SERVER, "test", "1.16.5"));
//		}
		if (first && Modloader.getModloaderGameSide() == GameSide.CLIENT && System.getProperty("serverIP") != null) {
			int port = 25565;
			if (System.getProperty("serverPort") != null)
				port = Integer.valueOf(System.getProperty("serverPort"));
			Minecraft.getInstance().setScreen(new ConnectScreen(new TitleScreen(), Minecraft.getInstance(),
					System.getProperty("serverIP"), port));
		}
		first = false;
	}

	@SimpleEvent(CyanClientHandshakeEvent.class)
	private void successfulCyanHandshakeClient(ClientConnectionEventObject event) {
		event = event;
	}

	@SimpleEvent(CyanServerHandshakeEvent.class)
	private void successfulCyanHandshakeServer(ServerConnectionEventObject event) {
		event = event;
	}

	@SimpleEvent(ClientSideLoginEvent.class)
	private void login(ClientConnectionEventObject event) {
//		TestPacketChannel ch = PacketChannel.getChannel(TestPacketChannel.class, event);
//		ch.sendPacket("test", "test",
//				new PacketWriter.RawWriter(ch.newPacket().writeString("Hello 123")).writeString("123").getWriter());
	}

	@SimpleEvent(ServerSideConnectedEvent.class)
	private void login(ServerConnectionEventObject event) {
//		event.sendNewClientPacket("test", new FriendlyByteBuf(Unpooled.buffer()).writeUtf("tester 123"));
	}

	@SimpleEvent(EntityRendererRegistryEvent.class)
	public void test(EntityRendererRegistryEventObject event) {
		event.addEntity(ent, new TestRenderer(event.getDispatcher()));
	}

	@SimpleEvent(EntityAttributesEvent.class)
	public void test(EntityAttributesEventObject event) {
		event.addSupplier(ent, TestEntity.createAttributes().build());
	}

	@SimpleEvent(value = EntityRegistryEvent.class)
	public void test(EntityRegistryEventObject event) {
		event.addEntity("testmod", "testentity", TestEntity::new,
				EntityType.Builder.of(TestEntity::new, MobCategory.MISC), new EntityRegistryCallback<TestEntity>() {

					@Override
					protected void call() {
						ent = getEntityType();
					}

				});
	}
}
