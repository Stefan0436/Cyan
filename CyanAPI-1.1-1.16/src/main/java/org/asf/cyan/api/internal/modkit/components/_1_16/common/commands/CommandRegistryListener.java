package org.asf.cyan.api.internal.modkit.components._1_16.common.commands;

import org.asf.cyan.api.commands.Command;
import org.asf.cyan.api.commands.CommandManager;
import org.asf.cyan.api.events.ingame.commands.CommandManagerStartupEvent;
import org.asf.cyan.api.events.objects.ingame.commands.CommandManagerEventObject;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.internal.modkitimpl.CommandManagerImplementation;
import org.asf.cyan.internal.modkitimpl.CommandManagerImplementation.RegisteryListener;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

import net.minecraft.commands.Commands;

public class CommandRegistryListener implements IModKitComponent, RegisteryListener, IEventListenerContainer {

	@Override
	public void initializeComponent() {
		CommandManagerImplementation.assignListener(this);
		BaseEventController.addEventContainer(this);
	}

	@SimpleEvent(CommandManagerStartupEvent.class)
	private void commandStartup(CommandManagerEventObject event) {
		commands = event.getCommandManager();
		if (manager == null)
			manager = CommandManager.getMain();
		for (Command cmd : manager.getCommands()) {
			commands.getDispatcher().register(cmd.build(manager));
		}
	}

	private Commands commands;
	private CommandManager manager;

	@Override
	public void register(Command command, CommandManager manager) {
		this.manager = manager;
		if (commands != null)
			commands.getDispatcher().register(command.build(manager));
	}

}
