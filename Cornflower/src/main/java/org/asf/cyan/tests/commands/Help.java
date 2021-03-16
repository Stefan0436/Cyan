package org.asf.cyan.tests.commands;

import java.util.Arrays;
import java.util.List;

import org.asf.cyan.tests.InteractiveTestCommand;

public class Help extends InteractiveTestCommand {
	
	@Override
	public String getId() { return "help"; }
	
	@Override
	public List<String> getAliases() { return Arrays.asList("hlp", "?"); }

	@Override
	public String helpSyntax() { return "[command]"; }

	@Override
	public String helpDescription() { return "command to list all registered commands"; }

	@Override
	protected Boolean execute(String[] arguments) {
		if (arguments.length == 0)
		{
			getInterface().WriteLine("List of commands:");
			for (InteractiveTestCommand cmd : getInterface().getAllCommands())
			{
				getInterface().WriteLine(" - "+cmd.getHelpMessage(true));
			}
			return true;
		}
		else if (arguments.length == 1)
		{
			getInterface().WriteLine("List of commands matching query:");
			for (InteractiveTestCommand cmd : getInterface().getAllCommands())
			{
				if (!cmd.getId().contains(arguments[0].toLowerCase()))
				{
					boolean found=false;
					for (String alias : cmd.getAliases())
					{
						if (alias.contains(arguments[0].toLowerCase()))
						{
							found=true;
							break;
						}
					}
					if (!found) continue;
				}
				getInterface().WriteLine(" - "+cmd.getHelpMessage(true));
			}
			return true;
		}
		return false;
	}

}
