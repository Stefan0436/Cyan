package modkit.events.objects.ingame.blocks;

import java.util.Iterator;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.BlockEntityType.Builder;

/**
 * 
 * Block Entity Registry Event Object -- Register your block entities by using this
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class BlockEntityRegistryEventObject extends EventObject {

	private EntityInfo<?> first = null;

	public static interface EntityRegistryCallback<T extends BlockEntity> {
		@SuppressWarnings("unchecked")
		public default void run(Object out) {
			call((BlockEntityType<T>) out);
		}

		public void call(BlockEntityType<T> output);
	}

	public class EntityInfo<T extends BlockEntity> {
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
	 * Adds custom block entities to the game
	 * 
	 * @param <T>     Entity Class Type
	 * @param id      Entity id
	 * @param builder Entity builder
	 * @param result  Result function (receives the EntityType instance)
	 */
	public <T extends BlockEntity> void addEntity(String id, BlockEntityType.Builder<T> builder,
			EntityRegistryCallback<T> result) {
		addEntity(id, "cyan", builder, result);
	}

	/**
	 * Adds custom block entities to the game
	 * 
	 * @param <T>       Entity Class Type
	 * @param namespace Entity namespace
	 * @param id        Entity id
	 * @param builder   Entity builder
	 * @param result    Result function (receives the EntityType instance)
	 */
	public <T extends BlockEntity> void addEntity(String namespace, String id, BlockEntityType.Builder<T> builder,
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
	 * Adds custom block entities to the game
	 * 
	 * @param <T>     Entity Class Type
	 * @param id      Entity id
	 * @param builder Entity builder
	 */
	public <T extends BlockEntity> void addEntity(String id, BlockEntityType.Builder<T> builder) {
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
	public <T extends BlockEntity> void addEntity(String namespace, String id, BlockEntityType.Builder<T> builder) {
		addEntity(id, namespace, builder, null);
	}

	/**
	 * Retrieves the map of mod block entities to be registered
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
