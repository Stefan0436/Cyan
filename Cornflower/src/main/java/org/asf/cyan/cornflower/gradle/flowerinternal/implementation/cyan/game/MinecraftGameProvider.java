package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.CyanModloader;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGame;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IModloader;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;

public class MinecraftGameProvider implements IGame {

	private String version;

	public MinecraftGameProvider() {
	}

	public MinecraftGameProvider(String version) {
		this.version = version;
	}

	@Override
	public String name() {
		return "minecraft";
	}

	@Override
	public IGame newInstance(Project proj, String version) {
		return new MinecraftGameProvider(version);
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

}
