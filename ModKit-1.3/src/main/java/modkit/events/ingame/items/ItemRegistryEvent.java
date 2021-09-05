package modkit.events.ingame.items;

import java.util.Iterator;
import java.util.function.Function;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.ingame.items.ItemRegistryEventObject;
import modkit.events.objects.ingame.items.ItemRegistryEventObject.ItemInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Item.Properties;

/**
 * 
 * Event called to register custom items
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ItemRegistryEvent extends AbstractExtendedEvent<ItemRegistryEventObject> {

	public static class ItemTypeEntry {
		public ItemTypeEntry next;
		public Item type;
		public Function<Properties, Item> constructor;

		public Function<Properties, Item> getConstructor() {
			return constructor;
		}

		public int id;
	}

	@Override
	public boolean requiresSynchronizedListeners() {
		return true;
	}

	private static class TypeIterator implements Iterator<ItemTypeEntry> {
		public ItemTypeEntry itm;

		@Override
		public boolean hasNext() {
			return itm != null;
		}

		@Override
		public ItemTypeEntry next() {
			ItemTypeEntry itm = this.itm;
			this.itm = this.itm.next;
			return itm;
		}

	}

	private static class TypeIterable implements Iterable<ItemTypeEntry> {
		public ItemTypeEntry itm;
		public ItemTypeEntry current;

		@Override
		public Iterator<ItemTypeEntry> iterator() {
			TypeIterator iter = new TypeIterator();
			iter.itm = itm;
			return iter;
		}

		public void add(Item type) {
			if (itm == null) {
				ItemTypeEntry cont = new ItemTypeEntry();
				cont.type = type;
				cont.id = Registry.ITEM.getId(type);
				current = cont;
				itm = cont;
				return;
			}

			ItemTypeEntry cont = new ItemTypeEntry();
			cont.type = type;
			cont.id = Registry.ITEM.getId(type);
			current.next = cont;
			current = current.next;
		}

	}

	private static TypeIterable items = new TypeIterable();
	private static ItemRegistryEvent implementation;

	@Override
	public String channelName() {
		return "modkit.item.register";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static ItemRegistryEvent getInstance() {
		return implementation;
	}

	public void registerItems(ItemRegistryEventObject cyanItems) {
		for (ItemInfo<?> item : cyanItems.getItems()) {
			Item input = item.constructor.apply(item.props);
			Item type = Registry.register(Registry.ITEM, new ResourceLocation(item.namespace, item.id), input);
			if (input instanceof BlockItem) {
				((BlockItem) input).registerBlocks(Item.BY_BLOCK, input);
			}

			items.add(type);
			if (item.callback != null)
				item.callback.run(type);
		}
	}

	public ItemTypeEntry findEntity(Item type) {
		for (ItemTypeEntry entity : items) {
			if (type == entity.type)
				return entity;
		}

		return null;
	}
}
