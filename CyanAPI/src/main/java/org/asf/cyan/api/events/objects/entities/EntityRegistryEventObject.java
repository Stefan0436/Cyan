package org.asf.cyan.api.events.objects.entities;

import java.util.HashMap;
import java.util.Map;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.core.Registry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * 
 * Entity Registry Event Object -- Register your entities by using this
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityRegistryEventObject extends EventObject {

	private HashMap<String, EntityType<?>> entities = new HashMap<String, EntityType<?>>();

	/**
	 * Adds custom entities to the game
	 * 
	 * @param <T>     Entity Class Type
	 * @param id      Entity id
	 * @param builder Entity builder
	 * @return EntityType instance
	 */
	public <T extends Entity> EntityType<T> addEntity(String id, EntityType.Builder<T> builder) {
		EntityType<T> output = Registry.register(Registry.ENTITY_TYPE, id, builder.build(id));
		entities.put(id, output);
		return output;
	}

	/**
	 * Retrieves the map of registered mod entities
	 */
	public Map<String, EntityType<?>> getEntities() {
		return new HashMap<String, EntityType<?>>(entities);
	}

}
