package org.asf.cyan.api.classloading;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 
 * Formerly-mixin class tracker for Cyan, allows for finding loaded classes (previously based on Mixin, now not anymore)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CyanClassTracker { 

	static final ArrayList<String> invalidClasses = new ArrayList<String>();
	static final HashMap<String, Class<?>> loadedClasses = new HashMap<String, Class<?>>();
	
	public void registerInvalidClass(String className) {
		synchronized (invalidClasses) {
			invalidClasses.add(className);
        }
	}

	public boolean isClassLoaded(String className) {
		synchronized (loadedClasses) {
            return loadedClasses.containsKey(className);
        }
	}
}
