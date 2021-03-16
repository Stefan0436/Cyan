package org.asf.cyan.tests.commands.exceptions;

import org.asf.cyan.tests.InteractiveTestCommand;

public class InvalidAliasException extends Exception {
	
	/**
	 * Initialize a new InvalidAliasException object
	 * @param message The error message
	 * @param alias The invalid alias
	 * @param cmd The command that the alias is part of
	 */
	public InvalidAliasException(String message, String alias, InteractiveTestCommand cmd)
	{
		super(message+"\nInvalid alias: "+alias + ", command id: "+cmd.getId());
	}
	
	/**
	 * Initialize a new InvalidAliasException object
	 * @param message The error message
	 * @param t The throwable
	 * @param alias The invalid alias
	 * @param cmd The command that the alias is part of
	 */
	public InvalidAliasException(String message, Throwable t, String alias, InteractiveTestCommand cmd)
	{
		super(message+"\nInvalid alias: "+alias + ", command id: "+cmd.getId(), t);
	}
	
	/**
	 * Initialize a new InvalidAliasException object
	 * @param t The throwable
	 * @param alias The invalid alias
	 * @param cmd The command that the alias is part of
	 */
	public InvalidAliasException(Throwable t, String alias, InteractiveTestCommand cmd)
	{
		super("Invalid alias: "+alias + ", command id: ", t);
	}
	
	/**
	 * Initialize a new InvalidAliasException object
	 * @param alias The invalid alias
	 * @param cmd The command that the alias is part of
	 */
	public InvalidAliasException(String alias, InteractiveTestCommand cmd)
	{
		super("Invalid alias: "+alias + ", command id: ");
	}
	
	private static final long serialVersionUID = 1L;
}
