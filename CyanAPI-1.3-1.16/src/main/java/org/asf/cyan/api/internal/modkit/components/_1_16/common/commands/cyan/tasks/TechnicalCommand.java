package org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.tasks;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.CyanCommandProvider;
import org.asf.cyan.internal.modkitimpl.info.Protocols;

import modkit.util.server.ClientSoftware;
import modkit.commands.Command;
import modkit.util.server.language.ClientLanguage;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;

public class TechnicalCommand extends CyanComponent implements Command {

	@Override
	public String getPermission() {
		return "cyan.commands.player.cyan.technical";
	}

	@Override
	public String getId() {
		return "technical";
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
			context.success(new TextComponent("\n" + ClientLanguage.createComponent(player, "cyan.info.technical",
					"\u00A76" + Protocols.MODKIT_PROTOCOL, "\u00A76" + Protocols.LOADER_PROTOCOL,
					"\u00A76" + (client.getModKitProtocol() == -1 ? "Not present" : client.getModKitProtocol()),
					"\u00A76" + (client.getRootModloader() == null
							|| client.getRootModloader().getModloaderProtocolVersion() == -1 ? "Not present"
									: client.getRootModloader().getModloaderProtocolVersion()),
					"\u00A76" + (client.getGameVersion() == null ? "Not present" : client.getGameVersion()),
					"\u00A76" + (client.getRootModloader() == null ? "Not present"
							: client.getRootModloader().getVersion()))
					.getString().trim()));
		} else {
			context.successTranslatable("cyan.info.technical", "\u00A76" + Protocols.MODKIT_PROTOCOL,
					"\u00A76" + Protocols.LOADER_PROTOCOL,
					"\u00A76" + (client.getModKitProtocol() == -1 ? "Not present" : client.getModKitProtocol()),
					"\u00A76" + (client.getRootModloader() == null
							|| client.getRootModloader().getModloaderProtocolVersion() == -1 ? "Not present"
									: client.getRootModloader().getModloaderProtocolVersion()),
					"\u00A76" + (client.getGameVersion() == null ? "Not present" : client.getGameVersion()),
					"\u00A76" + (client.getRootModloader() == null ? "Not present"
							: client.getRootModloader().getVersion()));
		}
		return 0;
	}

}
