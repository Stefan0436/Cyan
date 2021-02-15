package org.asf.cyan.mixin;

import java.util.Collection;

import org.asf.cyan.core.CyanCore;
import org.spongepowered.asm.launch.platform.IMixinPlatformServiceAgent;
import org.spongepowered.asm.launch.platform.MixinPlatformAgentAbstract;
import org.spongepowered.asm.launch.platform.MixinPlatformManager;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;

public class CyanMixinPlatformServiceAgent extends MixinPlatformAgentAbstract implements IMixinPlatformServiceAgent {

	@Override
	public AcceptResult accept(MixinPlatformManager manager, IContainerHandle handle) {
		// TODO Auto-generated method stub
		return AcceptResult.REJECTED;
	}
	
	@Override
	public void init() {
		// TODO Auto-generated method stub

	}

	@Override
	public String getSideName() {
		return CyanCore.getSide().toString();
	}

	@Override
	public Collection<IContainerHandle> getMixinContainers() {
		// TODO Auto-generated method stub
		return null;
	}
}
