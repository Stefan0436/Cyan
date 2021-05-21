package org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.tasks;

import org.asf.cyan.api.advanced.Client;
import org.asf.cyan.api.commands.Command;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.CyanCommandProvider;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.util.server.language.ClientLanguage;

import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class InfoCommand extends CyanComponent implements Command {

	@Override
	public String getPermission() {
		return "cyan.commands.player.cyan.info";
	}

	@Override
	public String getId() {
		return "info";
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
		Client client = Client.getForPlayer(player);
		if (player == null) {
			context.success(new TextComponent("\n" + ClientLanguage.createComponent(player, "cyan.info.data",
					"§6" + context.getServer().getServerModName(), "§6" + client.getBrand(),
					"§6" + Modloader.getModloaderVersion(), "§6" + Modloader.getModloaderGameVersion(),
					"§6" + Modloader.getAllMods().length, "§6" + client.getMods().size()).getString().trim()));
		} else {
			context.successTranslatable("cyan.info.data", "§6" + context.getServer().getServerModName(),
					"§6" + client.getBrand(), "§6" + Modloader.getModloaderVersion(),
					"§6" + Modloader.getModloaderGameVersion(), "§6" + Modloader.getAllMods().length,
					"§6" + client.getMods().size());
		}
		return 0;
	}

}
