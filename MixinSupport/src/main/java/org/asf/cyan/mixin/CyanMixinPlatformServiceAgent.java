package org.asf.cyan.mixin;

import java.util.Arrays;
import java.util.Collection;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.mixin.providers.CyanMixinClassProvider;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;

public class CyanMixinPlatformServiceAgent extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {

	@Override
	public AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
		return AcceptResult.REJECTED;
	}
	
	@Override
	public void init() {
	}

	@Override
	public String getSideName() {
		return Modloader.getModloaderGameSide().toString();
	}

	@Override
	public Collection<IContainerHandle> getMixinContainers() {
		return Arrays.asList(CyanMixinClassProvider.getPrimaryContainer());
	}
}
