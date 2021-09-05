package org.asf.cyan.api.internal.modkit.components._1_17.common.commands;

import org.asf.cyan.api.internal.IModKitComponent;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import modkit.commands.Command.CommandExecutionContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.BaseComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public class CommandExecutionContextImpl extends CommandExecutionContext implements IModKitComponent {

	private CommandContext<CommandSourceStack> context;

	public CommandExecutionContextImpl() {
	}

	public CommandExecutionContextImpl(CommandContext<CommandSourceStack> context) {
		this.context = context;
	}

	@Override
	public <T> T getArgument(String name, Class<T> type, T def) {
		T value = def;
		try {
			value = (T) context.getArgument(name, type);
		} catch (IllegalArgumentException e) {
			if (!e.getMessage().startsWith("No such argument "))
				throw e;
		}
		return value;
	}

	@Override
	public ServerPlayer getPlayer() {
		ServerPlayer pl = null;
		try {
			pl = context.getSource().getPlayerOrException();
		} catch (CommandSyntaxException e) {
		}
		return pl;
	}

	@Override
	public Entity getEntity() {
		return context.getSource().getEntity();
	}

	@Override
	public Level getWorld() {
		if (getEntity() == null)
			return null;
		else
			return getEntity().level;
	}

	@Override
	public MinecraftServer getServer() {
		return context.getSource().getServer();
	}

	@Override
	public CommandContext<CommandSourceStack> toGameType() {
		return context;
	}

	@Override
	public void initializeComponent() {
		implementation = this;
	}

	@Override
	protected CommandExecutionContext newInstance(CommandContext<CommandSourceStack> context) {
		return new CommandExecutionContextImpl(context);
	}

	@Override
	public void success(BaseComponent message, boolean broadcastToAdmins) {
		toGameType().getSource().sendSuccess(message, broadcastToAdmins);
	}

	@Override
	public void failure(BaseComponent message) {
		toGameType().getSource().sendFailure(message);
	}

}
