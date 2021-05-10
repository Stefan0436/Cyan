package org.asf.cyan.api.commands;

import org.asf.cyan.api.permissions.Permission;

/**
 * 
 * Command interface -- signs command information for paper servers
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface Command {

	/**
	 * Retrieves the base permission of the command
	 */
	public Permission getPermission();

	/**
	 * Retrieves the id of the command
	 */
	public String getId();

	/**
	 * Retrieves the display name of the command
	 */
	public String getDisplayName();

	/**
	 * Retrieves the description message of the command
	 */
	public String getDescription();

	/**
	 * Retrieves the usage message of the command
	 */
	public String getUsage();

}
