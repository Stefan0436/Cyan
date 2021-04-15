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
 * Remaps the Spigot mappings to allow Paper to run Cyan, credits to PaperMC and
 * SpigotMC
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class PaperCompatibilityMappings extends CompatibilityMappings {
	public PaperCompatibilityMappings(Mapping<?> mappings, String mappingsVersion) {
		this(mappings, CyanInfo.getModloaderVersion(),
				MinecraftVersionToolkit.createOrGetVersion(CyanInfo.getMinecraftVersion(), MinecraftVersionType.UNKNOWN,
						null, CyanInfo.getReleaseDate()),
				true, mappingsVersion);
	}

	public PaperCompatibilityMappings(Mapping<?> mappings, String modloader, MinecraftVersionInfo info, boolean msg,
			String mappingsVersion) {
		try {
			MinecraftToolkit.infoLog("Loading paper support... Preparing SPIGOT mappings for compatibility...");
			if (!MinecraftMappingsToolkit.areMappingsAvailable((modloader.isEmpty() ? "" : "-" + modloader), "spigot",
					info, GameSide.SERVER)) {

				if (msg)
					MinecraftToolkit.infoLog("First time loading with paper support for version " + modloader
							+ ", downloading SPIGOT mappings...");

				MinecraftMappingsToolkit.downloadSpigotMappings(mappings, info, mappingsVersion);
				MinecraftMappingsToolkit.saveMappingsToDisk((modloader.isEmpty() ? "" : "-" + modloader), "spigot",
						info, GameSide.SERVER);
			}

			Mapping<?> spigotMappings = MinecraftMappingsToolkit
					.loadMappings((modloader.isEmpty() ? "" : "-" + modloader), "spigot", info, GameSide.SERVER);
			combine("SPIGOT", mappings, spigotMappings, true, false);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
