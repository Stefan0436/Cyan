package org.asf.cyan.api.internal.modkit.components._1_17.common.commands.cyan.tasks;

import java.util.LinkedHashMap;
import java.util.Map;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.internal.modkit.components._1_17.common.commands.cyan.CyanCommandProvider;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.mods.IModManifest;

import modkit.commands.Command;
import modkit.util.remotedata.RemoteMod;
import modkit.util.server.ClientSoftware;
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
		ClientSoftware client = ClientSoftware.getForPlayer(player);
		for (IModManifest mod : Modloader.getAllMods()) {
			if (!serverMods.isEmpty())
				serverMods += "\u00A77, ";
			serverMods += "\u00A72";
			serverMods += mod.id();
			serverMods += "\u00A77 (\u00A76";
			serverMods += mod.version();
			serverMods += "\u00A77)";
			serverMods += "\u00A77";
		}

		Map<String, String> mods = new LinkedHashMap<String, String>();
		for (RemoteMod mod : client.getAllMods()) {
			mods.put(mod.getModID(), mod.getVersion().toString());
		}

		for (String mod : mods.keySet()) {
			if (!clientMods.isEmpty())
				clientMods += "\u00A77, ";
			clientMods += "\u00A72";
			clientMods += mod;
			clientMods += "\u00A77 (\u00A76";
			clientMods += mods.get(mod);
			clientMods += "\u00A77)";
			clientMods += "\u00A77";
		}

		if (serverMods.isEmpty())
			serverMods = "\u00A74None installed";
		if (clientMods.isEmpty())
			clientMods = "\u00A74None installed";

		if (player == null) {
			context.success(new TextComponent("\n" + ClientLanguage
					.createComponent(player, "cyan.info.mods", serverMods, clientMods).getString().trim()));
		} else {
			context.successTranslatable("cyan.info.mods", serverMods, clientMods);
		}
		return 0;
	}

}
