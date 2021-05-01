package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.CyanModloader;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IAPIDependency;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IModloader;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;

public class CyanAPI implements IAPIDependency {

	private Project proj;
	private String version;
	private CyanModloader modloader;

	public CyanAPI() {
	}

	public CyanAPI(Project proj, String version, IModloader modloader) {
		this.proj = proj;
		this.version = version;
		this.modloader = (CyanModloader) modloader;
	}

	@Override
	public Class<? extends IModloader> modloader() {
		return CyanModloader.class;
	}

	@Override
	public String name() {
		return "ModKit";
	}

	@Override
	public IAPIDependency newInstance(Project proj, String version, IModloader modloader) {
		return new CyanAPI(proj, version, modloader);
	}

	@Override
	public void addRepositories(RepositoryHandler repositories) {
	}

	@Override
	public void addDependencies(ConfigurationContainer configurations) {
		proj.getDependencies().add("implementation",
				"org.asf.cyan:CyanAPI:" + modloader.libraries.get("CyanAPI-" + version));
	}

	@Override
	public String getVersion() {
		return version;
	}

}
