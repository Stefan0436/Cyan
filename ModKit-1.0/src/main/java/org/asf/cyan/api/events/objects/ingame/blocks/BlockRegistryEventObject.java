package org.asf.cyan.api.events.objects.ingame.blocks;

import java.util.Iterator;
import java.util.function.Function;

import org.asf.cyan.api.events.extended.EventObject;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

/**
 * 
 * Block Registry Event Object -- Register your blocks by using this
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class BlockRegistryEventObject extends EventObject {

	private BlockInfo<?> first = null;

	public static interface BlockRegistryCallback<T extends Block> {
		@SuppressWarnings("unchecked")
		public default void run(Object out) {
			call((T) out);
		}

		public void call(T output);
	}

	public class BlockInfo<T extends Block> {
		public String id;
		public String namespace;
		public Properties props;
		public BlockRegistryCallback<T> callback;
		public BlockInfo<?> next;
		public Function<Properties, T> constructor;

		public BlockInfo(String id, String namespace, Function<Properties, T> constructor, Properties props,
				BlockRegistryCallback<T> callback) {
			this.namespace = namespace;
			this.id = id;
			this.props = props;
			this.callback = callback;
			this.constructor = constructor;
		}
	}

	/**
	 * Adds custom blocks to the game
	 * 
	 * @param <T>         Block Class Type
	 * @param id          Block id
	 * @param constructor Block constructor
	 * @param properties  Block properties
	 * @param result      Result function (receives the Block type instance)
	 */
	public <T extends Block> void addBlock(String id, Function<Properties, T> constructor, Properties properties,
			BlockRegistryCallback<T> result) {
		addBlock(id, "cyan", constructor, properties, result);
	}

	/**
	 * Adds custom blocks to the game
	 * 
	 * @param <T>         Block Class Type
	 * @param namespace   Block namespace
	 * @param id          Block id
	 * @param constructor Block constructor
	 * @param properties  Block properties
	 * @param result      Result function (receives the Block type instance)
	 */
	public <T extends Block> void addBlock(String namespace, String id, Function<Properties, T> constructor,
			Properties properties, BlockRegistryCallback<T> result) {

		BlockInfo<T> entry = new BlockInfo<T>(id, namespace, constructor, properties, result);
		if (first == null) {
			first = entry;
			return;
		}

		BlockInfo<?> itm = first;
		while (itm.next != null) {
			itm = itm.next;
		}
		itm.next = entry;
	}

	/**
	 * Adds custom blocks to the game
	 * 
	 * @param <T>         Block Class Type
	 * @param id          Block id
	 * @param constructor Block constructor
	 * @param properties  Block builder
	 */
	public <T extends Block> void addBlock(String id, Function<Properties, T> constructor, Properties builder) {
		addBlock(id, "cyan", constructor, builder, null);
	}

	/**
	 * Adds custom blocks to the game
	 * 
	 * @param <T>         Block Class Type
	 * @param namespace   Block namespace
	 * @param id          Block id
	 * @param constructor Block constructor
	 * @param properties  Block builder
	 */
	public <T extends Block> void addBlock(String namespace, String id, Function<Properties, T> constructor,
			Properties properties) {
		addBlock(id, namespace, constructor, properties, null);
	}

	/**
	 * Retrieves the map of mod blocks to be registered
	 */
	public Iterable<BlockInfo<?>> getBlocks() {
		return new BlockInfoIterable(first);
	}

	private class BlockInfoIterable implements Iterable<BlockInfo<?>> {
		class BlockInfoIterator implements Iterator<BlockInfo<?>> {
			private BlockInfo<?> item = null;

			public BlockInfoIterator(BlockInfo<?> item) {
				this.item = item;
			}

			@Override
			public boolean hasNext() {
				return item != null;
			}

			@Override
			public BlockInfo<?> next() {
				BlockInfo<?> itm = item;
				item = item.next;
				return itm;
			}

		}

		private BlockInfo<?> item = null;

		public BlockInfoIterable(BlockInfo<?> first) {
			item = first;
		}

		@Override
		public Iterator<BlockInfo<?>> iterator() {
			return new BlockInfoIterator(item);
		}
	}

}
