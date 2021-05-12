package org.asf.cyan.api.util;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;

class DataFixerContainerCondition implements ContainerConditions {

	@Override
	public boolean applies() {
		return Modloader.getModloaderLaunchPlatform() != LaunchPlatform.MCP;
	}

}
