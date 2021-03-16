package org.asf.cyan.tests.commands;

import java.util.Arrays;
import java.util.List;

import org.asf.cyan.tests.InteractiveTestCommand;

public class Exit extends InteractiveTestCommand {

	@Override
	public String getId() { return "exit"; }

	@Override
	public List<String> getAliases() { return Arrays.asList("quit", "leave", "stop"); }

	@Override
	public String helpSyntax() { return ""; }

	@Override
	public String helpDescription() { return "close the testing session"; }

	@Override
	protected Boolean execute(String[] arguments) throws Exception {
		System.exit(0);
		return true;
	}

}
