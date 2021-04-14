package org.asf.cyan.security;

/**
 * 
 * Class trust entry - contains the hash and simple name of a class.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ClassTrustEntry {
	private String name;
	private String[] hashes;

	protected ClassTrustEntry() {
	}

	/**
	 * Creates a new class trust entry
	 * 
	 * @param name   Class simple name
	 * @param hashes Class hashes
	 * @return New ClassTrustEntry instance
	 */
	public static ClassTrustEntry create(String name, String[] hashes) {
		ClassTrustEntry ent = new ClassTrustEntry();
		ent.name = name;
		ent.hashes = hashes;
		return ent;
	}

	/**
	 * Retrieves the simple name of the class
	 * 
	 * @return Class simple name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves the class hashes
	 * 
	 * @return Class SHA-256 hash array
	 */
	public String[] getHashes() {
		return hashes.clone();
	}
}
