package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Scanner;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.cyanloader.CyanSide;
import org.asf.cyan.fluid.mappings.Mapping;
import org.asf.cyan.fluid.mappings.Mappings;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

/**
 * 
 * Minecraft Mappings Toolkit: create Minecraft jar mappings for Fluid and
 * Cornflower.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class MinecraftMappingsToolkit extends CyanComponent {

	protected static void initComponent() {
		trace("INITIALIZE Minecraft Mappings Toolkit, caller: " + KDebug.getCallerClassName());
	}

	static Mapping<?> clientMappings = null;
	static Mapping<?> serverMappings = null;
	static MinecraftVersionInfo clientMappingsVersion = null;
	static MinecraftVersionInfo serverMappingsVersion = null;

	/**
	 * Check if mappings are saved in cache
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return True if file exists, false otherwise.
	 */
	public static boolean areMappingsAvailable(MinecraftVersionInfo version, CyanSide side) {
		return new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/mappings/vanilla-"
				+ version.getVersion() + "-" + side.toString().toLowerCase() + ".mappings.ccfg").exists();
	}

	/**
	 * Download version mappings into ram
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return Mapping object representing the version mappings
	 * @throws IOException If downloading fails
	 */
	public static Mapping<?> downloadMappings(MinecraftVersionInfo version, CyanSide side) throws IOException {
		MinecraftInstallationToolkit.downloadVersionManifest(version);
		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");
		
		info("Resolving official " + side.toString().toLowerCase() + " mappings of minecraft version "
				+ version.getVersion() + "...");
		trace("CREATE scanner for MAPPINGS URL in VERSION JSON, caller: " + KDebug.getCallerClassName());
		StringBuilder mappings_text = new StringBuilder();

		try (Scanner sc = new Scanner(
				new URL(MinecraftInstallationToolkit.getVersionManifest(version).get("downloads").getAsJsonObject()
						.get(side.toString().toLowerCase() + "_mappings").getAsJsonObject().get("url").getAsString())
								.openStream()).useDelimiter("\\A")) {
			trace("SCAN version " + side + " mappings, caller: " + KDebug.getCallerClassName());
			while (sc.hasNext())
				mappings_text.append(sc.next()).append(System.lineSeparator());
			trace("CLOSE mappings scanner, caller: " + KDebug.getCallerClassName());
			sc.close();
		}

		info("Mapping the " + side.toString().toLowerCase() + " jar mappings into CCFG format...");
		trace("MAP version " + side + " mappings into CCFG, caller: " + KDebug.getCallerClassName());
		Mappings mappings = new Mappings().parseProGuardMappings(mappings_text.toString());

		trace("SET " + side.toString().toLowerCase() + "Mappings property, caller: " + KDebug.getCallerClassName());
		if (side.equals(CyanSide.CLIENT)) {
			clientMappings = mappings;
			clientMappingsVersion = version;
		} else if (side.equals(CyanSide.SERVER)) {
			serverMappings = mappings;
			serverMappingsVersion = version;
		}

		return mappings;
	}

	/**
	 * Load mappings from disk
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return Mapping object representing the version mappings
	 * @throws IOException If saving fails
	 */
	public static Mapping<?> loadMappings(MinecraftVersionInfo version, CyanSide side) throws IOException {
		if (!areMappingsAvailable(version, side))
			throw new IOException("File does not exist");
		trace("LOAD version " + version + " " + side + " mappings, caller: " + KDebug.getCallerClassName());
		info("Loading " + version + " " + side + " mappings...");
		Mappings mappings = new Mappings().readAll(Files
				.readString(new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/mappings/vanilla-"
						+ version.getVersion() + "-" + side.toString().toLowerCase() + ".mappings.ccfg").toPath()));

		trace("SET " + side.toString().toLowerCase() + "Mappings property, caller: " + KDebug.getCallerClassName());
		if (side.equals(CyanSide.CLIENT)) {
			clientMappings = mappings;
			clientMappingsVersion = version;
		} else if (side.equals(CyanSide.SERVER)) {
			serverMappings = mappings;
			serverMappingsVersion = version;
		}

		return mappings;
	}

	/**
	 * Save mappings to disk
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return Mapping object representing the version mappings
	 * @throws IOException If saving fails
	 */
	public static Mapping<?> saveMappingsToDisk(MinecraftVersionInfo version, CyanSide side) throws IOException {
		return saveMappingsToDisk(version, side, false);
	}

	/**
	 * Save mappings to disk
	 * 
	 * @param version   Minecraft version
	 * @param side      Which side (server or client)
	 * @param overwrite True to overwrite existing mappings, false throws an
	 *                  exception if already saved
	 * @return Mapping object representing the version mappings
	 * @throws IOException If saving fails
	 */
	public static Mapping<?> saveMappingsToDisk(MinecraftVersionInfo version, CyanSide side, boolean overwrite)
			throws IOException {
		Mapping<?> mappings = null;
		if (side.equals(CyanSide.CLIENT)) {
			if (!clientMappingsVersion.getVersion().equals(version.getVersion()))
				throw new IOException(
						"Cannot write " + clientMappingsVersion + " mappings to a " + version + " mappings file.");
			mappings = clientMappings;
		} else if (side.equals(CyanSide.SERVER)) {
			if (!serverMappingsVersion.getVersion().equals(version.getVersion()))
				throw new IOException(
						"Cannot write " + serverMappingsVersion + " mappings to a " + version + " mappings file.");
			mappings = serverMappings;
		}

		if (!overwrite && areMappingsAvailable(version, side))
			throw new IOException("File already exists and overwrite is set to false!");

		String mappings_file = "vanilla-" + version.getVersion() + "-" + side.toString().toLowerCase()
				+ ".mappings.ccfg";

		trace("GENERATE CCFG mappings file, caller: " + KDebug.getCallerClassName());
		debug("Generating CCFG string...");
		String generated = mappings.toString();
		debug("Preparing mappings directory...");
		trace("CREATE mappings directory IF NONEXISTENT, caller: " + KDebug.getCallerClassName());
		File mappingsDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/mappings");
		if (!mappingsDir.exists())
			mappingsDir.mkdirs();
		debug("Generating CCFG mappings file...");
		trace("WRITE mappings file into '" + mappings_file + "', caller: " + KDebug.getCallerClassName());
		Files.writeString(new File(mappingsDir, mappings_file).toPath(), generated);
		info("Saved CCFG mappings to '<mtk>/caches/mappings/" + mappings_file + "'.");

		return mappings;
	}
	
	/**
	 * Get the version of the currently loaded mappings
	 * @param side Which side (server or client)
	 * @return Minecraft version of the loaded mappings for the specified side
	 */
	public static MinecraftVersionInfo getLoadedMappingsVersion(CyanSide side) {
		if (side.equals(CyanSide.CLIENT))
			return clientMappingsVersion;
		else if (side.equals(CyanSide.SERVER))
			return serverMappingsVersion;
		else return null; // Can't happen yet, no other sides
	}
	
	/**
	 * Get the mappings for the specified side
	 * @param side Which side (server or client)
	 * @return Mapping object representing the version mappings
	 */
	public static Mapping<?> getMappings(CyanSide side) {
		if (side.equals(CyanSide.CLIENT))
			return clientMappings;
		else if (side.equals(CyanSide.SERVER))
			return serverMappings;
		else return null; // Can't happen yet, no other sides
	}
	
}
