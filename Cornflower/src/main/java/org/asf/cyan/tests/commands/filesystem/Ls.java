package org.asf.cyan.tests.commands.filesystem;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.asf.cyan.tests.InteractiveTestCommand;

public class Ls extends InteractiveTestCommand {

	@Override
	public String getId() { return "ls"; }

	@Override
	public List<String> getAliases() { return Arrays.asList("dir", "list", "lst"); }

	@Override
	public String helpSyntax() { return "[folder]"; }

	@Override
	public String helpDescription() { return "list files and folders in current directory"; }

	@Override
	protected Boolean execute(String[] arguments) throws Exception {
		if (arguments.length == 0)
		{
			execute(new String[] { new File(".").getCanonicalPath() });
		}
		else if (arguments.length == 1)
		{
			if (new File(arguments[0]).exists() && new File(arguments[0]).isDirectory())
			{
				for (File folder : new File(arguments[0]).listFiles())
				{
					if (folder.isDirectory())
					{
						getInterface().WriteLine("[FOLDER] "+folder.getName());
					}
				}
				for (File file : new File(arguments[0]).listFiles())
				{
					if (!file.isDirectory())
					{
						getInterface().WriteLine("[ FILE ] "+file.getName());
					}
				}
			}
			else getInterface().WriteLine("Could not find directory.");
		}
		return true;
	}

}
