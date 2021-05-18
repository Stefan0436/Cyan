package org.example.mods.examplemod.commands;

import org.asf.cyan.api.commands.Command;
import org.asf.cyan.api.commands.CommandManager;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

public class ExampleCommand implements Command {

	@Override
	public String getPermission() {
		// The permission node, by default, all players have cyan.commands.player
		return "cyan.commands.player.examplemod.test";
	}

	@Override
	public String getId() {
		return "example";
	}

	@Override
	public String getDisplayName() {
		return "Example Command";
	}

	@Override
	public String getDescription() {
		return "An example command";
	}

	@Override
	public String getUsage() {
		return "<string argument 1> <numeric argument 2>";
	}

	@Override
	public LiteralArgumentBuilder<CommandSourceStack> setupCommand(CommandManager manager,
			LiteralArgumentBuilder<CommandSourceStack> cmd) {
		// This method sets up the command,
		// We use it for defining required arguments.

		// Container for the command builder
		CommandContainer container = CommandContainer.getFor(this);

		// Add the arguments
		container.add(Commands.argument("example string", StringArgumentType.string()));
		container.add(Commands.argument("example number", IntegerArgumentType.integer()));

		// Attach the permission and execution system to the argument chain
		container.attachExecutionEngine();
		container.attachPermission();

		// Build and return the command output object
		return container.build(cmd);
	}

	@Override
	public int execute(CommandExecutionContext context) {
		// This is the command processor

		// Retrieves the arguments
		String str = context.getArgument("example string", String.class);
		int num = context.getArgument("example number", int.class);

		// Retrieves the player or null (no longer needed)
		// ServerPlayer player = context.getPlayer();

		//
		// Sends a success message taken from the mod language file.
		// On vanilla, this sends the fallback message configured by the mod main class.
		//
		// In the language file, you can use %s for language arguments.
		context.successTranslatable("commands.example.success", str, num);

		// Exit code, 0 = success, 1 = failed
		return 0;
	}
}
