package org.asf.cyan.api.events.ingame.blocks;

import java.util.Iterator;
import java.util.function.BiFunction;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.ingame.blocks.BlockEntityRegistryEventObject;
import org.asf.cyan.api.events.objects.ingame.blocks.BlockEntityRegistryEventObject.EntityInfo;

import com.mojang.datafixers.types.Type;

import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.Level;

/**
 * 
 * Event called to register custom block entities
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class BlockEntityRegistryEvent extends AbstractExtendedEvent<BlockEntityRegistryEventObject> {

	public static class EntityTypeEntry {
		public EntityTypeEntry next;
		public BlockEntityType<?> type;
		public BiFunction<?, Level, ?> constructor;

		@SuppressWarnings("unchecked")
		public BiFunction<BlockEntityType<?>, Level, BlockEntity> getConstructor() {
			return (BiFunction<BlockEntityType<?>, Level, BlockEntity>) constructor;
		}

		public int id;
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

		public void add(BlockEntityType<?> type) {
			if (itm == null) {
				EntityTypeEntry cont = new EntityTypeEntry();
				cont.type = type;
				cont.id = Registry.BLOCK_ENTITY_TYPE.getId(type);
				current = cont;
				itm = cont;
				return;
			}

			EntityTypeEntry cont = new EntityTypeEntry();
			cont.type = type;
			cont.id = Registry.BLOCK_ENTITY_TYPE.getId(type);
			current.next = cont;
			current = current.next;
		}

	}

	private static TypeIterable entities = new TypeIterable();
	private static BlockEntityRegistryEvent implementation;

	@Override
	public String channelName() {
		return "modkit.block.entity.register";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static BlockEntityRegistryEvent getInstance() {
		return implementation;
	}

	public void registerEntities(BlockEntityRegistryEventObject cyanEntities) {
		for (EntityInfo<?> entity : cyanEntities.getEntities()) {
			ResourceLocation loc = new ResourceLocation(entity.namespace, entity.id);
			
			Type<?> fixerType = Util.fetchChoiceType(References.BLOCK_ENTITY, loc.toString());
			BlockEntityType<?> type = Registry.register(Registry.BLOCK_ENTITY_TYPE, loc,
					entity.builder.build(fixerType));

			entities.add(type);
			if (entity.callback != null)
				entity.callback.run(type);
		}
	}

	public EntityTypeEntry findEntity(BlockEntityType<?> type) {
		for (EntityTypeEntry entity : entities) {
			if (type == entity.type)
				return entity;
		}

		return null;
	}
}
