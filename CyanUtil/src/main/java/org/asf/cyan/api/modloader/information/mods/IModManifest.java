package org.asf.cyan.api.modloader.information.mods;

import org.asf.cyan.api.versioning.Version;

/**
 * 
 * Mod information manifest - very basic mod information interface.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface IModManifest {
	public String id();
	public String displayName();
	public Version version();
	
	public String[] dependencies();
	public String[] optionalDependencies();
	public String description();
}
