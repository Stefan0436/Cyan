package org.asf.cyan.cornflower.gradle.flowerutil.modloaders;

import java.io.File;
import java.util.ArrayList;

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

	public void addTasks(Project project, IGameExecutionContext[] contexts, ArrayList<File> dependencies,
			ArrayList<File> sourceLookup);

	public void saveContexts(ArrayList<IGameExecutionContext> contexts);

	public ArrayList<IGameExecutionContext> getContextsList();
}
