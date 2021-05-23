package org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.tasks;

import java.util.Map;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.CyanCommandProvider;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;

import modkit.advanced.Client;
import modkit.commands.Command;
import modkit.util.server.language.ClientLanguage;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class ModsCommand extends CyanComponent implements Command {

	@Override
	public String getPermission() {
		return "cyan.commands.player.cyan.mods";
	}

	@Override
	public String getId() {
		return "mods";
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
		return "";
	}

	@Override
	public int execute(CommandExecutionContext context) {
		ServerPlayer player = CyanCommandProvider.header(context);
		String serverMods = "";
		String clientMods = "";
		Client client = Client.getForPlayer(player);
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
		Map<String, String> mods = client.getMods();
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
			context.success(new TextComponent("\n" + ClientLanguage
					.createComponent(player, "cyan.info.mods", serverMods, clientMods).getString().trim()));
		} else {
			context.successTranslatable("cyan.info.mods", serverMods, clientMods);
		}
		return 0;
	}

}
