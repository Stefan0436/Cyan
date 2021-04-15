package org.asf.cyan.cornflower.gradle.utilities.modding;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;

public interface IPlatformConfiguration {
	public IPlatformConfiguration importClosure(PlatformClosureOwner output);
	public LaunchPlatform getPlatform();
	public String getModloaderVersion();
	public String getMappingsVersion(GameSide side);
	public String getDisplayVersion();
}
