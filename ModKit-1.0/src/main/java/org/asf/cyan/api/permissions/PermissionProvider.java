package org.asf.cyan.api.permissions;

import java.util.UUID;

import org.asf.cyan.api.permissions.Permission.Mode;

import net.minecraft.server.MinecraftServer;

/**
 * 
 * Permission provider -- provides keys and handles permissions (interface)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface PermissionProvider {

	/**
	 * Retrieves the permission nodes for a given UUID
	 * 
	 * @param target Target UUID
	 * @param server Minecraft server
	 * @return Array of permission nodes
	 */
	public Permission[] getPermissions(UUID target, MinecraftServer server);

	/**
	 * Checks the permission
	 * 
	 * @param target     Target owner
	 * @param permission Permission to check
	 * @param server     Minecraft server
	 * @return 0 if allowed, 1 if disallowed, 2 if not present
	 */
	public default int checkPermission(UUID target, String permission, MinecraftServer server) {
		int value = 2;
		for (Permission perm : getPermissions(target, server)) {
			if (PermissionManager.getInstance().checkPermissionApplies(perm, permission)) {
				if (perm.getMode() == Mode.ALLOW)
					value = 0;
				else if (perm.getMode() == Mode.DISALLOW)
					return 1;
			}
		}
		return value;
	}

}
