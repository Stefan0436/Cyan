package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.CyanModloader;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGameExecutionContext;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftModdingToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;
import org.gradle.api.Project;

public class ServerGame implements IGameExecutionContext {

	private String version;
	private MinecraftVersionInfo gameVersion;

	private CyanModloader modloader;

	public ServerGame(CyanModloader modloader) {
		this.modloader = modloader;
	}

	public ServerGame(Project proj, String version, CyanModloader modloader) {
		this.version = version;
		this.modloader = modloader;
	}

	private void prepare() {
		if (gameVersion != null)
			return;

		try {
			gameVersion = MinecraftVersionToolkit.getVersion(version);
			if (gameVersion == null)
				gameVersion = new MinecraftVersionInfo(version, MinecraftVersionType.UNKNOWN, null,
						OffsetDateTime.now());

			if (!MinecraftInstallationToolkit.isVersionManifestSaved(gameVersion))
				MinecraftInstallationToolkit.saveVersionManifest(gameVersion);

			if (MinecraftInstallationToolkit.getVersionJar(gameVersion, GameSide.SERVER) == null)
				MinecraftInstallationToolkit.downloadVersionJar(gameVersion, GameSide.SERVER);

			if (!MinecraftMappingsToolkit.areMappingsAvailable(gameVersion, GameSide.SERVER)) {
				MinecraftMappingsToolkit.downloadVanillaMappings(gameVersion, GameSide.SERVER);
				MinecraftMappingsToolkit.saveMappingsToDisk(gameVersion, GameSide.SERVER);
			}

			MinecraftMappingsToolkit.loadMappings(gameVersion, GameSide.SERVER);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String name() {
		return "Cyan " + version + " Server";
	}

	@Override
	public IGameExecutionContext newInstance(Project proj, String version) {
		return new ServerGame(proj, version, modloader);
	}

	@Override
	public String gameJarDependency() {
		prepare();
		return ":server:" + gameVersion + "-deobf";
	}

	@Override
	public String deobfuscatedJarDependency() {
		prepare();
		try {
			MinecraftModdingToolkit.deobfuscateJar(gameVersion, GameSide.SERVER);
			return ":server:" + gameVersion + "-deobf";
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] libraries() {
		prepare();
		return MinecraftInstallationToolkit.getLibrariesMavenFormat(gameVersion);
	}

	@Override
	public String mainClass() {
		return "org.asf.cyan.CyanIDEWrapper";
	}

	@Override
	public String[] jvm() {
		prepare();
		return new String[] { "-javaagent:" }; // TODO
	}

	@Override
	public String[] commandline() {
		return new String[0];
	}

	@Override
	public File[] flatDirs() {
		return new File[0];
	}

}
