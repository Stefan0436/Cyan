package modkit.util.server;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.versioning.Version;

import modkit.events.objects.network.ServerConnectionEventObject;
import modkit.util.remotedata.RemoteMod;
import modkit.util.remotedata.RemoteModloader;
import net.minecraft.server.level.ServerPlayer;

/**
 * 
 * Client information class -- provides client information
 * 
 * @since ModKit 1.0, renamed in ModKit 1.3
 * @author Sky Swimmer - AerialWorks Software Foundation
 *
 */
public abstract class ClientSoftware extends CyanComponent {
	protected static ClientSoftware provider;

	//
	// Game type interaction methods
	//

	/**
	 * Retrieves the player name
	 * 
	 * @return Player name or null
	 */
	public abstract String getName();

	/**
	 * Retrieves the player UUID
	 * 
	 * @return Player uuid or null
	 */
	public abstract UUID getUUID();

	/**
	 * Disconnects the client with a message
	 * 
	 * @param message Disconnect message
	 * @since ModKit 1.3
	 */
	public abstract void disconnect(String message);

	/**
	 * Disconnects the client with a translatable message
	 * 
	 * @param message Translatable disconnect message
	 * @param args    Message arguments
	 * @since ModKit 1.3
	 */
	public abstract void disconnectTranslatable(String message, Object... args);

	/**
	 * Retrieves the game type player instance
	 * 
	 * @return Player instance or null
	 */
	public ServerPlayer toGameType() {
		return (ServerPlayer) toGameTypeImpl();
	}

	protected abstract Object toGameTypeImpl();

	//
	// Instance retrieval methods
	//

	/**
	 * Retrieves the client information for the given player
	 * 
	 * @param uuid Player uuid
	 * @return ClientInformation instance or null
	 */
	public static ClientSoftware getForUUID(UUID uuid) {
		return provider.getFor(uuid);
	}

	/**
	 * Retrieves the information for a given player uuid
	 * 
	 * @param uuid Player UUID
	 * @return ClientInformation instance or null
	 */
	protected abstract ClientSoftware getFor(UUID uuid);

	/**
	 * Retrieves the information for a given player
	 * 
	 * @param player Player instance
	 * @return ClientInformation instance or null
	 */
	public static ClientSoftware getForPlayer(ServerPlayer player) {
		if (player == null)
			return getForUUID(null);
		return getForUUID(player.getUUID());
	}

	/**
	 * Retrieves the information for a given player
	 * 
	 * @param event Connection event
	 * @return ClientInformation instance or null
	 */
	public static ClientSoftware getForConnection(ServerConnectionEventObject event) {
		if (event.getPlayer() == null)
			return getForUUID(null);
		return getForPlayer(event.getPlayer());
	}

	//
	// Deprecated API methods
	//

	/**
	 * Retrieves the client mods
	 * 
	 * @return Map of client mods
	 * @deprecated Use getAllMods(), getAllCoreMods(). getAllRegularMods() instead.
	 */
	@Deprecated
	public Map<String, String> getMods() {
		LinkedHashMap<String, String> mods = new LinkedHashMap<String, String>();
		for (RemoteMod mod : getAllMods()) {
			mods.put(mod.getModID(), mod.getVersion().toString());
		}
		return mods;
	}

	/**
	 * Retrieves the client brand
	 * 
	 * @return Client brand
	 * @deprecated Renamed, use getBrandName() instead.
	 */
	@Deprecated
	public String getBrand() {
		return getBrandName();
	}

	/**
	 * Retrieves the client ModKit protocol version
	 * 
	 * @return Protocol version or -1
	 * @deprecated Misleading method name, use getModKitProtocol() instead.
	 */
	@Deprecated
	public double getProtocol() {
		return getModKitProtocol();
	}

	/**
	 * Retrieves the modloader version
	 * 
	 * @return Version or null if not present
	 * @deprecated Modloader versions should be accessed via the
	 *             {@link RemoteModloader} type.
	 */
	@Deprecated
	public Version getModloaderVersion() {
		if (getRootModloader() == null)
			return null;
		return getRootModloader().getVersion();
	}

	/**
	 * Retrieves the client modloader protocol version
	 * 
	 * @return Protocol version or -1
	 * @deprecated Modloader versions should be accessed via the
	 *             {@link RemoteModloader} type.
	 */
	@Deprecated
	public double getModloaderProtocol() {
		if (getRootModloader() == null)
			return -1;
		return getRootModloader().getModloaderProtocolVersion();
	}

	//
	// Improved API
	//

	/**
	 * Retrieves the server brand name
	 * 
	 * @return Server brand string
	 * @since ModKit 1.3
	 */
	public abstract String getBrandName();

