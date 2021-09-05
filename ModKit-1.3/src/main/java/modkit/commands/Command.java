package modkit.commands;

import java.util.function.Function;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import modkit.permissions.Permission;
import modkit.permissions.PermissionManager;
import modkit.util.server.language.ClientLanguage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * 
 * Command interface -- interface for creating modded commands
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface Command {

	/**
	 * 
	 * Command Execution Context -- Small wrapper around the CommandContext type to
	 * help with mod commands.
	 * 
	 * @author Stefan0436 - AerialWorks Software Foundation
	 *
	 */
	public static abstract class CommandExecutionContext {

		protected static CommandExecutionContext implementation;

		public static CommandExecutionContext getNew(CommandContext<CommandSourceStack> context) {
			return implementation.newInstance(context);
		}

		protected abstract CommandExecutionContext newInstance(CommandContext<CommandSourceStack> context);

		/**
		 * Retrieves a argument value
		 * 
		 * @param <T>  Value type
		 * @param name Value name
		 * @param type Value class
		 * @return Value or default
		 */
		public <T> T getArgument(String name, Class<T> type) {
			return getArgument(name, type, (T) null);
		}

		/**
		 * Retrieves a argument value
		 * 
		 * @param <T>  Value type
		 * @param name Value name
		 * @param def  Default value
		 * @return Value or default
		 */
		public abstract <T> T getArgument(String name, Class<T> type, T def);

		/**
		 * Retrieves the player that ran the command, returns null if not a player
		 * 
		 * @return Player or null.
		 */
		public abstract ServerPlayer getPlayer();

		/**
		 * Retrieves the entity that ran the command, returns null if not an entity
		 * 
		 * @return Player or null.
		 */
		public abstract Entity getEntity();

		/**
		 * Retrieves the world the command was run in.
		 * 
		 * @return Level instance or null if called from the console
		 */
		public abstract Level getWorld();

		/**
		 * Retrieves the server the command was run in.
		 * 
		 * @return Server instance
		 */
		public abstract MinecraftServer getServer();

		/**
		 * Retrieves the game type of the Command Execution Context
		 * 
		 * @return Vanilla CommandContext instance.
		 */
		public abstract CommandContext<CommandSourceStack> toGameType();

		/**
		 * Sends command success
		 * 
		 * @param message Success message
		 */
		public void success(String message) {
			success(message, false);
		}

		/**
		 * Sends command success
		 * 
		 * @param message           Success message
		 * @param broadcastToAdmins True to broadcast to admins, false otherwise
		 */
		public void success(String message, boolean broadcastToAdmins) {
			success(new TextComponent(message), broadcastToAdmins);
		}

		/**
		 * Sends command success (translatable through ClientLanguage)
		 * 
		 * @param message Success message
		 * @param params  Language parameters
		 */
		public void successTranslatable(String message, Object... params) {
			success(ClientLanguage.createComponent(getPlayer(), message, params));
		}

		/**
		 * Sends command success
		 * 
		 * @param message Success message
		 */
		public void success(BaseComponent message) {
			success(message, false);
		}

		/**
		 * Sends command success
		 * 
		 * @param message           Success message
		 * @param broadcastToAdmins True to broadcast to admins, false otherwise
		 */
		public abstract void success(BaseComponent message, boolean broadcastToAdmins);

		/**
		 * Sends command failure
		 * 
		 * @param message Failure message
		 */
		public void failure(String message) {
			failure(new TextComponent(message));
		}

		/**
		 * Sends command failure
		 * 
		 * @param message Failure message
		 */
		public abstract void failure(BaseComponent message);

		/**
		 * Sends command failure (translatable through ClientLanguage)
		 * 
		 * @param message Failure message
		 * @param params  Language parameters
		 */
		public void failureTranslatable(String message, Object... params) {
			failure(ClientLanguage.createComponent(getPlayer(), message, params));
		}

	}

	/**
	 * 
	 * Command Container -- Small wrapper around Mojang Brigadier to help with mod
	 * commands.
	 * 
	 * @author Stefan0436 - AerialWorks Software Foundation
	 *
	 */
	public static abstract class CommandContainer {
		protected static CommandContainer implementation;

		/**
		 * Instantiates the container
		 * 
		 * @param owner Owner command
		 * @return New CommandContainer instance
		 */
		protected abstract CommandContainer newInstance(Command owner);

		/**
		 * Retrieves a new CommandContainer instance for the given command
		 * 
		 * @param owner Owner command
		 * @return New CommandContainer instance
		 */
		public static CommandContainer getFor(Command owner) {
			return implementation.newInstance(owner);
		}

		/**
		 * Adds child builders to the command
		 * 
		 * @param node The node to add
		 */
		public abstract void add(ArgumentBuilder<CommandSourceStack, ?> node);

		/**
		 * Closes the current entry, allows for new arguments to be added
		 */
		public abstract void closeEntry();

		/**
		 * Retrieves the current node
		 */
		public abstract ArgumentBuilder<CommandSourceStack, ?> getCurrent();

		/**
		 * Attaches a permission requirement to the current node
		 * 
		 * @param perm  Permission to use
		 * @param level Fallback OP level if the permission manager fails
		 */
		public void attachPermission(Permission perm, int level) {
			attachPermission(perm.getKey(), level);
		}

		/**
		 * Attaches the default permission requirement to the current node
		 */
		public abstract void attachPermission();

		/**
		 * Attaches the owning command's execution engine to the current node
		 */
		public abstract void attachExecutionEngine();

		/**
		 * Attaches the owning command's execution engine to the current node
		 */
		public abstract void attachExecutionEngine(Function<CommandExecutionContext, Integer> engine);

		/**
		 * Attaches a permission requirement to the current node
		 * 
		 * @param perm  Permission to use
		 * @param level Fallback OP level if the permission manager fails (0 or -1 to
		 *              allow without)
		 */
		public abstract void attachPermission(String perm, int level);

		/**
		 * Builds the final command (wipes the command cache)
		 * 
		 * @param newOwner Input LiteralArgumentBuilder
		 * @return LiteralArgumentBuilder instance to replace the input
		 */
		public abstract LiteralArgumentBuilder<CommandSourceStack> build(
				LiteralArgumentBuilder<CommandSourceStack> newOwner);
	}

	/**
	 * Retrieves the base permission of the command
	 */
	public String getPermission();

	/**
	 * Retrieves the id of the command
	 */
	public String getId();

	/**
	 * Retrieves the display name of the command
	 */
	public String getDisplayName();

	/**
	 * Retrieves the description message of the command
	 */
	public String getDescription();

	/**
	 * Retrieves the usage message of the command
	 */
	public String getUsage();

	/**
	 * The fallback Operator level if the permission manager cannot be reached.
	 */
	public default int fallbackMinOpLevel() {
		return 0;
	}

	/**
	 * The child commands of this command (semi-arguments)
	 */
	public default Command[] childCommands() {
		return new Command[0];
	}

	/**
	 * Command execution handler
	 * 
	 * @param context The command context
	 * @return 0 if successful, non-zero if failed.
	 */
	public int execute(CommandExecutionContext context);

	/**
	 * Configures the command
	 * 
	 * @param manager Owning command manager
	 * @param cmd     Input command
	 * @return Output command
	 */
	public default ArgumentBuilder<CommandSourceStack, ?> setupCommand(CommandManager manager,
			LiteralArgumentBuilder<CommandSourceStack> cmd) {
		cmd = cmd.requires(t -> hasPermission(t));
		cmd = cmd.executes(t -> execute(CommandExecutionContext.getNew(t)));
		return cmd;
	}

	public default boolean hasPermission(CommandSourceStack stack) {
		int level = fallbackMinOpLevel();
		try {
			return PermissionManager.getInstance().hasPermission(stack.getEntityOrException(), getPermission());
		} catch (CommandSyntaxException ex) {
			if (level == 0 || level == -1)
				return true;
			else
				return stack.hasPermission(level);
		}
	}

	/**
	 * Builds the game type instance
	 * 
	 * @param manager The command manager calling this method
	 * @return LiteralArgumentBuilder instance
	 */
	@SuppressWarnings("unchecked")
	public default LiteralArgumentBuilder<CommandSourceStack> build(CommandManager manager) {
		LiteralArgumentBuilder<CommandSourceStack> cmd = Commands.literal(getId());

		ArgumentBuilder<CommandSourceStack, ?> builder = cmd;
		builder = setupCommand(manager, cmd);

		if (builder == cmd) {
			cmd = (LiteralArgumentBuilder<CommandSourceStack>) builder;
		}
		for (Command subCommand : childCommands()) {
			cmd = cmd.then(subCommand.build(manager));
		}

		return cmd;
	}

}
