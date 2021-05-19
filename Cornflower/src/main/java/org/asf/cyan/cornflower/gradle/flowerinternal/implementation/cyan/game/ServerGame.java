package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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

public class ServerGame implements IGameExecutionContext, ILaunchProvider {

	private String version;
	private MinecraftVersionInfo gameVersion;

	private CyanModloader modloader;
	private MinecraftVersionInfo cyanVersion;

	public ServerGame(CyanModloader modloader) {
		this.modloader = modloader;
	}

	public ServerGame(Project proj, String version, CyanModloader modloader) {
		this.version = version;
		this.modloader = modloader;

		try {
			cyanVersion = new MinecraftVersionInfo(version + "-cyan-" + modloader.getVersion(),
					MinecraftVersionType.UNKNOWN,
					new URL(CyanModloader.maven + "/org/asf/cyan/CyanWrapper/" + modloader.libraries.get("CyanWrapper")
							+ "/CyanWrapper-" + modloader.libraries.get("CyanWrapper") + "-" + version + "-cyan-"
							+ modloader.getVersion() + ".json"),
					OffsetDateTime.now());

			if (!MinecraftInstallationToolkit.isVersionManifestSaved(cyanVersion)) {
				MinecraftInstallationToolkit.saveVersionManifest(cyanVersion);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
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
		return null;
	}

	@Override
	public String deobfuscatedJarDependency() {
		prepare();
		MinecraftModdingToolkit.deobfuscateJar(gameVersion, GameSide.SERVER);
		return null;
	}

	@Override
	public String[] runtimeLibraries() {
		prepare();
		return MinecraftInstallationToolkit.getLibrariesMavenFormat(cyanVersion, true);
	}

	@Override
	public String mainClass() {
		return "org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game.CornflowerLaunchWrapper";
	}

	@Override
	public String[] jvm() {
		prepare();

		File jar = null;
		for (File lib : MinecraftInstallationToolkit.getLibraries(cyanVersion)) {
			if (lib.getName().toLowerCase().contains("fluid-")) {
				jar = lib;
				break;
			}
		}

		try {
			return new String[] { "-javaagent:" + jar.getCanonicalPath(), "-Dcyan.side=SERVER", "-Dcyan.deobfuscated=true" };
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] commandline() {
		return new String[0];
	}

	@Override
	public File[] flatDirs() {
		return new File[0];
	}

	@Override
	public String[] libraries() {
		prepare();
		return new String[0];
	}

	@Override
	public File[] libraryJars() {
		prepare();
		return MinecraftInstallationToolkit.getLibraries(cyanVersion, true);
	}

	@Override
	public File mainJar() {
		prepare();
		try {
			MinecraftModdingToolkit.sourcesJar(gameVersion, GameSide.SERVER);
			return MinecraftModdingToolkit.deobfuscateJar(gameVersion, GameSide.SERVER);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public String launchName() {
		return "createServerLaunch";
	}
}
