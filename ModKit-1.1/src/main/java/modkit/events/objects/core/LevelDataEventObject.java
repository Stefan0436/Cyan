package modkit.events.objects.core;

import org.asf.cyan.api.events.extended.EventObject;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;

import net.minecraft.nbt.CompoundTag;

/**
 * 
 * World data event object -- contains world data tags for reading and writing
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class LevelDataEventObject extends EventObject {
	private CompoundTag global;
	private CompoundTag modData;

	public LevelDataEventObject(CompoundTag global, CompoundTag modData) {
		this.global = global;
		this.modData = modData;
	}

	/**
	 * Retrieves the root world data tag
	 * 
	 * @return Root (Data) world CompoundTag instance
	 */
	public CompoundTag getGlobalData() {
		return global;
	}

	/**
	 * Retrieves the global mod data tag
	 * 
	 * @return Global mod CompoundTag instance
	 */
	public CompoundTag getGlobalModData() {
		return modData;
	}

	/**
	 * Retrieves the data tag for the given mod instance
	 * 
	 * @param <T> Mod type
	 * @param mod Mod instance
	 * @return Mod CompoundTag instance
	 */
	public <T extends IBaseMod> CompoundTag getModData(T mod) {
		if (mod == null || mod.getManifest() == null)
			return null;

		String modPath = mod.getManifest().id();
		CompoundTag modData = this.modData;
		for (String pth : modPath.split(":")) {
			if (!modData.contains(pth))
				modData.put(pth, new CompoundTag());
			modData = modData.getCompound(pth);
		}
		if (modData == this.modData)
			return null;
		return modData;
	}
}
