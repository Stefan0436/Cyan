package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

public class DynamicCompatibilityMappings extends CompatibilityMappings {

	public DynamicCompatibilityMappings(Mapping<?> deobf, Mapping<?> obfus, MinecraftVersionInfo info,
			String loaderVersion) {
		try {
			String loader = "dynamic";
			if (loaderVersion.contains("-")) {
				loader = loaderVersion.substring(0, loaderVersion.indexOf("-"));
				loaderVersion = loaderVersion.substring(loaderVersion.indexOf("-") + 1);
			}
			combine("DYNAMIC", deobf, obfus);
			applyInconsistencyMappings(info, loader, loaderVersion);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

}
