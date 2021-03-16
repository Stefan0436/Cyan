package org.asf.cyan.cornflower.classpath.util;

/**
 * Classpath Entry Priority (based on Eclipse's standard, needed to create launch configs)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public enum PathPriority {
	/**
	 * Standard classes, JRE and Libraries
	 */
	STANDARD_CLASSES(1),

	/**
	 * Other bootstrap entries
	 */
	BOOTSTRAP_CLASSES(2),

	/**
	 * User entries
	 */
	USER_ENTIRES(3),

	/**
	 * Entries that should appear on the module path (Modular projects only)
	 */
	MODULEPATH(4),

	/**
	 * Entries that should appear on the class path (Modular projects only)
	 */
	CLASSPATH(5);

	public final int value;

	private PathPriority(int value) {
		this.value = value;
	}
}
