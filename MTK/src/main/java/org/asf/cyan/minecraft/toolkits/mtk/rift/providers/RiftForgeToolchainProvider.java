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

	public RiftForgeToolchainProvider(MinecraftVersionInfo version,
			GameSide side, String modloaderVersion,
			String mcpVersion) {
		this.version = version;
		this.modloaderVersion = modloaderVersion;
		this.mcpVersion = mcpVersion;
	}

	@Override
	public Mapping<?> getRiftMappings() throws IOException {
		String mappingsId = "-" + mcpVersion.replaceAll("[!?/:\\\\]", "-") + "-" + modloaderVersion;
		if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "mcp", version, GameSide.CLIENT)) {
			MinecraftMappingsToolkit.downloadMCPMappings(version, GameSide.CLIENT, mcpVersion);
			MinecraftMappingsToolkit.saveMappingsToDisk(mappingsId, "mcp", version, GameSide.CLIENT);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, GameSide.CLIENT)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(version, GameSide.CLIENT);
			MinecraftMappingsToolkit.saveMappingsToDisk(version, GameSide.CLIENT);
		}

		MinecraftMappingsToolkit.loadMappings(mappingsId, "mcp", version,  GameSide.CLIENT);
		MinecraftMappingsToolkit.loadMappings(version, GameSide.CLIENT);

		if (side == GameSide.CLIENT)
			verify();

		return MinecraftRifterToolkit.generateCyanForgeRiftTargets(version, modloaderVersion, mcpVersion);
	}

	@Override
	public File getJar() throws IOException {
		File jarDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/jars");
		File jarFile = new File(jarDir, "client-" + version.getVersion() + "-deobf.jar");

		if (!jarFile.exists())
			MinecraftModdingToolkit.deobfuscateJar(version, GameSide.CLIENT);

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

		if (!MinecraftInstallationToolkit.checkInstallation(version, false) && side == GameSide.CLIENT) {
			MinecraftInstallationToolkit.downloadVersionFiles(version, false);
		}

		verified = true;
	}

}
