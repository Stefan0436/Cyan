package org.asf.cyan.minecraft.toolkits.mtk.versioninfo;

/**
 * 
 * Minecraft version types
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public enum MinecraftVersionType {
	/**
	 * Minecraft release versions
	 */
	RELEASE,
	
	/**
	 * Minecraft snapshot versions (discouraged)
	 */
	SNAPSHOT,
	
	/**
	 * Minecraft old alpha versions (unsupported)
	 */
	UNSUPPORTED_ALPHA,
	
	/**
	 * Minecraft old beta versions (unsupported)
	 */
	UNSUPPORTED_BETA,
	
	/**
	 * Unknown version type
	 */
	UNKNOWN;
	
	/**
	 * Parse version type
	 * @param type The version type
	 * @return MinecraftVersionType object representing the version type
	 */
	public static MinecraftVersionType parse(String type)
	{
		switch(type.toLowerCase().stripTrailing().stripLeading()) {
			case "release": return RELEASE;
			case "snapshot": return SNAPSHOT;
			case "old alpha": return UNSUPPORTED_ALPHA;
			case "old_alpha": return UNSUPPORTED_ALPHA;
			case "old bete": return UNSUPPORTED_BETA;
			case "old_beta": return UNSUPPORTED_BETA;
			default: return UNKNOWN;
		}
	}
}
