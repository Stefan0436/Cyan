package org.asf.cyan.api.commands;

import org.asf.cyan.api.events.ingame.commands.CommandManagerStartupEvent;
import org.asf.cyan.api.events.objects.ingame.commands.CommandManagerEventObject;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.events.SimpleEvent;
import org.asf.cyan.mods.internal.BaseEventController;

/**
 * 
 * Command Manager - Mod Command System
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CommandManager implements IEventListenerContainer {
	private static CommandManager implementation;

	private CommandManager() {
	}

	/**
	 * Retrieves the main command manager
	 */
	public static CommandManager getMain() {
		if (implementation == null) {
			implementation = new CommandManager();
			BaseEventController.addEventContainer(implementation);
		}

		return implementation;
	}

	@SimpleEvent(CommandManagerStartupEvent.class)
	private void commandSetup(CommandManagerEventObject event) {
		event = event;
	}

	/**
	 * Instantiates a new command manager
	 */
	public CommandManager newInstance() {
		return new CommandManager();
	}

}
