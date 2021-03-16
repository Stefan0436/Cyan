package org.asf.cyan.tests;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * 
 * Command type for the interactive test command line.
 * @author AerialWorks Software Foundation
 * 
 */
@TestCommand
public abstract class InteractiveTestCommand {
	protected InteractiveTestCommand() {}
	protected static <T extends InteractiveTestCommand> T CreateInstance(TestingInterface inter, Class<? extends T> cmd) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException 
	{
		Constructor<? extends T> ctor = cmd.getConstructor();
		T o = ctor.newInstance();
		o._interface=inter;
		return o;
	}
	
	/**
	 * Command id
	 * @return ID
	 */
	public abstract String getId();
	
	/**
	 * Command aliases
	 * @return List of aliases
	 */
	public abstract List<String> getAliases();
	
	/**
	 * Syntax of command (displayed in help and the invalid usage message)
	 * @return Help syntax
	 */
	public abstract String helpSyntax();
	
	/**
	 * Help description of command (displayed in help and the invalid usage message)
	 * @return Command description
	 */
	public abstract String helpDescription();
	
	/**
	 * Execute the command (and return true if the usage is valid)
	 * @param arguments Command argument list
	 * @return true if success, false if invalid usage
	 * @throws Exception Can throw exceptions
	 */
	protected abstract Boolean execute(String[] arguments) throws Exception;
	
	/**
	 * Execute the command (and return true if the usage is valid)
	 * @param _interface The interface
	 * @param arguments Command argument list
	 * @return true if success, false if invalid usage
	 * @throws Exception Can throw exceptions
	 */
	public Boolean execute(TestingInterface _interface, String[] arguments) throws Exception
	{
		this._interface=_interface;
		return execute(arguments);
	}

	/**
	 * Get the testing interface object
	 * @return Interface used to create/run the command
	 */
	protected TestingInterface getInterface() {
		return _interface;
	}
	
	TestingInterface _interface;

	/**
	 * Return a string for help messages
	 * @param addDescription true if the description must be added to the string.
	 * @return Help message string
	 */
	public String getHelpMessage(boolean addDescription)
	{
		String str = getId();
		for (String alias : getAliases())
		{
			str += "/"+alias;
		}
		
		str += (helpSyntax() != ""? " - "+helpSyntax():"");
		if (addDescription) str += " - "+ helpDescription();
		return str;
	}
}
