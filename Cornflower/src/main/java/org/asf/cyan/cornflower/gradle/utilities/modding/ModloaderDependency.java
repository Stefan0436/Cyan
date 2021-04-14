package org.asf.cyan.cornflower.gradle.utilities.modding;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.AbstractDependency;

public class ModloaderDependency extends AbstractDependency {

	private String name;
	private String version;

	public ModloaderDependency(String name, String version) {
		this.name = name;
		this.version = version;
	}
	
	public ModloaderDependency() {
	}

	public static ModloaderDependency forModloader(String name) {
		return new ModloaderDependency(name, "latest");
	}

	public static ModloaderDependency forModloader(String name, int api) {
		return new ModloaderDependency(name + "/" + api, "latest");
	}

	public static ModloaderDependency forModloader(String name, String version) {
		return new ModloaderDependency(name, version);
	}

	public static ModloaderDependency forModloader(String name, String version, int api) {
		return new ModloaderDependency(name + "/" + api, version);
	}

	@Override
	public String getGroup() {
		return "cornflower.internal.modloader";
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public boolean contentEquals(Dependency dependency) {
		return dependency.getName().equals(name) && dependency.getGroup().equals(getGroup())
				&& dependency.getVersion().equals(getVersion());
	}

	@Override
	public Dependency copy() {
		return new ModloaderDependency(name, version);
	}

}
