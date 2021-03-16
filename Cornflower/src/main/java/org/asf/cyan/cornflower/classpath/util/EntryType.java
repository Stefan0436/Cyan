package org.asf.cyan.cornflower.classpath.util;

/**
 * Classpath Entry Types. (based on Eclipse's standard, needed to create launch configs)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public enum EntryType {
	/**
	 * Project classpath entries
	 */
	PROJECT(1),

	/**
	 * Internal/External archives and class folders
	 */
	ARCHIVE(2),

	/**
	 * Classpath variables
	 */
	VARIABLE(3),

	/**
	 * Libraries (containers such as JRE)
	 */
	CONTAINER(4),

	/**
	 * Other entries
	 */
	OTHER(5);

	public final int value;

	private EntryType(int value) {
		this.value = value;
	}
}
