package org.asf.cyan.api.commands;

import org.asf.cyan.mods.events.IEventListenerContainer;

/**
 * 
 * Command Manager - Mod Command System (<b>NOT FULLY IMPLEMENTED, full
 * implementation coming in ModKit 1.2</b>)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class CommandManager implements IEventListenerContainer { // TODO
	protected static CommandManager implementation;

	/**
	 * Retrieves the main command manager
	 */
	public static CommandManager getMain() {
		return implementation;
	}

	/**
	 * Retrieves a new command manager
	 */
	public static CommandManager create() {
		return implementation.newInstance();
	}

	/**
	 * Instantiates a new command manager
	 */
	public abstract CommandManager newInstance();

	/**
	 * Registers the given command (do not call more than once)
	 * 
	 * @param command Command to register
	 * @return Self
	 */
	public abstract CommandManager registerCommand(Command command);

	/**
	 * Retrieves all known commands
	 * 
	 * @return Array of known commands
	 */
	public abstract Command[] getCommands();

}
