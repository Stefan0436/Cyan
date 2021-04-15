package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import java.time.OffsetDateTime;

import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.CyanModloader;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGame;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGameExecutionContext;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IModloader;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;

public class MinecraftGameProvider implements IGame {

	private String version;
	public MinecraftVersionInfo gameVersion;

	private CyanModloader modloader;

	public MinecraftGameProvider() {
	}

	public MinecraftGameProvider(Project project, String version, CyanModloader modloader) {
		this.version = version;
		this.modloader = modloader;

		gameVersion = MinecraftVersionToolkit.getVersion(version);
		if (gameVersion == null)
			gameVersion = new MinecraftVersionInfo(version, MinecraftVersionType.UNKNOWN, null, OffsetDateTime.now());
	}

	@Override
	public String name() {
		return "minecraft";
	}

	@Override
	public IGame newInstance(Project proj, String version, IModloader modloader) {
		return new MinecraftGameProvider(proj, version, (CyanModloader) modloader);
	}

	@Override
	public Class<? extends IModloader> modloader() {
		return CyanModloader.class;
	}

	@Override
	public void addRepositories(RepositoryHandler repositories) {

	}

	@Override
	public void addDependencies(ConfigurationContainer configurations) {

	}

	@Override
	public IGameExecutionContext[] getContexts() {
		return new IGameExecutionContext[] { new ClientGame(modloader), new ServerGame(modloader) };
	}

	@Override
	public String getVersion() {
		return version;
	}

}
