package org.asf.cyan.api.cyanloader;

/**
 * 
 * Cyan Loading Phases
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public enum CyanLoadPhase {
	/**
	 * Cyan is not doing anything game-related and has yet to start
	 */
	NOT_READY,
	
	/**
	 * Cyan is loading core modules and itself
	 */
	CORELOAD,
	
	/**
	 * Cyan begun loading normal mods
	 */
	PRELOAD,
	
	/**
	 * Cyan is initializing mods
	 */
	INIT,
	
	/**
	 * Cyan is calling mod post-init
	 */
	POSTINIT,
	
	/**
	 * Minecraft is up and running
	 */
	RUNTIME
}
