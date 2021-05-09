package org.asf.cyan.api.permissions;

import java.util.UUID;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 
 * Permissions Manager -- Permissions for commands and other features.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class PermissionManager {

	protected static PermissionManager implementation;

	/**
	 * Retrieves the permission manager for the program
	 */
	public static PermissionManager getInstance() {
		return implementation;
	}

	/**
	 * Checks if a player has the given permission node
	 * 
	 * @param user           UUID to check
	 * @param server         Minecraft server
	 * @param permissionNode Permission node
	 * @return True if the node applies, false otherwise
	 */
	public abstract boolean hasPermission(UUID user, MinecraftServer server, String permissionNode);

	/**
	 * Checks if an entity has the given permission node
	 * 
	 * @param user           Entity to check
	 * @param permissionNode Permission node
	 * @return True if the node applies, false otherwise
	 */
	public boolean hasPermission(Entity user, String permissionNode) {
		return hasPermission(user.getUUID(), user.getServer(), permissionNode);
	}

	/**
	 * Checks if a player has the given permission node
	 * 
	 * @param user           Player to check
	 * @param permissionNode Permission node
	 * @return True if the node applies, false otherwise
	 */
	public boolean hasPermission(Player user, String permissionNode) {
		return hasPermission(user.getUUID(), user.getServer(), permissionNode);
	}

	/**
	 * Retrieves the permissions of a player
	 * 
	 * @param user   UUID of player
	 * @param server Minecraft server
	 * @return Array of permissions
	 */
	public abstract Permission[] getPermissions(UUID user, MinecraftServer server);

	/**
	 * Retrieves the permissions of a player
	 * 
	 * @param user Entity to retrieve the permissions of
	 * @return Array of permissions
	 */
	public Permission[] getPermissions(Entity user) {
		return getPermissions(user.getUUID(), user.getServer());
	}

	/**
	 * Retrieves the permissions of a player
	 * 
	 * @param user Player to retrieve the permissions of
	 * @return Array of permissions
	 */
	public Permission[] getPermissions(Player user) {
		return getPermissions(user.getUUID(), user.getServer());
	}

	/**
	 * Parses the permission node, handles control characters and wildcards
	 * 
	 * @param perm Permission node
	 * @param node Node to check for
	 * @return True if the permission node applies, false otherwise
	 */
	protected abstract boolean checkPermissionApplies(Permission perm, String node);

	/**
	 * Adds the given permission provider (do not call more than once, use the
	 * mods.preinit event)
	 * 
	 * @param provider Permission provider to add
	 */
	public abstract void addPermissionProvider(PermissionProvider provider);

	/**
	 * Adds default permissions
	 * 
	 * @param perm Default permission to add
	 */
	public abstract void addDefaultPermission(Permission perm);

}
