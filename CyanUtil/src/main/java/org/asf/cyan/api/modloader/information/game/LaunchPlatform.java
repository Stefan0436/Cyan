package org.asf.cyan.api.modloader.information.game;

/**
 * 
 * Get the platform Cyan is running on, stored by mapping types
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public enum LaunchPlatform {
	/**
	 * Running in the vanilla game (pure Cyan)
	 */
	VANILLA,

	/**
	 * Running in a deobfuscated vanilla environment (Cornflower, Forge 1.16.5+)
	 */
	DEOBFUSCATED,

	/**
	 * Running alongside an MCP-based modloader (such as Forge up to 1.16.5)
	 */
	MCP,

	/**
	 * Running alongside an YARN-based modloader (such as Fabric)
	 * 
	 * @deprecated Incorrectly named, use INTERMEDIARY
	 */
	@Deprecated
	YARN,

	/**
	 * Running alongside an INTERMEDIARY-based modloader (such as Fabric)
	 */
	INTERMEDIARY,

	/**
	 * Running alongside Spigot or Paper
	 */
	SPIGOT,

	/**
	 * Unknown platform
	 */
	UNKNOWN
}
