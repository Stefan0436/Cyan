package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;

/**
 * 
 * Remaps the Yarn mappings to allow Fabric to run Cyan, credits to FabricMC
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FabricCompatibilityMappings extends CompatibilityMappings {
	public FabricCompatibilityMappings(Mapping<?> mappings, GameSide side, String mappingsVersion) {
		this(mappings, CyanInfo.getModloaderVersion(),
				MinecraftVersionToolkit.createOrGetVersion(CyanInfo.getMinecraftVersion(), MinecraftVersionType.UNKNOWN,
						null, CyanInfo.getReleaseDate()),
				side, true, mappingsVersion);
	}

	public FabricCompatibilityMappings(Mapping<?> mappings, String modloader, MinecraftVersionInfo info, GameSide side,
			boolean msg, String mappingsVersion) {
		try {
			MinecraftToolkit.infoLog("Loading fabric support... Preparing YARN mappings for compatibility...");
			if (!MinecraftMappingsToolkit.areMappingsAvailable((modloader.isEmpty() ? "" : "-" + modloader), "yarn",
					info, side)) {

				if (msg)
					MinecraftToolkit.infoLog("First time loading with fabric support for version " + modloader
							+ ", downloading YARN mappings...");

				MinecraftMappingsToolkit.downloadYarnMappings(info, side, mappingsVersion);
				MinecraftMappingsToolkit.saveMappingsToDisk((modloader.isEmpty() ? "" : "-" + modloader), "yarn", info,
						side);
			}

			Mapping<?> yarnMappings = MinecraftMappingsToolkit
					.loadMappings((modloader.isEmpty() ? "" : "-" + modloader), "yarn", info, side);
			combine("YARN", mappings, yarnMappings);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
