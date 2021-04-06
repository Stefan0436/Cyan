package org.asf.cyan.api.versioning;

/**
 * 
 * Cyan Version API - parses versions and has various features;
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class Version {
	
	public class VersionSegment {
		public char character = (char)-1;
		public char separator = (char)-1;
		public boolean hasSeparator = false;
	}

	protected Version() {
	}

	/**
	 * Parses the given string into a version wrapper instance.
	 * 
	 * @param version Version String
	 * @return Version instance.
	 */
	public static Version fromString(String version) {
		return null; // TODO
	}

	/**
	 * Compares this version to another
	 * 
	 * @param other Version to compare to
	 * @return 1 if greater, 0 if equal and -1 if less.
	 */
	public int compareTo(Version other) {
		if (isGreaterThan(other))
			return 1;
		if (isLessThan(other))
			return -1;

		return 0;
	}

	public boolean isEqualTo(Version other) {
		return false; // TODO
	}

	public boolean isGreaterThan(Version other) {
		return false; // TODO
	}

	public boolean isLessThan(Version other) {
		return false; // TODO
	}

	public boolean isGreaterOrEqualTo(Version other) {
		return false; // TODO
	}

	public boolean isLessOrEqualTo(Version other) {
		return false; // TODO
	}

	@Override
	public String toString() {
		return null; // TODO
	}

}
