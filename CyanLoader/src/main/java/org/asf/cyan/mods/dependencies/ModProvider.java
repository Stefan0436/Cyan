package org.asf.cyan.mods.dependencies;

import org.asf.cyan.mods.IMod;

/**
 * 
 * Mod provider -- uses type erasure to work around ClassNoDefErrors with
 * optional dependencies.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 * @param <T> Mod type
 */
public interface ModProvider<T extends IMod> {
	public T get();
}
