package org.asf.cyan.api.internal.modkit.components._1_16.common;

import java.util.UUID;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.modkit.components._1_16.common.permissions.BukkitPermissionProvider;
import org.asf.cyan.api.internal.modkit.components._1_16.common.permissions.OpPermissionProvider;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.api.permissions.Permission;
import org.asf.cyan.api.permissions.PermissionManager;
import org.asf.cyan.api.permissions.PermissionProvider;
import org.asf.cyan.internal.modkitimpl.util.PermissionProviderUtils;

import net.minecraft.server.MinecraftServer;

public class PermissionProviderUtilsImpl extends PermissionProviderUtils implements IModKitComponent {

	@Override
	public void initializeComponent() {
		impl = this;
	}

	@Override
	public int checkPermission(PermissionProvider provider, UUID user, String permissionNode, Object server) {
		return provider.checkPermission(user, permissionNode, (MinecraftServer) server);
	}

	@Override
	public void addDefaultProviders(PermissionManager implementation) {
		implementation.addPermissionProvider(new OpPermissionProvider());
		if (Modloader.getModloaderGameSide() == GameSide.SERVER
				&& Modloader.getModloaderLaunchPlatform() == LaunchPlatform.SPIGOT) {
			implementation.addPermissionProvider(new BukkitPermissionProvider());
		}
	}

	@Override
	public Permission[] getPermissions(PermissionProvider provider, UUID user, Object server) {
		return provider.getPermissions(user, (MinecraftServer)server);
	}

}
