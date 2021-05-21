package org.asf.cyan.api.util;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;

class ClientContainerCondition implements ContainerConditions {

	@Override
	public boolean applies() {
		return Modloader.getModloaderGameSide() == GameSide.CLIENT;
	}

}
