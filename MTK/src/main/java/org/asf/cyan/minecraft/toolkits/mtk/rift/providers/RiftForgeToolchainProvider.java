package org.asf.cyan.minecraft.toolkits.mtk.rift.providers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.fluid.bytecode.sources.FileClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.IClassSourceProvider;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftModdingToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftRifterToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

public class RiftForgeToolchainProvider implements IRiftToolchainProvider {

	private MinecraftVersionInfo version;
	private GameSide side;
	private String modloaderVersion;
	private String mcpVersion;

	public RiftForgeToolchainProvider(MinecraftVersionInfo version, GameSide side, String modloaderVersion,
			String mcpVersion) {
		this.version = version;
		this.side = side;
		this.modloaderVersion = modloaderVersion;
		this.mcpVersion = mcpVersion;
	}

	@Override
	public Mapping<?> getRiftMappings() throws IOException {
		String mappingsId = "-" + mcpVersion.replaceAll("[!?/:\\\\]", "-") + "-" + modloaderVersion;
		if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "mcp", version, side)) {
			MinecraftMappingsToolkit.downloadMCPMappings(version, side, mcpVersion);
			MinecraftMappingsToolkit.saveMappingsToDisk(mappingsId, "mcp", version, side);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, side)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(version, side);
			MinecraftMappingsToolkit.saveMappingsToDisk(version, side);
		}

		MinecraftMappingsToolkit.loadMappings(mappingsId, "mcp", version, side);
		MinecraftMappingsToolkit.loadMappings(version, side);

		if (side == GameSide.CLIENT)
			verify();

		return MinecraftRifterToolkit.generateCyanForgeRiftTargets(version, side, modloaderVersion, mcpVersion);
	}

	@Override
	public File getJar() throws IOException {
		File jarDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/jars");
		File jarFile = new File(jarDir, side.toString().toLowerCase() + "-" + version.getVersion() + "-deobf.jar");

		if (!jarFile.exists())
			MinecraftModdingToolkit.deobfuscateJar(version, side);

		if (side == GameSide.CLIENT)
			verify();

		return jarFile;
	}

	@Override
	public IClassSourceProvider<?>[] getSources() throws IOException {
		if (side == GameSide.CLIENT) {
			verify();
			ArrayList<IClassSourceProvider<?>> sources = new ArrayList<IClassSourceProvider<?>>();
			for (File lib : MinecraftInstallationToolkit.getLibraries(version)) {
				sources.add(new FileClassSourceProvider(lib));
			}
			return sources.toArray(t -> new IClassSourceProvider<?>[t]);
		}

		return new IClassSourceProvider<?>[0];
	}

	private boolean verified = false;

	private void verify() throws IOException {
		if (verified)
			return;

		if (!MinecraftInstallationToolkit.checkIntallation(version, false) && side == GameSide.CLIENT) {
			MinecraftInstallationToolkit.downloadVersionFiles(version, false);
		}

		verified = true;
	}

}
