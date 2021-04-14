package org.asf.cyan.cornflower.gradle.flowerutil.modloaders;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;

public interface IModloader {
	public String name();

	public String fullName();

	public IModloader newInstance(Project project, String version, int apiLevel);

	public void addRepositories(RepositoryHandler repositories);

	public void addDependencies(ConfigurationContainer configurations);
}
