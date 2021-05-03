package org.asf.cyan.api.events.objects.entities;

import java.util.Iterator;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityType.Builder;

/**
 * 
 * Entity Registry Event Object -- Register your entities by using this
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class EntityRegistryEventObject extends EventObject {

	private EntityInfo<?> first = null;

	public abstract static class EntityRegistryCallback<T extends Entity> {
		private EntityType<T> type;

		/**
		 * Runs the callback
		 * 
		 * @param type EntityType instance (unchecked to prevent loading issue)
		 */
		@SuppressWarnings("unchecked")
		public void run(Object type) {
			this.type = (EntityType<T>) type;
			call();
		}

		protected EntityType<T> getEntityType() {
			return type;
		}

		protected abstract void call();
	}

	public class EntityInfo<T extends Entity> {
		public String id;
		public String namespace;
		public Builder<?> builder;
		public EntityRegistryCallback<T> callback;
		public EntityInfo<?> next;

		public EntityInfo(String id, String namespace, Builder<?> builder, EntityRegistryCallback<T> callback) {
			this.namespace = namespace;
			this.id = id;
			this.builder = builder;
			this.callback = callback;
		}
	}

	/**
	 * Adds custom entities to the game
	 * 
	 * @param <T>     Entity Class Type
	 * @param id      Entity id
	 * @param builder Entity builder
	 * @param result  Result function (receives the EntityType instance)
	 */
	public <T extends Entity> void addEntity(String id, EntityType.Builder<T> builder,
			EntityRegistryCallback<T> result) {
		addEntity(id, "cyan", builder, result);
	}

	/**
	 * Adds custom entities to the game
	 * 
	 * @param <T>       Entity Class Type
	 * @param namespace Entity namespace
	 * @param id        Entity id
	 * @param builder   Entity builder
	 * @param result    Result function (receives the EntityType instance)
	 */
	public <T extends Entity> void addEntity(String namespace, String id, EntityType.Builder<T> builder,
			EntityRegistryCallback<T> result) {
		EntityInfo<T> entry = new EntityInfo<T>(id, namespace, builder, result);
		if (first == null) {
			first = entry;
			return;
		}

		EntityInfo<?> itm = first;
		while (itm.next != null) {
			itm = itm.next;
		}
		itm.next = entry;
	}

	/**
	 * Adds custom entities to the game
	 * 
	 * @param <T>     Entity Class Type
	 * @param id      Entity id
	 * @param builder Entity builder
	 */
	public <T extends Entity> void addEntity(String id, EntityType.Builder<T> builder) {
		addEntity(id, "cyan", builder, null);
	}

	/**
	 * Adds custom entities to the game
	 * 
	 * @param <T>       Entity Class Type
	 * @param namespace Entity namespace
	 * @param id        Entity id
	 * @param builder   Entity builder
	 */
	public <T extends Entity> void addEntity(String namespace, String id, EntityType.Builder<T> builder) {
		addEntity(id, namespace, builder, null);
	}

	/**
	 * Retrieves the map of mod entities to be registered
	 */
	public Iterable<EntityInfo<?>> getEntities() {
		return new EntityInfoIterable(first);
	}

	private class EntityInfoIterable implements Iterable<EntityInfo<?>> {
		class EntityInfoIterator implements Iterator<EntityInfo<?>> {
			private EntityInfo<?> item = null;

			public EntityInfoIterator(EntityInfo<?> item) {
				this.item = item;
			}

			@Override
			public boolean hasNext() {
				return item != null;
			}

			@Override
			public EntityInfo<?> next() {
				EntityInfo<?> itm = item;
				item = item.next;
				return itm;
			}

		}

		private EntityInfo<?> item = null;

		public EntityInfoIterable(EntityInfo<?> first) {
			item = first;
		}

		@Override
		public Iterator<EntityInfo<?>> iterator() {
			return new EntityInfoIterator(item);
		}
	}

}
