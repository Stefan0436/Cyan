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

public class RiftFabricToolchainProvider implements IRiftToolchainProvider {

	private MinecraftVersionInfo version;
	private GameSide side;
	private String modloaderVersion;

	public RiftFabricToolchainProvider(MinecraftVersionInfo version, GameSide side, String modloaderVersion) {
		this.version = version;
		this.side = side;
		this.modloaderVersion = modloaderVersion;
	}

	@Override
	public Mapping<?> getRiftMappings() throws IOException {
		if (!MinecraftMappingsToolkit.areMappingsAvailable("-" + modloaderVersion, "yarn", version, side)) {
			MinecraftMappingsToolkit.downloadYarnMappings(version, side);
			MinecraftMappingsToolkit.saveMappingsToDisk("-" + modloaderVersion, "yarn", version, side);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, side)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(version, side);
			MinecraftMappingsToolkit.saveMappingsToDisk(version, side);
		}

		MinecraftMappingsToolkit.loadMappings("-" + modloaderVersion, "yarn", version, side);
		MinecraftMappingsToolkit.loadMappings(version, side);

		if (side == GameSide.CLIENT)
			verify();

		return MinecraftRifterToolkit.generateCyanFabricRiftTargets(version, side, modloaderVersion);
	}

	@Override
	public File getJar() throws IOException {
		File jarDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/jars");
		File jarFile = new File(jarDir, version.getVersion() + "-" + side.toString().toLowerCase() + "-deobf.jar");

		if (!jarFile.exists())
			MinecraftModdingToolkit.deobfuscateJar(version, side);

		if (side == GameSide.CLIENT)
			verify();

		return jarFile;
	}

	@Override
	public File[] getLibraries() throws IOException {
		if (side == GameSide.CLIENT) {
			verify();
			return MinecraftInstallationToolkit.getLibraries(version);
		}

		return new File[0];
	}

	private boolean verified = false;

	private void verify() throws IOException {
		if (verified)
			return;

		if (!MinecraftInstallationToolkit.checkVersion(version)) {
			MinecraftInstallationToolkit.downloadVersionAndLibraries(version);
		}

		verified = true;
	}

}
