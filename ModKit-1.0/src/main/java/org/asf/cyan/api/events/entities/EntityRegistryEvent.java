package org.asf.cyan.api.events.entities;

import java.util.Iterator;
import java.util.function.BiFunction;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject.EntityInfo;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

/**
 * 
 * Event called to register custom entities
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityRegistryEvent extends AbstractExtendedEvent<EntityRegistryEventObject> {

	public static class EntityTypeEntry {
		public EntityTypeEntry next;
		public EntityType<?> type;
		public BiFunction<?, Level, ?> constructor;

		@SuppressWarnings("unchecked")
		public BiFunction<EntityType<?>, Level, Entity> getConstructor() {
			return (BiFunction<EntityType<?>, Level, Entity>) constructor;
		}

		public int id;
	}
	
	@Override
	public boolean requiresSynchronizedListeners() {
		return true;
	}

	private static class TypeIterator implements Iterator<EntityTypeEntry> {
		public EntityTypeEntry itm;

		@Override
		public boolean hasNext() {
			return itm != null;
		}

		@Override
		public EntityTypeEntry next() {
			EntityTypeEntry itm = this.itm;
			this.itm = this.itm.next;
			return itm;
		}

	}

	private static class TypeIterable implements Iterable<EntityTypeEntry> {
		public EntityTypeEntry itm;
		public EntityTypeEntry current;

		@Override
		public Iterator<EntityTypeEntry> iterator() {
			TypeIterator iter = new TypeIterator();
			iter.itm = itm;
			return iter;
		}

		public void add(EntityType<?> type) {
			if (itm == null) {
				EntityTypeEntry cont = new EntityTypeEntry();
				cont.type = type;
				cont.id = Registry.ENTITY_TYPE.getId(type);
				current = cont;
				itm = cont;
				return;
			}

			EntityTypeEntry cont = new EntityTypeEntry();
			cont.type = type;
			cont.id = Registry.ENTITY_TYPE.getId(type);
			current.next = cont;
			current = current.next;
		}

	}

	private static TypeIterable entities = new TypeIterable();
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
					new ResourceLocation(entity.namespace, entity.id),
					entity.builder.build(entity.namespace + ":" + entity.id));

			entities.add(type);
			if (entity.callback != null)
				entity.callback.run(type);
		}
	}

	public EntityTypeEntry findEntity(EntityType<?> type) {
		for (EntityTypeEntry entity : entities) {
			if (type == entity.type)
				return entity;
		}

		return null;
	}
}
