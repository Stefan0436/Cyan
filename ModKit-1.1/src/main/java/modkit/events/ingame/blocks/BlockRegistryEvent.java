package modkit.events.ingame.blocks;

import java.util.Iterator;
import java.util.function.Function;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;

import modkit.events.objects.ingame.blocks.BlockRegistryEventObject;
import modkit.events.objects.ingame.blocks.BlockRegistryEventObject.BlockInfo;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;

/**
 * 
 * Block Registry Event -- Called to register custom blocks
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class BlockRegistryEvent extends AbstractExtendedEvent<BlockRegistryEventObject> {

	public static class BlockTypeEntry {
		public BlockTypeEntry next;
		public Block type;
		public Function<Properties, ?> constructor;

		@SuppressWarnings("unchecked")
		public Function<Properties, Block> getConstructor() {
			return (Function<Properties, Block>) constructor;
		}

		public int id;
	}

	@Override
	public boolean requiresSynchronizedListeners() {
		return true;
	}

	private static class TypeIterator implements Iterator<BlockTypeEntry> {
		public BlockTypeEntry itm;

		@Override
		public boolean hasNext() {
			return itm != null;
		}

		@Override
		public BlockTypeEntry next() {
			BlockTypeEntry itm = this.itm;
			this.itm = this.itm.next;
			return itm;
		}

	}

	private static class TypeIterable implements Iterable<BlockTypeEntry> {
		public BlockTypeEntry itm;
		public BlockTypeEntry current;

		@Override
		public Iterator<BlockTypeEntry> iterator() {
			TypeIterator iter = new TypeIterator();
			iter.itm = itm;
			return iter;
		}

		public void add(Block type, Function<Properties, ?> constructor) {
			if (itm == null) {
				BlockTypeEntry cont = new BlockTypeEntry();
				cont.type = type;
				cont.id = Registry.BLOCK.getId(type);
				cont.constructor = constructor;
				current = cont;
				itm = cont;
				return;
			}

			BlockTypeEntry cont = new BlockTypeEntry();
			cont.type = type;
			cont.id = Registry.BLOCK.getId(type);
			cont.constructor = constructor;
			current.next = cont;
			current = current.next;
		}

	}

	private static TypeIterable blocks = new TypeIterable();
	private static BlockRegistryEvent implementation;

	@Override
	public String channelName() {
		return "modkit.block.register";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static BlockRegistryEvent getInstance() {
		return implementation;
	}

	public void registerBlocks(BlockRegistryEventObject cyanBlocks) {
		for (BlockInfo<?> block : cyanBlocks.getBlocks()) {
			Block type = Registry.register(Registry.BLOCK, new ResourceLocation(block.namespace, block.id),
					block.constructor.apply(block.props));

			blocks.add(type, block.constructor);
			if (block.callback != null)
				block.callback.run(type);
		}
	}

	public BlockTypeEntry findEntity(Block type) {
		for (BlockTypeEntry block : blocks) {
			if (type == block.type)
				return block;
		}

		return null;
	}

}
