package org.asf.cyan.fluid.mappings;

/**
 * 
 * Mapping types
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public enum MAPTYPE {
	/**
	 * Top-level mapping, only holds children
	 */
	TOPLEVEL,
	
	/**
	 * Method mapping (must be part of a class mapping)
	 */
	METHOD,
	
	/**
	 * Property mapping (must be part of a class mapping)
	 */
	PROPERTY,
	
	/**
	 * Class mapping (can only be nested in a top-level mapping)
	 */
	CLASS
}
