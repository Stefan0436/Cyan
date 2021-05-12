package org.asf.cyan.internal.modkitimpl;

import java.util.ArrayList;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.commands.Command;
import org.asf.cyan.api.commands.CommandManager;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.TargetModloader;

@TargetModloader(CyanLoader.class)
public class CommandManagerImplementation extends CommandManager implements IPostponedComponent {

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
	public void initComponent() {
		implementation = new CommandManagerImplementation();
	}

}
