package org.asf.cyan.api.classloading;

import java.util.ArrayList;
import java.util.HashMap;

//import org.spongepowered.asm.service.IClassTracker;

/**
 * 
 * Mixin class tracker for Cyan, allows for finding loaded classes (previously based on Mixin, now not anymore)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CyanClassTracker { //implements IClassTracker {

	static final ArrayList<String> invalidClasses = new ArrayList<String>();
	static final HashMap<String, Class<?>> loadedClasses = new HashMap<String, Class<?>>();
	
	//@Override
	public void registerInvalidClass(String className) {
		synchronized (invalidClasses) {
			invalidClasses.add(className);
        }
	}

	//@Override
	public boolean isClassLoaded(String className) {
		synchronized (loadedClasses) {
            return loadedClasses.containsKey(className);
        }
	}

	//@Override
	public String getClassRestrictions(String className) {
		return ""; // I don't know, new to modloader writing...
	}
	
}
