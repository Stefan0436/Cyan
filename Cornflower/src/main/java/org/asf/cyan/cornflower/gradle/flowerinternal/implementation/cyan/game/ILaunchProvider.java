package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import java.io.File;

public interface ILaunchProvider {
	public File[] libraryJars();

	public File mainJar();
	
	public String launchName();
}
