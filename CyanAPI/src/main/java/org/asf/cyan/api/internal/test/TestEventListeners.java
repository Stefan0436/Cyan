package org.asf.cyan.api.internal.test;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.core.ReloadEvent;
import org.asf.cyan.api.events.entities.EntityAttributesEvent;
import org.asf.cyan.api.events.entities.EntityRegistryEvent;
import org.asf.cyan.api.events.entities.EntityRendererRegistryEvent;
import org.asf.cyan.api.events.objects.core.ReloadEventObject;
import org.asf.cyan.api.events.objects.entities.EntityAttributesEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject.EntityRegistryCallback;
import org.asf.cyan.api.resources.Resources;
import org.asf.cyan.api.events.objects.entities.EntityRendererRegistryEventObject;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

@CYAN_COMPONENT
public class TestEventListeners extends CyanComponent implements IEventListenerContainer {
	EntityType<TestEntity> ent;

	protected static void initComponent() {
		BaseEventController.addEventContainer(new TestEventListeners());
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
