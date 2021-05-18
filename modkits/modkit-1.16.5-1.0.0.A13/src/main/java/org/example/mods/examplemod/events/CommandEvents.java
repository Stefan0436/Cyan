package org.example.mods.examplemod.events;

import org.asf.cyan.api.commands.CommandManager;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.example.mods.examplemod.commands.ExampleCommand;

public class CommandEvents implements IEventListenerContainer {
	// This event container is for our custom commands
	// We only use the CommandManagerStartupEvent type here

	@AttachEvent(value = "mods.preinit", synchronize = true)
	public void preInit() {
		CommandManager.getMain().registerCommand(new ExampleCommand());
	}

}
