package org.asf.cyan.security;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

/**
 * 
 * Class needed to create {@link TrustContainer} instances.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class TrustContainerBuilder {
	private String name;
	private ArrayList<PackageTrustEntry> packages = new ArrayList<PackageTrustEntry>();

	/**
	 * Instanciates a new trust container builder with trust name
	 * 
	 * @param name Trust name
	 */
	public TrustContainerBuilder(String name) {
		this.name = name;
	}

	/**
	 * Adds classes manually
	 * 
	 * @param packagePath Package path
	 * @param name        Class simple name
	 * @param sha256      Class SHA-256 hash
	 * @return Self
	 */
	public TrustContainerBuilder addClass(String packagePath, String name, String sha256) {
		PackageTrustEntry pkg = null;
		if (packages.stream().anyMatch(t -> t.getName().equals(packagePath))) {
			pkg = packages.stream().filter(t -> t.getName().equals(packagePath)).findFirst().get();
		} else {
			pkg = PackageTrustEntry.create(packagePath, true);
			packages.add(pkg);
		}

		pkg.pushClass(name, sha256);

		return this;
	}

	/**
	 * Adds classes
	 * 
	 * @param cls Class to add
	 * @return Self
	 * @throws IOException If retrieving the class hash fails
	 */
	public TrustContainerBuilder addClass(Class<?> cls) throws IOException {
		PackageTrustEntry pkg = null;
		if (packages.stream().anyMatch(t -> t.getName().equals(cls.getPackageName()))) {
			pkg = packages.stream().filter(t -> t.getName().equals(cls.getPackageName())).findFirst().get();
		} else {
			pkg = PackageTrustEntry.create(cls.getPackageName(), true);
			packages.add(pkg);
		}

		URL location = cls.getProtectionDomain().getCodeSource().getLocation();

		if (!location.toString().endsWith(".class")) {
			String pref = location.toString();
			if ((pref.endsWith(".jar") || pref.endsWith(".zip")) && !pref.startsWith("jar:")) {
				pref = "jar:" + pref + "!/";
			}
			pref += cls.getTypeName().replace(".", "/") + ".class";
			location = new URL(pref);
			System.out.println(pref);
		}

		InputStream strm = location.openStream();
		String sha256 = sha256HEX(strm.readAllBytes());
		strm.close();
		pkg.pushClass(cls.getSimpleName(), sha256);

		return this;
	}

	private String sha256HEX(byte[] array) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		byte[] sha = digest.digest(array);
		StringBuilder result = new StringBuilder();
		for (byte aByte : sha) {
			result.append(String.format("%02x", aByte));
		}
		return result.toString();
	}

	/**
	 * Builds the container
	 */
	public TrustContainer build() {
		TrustContainer container = new TrustContainer();
		packages.forEach((pkg) -> pkg.preventEdit());
		container.assign(name, packages.toArray(t -> new PackageTrustEntry[t]));

		return container;
	}
}
