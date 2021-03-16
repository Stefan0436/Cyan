package org.asf.cyan.tests.commands.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.asf.cyan.tests.InteractiveTestCommand;

public class Pwd extends InteractiveTestCommand {

	@Override
	public String getId() {return "pwd";}

	@Override
	public List<String> getAliases() { return Arrays.asList();	}

	@Override
	public String helpSyntax() { return "[path]"; }

	@Override
	public String helpDescription() { return "get absolute path of current directory (or specific directory)"; }

	@Override
	protected Boolean execute(String[] arguments) throws IOException {
		if (arguments.length == 0)
		{
			getInterface().WriteLine(new File(".").getCanonicalPath());
			return true;
		}
		else if (arguments.length == 1)
		{
			getInterface().WriteLine(new File(arguments[0]).getCanonicalPath());
			return true;
		}
		return false;
	}
}
