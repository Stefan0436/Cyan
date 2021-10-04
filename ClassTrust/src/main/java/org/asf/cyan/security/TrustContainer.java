package org.asf.cyan.security;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.stream.Stream;

import org.asf.cyan.api.packet.PacketEntryWriter;
import org.asf.cyan.api.packet.PacketEntryReader;

/**
 * 
 * Class trust container
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class TrustContainer {
	private String name = null;
	private String version;
	private PackageTrustEntry[] entries = new PackageTrustEntry[0];

	TrustContainer() {
	}

	/**
	 * Retrieves the container name
	 * 
	 * @return Container name
	 */
	public String getContainerName() {
		return name;
	}

	/**
	 * Retrieves the container version timestamp
	 * 
	 * @return Container version
	 */
	public String getVersion() {
		return version;
	}

	/**
	 * Retrieves the container entries
	 * 
	 * @return Container entry array
	 */
	public PackageTrustEntry[] getEntries() {
		return entries;
	}

	/**
	 * Imports container files (.ctc)
	 * 
	 * @param input Input file
	 * @return Imported container
	 * @throws IOException If importing fails
	 */
	public static TrustContainer importContainer(File input) throws IOException {
		TrustContainer store = new TrustContainer();

		FileInputStream strm = new FileInputStream(input);
		PacketEntryReader parser = new PacketEntryReader();
		parser.setSupportedVersion(2);
		parser.read(strm);

		store.name = parser.<String>nextEntry().get();
		store.version = parser.<String>nextEntry().get();

		int count = parser.<Integer>nextEntry().get();
		store.entries = new PackageTrustEntry[count];

		for (int i = 0; i < count; i++) {
			String name = parser.<String>nextEntry().get();
			int entryCount = parser.<Integer>nextEntry().get();

			ClassTrustEntry[] entries = new ClassTrustEntry[entryCount];
			for (int ind = 0; ind < entryCount; ind++) {
				String entryName = parser.<String>nextEntry().get();
				int hashes = parser.<Integer>nextEntry().get();

				String[] hashArray = new String[hashes];
				for (int i2 = 0; i2 < hashes; i2++) {
					hashArray[i2] = parser.<String>nextEntry().get();
				}
				entries[ind] = ClassTrustEntry.create(entryName, hashArray);
			}

			store.entries[i] = PackageTrustEntry.create(name, entries);
		}

		return store;
	}

	/**
	 * Exports this trust container to a container file (.ctc file)
	 * 
	 * @param output Output file
	 * @throws IOException If exporting fails
	 */
	public void exportContainer(File output) throws IOException {
		PacketEntryWriter builder = new PacketEntryWriter();
		builder.setVersion(2);
		builder.add(name);
		ZonedDateTime tm = ZonedDateTime.ofInstant(new Date().toInstant(), ZoneId.of("UTC"));
		String newVer = tm.toEpochSecond() + "-" + tm.getNano();
		version = newVer;
		builder.add(newVer);

		builder.add(entries.length);
		for (PackageTrustEntry entry : entries) {
			builder.add(entry.getName());
			builder.add(entry.getEntries().length);
			for (ClassTrustEntry cls : entry.getEntries()) {
				builder.add(cls.getName());
				builder.add(cls.getHashes().length);
				for (String hash : cls.getHashes())
					builder.add(hash);
			}
		}

		FileOutputStream destination = new FileOutputStream(output);
		builder.write(destination);
		destination.close();
	}

	/**
	 * Validates the given class
	 * 
	 * @param cls Class to validate
	 * @return 0 if nothing is wrong, 1 if invalid, 2 if not found
	 * @throws IOException If validating fails
	 */
	public int validateClass(Class<?> cls) throws IOException {
		String name = cls.getTypeName();
		if (name.contains("."))
			name = name.substring(name.lastIndexOf(".") + 1);
		final String nameFinal = name;
		if (!Stream.of(entries).anyMatch(t -> t.getName().equals(cls.getPackageName())
				&& Stream.of(t.getEntries()).anyMatch(t2 -> t2.getName().equals(nameFinal)))) {
			return 2;
		}

		PackageTrustEntry pkg = Stream.of(entries).filter(t -> t.getName().equals(cls.getPackageName())
				&& Stream.of(t.getEntries()).anyMatch(t2 -> t2.getName().equals(nameFinal))).findFirst().get();

		ClassTrustEntry ent = Stream.of(pkg.getEntries()).filter(t2 -> t2.getName().equals(nameFinal)).findFirst()
				.get();

		URL location = cls.getProtectionDomain().getCodeSource().getLocation();

		if (!location.toString().endsWith(".class")) {
			String pref = location.toString();
			if ((pref.endsWith(".jar") || pref.endsWith(".zip")) && !pref.startsWith("jar:")) {
				pref = "jar:" + pref + "!/";
			}
			pref += cls.getTypeName().replace(".", "/") + ".class";
			location = new URL(pref);
		}

		InputStream strm = location.openStream();
		String sha256 = sha256HEX(strm.readAllBytes());
		strm.close();

		for (String hash : ent.getHashes()) {
			if (hash.equals(sha256)) {
				return 0;
			}
		}

		return 1;
	}

	/**
	 * Validates the given class
	 * 
	 * @param cls  Class stream to validate
	 * @param name Class name
	 * @return 0 if nothing is wrong, 1 if invalid, 2 if not found
	 * @throws IOException If validating fails
	 */
	public int validateClass(InputStream cls, String name) throws IOException {
		String pkgN = "";
		if (name.contains(".")) {
			pkgN = name.substring(0, name.lastIndexOf("."));
			name = name.substring(name.lastIndexOf(".") + 1);
		}
		final String nameFinal = name;
		final String pkgF = pkgN;
		if (!Stream.of(entries).anyMatch(t -> t.getName().equals(pkgF)
				&& Stream.of(t.getEntries()).anyMatch(t2 -> t2.getName().equals(nameFinal)))) {
			return 2;
		}

		PackageTrustEntry pkg = Stream.of(entries).filter(t -> t.getName().equals(pkgF)
				&& Stream.of(t.getEntries()).anyMatch(t2 -> t2.getName().equals(nameFinal))).findFirst().get();

		ClassTrustEntry ent = Stream.of(pkg.getEntries()).filter(t2 -> t2.getName().equals(nameFinal)).findFirst()
				.get();

		String sha256 = sha256HEX(cls.readAllBytes());
		for (String hash : ent.getHashes()) {
			if (hash.equals(sha256)) {
				return 0;
			}
		}

		return 1;
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

	void assign(String name, PackageTrustEntry[] entries) {
		if (this.name != null)
			return;

		this.name = name;
		this.entries = entries;
	}
}
