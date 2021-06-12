package org.asf.cyan.api.internal.modkit.transformers._1_16.common.world.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;

public class ModloaderMeta {
	public String version;
	public HashMap<String, String> mods = new HashMap<String, String>();
	public HashMap<String, String> coremods = new HashMap<String, String>();

	public static void loadAll(HashMap<String, ModloaderMeta> cyanModloaders, File levelDat) {
		if (!levelDat.exists())
			return;
		try {
			CompoundTag root = NbtIo.readCompressed(levelDat).getCompound("Data");
			if (root.contains("ModKitLoaders")) {
				ListTag lst = (ListTag) root.get("ModKitLoaders");
				for (int i = 0; i < lst.size(); i++) {
					String loader = lst.getString(i);
					CompoundTag loaderData = root.getCompound(loader);
					ModloaderMeta meta = new ModloaderMeta();
					meta.version = loaderData.getString("version");

					CompoundTag mods = loaderData.getCompound("mods");
					for (String key : mods.getAllKeys()) {
						meta.mods.put(key, mods.getString(key));
					}

					CompoundTag coremods = loaderData.getCompound("coremods");
					for (String key : coremods.getAllKeys()) {
						meta.coremods.put(key, coremods.getString(key));
					}

					cyanModloaders.put(loader, meta);
				}
			}
		} catch (IOException e) {
		}
	}

}
