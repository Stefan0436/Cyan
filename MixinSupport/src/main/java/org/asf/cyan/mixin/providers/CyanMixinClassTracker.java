package org.asf.cyan.mixin.providers;

import org.asf.cyan.api.classloading.CyanClassTracker;
import org.spongepowered.asm.service.IClassTracker;

public class CyanMixinClassTracker implements IClassTracker {

	@Override
	public String getClassRestrictions(String className) {
		return "";
	}

	@Override
	public void registerInvalidClass(String className) {
		CyanClassTracker.registerInvalidClass(className);
	}

	@Override
	public boolean isClassLoaded(String className) {
		return CyanClassTracker.isClassLoaded(className);
	}

}
