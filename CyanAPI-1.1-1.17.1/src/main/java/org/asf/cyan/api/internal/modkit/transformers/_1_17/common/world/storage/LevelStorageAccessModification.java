package org.asf.cyan.api.internal.modkit.transformers._1_17.common.world.storage;

import java.io.File;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.core.LevelSaveEvent;
import modkit.events.objects.core.LevelDataEventObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.storage.WorldData;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.level.storage.LevelStorageSource$LevelStorageAccess")
public class LevelStorageAccessModification {

	@InjectAt(location = InjectLocation.HEAD, targetCall = "createTempFile(java.lang.String,java.lang.String,java.io.File)", targetOwner = "java.io.File")
	public void saveDataTag(@TargetType(target = "net.minecraft.core.RegistryAccess") RegistryAccess var1,
			@TargetType(target = "net.minecraft.world.level.storage.WorldData") WorldData var2,
			@TargetType(target = "net.minecraft.nbt.CompoundTag") CompoundTag var3,

			@LocalVariable File var4, @LocalVariable CompoundTag var5) {
		processCyan(var5);
	}

	private void processCyan(CompoundTag var5) {
		if (!var5.contains("ModKitMods"))
			var5.put("ModKitMods", new CompoundTag());

		LevelDataEventObject dataObject = new LevelDataEventObject(var5, var5.getCompound("ModKitMods"));
		LevelSaveEvent.getInstance().dispatch(dataObject).getResult();

		ListTag lst = new ListTag();
		for (Modloader loader : Modloader.getAllModloaders()) {
			CompoundTag modloaderData = new CompoundTag();
			modloaderData.putString("version", loader.getVersion().toString());

			CompoundTag mods = new CompoundTag();
			for (IModManifest mod : loader.getLoadedMods()) {
				mods.putString(mod.id(), mod.version().toString());
			}

			modloaderData.put("mods", mods);

			CompoundTag coremods = new CompoundTag();
			for (IModManifest mod : loader.getLoadedCoremods()) {
				coremods.putString(mod.id(), mod.version().toString());
			}

			modloaderData.put("coremods", coremods);
			var5.put(loader.getName(), modloaderData);
			lst.add(StringTag.valueOf(loader.getName()));
		}
		var5.put("ModKitLoaders", lst);
	}
}
