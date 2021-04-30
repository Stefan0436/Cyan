package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.stream.Stream;

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

public class ClientGame implements IGameExecutionContext, ILaunchProvider {

	private String version;
	private MinecraftVersionInfo gameVersion;

	private CyanModloader modloader;
	private MinecraftVersionInfo cyanVersion;

	public ClientGame(CyanModloader modloader) {
		this.modloader = modloader;
	}

	public ClientGame(Project proj, String version, CyanModloader modloader) {
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

	@Override
	public String name() {
		return "Cyan " + version + " Client";
	}

	@Override
	public IGameExecutionContext newInstance(Project proj, String version) {
		return new ClientGame(proj, version, modloader);
	}

	@Override
	public String gameJarDependency() {
		prepare();
		return ":client:" + gameVersion;
	}

	@Override
	public String deobfuscatedJarDependency() {
		prepare();
		try {
			MinecraftModdingToolkit.deobfuscateJar(gameVersion, GameSide.CLIENT);
			return ":client:" + gameVersion + "-deobf";
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
		return "org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game.CornflowerLaunchWrapper";
	}

	@Override
	public String[] jvm() {
		prepare();

		ArrayList<String> args = new ArrayList<String>();
		try {
			args.add("-Djdk.attach.allowAttachSelf=true");
			args.add("-Dcyan.deobfuscated=true");
			args.add("-Dcyan.side=CLIENT");
			args.add("-DassetRoot=" + MinecraftInstallationToolkit.getAssetsRoot().getCanonicalPath());
			args.add("-DassetIndex=" + MinecraftInstallationToolkit.getAssetId(gameVersion));

			MinecraftInstallationToolkit.setIDE();
			return MinecraftInstallationToolkit.generateJvmArguments(gameVersion, args,
					MinecraftInstallationToolkit.getLibraries(cyanVersion),
					MinecraftInstallationToolkit.getNativesDirectory(gameVersion), mainJar(),
					MinecraftInstallationToolkit.getAssetsRoot(), MinecraftInstallationToolkit.getAssetId(gameVersion),
					null, false);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String[] commandline() {
		return new String[0];
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

			if (!MinecraftInstallationToolkit.checkVersion(cyanVersion))
				MinecraftInstallationToolkit.downloadVersionAndLibraries(cyanVersion);

			if (MinecraftInstallationToolkit.getVersionJar(gameVersion, GameSide.CLIENT) == null)
				MinecraftInstallationToolkit.downloadVersionJar(gameVersion, GameSide.CLIENT);

			if (!MinecraftMappingsToolkit.areMappingsAvailable(gameVersion, GameSide.CLIENT)) {
				MinecraftMappingsToolkit.downloadVanillaMappings(gameVersion, GameSide.CLIENT);
				MinecraftMappingsToolkit.saveMappingsToDisk(gameVersion, GameSide.CLIENT);
			}

			MinecraftMappingsToolkit.loadMappings(gameVersion, GameSide.CLIENT);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public File[] flatDirs() {
		File[] dirs = Stream.of(MinecraftInstallationToolkit.getLibraries(cyanVersion)).map(t -> t.getParentFile())
				.toArray(t -> new File[t]);
		File[] allDirs = new File[dirs.length + 1];
		int i = 0;
		for (File dir : dirs) {
			allDirs[i++] = dir;
		}
		allDirs[i] = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/jars");
		return allDirs;
	}

	@Override
	public String[] runtimeLibraries() {
		prepare();
		return MinecraftInstallationToolkit.getLibrariesMavenFormat(cyanVersion);
	}

	@Override
	public File[] libraryJars() {
		prepare();
		return MinecraftInstallationToolkit.getLibraries(cyanVersion);
	}

	@Override
	public File mainJar() {
		prepare();
		try {
			return MinecraftModdingToolkit.deobfuscateJar(gameVersion, GameSide.CLIENT);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public String launchName() {
		return "createClientLaunch";
	}
}
