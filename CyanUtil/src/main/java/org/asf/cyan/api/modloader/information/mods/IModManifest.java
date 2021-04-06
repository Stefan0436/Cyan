package org.asf.cyan.api.modloader.information.mods;

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
	
	public String[] dependencies();
	public String description();
}
