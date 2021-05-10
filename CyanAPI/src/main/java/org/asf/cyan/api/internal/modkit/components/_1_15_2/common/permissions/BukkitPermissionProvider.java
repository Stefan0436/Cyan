package org.asf.cyan.api.internal.modkit.components._1_15_2.common.permissions;

import java.util.ArrayList;
import java.util.UUID;

import org.asf.cyan.api.permissions.Permission;
import org.asf.cyan.api.permissions.PermissionProvider;
import org.asf.cyan.api.permissions.Permission.Mode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import net.minecraft.server.MinecraftServer;

public class BukkitPermissionProvider implements PermissionProvider {

	@Override
	public Permission[] getPermissions(UUID target, MinecraftServer server) {
		Player pl = Bukkit.getServer().getPlayer(target);
		if (pl != null) {
			ArrayList<Permission> perms = new ArrayList<Permission>();
			pl.getEffectivePermissions().forEach((perm) -> {
				perms.add(new Permission(perm.getPermission(), perm.getValue() ? Mode.ALLOW : Mode.DISALLOW));
			});
			return perms.toArray(t -> new Permission[t]);
		}
		return new Permission[0];
	}

	@Override
	public int checkPermission(UUID target, String permission, MinecraftServer server) {
		Player pl = Bukkit.getServer().getPlayer(target);
		if (pl != null) {
			if (!pl.isPermissionSet(permission))
				return 2;
			return pl.hasPermission(permission) ? 0 : 1;
		}
		return 2;
	}

}
