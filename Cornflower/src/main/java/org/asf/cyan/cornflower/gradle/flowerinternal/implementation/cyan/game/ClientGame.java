package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import java.io.File;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGameExecutionContext;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftModdingToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;
import org.gradle.api.Project;

public class ClientGame implements IGameExecutionContext {

	private Project proj;
	private String version;

	private MinecraftVersionInfo gameVersion;

	public ClientGame() {
	}

	public ClientGame(Project proj, String version) {
		this.proj = proj;
		this.version = version;
	}

	@Override
	public String name() {
		return "Cyan " + version;
	}

	@Override
	public IGameExecutionContext newInstance(Project proj, String version) {
		return new ClientGame(proj, version);
	}

	@Override
	public File gameJar() {
		prepare();
		return MinecraftInstallationToolkit.getVersionJar(gameVersion, GameSide.CLIENT);
	}

	@Override
	public File deobfuscatedJar() {
		prepare();
		try {
			return MinecraftModdingToolkit.deobfuscateJar(gameVersion, GameSide.CLIENT);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public File[] libraries() {
		prepare();
		return MinecraftInstallationToolkit.getLibraries(gameVersion); // TODO: cyan libraries
	}

	@Override
	public String mainClass() {
		return "org.asf.cyan";
	}

	@Override
	public String[] jvm() {
		prepare();
		
		ArrayList<String> args = new ArrayList<String>();
		args.add("-Djdk.attach.allowAttachSelf=true");
		try {
			args.add("-DassetRoot=" + MinecraftInstallationToolkit.getAssetsRoot().getCanonicalPath());
			args.add("-DassetIndex=" + MinecraftInstallationToolkit.getAssetId(gameVersion));

			return MinecraftInstallationToolkit.generateJvmArguments(gameVersion, args);
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

			if (!MinecraftInstallationToolkit.checkVersion(gameVersion))
				MinecraftInstallationToolkit.downloadVersionAndLibraries(gameVersion);

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
}
