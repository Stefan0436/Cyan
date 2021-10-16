package org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.tasks;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.CyanCommandProvider;
import org.asf.cyan.api.modloader.Modloader;

import modkit.commands.Command;
import modkit.util.server.ClientSoftware;
import modkit.util.server.language.ClientLanguage;
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
		ClientSoftware client = ClientSoftware.getForPlayer(player);
		if (player == null) {
			context.success(new TextComponent("\n" + ClientLanguage.createComponent(player, "cyan.info.data",
					"\u00A76" + context.getServer().getServerModName(), "\u00A76" + client.getBrandName(),
					"\u00A76" + Modloader.getModloaderVersion(), "\u00A76" + Modloader.getModloaderGameVersion(),
					"\u00A76" + Modloader.getAllMods().length, "\u00A76" + client.getAllMods().length).getString()
					.trim()));
		} else {
			context.successTranslatable("cyan.info.data", "\u00A76" + context.getServer().getServerModName(),
					"\u00A76" + client.getBrandName(), "\u00A76" + Modloader.getModloaderVersion(),
					"\u00A76" + Modloader.getModloaderGameVersion(), "\u00A76" + Modloader.getAllMods().length,
					"\u00A76" + client.getAllMods().length);
		}
		return 0;
	}

}
