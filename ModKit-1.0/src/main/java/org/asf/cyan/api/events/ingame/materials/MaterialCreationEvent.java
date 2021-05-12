package org.asf.cyan.api.events.ingame.materials;

import java.util.Iterator;
import java.util.function.BiFunction;

import org.asf.cyan.api.events.extended.AbstractExtendedEvent;
import org.asf.cyan.api.events.objects.ingame.materials.MaterialCreationEventObject;
import org.asf.cyan.api.events.objects.ingame.materials.MaterialCreationEventObject.MaterialInfo;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Material;

/**
 * 
 * Event called to create custom materials
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class MaterialCreationEvent extends AbstractExtendedEvent<MaterialCreationEventObject> {

	public static class MaterialTypeEntry {
		public MaterialTypeEntry next;
		public Material type;
		public BiFunction<?, Level, ?> constructor;

		@SuppressWarnings("unchecked")
		public BiFunction<Material, Level, Material> getConstructor() {
			return (BiFunction<Material, Level, Material>) constructor;
		}
	}

	private static class TypeIterator implements Iterator<MaterialTypeEntry> {
		public MaterialTypeEntry itm;

		@Override
		public boolean hasNext() {
			return itm != null;
		}

		@Override
		public MaterialTypeEntry next() {
			MaterialTypeEntry itm = this.itm;
			this.itm = this.itm.next;
			return itm;
		}

	}

	private static class TypeIterable implements Iterable<MaterialTypeEntry> {
		public MaterialTypeEntry itm;
		public MaterialTypeEntry current;

		@Override
		public Iterator<MaterialTypeEntry> iterator() {
			TypeIterator iter = new TypeIterator();
			iter.itm = itm;
			return iter;
		}

		public void add(Material type) {
			if (itm == null) {
				MaterialTypeEntry cont = new MaterialTypeEntry();
				cont.type = type;
				current = cont;
				itm = cont;
				return;
			}

			MaterialTypeEntry cont = new MaterialTypeEntry();
			cont.type = type;
			current.next = cont;
			current = current.next;
		}

	}

	private static TypeIterable materials = new TypeIterable();
	private static MaterialCreationEvent implementation;

	@Override
	public String channelName() {
		return "modkit.material.register";
	}

	@Override
	public void afterInstantiation() {
		implementation = this;
	}

	public static MaterialCreationEvent getInstance() {
		return implementation;
	}

	public void registerMaterials(MaterialCreationEventObject cyanMaterials) {
		for (MaterialInfo material : cyanMaterials.getMaterials()) {
			Material outp = material.builder.build();
			materials.add(outp);
			if (material.callback != null)
				material.callback.call(outp);
		}
	}

	public MaterialTypeEntry findMaterial(Material type) {
		for (MaterialTypeEntry material : materials) {
			if (type == material.type)
				return material;
		}

		return null;
	}
}
