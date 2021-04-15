package org.asf.cyan.cornflower.gradle.flowerutil.modloaders;

import java.io.File;

import org.gradle.api.Project;

public interface IGameExecutionContext {
	public String name();
	
	public IGameExecutionContext newInstance(Project proj, String version);

	public String gameJarDependency();
	
	public default String deobfuscatedJarDependency() {
		return null;
	}

	public String[] libraries();
	
	public File[] flatDirs();

	public String mainClass();

	public String[] jvm();

	public String[] commandline();
}
