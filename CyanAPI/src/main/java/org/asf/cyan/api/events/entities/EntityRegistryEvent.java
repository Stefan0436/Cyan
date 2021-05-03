package org.asf.cyan.api.events.entities;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject.EntityInfo;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

/**
 * 
 * Event called to register custom entities
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityRegistryEvent extends AbstractExtendedEvent<EntityRegistryEventObject> {

	private static EntityRegistryEvent implementation;

	@Override
	public String channelName() {
		return "modkit.entity.register";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static EntityRegistryEvent getInstance() {
		return implementation;
	}

	public void registerEntities(EntityRegistryEventObject cyanEntities) {
		for (EntityInfo<?> entity : cyanEntities.getEntities()) {
			EntityType<?> type = Registry.register(Registry.ENTITY_TYPE,
					new ResourceLocation(entity.namespace, entity.id), entity.builder.build(entity.id));
			if (entity.callback != null)
				entity.callback.run(type);
		}
	}
}
