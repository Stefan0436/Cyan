package org.asf.cyan.internal.modkitimpl;

import java.util.ArrayList;

import org.asf.cyan.api.commands.Command;
import org.asf.cyan.api.commands.CommandManager;
import org.asf.cyan.api.common.CYAN_COMPONENT;

@CYAN_COMPONENT
public class CommandManagerImplementation extends CommandManager {

	private ArrayList<Command> commands = new ArrayList<Command>();

	@Override
	public CommandManager newInstance() {
		return new CommandManagerImplementation();
	}

	@Override
	public CommandManager registerCommand(Command command) {
		commands.add(command);
		return this;
	}

	@Override
	public Command[] getCommands() {
		return commands.toArray(t -> new Command[t]);
	}
	
	protected static void initComponent() {
		implementation = new CommandManagerImplementation();
	}

}
