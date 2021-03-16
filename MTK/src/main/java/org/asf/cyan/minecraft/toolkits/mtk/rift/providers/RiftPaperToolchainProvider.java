package org.asf.cyan.minecraft.toolkits.mtk.rift.providers;

import java.io.File;
import java.io.IOException;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftModdingToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftRifterToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

public class RiftPaperToolchainProvider implements IRiftToolchainProvider {

	private MinecraftVersionInfo version;
	private String modloaderVersion;

	public RiftPaperToolchainProvider(MinecraftVersionInfo version, String modloaderVersion) {
		this.version = version;
		this.modloaderVersion = modloaderVersion;
	}

	@Override
	public Mapping<?> getRiftMappings() throws IOException {
		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(version, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk(version, GameSide.SERVER);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable("-" + modloaderVersion, "spigot", version, GameSide.SERVER)) {
			MinecraftMappingsToolkit
					.downloadSpigotMappings(MinecraftMappingsToolkit.loadMappings(version, GameSide.SERVER), version);
			MinecraftMappingsToolkit.saveMappingsToDisk("-" + modloaderVersion, "spigot", version, GameSide.SERVER);
		}

		MinecraftMappingsToolkit.loadMappings("-" + modloaderVersion, "spigot", version, GameSide.SERVER);
		MinecraftMappingsToolkit.loadMappings(version, GameSide.SERVER);

		return MinecraftRifterToolkit.generateCyanPaperRiftTargets(version, modloaderVersion);
	}

	@Override
	public File getJar() throws IOException {
		File jarDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/jars");
		File jarFile = new File(jarDir, version.getVersion() + "-server-deobf.jar");

		if (!jarFile.exists())
			MinecraftModdingToolkit.deobfuscateJar(version, GameSide.SERVER);

		return jarFile;
	}

	@Override
	public File[] getLibraries() throws IOException {
		return new File[0];
	}

}
