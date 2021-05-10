package org.asf.cyan.api.internal.modkit.components._1_15_2.common.commands;

import java.util.ArrayList;

import org.asf.cyan.api.commands.Command;
import org.asf.cyan.api.commands.CommandManager;
import org.asf.cyan.api.internal.IModKitComponent;

public class CommandManagerImplementation extends CommandManager implements IModKitComponent {

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

	@Override
	public void initializeComponent() {
		implementation = this;
	}

}
