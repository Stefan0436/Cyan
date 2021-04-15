package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.cornflower.gradle.utilities.modding.IPlatformConfiguration;
import org.asf.cyan.cornflower.gradle.utilities.modding.PlatformClosureOwner;

public class YarnPlatform implements IPlatformConfiguration {

	public String version = null;
	public String modloader = null;

	@Override
	public IPlatformConfiguration importClosure(PlatformClosureOwner output) {
		version = output.version;
		modloader = output.modloader;
		return this;
	}

	@Override
	public LaunchPlatform getPlatform() {
		return LaunchPlatform.YARN;
	}

	@Override
	public String getModloaderVersion() {
		return modloader;
	}

	@Override
	public String getMappingsVersion(GameSide side) {
		return version;
	}

	@Override
	public String getDisplayVersion() {
		return version + "-" + modloader;
	}

}
