package org.asf.cyan.minecraft.toolkits.mtk.rift.providers;

import java.io.File;
import java.io.IOException;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.fluid.bytecode.sources.IClassSourceProvider;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftModdingToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftRifterToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

public class RiftPaperToolchainProvider implements IRiftToolchainProvider {

	private MinecraftVersionInfo version;
	private String modloaderVersion;
	private String mappingsVersion;

	public RiftPaperToolchainProvider(MinecraftVersionInfo version, String modloaderVersion, String mappingsVersion) {
		this.version = version;
		this.modloaderVersion = modloaderVersion;
		this.mappingsVersion = mappingsVersion;
	}

	@Override
	public Mapping<?> getRiftMappings() throws IOException {
		String mappingsId = "-" + mappingsVersion.replaceAll("[!?/:\\\\]", "-") + "-" + modloaderVersion;
		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(version, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk(version, GameSide.SERVER);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(mappingsId, "spigot", version, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadSpigotMappings(
					MinecraftMappingsToolkit.loadMappings(version, GameSide.SERVER), version, mappingsVersion);
			MinecraftMappingsToolkit.saveMappingsToDisk(mappingsId, "spigot", version, GameSide.SERVER);
		}

		MinecraftMappingsToolkit.loadMappings(mappingsId, "spigot", version, GameSide.SERVER);
		MinecraftMappingsToolkit.loadMappings(version, GameSide.SERVER);

		return MinecraftRifterToolkit.generateCyanPaperRiftTargets(version, modloaderVersion, mappingsVersion);
	}

	@Override
	public File getJar() throws IOException {
		File jarDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/jars");
		File jarFile = new File(jarDir, "server-" + version.getVersion() + "-deobf.jar");

		if (!jarFile.exists())
			MinecraftModdingToolkit.deobfuscateJar(version, GameSide.SERVER);

		return jarFile;
	}

	@Override
	public IClassSourceProvider<?>[] getSources() throws IOException {
		return new IClassSourceProvider<?>[0];
	}

}
