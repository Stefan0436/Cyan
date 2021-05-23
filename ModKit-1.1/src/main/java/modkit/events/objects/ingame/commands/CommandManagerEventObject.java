package modkit.events.objects.ingame.commands;

import org.asf.cyan.api.events.extended.EventObject;

import net.minecraft.commands.Commands;

/**
 * 
 * Command Manager Event Object -- Event object for all events related to the
 * command manager.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CommandManagerEventObject extends EventObject {
	private Commands commandManager;

	public CommandManagerEventObject(Commands commandManager) {
		this.commandManager = commandManager;
	}

	/**
	 * Retrieves the command manager
	 */
	public Commands getCommandManager() {
		return commandManager;
	}

}
