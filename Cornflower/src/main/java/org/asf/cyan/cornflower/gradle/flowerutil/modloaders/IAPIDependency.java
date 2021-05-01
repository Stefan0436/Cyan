package org.asf.cyan.cornflower.gradle.flowerutil.modloaders;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;

public interface IAPIDependency {
	public Class<? extends IModloader> modloader();

	public String name();

	public IAPIDependency newInstance(Project proj, String version, IModloader modloader);

	public void addRepositories(RepositoryHandler repositories);

	public void addDependencies(ConfigurationContainer configurations);

	public String getVersion();
}
