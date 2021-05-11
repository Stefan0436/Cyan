package org.asf.cyan.internal.modkitimpl.util;

import java.util.UUID;

import org.asf.cyan.api.permissions.Permission;
import org.asf.cyan.api.permissions.PermissionManager;
import org.asf.cyan.api.permissions.PermissionProvider;

public abstract class PermissionProviderUtils {

	protected static PermissionProviderUtils impl;

	public static PermissionProviderUtils getImpl() {
		return impl;
	}

	public abstract int checkPermission(PermissionProvider provider, UUID user, String permissionNode, Object server);

	public abstract void addDefaultProviders(PermissionManager implementation);

	public abstract Permission[] getPermissions(PermissionProvider provider, UUID user, Object server);

}
