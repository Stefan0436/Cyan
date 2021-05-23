package org.asf.cyan.internal.modkitimpl;

import java.util.ArrayList;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.TargetModloader;

import modkit.commands.Command;
import modkit.commands.CommandManager;

@TargetModloader(CyanLoader.class)
public class CommandManagerImplementation extends CommandManager implements IPostponedComponent {

	public static interface RegisteryListener {
		public void register(Command command, CommandManager manager);
	}
	
	private static RegisteryListener listener;
	
	public static void assignListener(RegisteryListener listener) {
		if (CommandManagerImplementation.listener != null)
			return;
		CommandManagerImplementation.listener = listener;
	}
	
	private ArrayList<Command> commands = new ArrayList<Command>();

	@Override
	public CommandManager newInstance() {
		return new CommandManagerImplementation();
	}

	@Override
	public CommandManager registerCommand(Command command) {
		commands.add(command);
		listener.register(command, this);
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
