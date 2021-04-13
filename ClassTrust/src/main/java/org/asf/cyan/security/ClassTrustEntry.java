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
	private String sha256;

	protected ClassTrustEntry() {
	}

	/**
	 * Creates a new class trust entry
	 * 
	 * @param name   Class simple name
	 * @param sha256 Class hash
	 * @return New ClassTrustEntry instance
	 */
	public static ClassTrustEntry create(String name, String sha256) {
		ClassTrustEntry ent = new ClassTrustEntry();
		ent.name = name;
		ent.sha256 = sha256;
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
	 * Retrieves the class hash
	 * 
	 * @return Class SHA-256 hash
	 */
	public String getSha256() {
		return sha256;
	}
}
