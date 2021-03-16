package org.asf.cyan.api.modloader.information.modloader;

/**
 * 
 * Cyan Loading Phases
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public enum LoadPhase {
	/**
	 * Cyan is not doing anything game-related and has yet to start
	 */
	NOT_READY(0),
	
	/**
	 * Cyan is loading core modules and itself
	 */
	CORELOAD(1),
	
	/**
	 * Cyan begun loading normal mods
	 */
	PRELOAD(2),
	
	/**
	 * Cyan is initializing mods
	 */
	INIT(3),
	
	/**
	 * Cyan is calling mod post-init
	 */
	POSTINIT(4),
	
	/**
	 * Minecraft is up and running
	 */
	RUNTIME(5);
	
	private int val;
	LoadPhase(int val) {
		this.val = val;
	}
	
	public boolean ge(LoadPhase compareTo) {
		return val >= compareTo.val;
	}
	public boolean le(LoadPhase compareTo) {
		return val <= compareTo.val;
	}
	public boolean gt(LoadPhase compareTo) {
		return val > compareTo.val;
	}
	public boolean lt(LoadPhase compareTo) {
		return val < compareTo.val;
	}
	public boolean eq(LoadPhase compareTo) {
		return val == compareTo.val;
	}
}
