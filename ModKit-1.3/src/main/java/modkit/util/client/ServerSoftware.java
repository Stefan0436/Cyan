package modkit.util.client;

import java.util.ArrayList;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.versioning.Version;

import modkit.util.remotedata.RemoteMod;
import modkit.util.remotedata.RemoteModloader;

/**
 * 
 * Utility that describes the software running on a remote server
 * 
 * @since ModKit 1.3
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class ServerSoftware extends CyanComponent {

	protected static ServerSoftware implementation;

	/**
	 * Instantiates a new ServerSoftware container for the current session.
	 * 
	 * @return New {@link ServerSoftware} instance.
	 */
	protected abstract ServerSoftware describeCurrent();

	/**
	 * Retrieves the current ServerSoftware instance
	 * 
	 * @return ServerSoftware instance
	 */
	public static ServerSoftware getInstance() {
		return implementation.describeCurrent();
	}

	/**
	 * Retrieves the server brand name
	 * 
	 * @return Server brand string
	 */
	public abstract String getBrandName();

	/**
	 * Retrieves the game version
	 * 
	 * @return Version or null if not present
	 */
	public abstract Version getGameVersion();

	/**
	 * Retrieves the server ModKit protocol version number
	 * 
	 * @return Server ModKit protocol version
	 */
	public abstract double getModKitProtocol();

	/**
	 * Retrieves the server ModKit maximal protocol version number
	 * 
	 * @return Server ModKit maximal protocol version
	 */
	public abstract double getMaxModKitProtocol();

	/**
	 * Retrieves the server ModKit minimal protocol version number
	 * 
	 * @return Server ModKit minimal protocol version
	 */
	public abstract double getMinModKitProtocol();

	/**
	 * Retrieves the root {@link RemoteModloader}, returns null if not present
	 * 
	 * @return Root {@link RemoteModloader} instance or null
	 */
	public abstract RemoteModloader getRootModloader();

	/**
	 * Retrieves an array of {@link RemoteModloader} instances
	 * 
	 * @return Array of {@link RemoteModloader} instances
	 */
	public abstract RemoteModloader[] getServerModloaders();

	/**
	 * Retrieves a modloader by name
	 * 
	 * @param name Modloader name
	 * @return {@link RemoteModloader} instance or null
	 */
	public RemoteModloader getModloader(String name) {
		for (RemoteModloader loader : getServerModloaders())
			if (loader.getName().equalsIgnoreCase(name))
				return loader;
		for (RemoteModloader loader : getServerModloaders())
			if (loader.getSimpleName().equalsIgnoreCase(name))
				return loader;
		return null;
	}

	/**
	 * Retrieves a modloader by type name
	 * 
	 * @param type Modloader type name
	 * @return {@link RemoteModloader} instance or null
	 */
	public RemoteModloader getModloaderByTypeName(String type) {
		for (RemoteModloader loader : getServerModloaders())
			if (loader.getTypeName().equals(type))
				return loader;
		return null;
	}

	/**
	 * Retrieves a server (core)mod instance
	 * 
	 * @param id Mod ID
	 * @return ServerMod instance or null
	 */
	public RemoteMod getMod(String id) {
		for (RemoteMod md : getAllMods())
			if (md.getModID().equalsIgnoreCase(id))
				return md;
		return null;
	}

	/**
	 * Retrieves a server (core)mod instance
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return ServerMod instance or null
	 */
	public RemoteMod getMod(String group, String id) {
		return getMod(group + ":" + id);
	}

	/**
	 * Checks if a the given (core)mod ID is present on the server
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return True if loaded on the server, false otherwise
	 */
	public boolean isModLoaded(String group, String id) {
		return isModLoaded(group + ":" + id);
	}

	/**
	 * Checks if a the given (core)mod ID is present on the server
	 * 
	 * @param id Mod ID
	 * @return True if loaded on the server, false otherwise
	 */
	public boolean isModLoaded(String id) {
		for (RemoteMod md : getAllMods())
			if (md.getModID().equalsIgnoreCase(id))
				return true;
		return false;
	}

	/**
	 * Retrieves a server mod instance
	 * 
	 * @param id Mod ID
	 * @return ServerMod instance or null
	 */
	public RemoteMod getRegularMod(String id) {
		for (RemoteMod md : getAllRegularMods())
			if (md.getModID().equalsIgnoreCase(id))
				return md;
		return null;
	}

	/**
	 * Retrieves a server mod instance
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return ServerMod instance or null
	 */
	public RemoteMod getRegularMod(String group, String id) {
		return getRegularMod(group + ":" + id);
	}

	/**
	 * Checks if a the given mod ID is present on the server
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return True if loaded on the server, false otherwise
	 */
	public boolean isRegularModLoaded(String group, String id) {
		return isRegularModLoaded(group + ":" + id);
	}

	/**
	 * Checks if a the given mod ID is present on the server
	 * 
	 * @param id Mod ID
	 * @return True if loaded on the server, false otherwise
	 */
	public boolean isRegularModLoaded(String id) {
		for (RemoteMod md : getAllRegularMods())
			if (md.getModID().equalsIgnoreCase(id))
				return true;
		return false;
	}

	/**
	 * Retrieves a server coremod instance
	 * 
	 * @param id Mod ID
	 * @return ServerMod instance or null
	 */
	public RemoteMod getCoreMod(String id) {
		for (RemoteMod md : getAllCoreMods())
			if (md.getModID().equalsIgnoreCase(id))
				return md;
		return null;
	}

	/**
	 * Retrieves a server coremod instance
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return ServerMod instance or null
	 */
	public RemoteMod getCoreMod(String group, String id) {
		return getCoreMod(group + ":" + id);
	}

	/**
	 * Checks if a the given coremod ID is present on the server
	 * 
	 * @param group Mod group
	 * @param id    Mod ID
	 * @return True if loaded on the server, false otherwise
	 */
	public boolean isCorerModLoaded(String group, String id) {
		return isCorerModLoaded(group + ":" + id);
	}

	/**
	 * Checks if a the given coremod ID is present on the server
	 * 
	 * @param id Mod ID
	 * @return True if loaded on the server, false otherwise
	 */
	public boolean isCorerModLoaded(String id) {
		for (RemoteMod md : getAllCoreMods())
			if (md.getModID().equalsIgnoreCase(id))
				return true;
		return false;
	}

	/**
	 * Retrieves an array of all server mods (all regular and core mods)
	 * 
	 * @return Array of {@link RemoteMod} instances
	 */
	public RemoteMod[] getAllMods() {
		ArrayList<RemoteMod> mods = new ArrayList<RemoteMod>();
		for (RemoteMod md : getAllRegularMods())
			mods.add(md);
		for (RemoteMod md : getAllCoreMods())
			mods.add(md);
		return mods.toArray(t -> new RemoteMod[t]);
	}

	/**
	 * 
	 * Retrieves an array of all server mods
	 * 
	 * @return Array of {@link RemoteMod} instances
	 */
	public RemoteMod[] getAllRegularMods() {
		ArrayList<RemoteMod> mods = new ArrayList<RemoteMod>();
		for (RemoteModloader ld : getServerModloaders())
			for (RemoteMod md : ld.getMods())
				mods.add(md);
		return mods.toArray(t -> new RemoteMod[t]);
	}

	/**
	 * Retrieves an array of all server coremods
	 * 
	 * @return Array of {@link RemoteMod} instances
	 */
	public RemoteMod[] getAllCoreMods() {
		ArrayList<RemoteMod> mods = new ArrayList<RemoteMod>();
		for (RemoteModloader ld : getServerModloaders())
			for (RemoteMod md : ld.getCoreMods())
				mods.add(md);
		return mods.toArray(t -> new RemoteMod[t]);
	}

}
