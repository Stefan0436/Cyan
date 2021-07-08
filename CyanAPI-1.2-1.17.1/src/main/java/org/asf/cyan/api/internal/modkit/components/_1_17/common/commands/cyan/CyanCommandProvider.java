package org.asf.cyan.api.internal.modkit.components._1_17.common.commands.cyan;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.modkit.components._1_17.common.commands.cyan.tasks.BacktraceCommand;
import org.asf.cyan.api.internal.modkit.components._1_17.common.commands.cyan.tasks.InfoCommand;
import org.asf.cyan.api.internal.modkit.components._1_17.common.commands.cyan.tasks.ModsCommand;
import org.asf.cyan.api.internal.modkit.components._1_17.common.commands.cyan.tasks.TechnicalCommand;
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
				"\u00A76Cannot dump backtrace as another dump is in progress.");
		ClientLanguage.registerLanguageKey("cyan.dump.message.error.existing",
				"\u00A76Cannot dump backtrace as another dump has already been saved.");
		ClientLanguage.registerLanguageKey("cyan.dump.message1", "\u00A7bBeginning transformer backtrace dump...");
		ClientLanguage.registerLanguageKey("cyan.dump.error", "\u00A7bAn error occured on the server, Exception: %s");
		ClientLanguage.registerLanguageKey("cyan.dump.message2",
				"\u00A7bDump has been created on the server, \u00A76please remember that you are not allowed to distribute the dump.");

		ClientLanguage.registerLanguageKey("cyan.info.header",
				"\u00A7bMinecraft Cyan %s\u00A7b, Modloader Version: %s\u00A7b\n---------------%s\u00A7b---------------------%s\u00A7b\n");

		ClientLanguage.registerLanguageKey("cyan.info.data",
				"\u00A75Server Brand: \u00A76%s\n\u00A75Client Brand: \u00A76%s\n\u00A75Server Modloader Version: \u00A76%s\n\u00A75Game Version: \u00A76%s\n\u00A75Server Mods: \u00A76%s\n\u00A75Client Mods: \u00A76%s\n");
		ClientLanguage.registerLanguageKey("cyan.info.technical",
				"\u00A74ModKit Protocol Version: \u00A76%s\n\u00A74Modloader Protocol Version: \u00A76%s\n\u00A74Client ModKit Protocol Version: \u00A76%s\n\u00A74Client Modloader Protocol Version: \u00A76%s\n\u00A74Client Game Version: \u00A76%s\n\u00A74Client Modloader Version: \u00A76%s\n");
		ClientLanguage.registerLanguageKey("cyan.info.mods", "\u00A75Server Mods:\n\u00A74%s\n\n\u00A75Client Mods:\n\u00A74%s\n");

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
			context.successTranslatable("cyan.info.header", "\u00A7b" + game, "\u00A7b" + modloaderVersion, "\u00A7b" + line1,
					"\u00A7b" + line2);
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
