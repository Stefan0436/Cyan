package org.asf.cyan.internal.modkitimpl.util;

import java.util.UUID;

import modkit.permissions.Permission;
import modkit.permissions.PermissionManager;
import modkit.permissions.PermissionProvider;

public abstract class PermissionProviderUtils {

	protected static PermissionProviderUtils impl;

	public static PermissionProviderUtils getImpl() {
		return impl;
	}

	public abstract int checkPermission(PermissionProvider provider, UUID user, String permissionNode, Object server);

	public abstract void addDefaultProviders(PermissionManager implementation);

	public abstract Permission[] getPermissions(PermissionProvider provider, UUID user, Object server);

}
