package org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.tasks.BacktraceCommand;
import org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.tasks.InfoCommand;
import org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.tasks.ModsCommand;
import org.asf.cyan.api.internal.modkit.components._1_16.common.commands.cyan.tasks.TechnicalCommand;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.internal.BaseEventController;

import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import modkit.commands.Command;
import modkit.commands.CommandManager;
import modkit.util.server.language.ClientLanguage;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

public class CyanCommandProvider extends CyanComponent implements Command, IEventListenerContainer, IModKitComponent {

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

	@Override
	public ArgumentBuilder<CommandSourceStack, ?> setupCommand(CommandManager manager,
			LiteralArgumentBuilder<CommandSourceStack> cmd) {
		return cmd;
	}

	@Override
	public Command[] childCommands() {
		return new Command[] { new BacktraceCommand(), new InfoCommand(), new ModsCommand(), new TechnicalCommand() };
	}

	public static ServerPlayer header(CommandExecutionContext context) {
		String game = Modloader.getModloaderGameVersion();
		String modloaderVersion = Modloader.getModloader(CyanLoader.class).getVersion().toString();
		String line1 = "";
		String line2 = "";

		for (int i = 0; i < game.length(); i++)
			line1 += "-";
		for (int i = 0; i < modloaderVersion.length(); i++)
			line2 += "-";

		if (context.getPlayer() != null)
			context.successTranslatable("cyan.info.header", "§b" + game, "§b" + modloaderVersion, "§b" + line1,
					"§b" + line2);
		return context.getPlayer();
	}

	@Override
	public String getPermission() {
		return "cyan.commands.player";
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

	@Override
	public int execute(CommandExecutionContext context) {
		return 0;
	}

}
