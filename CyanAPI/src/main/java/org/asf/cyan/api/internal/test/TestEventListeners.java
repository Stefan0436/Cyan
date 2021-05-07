package org.asf.cyan.api.internal.test;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.entities.EntityAttributesEvent;
import org.asf.cyan.api.events.entities.EntityRegistryEvent;
import org.asf.cyan.api.events.entities.EntityRendererRegistryEvent;
import org.asf.cyan.api.events.network.ClientPacketEvent;
import org.asf.cyan.api.events.network.ClientSideLoginEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.network.ServerPacketEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.entities.EntityAttributesEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject.EntityRegistryCallback;
import org.asf.cyan.api.events.objects.entities.EntityRendererRegistryEventObject;
import org.asf.cyan.api.events.objects.network.ClientPacketEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ServerPacketEventObject;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

@CYAN_COMPONENT
public class TestEventListeners extends CyanComponent implements IEventListenerContainer {
	EntityType<TestEntity> ent;

	protected static void initComponent() {
		BaseEventController.addEventContainer(new TestEventListeners());
	}

	@SimpleEvent(ClientPacketEvent.class)
	private void packetEventClient(ClientPacketEventObject event) {
		event = event;
	}

	@SimpleEvent(ServerPacketEvent.class)
	private void packetEventServer(ServerPacketEventObject event) {
		event = event;
	}

	@SimpleEvent(ClientSideLoginEvent.class)
	private void login(ClientConnectionEventObject event) {
		event.sendNewServerPacket("test", new FriendlyByteBuf(Unpooled.buffer()).writeUtf("tester 123"));
	}

	@SimpleEvent(ServerSideConnectedEvent.class)
	private void login(ServerConnectionEventObject event) {
		event.sendNewClientPacket("test", new FriendlyByteBuf(Unpooled.buffer()).writeUtf("tester 123"));
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
		event.addEntity("testmod", "testentity", EntityType.Builder.of(TestEntity::new, MobCategory.MISC),
				new EntityRegistryCallback<TestEntity>() {

					@Override
					protected void call() {
						ent = getEntityType();
					}

				});
	}
}
