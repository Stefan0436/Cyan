package org.asf.cyan.api.internal.test.sides;

import java.util.Optional;

import org.asf.cyan.api.events.core.DataFixerEvent;
import org.asf.cyan.api.events.core.ReloadEvent;
import org.asf.cyan.api.events.core.ServerShutdownEvent;
import org.asf.cyan.api.events.entities.EntityAttributesEvent;
import org.asf.cyan.api.events.entities.EntityRegistryEvent;
import org.asf.cyan.api.events.ingame.commands.CommandManagerStartupEvent;
import org.asf.cyan.api.events.ingame.level.ServerLevelLoadEvent;
import org.asf.cyan.api.events.ingame.tags.TagManagerStartupEvent;
import org.asf.cyan.api.events.network.CyanServerHandshakeEvent;
import org.asf.cyan.api.events.network.PlayerLogoutEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.core.DataFixerEventObject;
import org.asf.cyan.api.events.objects.core.ReloadEventObject;
import org.asf.cyan.api.events.objects.core.ServerShutdownEventObject;
import org.asf.cyan.api.events.objects.entities.EntityAttributesEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject.EntityRegistryCallback;
import org.asf.cyan.api.events.objects.ingame.commands.CommandManagerEventObject;
import org.asf.cyan.api.events.objects.ingame.level.ServerLevelLoadEventObject;
import org.asf.cyan.api.events.objects.ingame.tags.TagManagerEventObject;
import org.asf.cyan.api.events.objects.network.PlayerLogoutEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.events.objects.resources.ResourceManagerEventObject;
import org.asf.cyan.api.events.resources.manager.ResourceManagerStartupEvent;
import org.asf.cyan.api.internal.test.TestEntity;
import org.asf.cyan.api.internal.test.datafixers.CustomEntityFixer.CustomEntityAddSchema;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;

import com.mojang.datafixers.schemas.Schema;

import net.minecraft.SharedConstants;
import net.minecraft.util.datafix.fixes.AddNewChoices;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ServerEvents implements IEventListenerContainer {
	public static EntityType<TestEntity> TEST_ENTITY;

	@SimpleEvent(EntityAttributesEvent.class)
	public void test(EntityAttributesEventObject event) {
		event.addSupplier(TEST_ENTITY, TestEntity.createAttributes().build());
	}

	@SimpleEvent(DataFixerEvent.class)
	public void test(DataFixerEventObject event) {
		Schema schem = event.getBuilder().addSchema(SharedConstants.getCurrentVersion().getWorldVersion(),
				CustomEntityAddSchema::new);
		event.getBuilder().addFixer(new AddNewChoices(schem, "Add custom entities", References.ENTITY));
	}

	@SimpleEvent(value = EntityRegistryEvent.class)
	public void test(EntityRegistryEventObject event) {
		event.addEntity("testmod", "testentity", TestEntity::new,
				EntityType.Builder.of(TestEntity::new, MobCategory.MISC), new EntityRegistryCallback<TestEntity>() {

					@Override
					protected void call() {
						TEST_ENTITY = getEntityType();
					}

				});
	}

	@SimpleEvent(value = ReloadEvent.class)
	public void reload(ReloadEventObject event) {
		event = event;
	}

	@SimpleEvent(value = CommandManagerStartupEvent.class)
	public void startCommandManager(CommandManagerEventObject event) {
		event = event;
	}

	@SimpleEvent(value = ResourceManagerStartupEvent.class)
	public void startResourceManager(ResourceManagerEventObject event) {
		Optional<EntityType<?>> type = EntityType.byString("testmod:testentity");
		event = event;
	}

	@SimpleEvent(value = TagManagerStartupEvent.class)
	public void startTagManager(TagManagerEventObject event) {
		event = event;
	}

	@SimpleEvent(CyanServerHandshakeEvent.class)
	private void successfulCyanHandshakeServer(ServerConnectionEventObject event) {
		event = event;
	}

	@SimpleEvent(PlayerLogoutEvent.class)
	private void logout(PlayerLogoutEventObject event) {
		event = event;
	}

	@SimpleEvent(ServerShutdownEvent.class)
	private void shutdown(ServerShutdownEventObject event) {
		event = event;
	}

	@SimpleEvent(ServerLevelLoadEvent.class)
	private void loadWorld(ServerLevelLoadEventObject event) {
		event = event;
	}

	@SimpleEvent(ServerSideConnectedEvent.class)
	private void login(ServerConnectionEventObject event) {
		event = event;
//		event.sendNewClientPacket("test", new FriendlyByteBuf(Unpooled.buffer()).writeUtf("tester 123"));
	}

}
