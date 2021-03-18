package org.asf.cyan.api.config.serializing;

import java.io.IOException;

/**
 * 
 * CCFG Configuration 'get-element' action
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class CCFGGetPropAction<T> {
	public Object[] data;
	
	/**
	 * Create a new instance of the CCFGGetPropAction class
	 * @param data Data to pass to the 'data' variable inside the actions
	 */
	public CCFGGetPropAction(Object... data) {
		this.data = data;
	}
	
	/**
	 * Process the prefix of a key, outputs the new prefix, null to ignore (suffixes
	 * can be comments and such)
	 * 
	 * @param key Input key name (null if using the topmost element)
	 * @return Key prefix or null
	 */
	public abstract String processPrefix(String key);

	/**
	 * Process the suffix of a key, outputs the new suffix, null to ignore (suffixes
	 * can be comments and such)
	 * 
	 * @param key Input key name (null if using the topmost element)
	 * @return Key suffix or null
	 */
	public abstract String processSuffix(String key);

	/**
	 * On-add action, optional override
	 * 
	 * @param key   Name of the property
	 * @param value Property value string (CCFG-serialized)
	 */
	public void onAdd(String key, String value) {
	}

	/**
	 * Gets the raw property
	 * 
	 * @param key Property name to take from config/map/etc
	 * @return Raw property value
	 */
	public abstract Object getProp(String key);

	/**
	 * Create a list of keys for the config/map/etc
	 * 
	 * @return Array of possible keys
	 */
	public abstract String[] keys();

	/**
	 * Error handler
	 * 
	 * @param exception the exception to handle
	 */
	public abstract void error(IOException exception);

	/**
	 * Post-process a key
	 * 
	 * @param key Raw key name
	 * @return Processed key name
	 */
	public String postProcessKey(String key) {
		return key;
	}
	
	/**
	 * Post-process a property
	 * 
	 * @param key Raw key name
	 * @param entry Finalized CCFG entry string
	 * @return Processed key name
	 */
	public String postProcess(String key, String entry) {
		return entry;
	}
	
	/**
	 * Set the line prefix, optional override
	 * @return Prefix for each line
	 */
	public String getLinePrefix() {
		return "";
	}
}
