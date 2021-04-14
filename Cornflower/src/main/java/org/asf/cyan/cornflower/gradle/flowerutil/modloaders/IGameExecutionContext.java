package org.asf.cyan.cornflower.gradle.flowerutil.modloaders;

import java.io.File;

import org.gradle.api.Project;

public interface IGameExecutionContext {
	public String name();
	
	public IGameExecutionContext newInstance(Project proj, String version);

	public File gameJar();
	
	public default File deobfuscatedJar() {
		return null;
	}

	public File[] libraries();

	public String mainClass();

	public String[] jvm();

	public String[] commandline();
}
