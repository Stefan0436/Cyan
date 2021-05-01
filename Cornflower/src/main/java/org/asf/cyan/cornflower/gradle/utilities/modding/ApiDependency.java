package org.asf.cyan.cornflower.gradle.utilities.modding;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.AbstractDependency;

public class ApiDependency extends AbstractDependency {

	private String name;
	private String version;

	public ApiDependency(String name, String version) {
		this.name = name;
		this.version = version;
	}
	
	public ApiDependency() {
	}

	public static ApiDependency forAPI(String name, String version) {
		return new ApiDependency(name, version);
	}

	@Override
	public String getGroup() {
		return "cornflower.internal.api";
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
		return new ApiDependency(name, version);
	}

}
