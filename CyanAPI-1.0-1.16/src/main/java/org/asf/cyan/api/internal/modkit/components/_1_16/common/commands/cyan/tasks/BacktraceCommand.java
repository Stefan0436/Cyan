package org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.tasks;

import java.io.File;
import java.io.IOException;

import org.asf.cyan.api.commands.Command;
import org.asf.cyan.api.commands.CommandManager;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.util.server.language.ClientLanguage;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.api.transforming.information.metadata.TransformerMetadata;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class BacktraceCommand extends CyanComponent implements Command {

	private static boolean dumping = false;

	@Override
	public String getPermission() {
		return "cyan.commands.admin.cyan.dump";
	}

	@Override
	public String getId() {
		return "backtrace";
	}

	@Override
	public String getDisplayName() {
		return null;
	}

	@Override
	public String getDescription() {
		return null;
	}

	@Override
	public String getUsage() {
		return "[level]";
	}

	@Override
	public ArgumentBuilder<CommandSourceStack, ?> setupCommand(CommandManager manager,
			LiteralArgumentBuilder<CommandSourceStack> cmd) {
		cmd = cmd.requires(t -> hasPermission(t));
		cmd = cmd.executes(t -> execute(CommandExecutionContext.getNew(t)));

		CommandContainer container = CommandContainer.getFor(this);
		
		container.add(Commands.literal("debug"));
		container.attachExecutionEngine(ctx -> dump("debug", ctx));
		container.closeEntry();

		container.add(Commands.literal("info"));
		container.attachExecutionEngine(ctx -> dump("info", ctx));
		container.closeEntry();

		container.add(Commands.literal("warn"));
		container.attachExecutionEngine(ctx -> dump("warn", ctx));
		container.closeEntry();

		container.add(Commands.literal("error"));
		container.attachExecutionEngine(ctx -> dump("error", ctx));
		container.closeEntry();
		
		return container.build(cmd);
	}

	@Override
	public int execute(CommandExecutionContext context) {
		return dump("info", context);
	}

	private int dump(String level, CommandExecutionContext context) {
		final ServerPlayer playerFinal = context.getPlayer();
		new Thread(() -> {
			if (dumping) {
				context.failure(ClientLanguage.createComponent(playerFinal, "cyan.dump.message.error"));
				return;
			}
			dumping = true;
			context.success(ClientLanguage.createComponent(playerFinal, "cyan.dump.message1"));
			try {
				if (new File(Fluid.getDumpDir(), "transformer-backtrace").exists()) {
					context.failure(ClientLanguage.createComponent(playerFinal, "cyan.dump.message.error.existing"));
					dumping = false;
					return;
				}

				TransformerMetadata.dumpBacktraceOnly(new File(Fluid.getDumpDir(), "transformer-backtrace"), str -> {
					if (level.equals("debug"))
						context.success(new TextComponent("[LOG] \u00A73" + str), true);
				}, str -> {
					if (level.equals("info") || level.equals("debug"))
						context.success(new TextComponent("[LOG] \u00A72" + str), true);
				}, str -> {
					if (level.equals("info") || level.equals("debug") || level.equals("warn"))
						context.success(new TextComponent("[LOG] \u00A76" + str), true);
				}, (str, e) -> {
					context.failure(new TextComponent("[LOG] \u00A74" + str));
					error(str, e);
				});

			} catch (IOException e) {
				error("Failed to dump backtrace", e);
				context.failure(ClientLanguage.createComponent(playerFinal, "cyan.dump.error",
						e.getClass().getTypeName() + ": " + e.getMessage()));
				return;
			}
			context.success(ClientLanguage.createComponent(playerFinal, "cyan.dump.message2"));
			dumping = false;
		}).start();
		return 0;
	}

	@Override
	public int fallbackMinOpLevel() {
		return 4;
	}

}
