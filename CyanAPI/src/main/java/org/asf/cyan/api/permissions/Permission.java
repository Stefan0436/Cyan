package org.asf.cyan.api.permissions;

/**
 * 
 * Permission Node
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class Permission {
	private String key;
	private Mode mode;

	/**
	 * Instantiates this node
	 * 
	 * @param key  Permission node (key)
	 * @param mode Permission mode (allow or disallow)
	 */
	public Permission(String key, Mode mode) {
		this.mode = mode;
		this.key = key;
	}

	/**
	 * Retrieves the key of this node
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Retrieves the mode of this node
	 */
	public Mode getMode() {
		return mode;
	}

	public static enum Mode {
		ALLOW, DISALLOW
	}
}
