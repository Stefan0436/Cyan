package org.asf.cyan.api.internal.modkit.transformers._1_16.common.world.storage;

import java.io.File;
import java.util.HashMap;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.storage.LevelVersion;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.level.storage.LevelSummary")
public class LevelSummaryModification implements LevelModDataReader {

	private HashMap<String, ModloaderMeta> cyanModloaders = new HashMap<String, ModloaderMeta>();

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	public void ctor(@TargetType(target = "net.minecraft.world.level.LevelSettings") LevelSettings settings,
			@TargetType(target = "net.minecraft.world.level.storage.LevelVersion") LevelVersion version, String id,
			boolean requiresConversion, boolean locked, File iconFile) {
		if (cyanModloaders == null)
			cyanModloaders = new HashMap<String, ModloaderMeta>();
		ModloaderMeta.loadAll(cyanModloaders, new File(iconFile.getParentFile(), "level.dat"));
	}

	@Override
	public String[] cyanGetAllLoaders() {
		return cyanModloaders.keySet().toArray(t -> new String[t]);
	}

	@Override
	public ModloaderMeta cyanGetLoader(String loader) {
		return cyanModloaders.get(loader);
	}

}
