package org.asf.cyan.api.internal.modkit.components._1_16.common.permissions;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.api.permissions.Permission;
import org.asf.cyan.api.permissions.PermissionManager;
import org.asf.cyan.api.permissions.PermissionProvider;
import org.asf.cyan.api.permissions.Permission.Mode;

import net.minecraft.server.MinecraftServer;

public class PermissionManagerImplementation extends PermissionManager implements IModKitComponent {

	private class ProviderEntry {
		public ProviderEntry next;
		public PermissionProvider provider;
	}

	private ArrayList<Permission> defaults = new ArrayList<Permission>();

	private ProviderEntry first;
	private ProviderEntry current;

	private ProviderList providers = new ProviderList();

	private void add(PermissionProvider provider) {
		if (current == null) {
			first = new ProviderEntry();
			current = first;
			first.provider = provider;
			return;
		}

		current.next = new ProviderEntry();
		current = current.next;
		current.provider = provider;
	}

	private class ProviderList implements Iterable<PermissionProvider> {

		@Override
		public Iterator<PermissionProvider> iterator() {
			Iter i = new Iter();
			i.current = first;
			return i;
		}

		public class Iter implements Iterator<PermissionProvider> {
			public ProviderEntry current;

			@Override
			public boolean hasNext() {
				return current != null;
			}

			@Override
			public PermissionProvider next() {
				ProviderEntry ent = current;
				current = current.next;
				return ent.provider;
			}

		}
	}

	@Override
	public void initializeComponent() {
		implementation = this;
		addPermissionProvider(new OpPermissionProvider());
		if (Modloader.getModloaderGameSide() == GameSide.SERVER
				&& Modloader.getModloaderLaunchPlatform() == LaunchPlatform.SPIGOT) {
			addPermissionProvider(new BukkitPermissionProvider());
		}
		defaults.add(new Permission("cyan.commands.player", Mode.ALLOW));
	}

	@Override
	public boolean hasPermission(UUID user, MinecraftServer server, String permissionNode) {
		int state = 2;
		for (PermissionProvider provider : providers) {
			int i = provider.checkPermission(user, permissionNode, server);
			if (i == 1)
				return false;
			else if (i == 0)
				state = 0;
		}
		if (state == 0)
			return true;

		state = 2;
		for (Permission defaultPerm : defaults) {
			if (checkPermissionApplies(defaultPerm, permissionNode)) {
				if (defaultPerm.getMode() == Mode.ALLOW)
					state = 0;
				else
					return false;
			}
		}
		if (state == 0)
			return true;
		else
			return false;
	}

	@Override
	protected boolean checkPermissionApplies(Permission perm, String node) {
		if (perm.getKey().equals("*")) {
			if (perm.getMode() == Mode.ALLOW)
				return true;
			else
				return false;
		}
		String[] segments = node.split("\\.");
		String[] segmentsPerm = perm.getKey().split("\\.");

		int i = 0;
		boolean match = true;
		if (segments.length == 0 || segments[0].equals(""))
			return false;

		for (String segment : segments) {
			if (i >= segmentsPerm.length) {
				return match;
			}

			if (segment.equalsIgnoreCase(segmentsPerm[i])) {
				match = true;
			} else if (segmentsPerm[i].equals("*")) {
				return true;
			} else if (segment.equals("*")) {
				return match;
			} else {
				return false;
			}
			i++;
		}

		return false;
	}

	@Override
	public void addPermissionProvider(PermissionProvider provider) {
		add(provider);
	}

	@Override
	public void addDefaultPermission(Permission perm) {
		defaults.add(perm);
	}

	@Override
	public Permission[] getPermissions(UUID user, MinecraftServer server) {
		ArrayList<Permission> permissions = new ArrayList<Permission>();
		for (Permission defaultPerm : defaults) {
			permissions.add(defaultPerm);
		}
		for (PermissionProvider provider : providers) {
			for (Permission perm : provider.getPermissions(user, server))
				permissions.add(perm);
		}
		return permissions.toArray(t -> new Permission[t]);
	}

}
