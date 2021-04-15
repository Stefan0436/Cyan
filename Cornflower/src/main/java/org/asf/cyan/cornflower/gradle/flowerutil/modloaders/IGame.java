package org.asf.cyan.cornflower.gradle.flowerutil.modloaders;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;

public interface IGame {
	public Class<? extends IModloader> modloader();
	
	public String name();
	public IGame newInstance(Project proj, String version, IModloader modloader);
	
	public void addRepositories(RepositoryHandler repositories);
	public void addDependencies(ConfigurationContainer configurations);
	
	public IGameExecutionContext[] getContexts();

	public String getVersion();
}
