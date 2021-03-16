package org.asf.cyan.mixin.providers;

import org.asf.cyan.api.classloading.CyanClassTracker;
import org.spongepowered.asm.service.IClassTracker;

public class CyanMixinClassTracker extends CyanClassTracker implements IClassTracker {

	@Override
	public String getClassRestrictions(String className) {
		return "";
	}

}
