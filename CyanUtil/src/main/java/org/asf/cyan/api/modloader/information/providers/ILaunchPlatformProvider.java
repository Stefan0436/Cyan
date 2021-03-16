package org.asf.cyan.api.modloader.information.providers;

import org.asf.cyan.api.modloader.information.game.LaunchPlatform;

public interface ILaunchPlatformProvider extends IModloaderInfoProvider {
	public LaunchPlatform getPlatform();
}
