package org.asf.cyan.tests.commands.filesystem;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.asf.cyan.tests.InteractiveTestCommand;

public class Cd extends InteractiveTestCommand {

	@Override
	public String getId() {
		return "cd";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList();
	}

	@Override
	public String helpSyntax() {
		return "<path>";
	}

	@Override
	public String helpDescription() {
		return "change current directory";
	}

	@Override
	protected Boolean execute(String[] arguments) throws IOException {
		if (arguments.length == 1) {
			if (arguments[0].contains("~")) {
				arguments[0] = arguments[0].replaceAll("~", System.getProperty("user.home"));
			}
			if (new File(arguments[0]).exists() && new File(arguments[0]).isDirectory()) {
				System.setProperty("user.dir", new File(arguments[0]).getCanonicalPath());
			} else
				getInterface().WriteLine("Could not find directory.");
			return true;
		}
		return false;
	}

}
