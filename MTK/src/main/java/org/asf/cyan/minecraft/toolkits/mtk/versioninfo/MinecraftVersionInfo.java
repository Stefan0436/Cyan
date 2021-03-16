package org.asf.cyan.minecraft.toolkits.mtk.versioninfo;

import java.net.URL;
import java.time.OffsetDateTime;

/**
 * 
 * Minecraft version information class
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class MinecraftVersionInfo {

	/**
	 * Initialize a new Minecraft version info object.
	 * 
	 * @param mc_version   The Minecraft version string.
	 * @param version_type The Minecraft version type.
	 * @param manifest_url The Minecraft version manifest url.
	 * @param release_date The Minecraft version release date.
	 */
	public MinecraftVersionInfo(String mc_version, MinecraftVersionType version_type, URL manifest_url,
			OffsetDateTime release_date) {
		version = mc_version;
		type = version_type;
		version_manifest = manifest_url;
		date = release_date;
	}

	String version;
	MinecraftVersionType type;
	URL version_manifest;
	OffsetDateTime date;

	/**
	 * Method to get the version this object represents
	 * 
	 * @return Minecraft version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Get the minecraft version type
	 * @return MinecraftVersionType object representing the version type
	 */
	public MinecraftVersionType getVersionType() {
		return type;
	}

	/**
	 * Get the version manifest url
	 * @return Manifest URL of this version
	 */
	public URL getManifestURL() {
		return version_manifest;
	}

	/**
	 * Get the version release date
	 * @return Version release date
	 */
	public OffsetDateTime getReleaseDate() {
		return date;
	}
	
	@Override
	public String toString() {
		return getVersion();
	}
	
	@Override
	public boolean equals(Object b) {
		return b.toString().equals(toString());
	}
}
