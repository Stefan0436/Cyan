package modkit.util.remotedata;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.versioning.Version;

/**
 * 
 * Remote mod information interface
 * 
 * @author Sky Swimmer - AerialWorks Software Foundation
 * @since Modkit 1.3
 */
public class RemoteMod {

	private String modId;
	private String displayName;
	private Version version;
	private RemoteModType type;

	private Modloader loader;

	public RemoteMod(String id, String displayName, Version version, RemoteModType type, Modloader loader) {
		this.modId = id;
		this.version = version;
		this.type = type;
		this.loader = loader;
		this.displayName = displayName;
	}

	/**
	 * Retrieves the mod display name
	 * 
	 * @return The mod display name
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Retrieves the mod ID
	 * 
	 * @return Usually a string of 'group:modid'
	 */
	public String getModID() {
		return modId;
	}

	/**
	 * Retrieves the mod version
	 * 
	 * @return Version instance representing the mod version
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * Tries to find the mod instance, returns null if not installed on this client.
	 * 
	 * @return Mod instance or null
	 */
	public IBaseMod findInstance() {
		if (loader == null)
			return null;
		if (type == RemoteModType.MOD)
			return loader.getModInstance(loader.getLoadedMod(modId));
		else
			return loader.getCoremodInstance(loader.getLoadedCoremod(modId));
	}

	/**
	 * Tries to find the mod manifest, returns null if not installed on this client.
	 * 
	 * @return IModManifest instance or null
	 */
	public IModManifest findManifest() {
		if (loader == null)
			return null;
		if (type == RemoteModType.MOD)
			return loader.getLoadedMod(modId);
		else
			return loader.getLoadedCoremod(modId);
	}

	/**
	 * Retrieves the mod type
	 * 
	 * @return RemoteModType value
	 */
	public RemoteModType getModType() {
		return type;
	}

}
