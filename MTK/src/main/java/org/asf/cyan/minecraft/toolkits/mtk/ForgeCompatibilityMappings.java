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
 * Loads the MCP mappings to allow forge to run Cyan, credits to Forge and the
 * developers of the Minecraft Coder Pack
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ForgeCompatibilityMappings extends CompatibilityMappings {
	public ForgeCompatibilityMappings(Mapping<?> mappings, GameSide side, String mcp) {
		this(mappings, CyanInfo.getModloaderVersion(),
				MinecraftVersionToolkit.createOrGetVersion(CyanInfo.getMinecraftVersion(), MinecraftVersionType.UNKNOWN,
						null, CyanInfo.getReleaseDate()),
				side, true, mcp);
	}

	public ForgeCompatibilityMappings(Mapping<?> mappings, String modloader, MinecraftVersionInfo info, GameSide side,
			boolean msg, String mcp) {
		try {
			String mappingsId = "-" + mcp.replaceAll("[!?/:\\\\]", "-") + (modloader.isEmpty() ? "" : "-" + modloader);
			MinecraftToolkit.infoLog("Loading forge support... Preparing MCP mappings for compatibility...");
			if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "mcp", info, side)) {

				if (msg)
					MinecraftToolkit.infoLog("First time loading with forge support for version " + modloader
							+ ", downloading MCP mappings...");

				MinecraftMappingsToolkit.downloadMCPMappings(info, side, mcp);
				MinecraftMappingsToolkit.saveMappingsToDisk(mappingsId, "mcp", info, side);
			}

			Mapping<?> MCPMappings = MinecraftMappingsToolkit.loadMappings(mappingsId, "mcp", info, side);
			combine("MCP", mappings, MCPMappings);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
