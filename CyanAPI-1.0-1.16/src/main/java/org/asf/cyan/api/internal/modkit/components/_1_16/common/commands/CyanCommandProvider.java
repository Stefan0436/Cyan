package org.asf.cyan.api.internal.modkit.components._1_16.common.commands;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.commands.Command;
import org.asf.cyan.api.commands.CommandManager;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.ingame.commands.CommandManagerStartupEvent;
import org.asf.cyan.api.events.objects.ingame.commands.CommandManagerEventObject;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.permissions.Permission;
import org.asf.cyan.api.permissions.PermissionManager;
import org.asf.cyan.api.permissions.Permission.Mode;
import org.asf.cyan.api.util.server.language.ClientLanguage;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.api.transforming.information.metadata.TransformerMetadata;
import org.asf.cyan.internal.modkitimpl.handshake.CyanHandshakePacketChannel;
import org.asf.cyan.internal.modkitimpl.handshake.CyanHandshakePacketChannel.ClientInformation;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class CyanCommandProvider extends CyanComponent implements Command, IEventListenerContainer, IModKitComponent {

	private static boolean dumping = false;

	@Override
	public void initializeComponent() {
		BaseEventController.addEventContainer(this);
		ClientLanguage.registerLanguageKey("cyan.dump.message.error",
				"§6Cannot dump backtrace as another dump is in progress.");
		ClientLanguage.registerLanguageKey("cyan.dump.message.error.existing",
				"§6Cannot dump backtrace as another dump has already been saved.");
		ClientLanguage.registerLanguageKey("cyan.dump.message1", "§bBeginning transformer backtrace dump...");
		ClientLanguage.registerLanguageKey("cyan.dump.error", "§bAn error occured on the server, Exception: %s");
		ClientLanguage.registerLanguageKey("cyan.dump.message2",
				"§bDump has been created on the server, §6please remember that you are not allowed to distribute the dump.");

		ClientLanguage.registerLanguageKey("cyan.info.header",
				"§bMinecraft Cyan %s§b, Modloader Version: %s§b\n---------------%s§b---------------------%s§b\n");

		ClientLanguage.registerLanguageKey("cyan.info.data",
				"§5Server Brand: §6%s\n§5Client Brand: §6%s\n§5Server Modloader Version: §6%s\n§5Game Version: §6%s\n§5Server Mods: §6%s\n§5Client Mods: §6%s\n");
		ClientLanguage.registerLanguageKey("cyan.info.technical",
				"§4ModKit Protocol Version: §6%s\n§4Modloader Protocol Version: §6%s\n§4Client ModKit Protocol Version: §6%s\n§4Client Modloader Protocol Version: §6%s\n§4Client Game Version: §6%s\n§4Client Modloader Version: §6%s\n");
		ClientLanguage.registerLanguageKey("cyan.info.mods", "§5Server Mods:\n§4%s\n\n§5Client Mods:\n§4%s\n");

		ClientLanguage.registerLanguageKey("cyan.commands.cyan.description", "Utility command for the cyan modloader.");
		ClientLanguage.registerLanguageKey("cyan.commands.cyan.usage", "<task> [arguments...]");
	}

	@AttachEvent(value = "mods.preinit", synchronize = true)
	public void preInit() {
		CommandManager.getMain().registerCommand(this);
	}

	@SimpleEvent(CommandManagerStartupEvent.class)
	public void startupCommandManager(CommandManagerEventObject event) {
		LiteralArgumentBuilder<CommandSourceStack> cyanInfo = LiteralArgumentBuilder.literal("cyan");
		cyanInfo = cyanInfo.then(Commands.literal("backtrace").requires(t -> {
			try {
				return PermissionManager.getInstance().hasPermission(t.getEntityOrException(),
						"cyan.commands.admin.cyan.dump");
			} catch (CommandSyntaxException ex) {
				return t.hasPermission(4);
			}
		}).executes(cmd -> {
			return dump("info", cmd);
		}).then(Commands.literal("debug").executes(cmd -> {
			return dump("debug", cmd);
		})).then(Commands.literal("info").executes(cmd -> {
			return dump("info", cmd);
		})).then(Commands.literal("warn").executes(cmd -> {
			return dump("warn", cmd);
		})).then(Commands.literal("error").executes(cmd -> {
			return dump("error", cmd);
		}))).then(Commands.literal("info").requires(t -> {
			try {
				return PermissionManager.getInstance().hasPermission(t.getEntityOrException(),
						"cyan.commands.player.cyan.info");
			} catch (CommandSyntaxException ex) {
				return true;
			}
		}).executes(cmd -> {
			ServerPlayer player = header(cmd);
			ClientInformation info = CyanHandshakePacketChannel.getClientInfo(player);
			if (player == null) {
				cmd.getSource()
						.sendSuccess(new TextComponent("\n" + ClientLanguage.createComponent(player, "cyan.info.data",
								"§6" + cmd.getSource().getServer().getServerModName(), "§6" + info.getBrand(),
								"§6" + Modloader.getModloaderVersion(), "§6" + Modloader.getModloaderGameVersion(),
								"§6" + Modloader.getAllMods().length, "§6" + info.getMods().size()).getString().trim()),
								true);
			} else {
				cmd.getSource()
						.sendSuccess(ClientLanguage.createComponent(player, "cyan.info.data",
								"§6" + cmd.getSource().getServer().getServerModName(), "§6" + info.getBrand(),
								"§6" + Modloader.getModloaderVersion(), "§6" + Modloader.getModloaderGameVersion(),
								"§6" + Modloader.getAllMods().length, "§6" + info.getMods().size()), true);
			}
			return 0;
		})).then(Commands.literal("mods").requires(t -> {
			try {
				return PermissionManager.getInstance().hasPermission(t.getEntityOrException(),
						"cyan.commands.player.cyan.mods");
			} catch (CommandSyntaxException ex) {
				return true;
			}
		}).executes(cmd -> {
			ServerPlayer player = header(cmd);

			String serverMods = "";
			String clientMods = "";
			ClientInformation info = CyanHandshakePacketChannel.getClientInfo(player);
			for (IModManifest mod : Modloader.getAllMods()) {
				if (!serverMods.isEmpty())
					serverMods += "§7, ";
				serverMods += "§2";
				serverMods += mod.id();
				serverMods += "§7 (§6";
				serverMods += mod.version();
				serverMods += "§7)";
				serverMods += "§7";
			}
			Map<String, String> mods = info.getMods();
			for (String mod : mods.keySet()) {
				if (!clientMods.isEmpty())
					clientMods += "§7, ";
				clientMods += "§2";
				clientMods += mod;
				clientMods += "§7 (§6";
				clientMods += mods.get(mod);
				clientMods += "§7)";
				clientMods += "§7";
			}

			if (serverMods.isEmpty())
				serverMods = "§4None installed";
			if (clientMods.isEmpty())
				clientMods = "§4None installed";

			if (player == null) {
				cmd.getSource()
						.sendSuccess(new TextComponent("\n" + ClientLanguage
								.createComponent(player, "cyan.info.mods", serverMods, clientMods).getString().trim()),
								true);
			} else {
				cmd.getSource().sendSuccess(
						ClientLanguage.createComponent(player, "cyan.info.mods", serverMods, clientMods), true);
			}
			return 0;
		})).then(Commands.literal("technical").requires(t -> {
			try {
				return PermissionManager.getInstance().hasPermission(t.getEntityOrException(),
						"cyan.commands.player.cyan.technical");
			} catch (CommandSyntaxException ex) {
				return true;
			}
		}).executes(cmd -> {
			ServerPlayer player = header(cmd);
			ClientInformation info = CyanHandshakePacketChannel.getClientInfo(player);
			if (player == null) {
				cmd.getSource()
						.sendSuccess(
								new TextComponent("\n" + ClientLanguage
										.createComponent(player, "cyan.info.technical",
												"§6" + Protocols.MODKIT_PROTOCOL, "§6" + Protocols.LOADER_PROTOCOL,
												"§6" + info.getProtocol(), "§6" + info.getModloaderProtocol(),
												"§6" + info.getGameVersion(), "§6" + info.getModloaderVersion())
										.getString().trim()),
								true);
			} else {
				cmd.getSource()
						.sendSuccess(ClientLanguage.createComponent(player, "cyan.info.technical",
								"§6" + Protocols.MODKIT_PROTOCOL, "§6" + Protocols.LOADER_PROTOCOL,
								"§6" + info.getProtocol(), "§6" + info.getModloaderProtocol(),
								"§6" + info.getGameVersion(), "§6" + info.getModloaderVersion()), true);
			}
			return 0;
		}));
		event.getCommandManager().getDispatcher().register(cyanInfo);
	}

	private ServerPlayer header(CommandContext<CommandSourceStack> cmd) {
		ServerPlayer player;
		try {
			player = cmd.getSource().getPlayerOrException();
		} catch (CommandSyntaxException ex) {
			player = null;
		}

		String game = Modloader.getModloaderGameVersion();
		String modloaderVersion = Modloader.getModloader(CyanLoader.class).getVersion().toString();
		String line1 = "";
		String line2 = "";

		for (int i = 0; i < game.length(); i++)
			line1 += "-";
		for (int i = 0; i < modloaderVersion.length(); i++)
			line2 += "-";

		if (player != null)
			cmd.getSource().sendSuccess(ClientLanguage.createComponent(player, "cyan.info.header", "§b" + game,
					"§b" + modloaderVersion, "§b" + line1, "§b" + line2), true);
		return player;
	}

	private int dump(String level, CommandContext<CommandSourceStack> cmd) {
		ServerPlayer player;
		try {
			player = cmd.getSource().getPlayerOrException();
		} catch (CommandSyntaxException ex) {
			player = null;
		}

		final ServerPlayer playerFinal = player;
		new Thread(() -> {
			if (dumping) {
				cmd.getSource().sendFailure(ClientLanguage.createComponent(playerFinal, "cyan.dump.message.error"));
				return;
			}
			dumping = true;
			cmd.getSource().sendSuccess(ClientLanguage.createComponent(playerFinal, "cyan.dump.message1"), true);
			try {
				if (new File(Fluid.getDumpDir(), "transformer-backtrace").exists()) {
					cmd.getSource().sendFailure(
							ClientLanguage.createComponent(playerFinal, "cyan.dump.message.error.existing"));
					dumping = false;
					return;
				}
				TransformerMetadata.dumpBacktraceOnly(new File(Fluid.getDumpDir(), "transformer-backtrace"), str -> {
					if (level.equals("debug"))
						cmd.getSource().sendSuccess(new TextComponent("[LOG] §3" + str), true);
				}, str -> {
					if (level.equals("info") || level.equals("debug"))
						cmd.getSource().sendSuccess(new TextComponent("[LOG] §2" + str), true);
				}, str -> {
					if (level.equals("info") || level.equals("debug") || level.equals("warn"))
						cmd.getSource().sendSuccess(new TextComponent("[LOG] §6" + str), true);
				}, (str, e) -> {
					cmd.getSource().sendFailure(new TextComponent("[LOG] §4" + str));
					error(str, e);
				});
			} catch (IOException e) {
				error("Failed to dump backtrace", e);
				cmd.getSource().sendFailure(ClientLanguage.createComponent(playerFinal, "cyan.dump.error",
						e.getClass().getTypeName() + ": " + e.getMessage()));
				return;
			}
			cmd.getSource().sendSuccess(ClientLanguage.createComponent(playerFinal, "cyan.dump.message2"), true);
			dumping = false;
		}).start();
		return 0;
	}

	@Override
	public Permission getPermission() {
		return new Permission("cyan.commands.player", Mode.ALLOW);
	}

	@Override
	public String getId() {
		return "cyan";
	}

	@Override
	public String getDisplayName() {
		return getId();
	}

	@Override
	public String getDescription() {
		return ClientLanguage.createComponent(null, "cyan.commands.cyan.description").getString();
	}

	@Override
	public String getUsage() {
		return ClientLanguage.createComponent(null, "cyan.commands.cyan.usage").getString();
	}

}
