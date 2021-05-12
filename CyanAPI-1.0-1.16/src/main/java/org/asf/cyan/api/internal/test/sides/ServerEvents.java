package org.asf.cyan.api.internal.test.sides;

import java.io.IOException;
import java.util.Optional;

import org.asf.cyan.api.config.ConfigManager;
import org.asf.cyan.api.events.core.ReloadEvent;
import org.asf.cyan.api.events.core.ServerShutdownEvent;
import org.asf.cyan.api.events.entities.EntityAttributesEvent;
import org.asf.cyan.api.events.entities.EntityRegistryEvent;
import org.asf.cyan.api.events.ingame.blocks.BlockEntityRegistryEvent;
import org.asf.cyan.api.events.ingame.blocks.BlockRegistryEvent;
import org.asf.cyan.api.events.ingame.commands.CommandManagerStartupEvent;
import org.asf.cyan.api.events.ingame.level.ServerLevelLoadEvent;
import org.asf.cyan.api.events.ingame.materials.MaterialCreationEvent;
import org.asf.cyan.api.events.ingame.tags.TagManagerStartupEvent;
import org.asf.cyan.api.events.network.CyanServerHandshakeEvent;
import org.asf.cyan.api.events.network.PlayerLogoutEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.core.ReloadEventObject;
import org.asf.cyan.api.events.objects.core.ServerShutdownEventObject;
import org.asf.cyan.api.events.objects.entities.EntityAttributesEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject;
import org.asf.cyan.api.events.objects.ingame.blocks.BlockEntityRegistryEventObject;
import org.asf.cyan.api.events.objects.ingame.blocks.BlockRegistryEventObject;
import org.asf.cyan.api.events.objects.ingame.commands.CommandManagerEventObject;
import org.asf.cyan.api.events.objects.ingame.level.ServerLevelLoadEventObject;
import org.asf.cyan.api.events.objects.ingame.materials.MaterialCreationEventObject;
import org.asf.cyan.api.events.objects.ingame.tags.TagManagerEventObject;
import org.asf.cyan.api.events.objects.network.PlayerLogoutEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.events.objects.resources.ResourceManagerEventObject;
import org.asf.cyan.api.events.resources.manager.ResourceManagerStartupEvent;
import org.asf.cyan.api.internal.test.ModConfigTest;
import org.asf.cyan.api.internal.test.TestEventListeners;
import org.asf.cyan.api.internal.test.testing.TestBlock;
import org.asf.cyan.api.internal.test.testing.TestEntity;
import org.asf.cyan.api.internal.test.testing.blockentities.TestBlockEntity;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

public class ServerEvents implements IEventListenerContainer {

	public static EntityType<TestEntity> TEST_ENTITY;
	public static Material CUSTOM_MATERIAL;
	public static TestBlock CUSTOM_BLOCK;
	public static BlockEntityType<?> CUSTOM_BLOCK_ENTITY;

	@AttachEvent(value = "mods.preinit", synchronize = true)
	private void preInit() throws IOException { // OK
		ConfigManager<TestEventListeners> manager = (ConfigManager<TestEventListeners>) ConfigManager
				.getFor(TestEventListeners.class);
		ModConfigTest config = manager.getConfiguration(ModConfigTest.class);
		config = config;
		this.equals(this); // OK
	}

	@SimpleEvent(EntityAttributesEvent.class)
	public void test(EntityAttributesEventObject event) { // OK
		event.addSupplier(TEST_ENTITY, TestEntity.createAttributes().build());
	}

	@SimpleEvent(value = BlockEntityRegistryEvent.class)
	public void test(BlockEntityRegistryEventObject event) {
		event.addEntity("cyan", "testblock", BlockEntityType.Builder.of(TestBlockEntity::new, CUSTOM_BLOCK), ent -> {
			CUSTOM_BLOCK_ENTITY = ent;
		});
	}

	@SimpleEvent(value = EntityRegistryEvent.class)
	public void test(EntityRegistryEventObject event) { // OK
		event.addEntity("testmod", "testentity", EntityType.Builder.of(TestEntity::new, MobCategory.MISC), ent -> {
			TEST_ENTITY = ent;
		});
	}

	@SimpleEvent(value = BlockRegistryEvent.class)
	public void test(BlockRegistryEventObject event) { // OK
		event.addBlock("cyan", "testblock", TestBlock::new, Properties.of(CUSTOM_MATERIAL).sound(SoundType.GRASS),
				out -> {
					CUSTOM_BLOCK = out;
				});
	}

	@SimpleEvent(value = MaterialCreationEvent.class)
	public void test(MaterialCreationEventObject event) {
		event.addMaterial(new Material.Builder(MaterialColor.COLOR_BLACK), mat -> {
			CUSTOM_MATERIAL = mat;
		});
	}

	@SimpleEvent(value = ReloadEvent.class)
	public void reload(ReloadEventObject event) { // OK
		event = event;
	}

	@SimpleEvent(value = CommandManagerStartupEvent.class)
	public void startCommandManager(CommandManagerEventObject event) { // OK
		event = event;
	}

	@SimpleEvent(value = ResourceManagerStartupEvent.class)
	public void startResourceManager(ResourceManagerEventObject event) { // OK
		Optional<EntityType<?>> type = EntityType.byString("testmod:testentity");
		event = event; // OK
	}

	@SimpleEvent(value = TagManagerStartupEvent.class)
	public void startTagManager(TagManagerEventObject event) { // OK
		event = event;
	}

	@SimpleEvent(CyanServerHandshakeEvent.class)
	private void successfulCyanHandshakeServer(ServerConnectionEventObject event) { // OK
		event = event;
	}

	@SimpleEvent(PlayerLogoutEvent.class)
	private void logout(PlayerLogoutEventObject event) { // OK
		event = event;
	}

	@SimpleEvent(ServerShutdownEvent.class)
	private void shutdown(ServerShutdownEventObject event) { // OK
		event = event;
	}

	@SimpleEvent(ServerLevelLoadEvent.class)
	private void loadWorld(ServerLevelLoadEventObject event) { // OK
		event = event;
	}

	@SimpleEvent(ServerSideConnectedEvent.class)
	private void login(ServerConnectionEventObject event) { // OK
		event = event;
//		event.sendNewClientPacket("test", new FriendlyByteBuf(Unpooled.buffer()).writeUtf("tester 123"));
	}

}
