package org.asf.cyan.api.events.objects.ingame.entities;

import org.asf.cyan.api.events.extended.EventObject;

import com.google.common.collect.ImmutableMap.Builder;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;

/**
 * 
 * Entity Attributes Event Object -- Register your entity attribute suppliers
 * with this event
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityAttributesEventObject extends EventObject {

	private Builder<EntityType<? extends LivingEntity>, AttributeSupplier> builder;

	public EntityAttributesEventObject(Builder<EntityType<? extends LivingEntity>, AttributeSupplier> builder) {
		this.builder = builder;
	}

	public Builder<EntityType<? extends LivingEntity>, AttributeSupplier> getBuilder() {
		return builder;
	}

	/**
	 * Adds attribute suppliers
	 * 
	 * @param <T>        Entity type
	 * @param entityType EntityType instance
	 * @param supplier   AttributeSupplier instance
	 */
	public synchronized <T extends LivingEntity> void addSupplier(EntityType<T> entityType,
			AttributeSupplier supplier) {
		builder = builder.put(entityType, supplier);
	}

}
