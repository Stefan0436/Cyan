package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared;

import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.cornflower.gradle.utilities.modding.IPlatformConfiguration;
import org.asf.cyan.cornflower.gradle.utilities.modding.PlatformClosureOwner;

public class SpigotPlatform implements IPlatformConfiguration {

	public String version = null;
	public String modloader = null;

	@Override
	public IPlatformConfiguration importClosure(PlatformClosureOwner output) {
		version = output.version;
		return this;
	}

	@Override
	public LaunchPlatform getPlatform() {
		return LaunchPlatform.SPIGOT;
	}

	@Override
	public String getModloaderVersion() {
		return modloader;
	}

	@Override
	public String getMappingsVersion() {
		return version;
	}

}
