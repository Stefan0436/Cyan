package org.asf.cyan.mixin.providers;

import java.io.IOException;

import org.asf.cyan.mixin.services.CyanMixinService;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.IClassBytecodeProvider;
import org.spongepowered.asm.service.MixinService;

/**
 * 
 * Fluid-based provider for MIXIN.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class FluidBytecodeProvider implements IClassBytecodeProvider {

	ClassLoader classLoader = null;
	
	@Override
	public ClassNode getClassNode(String name) throws ClassNotFoundException, IOException {
		return getClassNode(name, true);
	}

	@Override
	public ClassNode getClassNode(String type, boolean runTransformers) throws ClassNotFoundException, IOException {
		if (MixinService.getService() instanceof CyanMixinService) {
			return ((CyanMixinService)MixinService.getService()).getClassPool().getClassNode(type);
		}
		return null;
	}

}
