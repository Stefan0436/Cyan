package org.asf.cyan.api.events.objects.ingame.materials;

import java.util.Iterator;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.Material.Builder;

/**
 * 
 * Material Registry Event Object -- Create your materials by using this
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class MaterialCreationEventObject extends EventObject {

	private MaterialInfo first = null;

	public static interface MaterialRegistryCallback {
		public void call(Material output);
	}

	public class MaterialInfo {
		public Material.Builder builder;
		public MaterialRegistryCallback callback;
		public MaterialInfo next;

		public MaterialInfo(Builder builder, MaterialRegistryCallback callback) {
			this.builder = builder;
			this.callback = callback;
		}
	}

	/**
	 * Adds custom materials to the game
	 * 
	 * @param builder   Material builder
	 * @param result    Result function (receives the Material instance)
	 */
	public void addMaterial(Material.Builder builder, MaterialRegistryCallback result) {
		MaterialInfo entry = new MaterialInfo(builder, result);
		if (first == null) {
			first = entry;
			return;
		}

		MaterialInfo itm = first;
		while (itm.next != null) {
			itm = itm.next;
		}
		itm.next = entry;
	}

	/**
	 * Adds custom materials to the game
	 * 
	 * @param builder Material builder
	 */
	public void addMaterial(Material.Builder builder) {
		addMaterial(builder, null);
	}

	/**
	 * Retrieves the map of mod materials to be registered
	 */
	public Iterable<MaterialInfo> getMaterials() {
		return new MaterialInfoIterable(first);
	}

	private class MaterialInfoIterable implements Iterable<MaterialInfo> {
		class MaterialInfoIterator implements Iterator<MaterialInfo> {
			private MaterialInfo item = null;

			public MaterialInfoIterator(MaterialInfo item) {
				this.item = item;
			}

			@Override
			public boolean hasNext() {
				return item != null;
			}

			@Override
			public MaterialInfo next() {
				MaterialInfo itm = item;
				item = item.next;
				return itm;
			}

		}

		private MaterialInfo item = null;

		public MaterialInfoIterable(MaterialInfo first) {
			item = first;
		}

		@Override
		public Iterator<MaterialInfo> iterator() {
			return new MaterialInfoIterator(item);
		}
	}

}
