package org.asf.cyan.api.events.objects.ingame.items;

import java.util.Iterator;
import java.util.function.Function;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;
import net.minecraft.world.level.block.Block;

/**
 * 
 * Item Registry Event Object -- Register your mod items with this event
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ItemRegistryEventObject extends EventObject {

	private ItemInfo<?> first = null;

	public static interface ItemRegistryCallback<T extends Item> {
		@SuppressWarnings("unchecked")
		public default void run(Object out) {
			call((T) out);
		}

		public void call(T output);
	}

	public class ItemInfo<T extends Item> {
		public String id;
		public String namespace;
		public Properties props;
		public ItemRegistryCallback<T> callback;
		public Function<Properties, T> constructor;
		public ItemInfo<?> next;

		public ItemInfo(String id, String namespace, Properties props, ItemRegistryCallback<T> callback,
				Function<Properties, T> constructor) {
			this.namespace = namespace;
			this.id = id;
			this.props = props;
			this.constructor = constructor;
			this.callback = callback;
		}

	}

	/**
	 * Retrieves the map of mod items to be registered
	 */
	public Iterable<ItemInfo<?>> getItems() {
		return new ItemInfoIterable(first);
	}

	/**
	 * Adds custom items to the game
	 * 
	 * @param <T>         Item type
	 * @param namespace   Item namespace
	 * @param id          Item id
	 * @param constructor Item constructor
	 * @param properties  Item properties
	 * @param result      Result function (receives the item type instance)
	 */
	public <T extends Item> void addItem(String namespace, String id, Function<Properties, T> constructor,
			Properties properties, ItemRegistryCallback<T> result) {

		ItemInfo<T> entry = new ItemInfo<T>(id, namespace, properties, result, constructor);
		if (first == null) {
			first = entry;
			return;
		}

		ItemInfo<?> itm = first;
		while (itm.next != null) {
			itm = itm.next;
		}
		itm.next = entry;

	}

	/**
	 * Adds block items to the game
	 * 
	 * @param type   Block type
	 * @param result Result function (receives the item type instance)
	 */
	public void addBlock(Block type, ItemRegistryCallback<Item> result) {
		addBlock(type, new Properties(), result);
	}

	/**
	 * Adds block items to the game
	 * 
	 * @param type Block type
	 */
	public void addBlock(Block type) {
		addBlock(type, (ItemRegistryCallback<Item>) null);
	}

	/**
	 * Adds block items to the game
	 * 
	 * @param type       Block type
	 * @param properties Item properties
	 * @param result     Result function (receives the item type instance)
	 */
	public void addBlock(Block type, Properties properties, ItemRegistryCallback<Item> result) {
		ResourceLocation loc = Registry.BLOCK.getKey(type);
		addItem(loc.getNamespace(), loc.getPath(), t -> new BlockItem(type, t), properties, result);
	}

	/**
	 * Adds block items to the game
	 * 
	 * @param type       Block type
	 * @param properties Item properties
	 * @param tab        Creative mode tab
	 */
	public void addBlock(Block type, Properties properties, CreativeModeTab tab) {
		addBlock(type, properties, tab, null);
	}

	/**
	 * Adds block items to the game
	 * 
	 * @param type   Block type
	 * @param tab    Creative mode tab
	 * @param result Result function (receives the item type instance)
	 */
	public void addBlock(Block type, CreativeModeTab tab, ItemRegistryCallback<Item> result) {
		addBlock(type, new Properties(), tab, result);
	}

	/**
	 * Adds block items to the game
	 * 
	 * @param type Block type
	 * @param tab  Creative mode tab
	 */
	public void addBlock(Block type, CreativeModeTab tab) {
		addBlock(type, new Properties(), tab, null);
	}

	/**
	 * Adds block items to the game
	 * 
	 * @param type       Block type
	 * @param properties Item properties
	 * @param tab        Creative mode tab
	 * @param result     Result function (receives the item type instance)
	 */
	public void addBlock(Block type, Properties properties, CreativeModeTab tab, ItemRegistryCallback<Item> result) {
		addBlock(type, properties.tab(tab), result);
	}

	/**
	 * Adds block items to the game
	 * 
	 * @param type       Block type
	 * @param properties Item properties
	 */
	public void addBlock(Block type, Properties properties) {
		addBlock(type, properties, (ItemRegistryCallback<Item>) null);
	}

	/**
	 * Adds custom items to the game
	 * 
	 * @param <T>         Item type
	 * @param id          Item id
	 * @param constructor Item constructor
	 * @param properties  Item properties
	 * @param result      Result function (retrieves the item type instance)
	 */
	public <T extends Item> void addItem(String id, Function<Properties, T> constructor, Properties properties,
			ItemRegistryCallback<T> result) {
		addItem("cyan", id, constructor, properties, result);
	}

	/**
	 * Adds custom items to the game
	 * 
	 * @param <T>         Item type
	 * @param namespace   Item namespace
	 * @param id          Item id
	 * @param constructor Item constructor
	 * @param properties  Item properties
	 */
	public <T extends Item> void addItem(String namespace, String id, Function<Properties, T> constructor,
			Properties properties) {
		addItem(namespace, id, constructor, properties);
	}

	/**
	 * Adds custom items to the game
	 * 
	 * @param <T>         Item type
	 * @param id          Item id
	 * @param constructor Item constructor
	 * @param properties  Item properties
	 */
	public <T extends Item> void addItem(String id, Function<Properties, T> constructor, Properties properties) {
		addItem("cyan", id, constructor, properties);
	}

	private class ItemInfoIterable implements Iterable<ItemInfo<?>> {
		class ItemInfoIterator implements Iterator<ItemInfo<?>> {
			private ItemInfo<?> item = null;

			public ItemInfoIterator(ItemInfo<?> item) {
				this.item = item;
			}

			@Override
			public boolean hasNext() {
				return item != null;
			}

			@Override
			public ItemInfo<?> next() {
				ItemInfo<?> itm = item;
				item = item.next;
				return itm;
			}

		}

		private ItemInfo<?> item = null;

		public ItemInfoIterable(ItemInfo<?> first) {
			item = first;
		}

		@Override
		public Iterator<ItemInfo<?>> iterator() {
			return new ItemInfoIterator(item);
		}
	}

}
