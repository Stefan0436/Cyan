package org.asf.cyan.security;

/**
 * 
 * Package trust entry - contains class trust entries
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class PackageTrustEntry {

	/**
	 * Internal constructor
	 */
	protected PackageTrustEntry() {
	}

	private String packageName;
	private ClassTrustEntry[] entries = new ClassTrustEntry[0];
	private boolean editable = false;

	/**
	 * Creates a new package entry
	 * 
	 * @param name    Package name
	 * @param entries Class entries to add
	 * @return New PackageTrustEntry instance
	 */
	public static PackageTrustEntry create(String name, ClassTrustEntry... entries) {
		PackageTrustEntry pkg = new PackageTrustEntry();
		pkg.packageName = name;
		pkg.entries = entries;
		return pkg;
	}

	/**
	 * Creates a new package entry
	 * 
	 * @param name     Package name
	 * @param editable True to allow classes to be added with pushClass, false
	 *                 otherwise
	 * @param entries  Class entries to add
	 * @return New PackageTrustEntry instance
	 */
	public static PackageTrustEntry create(String name, boolean editable, ClassTrustEntry... entries) {
		PackageTrustEntry pkg = new PackageTrustEntry();
		pkg.packageName = name;
		pkg.entries = entries;
		pkg.editable = editable;
		return pkg;
	}

	/**
	 * Locks the package
	 */
	public void preventEdit() {
		editable = false;
	}

	/**
	 * Retrieves the package name
	 * 
	 * @return Package fully-quilified name
	 */
	public String getName() {
		return packageName;
	}

	/**
	 * Retrieves the entries in the package
	 * 
	 * @return Array of class entries
	 */
	public ClassTrustEntry[] getEntries() {
		return entries.clone();
	}

	/**
	 * Adds a class to the class array, this package must be marked editable in
	 * order to use this
	 * 
	 * @param simpleName Class simple name
	 * @param hashes     Class hashes (SHA-256)
	 */
	public void pushClass(String simpleName, String[] hashes) {
		if (editable) {
			ClassTrustEntry[] entries = this.entries.clone();
			this.entries = new ClassTrustEntry[this.entries.length + 1];
			for (int i = 0; i < entries.length; i++) {
				this.entries[i] = entries[i];
			}
			this.entries[this.entries.length - 1] = ClassTrustEntry.create(simpleName, hashes);
			for (int i = 0; i < entries.length; i++) {
				entries[i] = null;
			}
		}
	}
}
