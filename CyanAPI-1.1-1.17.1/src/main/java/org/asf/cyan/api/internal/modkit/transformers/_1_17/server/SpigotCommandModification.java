package org.asf.cyan.api.internal.modkit.transformers._1_17.server;

import java.util.List;

import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.commands.CommandManager;

@FluidTransformer
@PlatformOnly(LaunchPlatform.SPIGOT)
@TargetClass(target = "org.bukkit.command.Command")
public class SpigotCommandModification {

	private boolean cyanPermAssigned = false;

	@SuppressWarnings("unused")
	private String permission;

	private String name;
	protected String description;
	protected String usageMessage;

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	protected void ctor(String name, String description, String usageMessage, List<?> aliases) {
		modkit.commands.Command command = getCyanCmd(name);
		if (command != null) {
			this.description = command.getDescription();
			this.usageMessage = command.getUsage();
		}
	}

	private boolean cyanCheckVnCmd() {
		return getClass().getTypeName().endsWith(".command.VanillaCommandWrapper");
	}

	private modkit.commands.Command getCyanCmd(String command) {
		if (!cyanCheckVnCmd())
			return null;
		for (modkit.commands.Command cmd : CommandManager.getMain().getCommands()) {
			if (cmd.getId().equals(name)) {
				return cmd;
			}
		}
		return null;
	}

	@InjectAt(location = InjectLocation.TAIL)
	public void setPermission(String perm) {
		modkit.commands.Command command = getCyanCmd(name);
		if (command != null && !cyanPermAssigned) {
			permission = command.getPermission();
			cyanPermAssigned = true;
		}
	}
}
