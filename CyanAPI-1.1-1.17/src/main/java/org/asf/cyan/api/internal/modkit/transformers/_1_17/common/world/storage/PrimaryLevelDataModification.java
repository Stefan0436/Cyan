package org.asf.cyan.api.internal.modkit.transformers._1_17.common.world.storage;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.fluid.api.transforming.util.CodeControl;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;

import modkit.events.core.LevelLoadEvent;
import modkit.events.objects.core.LevelDataEventObject;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.levelgen.WorldGenSettings;
import net.minecraft.world.level.storage.LevelVersion;
import net.minecraft.world.level.storage.PrimaryLevelData;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.level.storage.PrimaryLevelData")
public class PrimaryLevelDataModification implements PrimaryLevelDataExtension {

	private CompoundTag modkitModData;

	@InjectAt(location = InjectLocation.TAIL)
	@TargetType(target = "net.minecraft.world.level.storage.PrimaryLevelData")
	public static PrimaryLevelData parse(@TargetType(target = "com.mojang.serialization.Dynamic") Dynamic<Tag> var0,
			@TargetType(target = "com.mojang.datafixers.DataFixer") DataFixer var1, int var2,
			@TargetType(target = "net.minecraft.nbt.CompoundTag") CompoundTag var3,
			@TargetType(target = "net.minecraft.world.level.LevelSettings") LevelSettings var4,
			@TargetType(target = "net.minecraft.world.level.storage.LevelVersion") LevelVersion var5,
			@TargetType(target = "net.minecraft.world.level.levelgen.WorldGenSettings") WorldGenSettings var6,
			@TargetType(target = "com.mojang.serialization.Lifecycle") Lifecycle var7) {

		PrimaryLevelData data = CodeControl.ASTORE();
		cyanSetupData(data, var0);
		return data;
	}

	private static void cyanSetupData(PrimaryLevelData data, Dynamic<Tag> var0) {
		((PrimaryLevelDataExtension) data)
				.setCyanModData((CompoundTag) var0.getElement("ModKitMods", new CompoundTag()));
		LevelDataEventObject levelDataObject = new LevelDataEventObject((CompoundTag) var0.getValue(),
				(CompoundTag) var0.getElement("ModKitMods", new CompoundTag()));
		LevelLoadEvent.getInstance().dispatch(levelDataObject).getResult();
	}

	@InjectAt(location = InjectLocation.TAIL)
	public void setTagData(@TargetType(target = "net.minecraft.core.RegistryAccess") RegistryAccess var1,
			@TargetType(target = "net.minecraft.nbt.CompoundTag") CompoundTag var2,
			@TargetType(target = "net.minecraft.nbt.CompoundTag") CompoundTag var3) {
		if (modkitModData != null)
			var2.put("ModKitMods", modkitModData);
	}

	@Override
	public void setCyanModData(CompoundTag data) {
		modkitModData = data;
	}

}
