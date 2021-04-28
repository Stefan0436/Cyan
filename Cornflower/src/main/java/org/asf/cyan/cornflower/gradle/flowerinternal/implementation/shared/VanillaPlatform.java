package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.cornflower.gradle.utilities.modding.IPlatformConfiguration;
import org.asf.cyan.cornflower.gradle.utilities.modding.IPrimaryPlatform;
import org.asf.cyan.cornflower.gradle.utilities.modding.PlatformClosureOwner;

public class VanillaPlatform implements IPlatformConfiguration, IPrimaryPlatform {

	public String version = null;

	@Override
	public IPlatformConfiguration importClosure(PlatformClosureOwner output) {
		version = output.version;
		return this;
	}

	@Override
	public LaunchPlatform getPlatform() {
		return LaunchPlatform.VANILLA;
	}

	@Override
	public String getModloaderVersion() {
		return null;
	}

	@Override
	public String getMappingsVersion(GameSide side) {
		return version;
	}

	@Override
	public String getDisplayVersion() {
		return version;
	}

	@Override
	public String getCommonMappingsVersion() {
		return version;
	}

}
