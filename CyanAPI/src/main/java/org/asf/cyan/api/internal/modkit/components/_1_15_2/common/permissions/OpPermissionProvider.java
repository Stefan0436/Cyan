package org.asf.cyan.api.internal.modkit.components._1_15_2.common.permissions;

import java.util.ArrayList;
import java.util.UUID;

import org.asf.cyan.api.internal.modkit.transformers._1_15_2.common.PlayerEntryExtension;
import org.asf.cyan.api.permissions.Permission;
import org.asf.cyan.api.permissions.Permission.Mode;
import org.asf.cyan.api.permissions.PermissionProvider;

import com.mojang.authlib.GameProfile;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.ServerOpListEntry;

public class OpPermissionProvider implements PermissionProvider {

	@Override
	public Permission[] getPermissions(UUID target, MinecraftServer server) {
		if (server.isSingleplayer() && server.getProfileCache().get(target) != null
				&& server.isSingleplayerOwner(server.getProfileCache().get(target)))
			return new Permission[] { new Permission("*", Mode.ALLOW) };

		for (ServerOpListEntry op : server.getPlayerList().getOps().getEntries()) {
			String str = ((GameProfile) ((PlayerEntryExtension) op).getUserCyan()).getId().toString();
			if (str.equals(target.toString())) {
				ArrayList<Permission> permissions = new ArrayList<Permission>();

				if (op.getLevel() >= 4)
					permissions.add(new Permission("*", Mode.ALLOW));
				else if (op.getLevel() >= 3)
					permissions.add(new Permission("cyan.commands.*", Mode.ALLOW));
				else if (op.getLevel() >= 2)
					permissions.add(new Permission("cyan.commands.admin.*", Mode.ALLOW));
				else if (op.getLevel() >= 1)
					permissions.add(new Permission("cyan.bypass.spawn", Mode.ALLOW));

				if (op.getBypassesPlayerLimit())
					permissions.add(new Permission("cyan.bypass.playerlimit", Mode.ALLOW));

				return permissions.toArray(t -> new Permission[t]);
			}
		}
		return new Permission[0];
	}

}
