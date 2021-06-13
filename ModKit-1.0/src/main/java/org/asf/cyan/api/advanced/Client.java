package org.asf.cyan.api.advanced;

import java.util.Map;
import java.util.UUID;

import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.versioning.Version;
import net.minecraft.server.level.ServerPlayer;

/**
 * 
 * Client information class -- provides client information
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class Client {
	protected static Client provider;

	/**
	 * Retrieves the player name
	 * 
	 * @return Player name or null
	 */
	public abstract String getName();

	/**
	 * Retrieves the game type player instance
	 * 
	 * @return Player instance or null
	 */
	public ServerPlayer toGameType() {
		return (ServerPlayer) toGameTypeImpl();
	}

	protected abstract Object toGameTypeImpl();

	/**
	 * Retrieves the player UUID
	 * 
	 * @return Player uuid or null
	 */
	public abstract UUID getUUID();

	/**
	 * Retrieves the client brand
	 * 
	 * @return Client brand
	 */
	public abstract String getBrand();

	/**
	 * Retrieves the client ModKit protocol version
	 * 
	 * @return Protocol version or -1
	 */
	public abstract double getProtocol();

	/**
	 * Retrieves the game version
	 * 
	 * @return Version or null if not present
	 */
	public abstract Version getGameVersion();

	/**
	 * Retrieves the modloader version
	 * 
	 * @return Version or null if not present
	 */
	public abstract Version getModloaderVersion();

	/**
	 * Retrieves the client midloader protocol version
	 * 
	 * @return Protocol version or -1
	 */
	public abstract double getModloaderProtocol();

	/**
	 * Retrieves the client mods
	 * 
	 * @return Map of client mods
	 */
	public abstract Map<String, String> getMods();

	/**
	 * Retrieves the client information for the given player
	 * 
	 * @param uuid Player uuid
	 * @return ClientInformation instance or null
	 */
	public static Client getForUUID(UUID uuid) {
		return provider.getFor(uuid);
	}

	/**
	 * Retrieves the information for a given player uuid
	 * 
	 * @param uuid Player UUID
	 * @return ClientInformation instance or null
	 */
	protected abstract Client getFor(UUID uuid);

	/**
	 * Retrieves the information for a given player
	 * 
	 * @param player Player instance
	 * @return ClientInformation instance or null
	 */
	public static Client getForPlayer(ServerPlayer player) {
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
	public static Client getForConnection(ServerConnectionEventObject event) {
		if (event.getPlayer() == null)
			return getForUUID(null);
		return getForPlayer(event.getPlayer());
	}

}
