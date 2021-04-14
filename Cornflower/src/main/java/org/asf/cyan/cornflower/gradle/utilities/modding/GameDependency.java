package org.asf.cyan.cornflower.gradle.utilities.modding;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.AbstractDependency;

public class GameDependency extends AbstractDependency {

	private String name;
	private String version;

	public GameDependency(String name, String version) {
		this.name = name;
		this.version = version;
	}
	
	public GameDependency() {
	}

	public static GameDependency forGame(String name, String version) {
		return new GameDependency(name, version);
	}

	@Override
	public String getGroup() {
		return "cornflower.internal.game";
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
		return new GameDependency(name, version);
	}

}
