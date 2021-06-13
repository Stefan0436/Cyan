package org.asf.cyan.api.internal.modkit.transformers._1_16.server.levelstorage;

import java.io.File;
import java.util.function.BiFunction;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import com.mojang.datafixers.DataFixer;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.level.storage.LevelStorageSource")
public class LevelStorageSourceModification {
	
	@InjectAt(location = InjectLocation.HEAD)
	private <T> T readLevelData(File var1, BiFunction<File, DataFixer, T> var2) {
		BackupServerUtil.backupIfNeeded(new File(var1, "level.dat"));
		return null;
	}
}