	/**
	 * Retrieves the server ModKit protocol version number
	 * 
	 * @return Server ModKit protocol version
	 * @since ModKit 1.3
	 */
	public abstract double getModKitProtocol();

	/**
	 * Retrieves the server ModKit maximal protocol version number
	 * 
	 * @return Server ModKit maximal protocol version
	 * @since ModKit 1.3
	 */
	public abstract double getMaxModKitProtocol();

	/**
	 * Retrieves the server ModKit minimal protocol version number
	 * 
	 * @return Server ModKit minimal protocol version
	 * @since ModKit 1.3
	 */
	public abstract double getMinModKitProtocol();

	/**
	 * Retrieves an array of {@link RemoteModloader} instances
	 * 
	 * @return Array of {@link RemoteModloader} instances
	 * @since ModKit 1.3
	 */
	public abstract RemoteModloader[] getClientModloaders();

	/**
	 * Retrieves the root {@link RemoteModloader}, returns null if not present
	 * 
	 * @return Root {@link RemoteModloader} instance or null
	 * @since ModKit 1.3
	 */
	public abstract RemoteModloader getRootModloader();

	//
	// New non-abstract methods
	//

	/**
	 * Retrieves a modloader by name
	 * 
	 * @param name Modloader name
	 * @return {@link ServerModloader} instance or null
	 * @since ModKit 1.3
	 */
	public RemoteModloader getModloader(String name) {
		for (RemoteModloader loader : getClientModloaders())
			if (loader.getName().equalsIgnoreCase(name))
				return loader;
		for (RemoteModloader loader : getClientModloaders())
			if (loader.getSimpleName().equalsIgnoreCase(name))
				return loader;
		return null;
	}

	/**
	 * Retrieves a modloader by type name
	 * 
	 * @param type Modloader type name
	 * @return {@link ServerModloader} instance or null
	 * @since ModKit 1.3
	 */
	public RemoteModloader getModloaderByTypeName(String type) {
		for (RemoteModloader loader : getClientModloaders())
			if (loader.getTypeName().equals(type))
				return loader;
		return null;
	}

	/**
	 * Retrieves a server (core)mod instance
	 * 
	 * @param id Mod ID
	 * @return ServerMod instance or null
	 * @since ModKit 1.3
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
	 * @since ModKit 1.3
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
	 * @since ModKit 1.3
	 */
	public boolean isModLoaded(String group, String id) {
		return isModLoaded(group + ":" + id);
	}

	/**
	 * Checks if a the given (core)mod ID is present on the server
	 * 
	 * @param id Mod ID
	 * @return True if loaded on the server, false otherwise
	 * @since ModKit 1.3
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
	 * @since ModKit 1.3
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
	 * @since ModKit 1.3
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
	 * @since ModKit 1.3
	 */
	public boolean isRegularModLoaded(String group, String id) {
		return isRegularModLoaded(group + ":" + id);
	}

	/**
	 * Checks if a the given mod ID is present on the server
	 * 
	 * @param id Mod ID
	 * @return True if loaded on the server, false otherwise
	 * @since ModKit 1.3
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
	 * @since ModKit 1.3
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
	 * @since ModKit 1.3
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
	 * @since ModKit 1.3
	 */
	public boolean isCorerModLoaded(String group, String id) {
		return isCorerModLoaded(group + ":" + id);
	}

	/**
	 * Checks if a the given coremod ID is present on the server
	 * 
	 * @param id Mod ID
	 * @return True if loaded on the server, false otherwise
	 * @since ModKit 1.3
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
	 * @since ModKit 1.3
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
	 * @since ModKit 1.3
	 */
	public RemoteMod[] getAllRegularMods() {
		ArrayList<RemoteMod> mods = new ArrayList<RemoteMod>();
		for (RemoteModloader ld : getClientModloaders())
			for (RemoteMod md : ld.getMods())
				mods.add(md);
		return mods.toArray(t -> new RemoteMod[t]);
	}

	/**
	 * Retrieves an array of all server coremods
	 * 
	 * @return Array of {@link RemoteMod} instances
	 * @since ModKit 1.3
	 */
	public RemoteMod[] getAllCoreMods() {
		ArrayList<RemoteMod> mods = new ArrayList<RemoteMod>();
		for (RemoteModloader ld : getClientModloaders())
			for (RemoteMod md : ld.getCoreMods())
				mods.add(md);
		return mods.toArray(t -> new RemoteMod[t]);
	}

	/**
	 * Retrieves the game version
	 * 
	 * @return Version or null if not present
	 * @since ModKit 1.0
	 */
	public Version getGameVersion() {
		if (getRootModloader() == null)
			return null;
		return getRootModloader().getGameVersion();
	}

}
