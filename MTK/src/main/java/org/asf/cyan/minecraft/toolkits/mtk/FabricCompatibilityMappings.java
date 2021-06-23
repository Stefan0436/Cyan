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
 * Remaps the Intermediary mappings to allow Fabric to run Cyan, credits to
 * FabricMC
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FabricCompatibilityMappings extends CompatibilityMappings {
	public FabricCompatibilityMappings(Mapping<?> mappings, GameSide side) {
		this(mappings, CyanInfo.getModloaderVersion(),
				MinecraftVersionToolkit.createOrGetVersion(CyanInfo.getMinecraftVersion(), MinecraftVersionType.UNKNOWN,
						null, CyanInfo.getReleaseDate()),
				side, true);
	}

	public FabricCompatibilityMappings(Mapping<?> mappings, String modloader, MinecraftVersionInfo info, GameSide side,
			boolean msg) {
		String mappingsId = (modloader.isEmpty() ? "" : "-" + modloader);
		try {
			MinecraftToolkit.infoLog("Loading fabric support... Preparing INTERMEDIARY mappings for compatibility...");
			if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "intermediary", info, side)) {

				if (msg)
					MinecraftToolkit.infoLog("First time loading with fabric support for version " + modloader
							+ ", downloading INTERMEDIARY mappings...");

				MinecraftMappingsToolkit.downloadIntermediaryMappings(info, side);
				MinecraftMappingsToolkit.saveMappingsToDisk(mappingsId, "intermediary", info, side);
			}

			Mapping<?> intermediaryMappings = MinecraftMappingsToolkit.loadMappings(mappingsId, "intermediary", info,
					side);
			combine("INTERMEDIARY", mappings, intermediaryMappings);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
