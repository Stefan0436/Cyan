package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.asf.cyan.api.config.annotations.Comment;
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
@Comment("MCP Compatibility Mappings, automatically generated by Cyan MTK on first startup.")
@Comment("These mappings were generated by Cyan Forge Support Extension to allow Cyan to run on Forge.")
@Comment("Cyan downloads the official and MCP mappings on first launch and converts them to CCFG so FLUID can read them.")
@Comment("The mappings have not been included in cyan's jar files and may NOT be distributed!")
public class ForgeCompatibilityMappings extends CompatibilityMappings {
	public ForgeCompatibilityMappings(Mapping<?> mappings, GameSide side, String mcp) {
		this(mappings, CyanInfo.getModloaderVersion(),
				MinecraftVersionToolkit.createOrGetVersion(CyanInfo.getMinecraftVersion(), MinecraftVersionType.UNKNOWN,
						null, CyanInfo.getReleaseDate()),
				side, true, mcp);
	}

	public ForgeCompatibilityMappings(Mapping<?> deobf, Mapping<?> mcp, MinecraftVersionInfo gameVersion,
			String loaderVersion) {
		try {
			create(deobf, mcp, gameVersion, loaderVersion);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void create(Mapping<?> deobf, Mapping<?> mcp, MinecraftVersionInfo gameVersion, String loaderVersion)
			throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			NoSuchMethodException, SecurityException, IOException {
		combine("MCP", deobf, mcp);
		applyInconsistencyMappings(gameVersion, "forge", loaderVersion);
	}

	public ForgeCompatibilityMappings(Mapping<?> mappings, String modloader, MinecraftVersionInfo info, GameSide side,
			boolean msg, String mcp) {
		setBuild(2);
		
		try {
			String mappingsId = "-" + mcp.replaceAll("[!?/:\\\\]", "-") + (modloader.isEmpty() ? "" : "-" + modloader);
			if (loadWhenPossible("forge", mappingsId, info, side))
				return;

			MinecraftToolkit.infoLog("Loading forge support... Preparing MCP mappings for compatibility...");
			if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "mcp", info, side)) {

				if (msg)
					MinecraftToolkit.infoLog("First time loading with forge support for version " + modloader
							+ ", downloading MCP mappings...");

				MinecraftMappingsToolkit.downloadMCPMappings(info, side, mcp);
				MinecraftMappingsToolkit.saveMappingsToDisk(mappingsId, "mcp", info, side);
			}

			Mapping<?> MCPMappings = MinecraftMappingsToolkit.loadMappings(mappingsId, "mcp", info, side);
			create(mappings, MCPMappings, info, modloader);
			saveToDisk("forge", mappingsId, info, side);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}
}
