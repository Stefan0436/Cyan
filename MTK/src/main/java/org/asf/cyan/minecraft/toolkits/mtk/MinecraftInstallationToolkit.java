package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.util.HashMap;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.cyanloader.CyanSide;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Minecraft Installation Toolkit: download, manage and run Minecraft
 * installations
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class MinecraftInstallationToolkit extends CyanComponent {
	static boolean initialized = false;

	// Version file cache, so the toolkit won't have to download the version file
	// with each operation
	static HashMap<String, JsonObject> versionCache = new HashMap<String, JsonObject>();

	protected static void initComponent() {
		MinecraftToolkit.trace("INITIALIZE Minecraft Installation Toolkit, caller: " + KDebug.getCallerClassName());
		try {
			if (minecraft_directory == null) {
				MinecraftToolkit.trace("SET Minecraft Installation directory to "
						+ new File("./Cyan-MTK").getCanonicalPath() + ", caller: " + KDebug.getCallerClassName());
				minecraft_directory = new File("./Cyan-MTK").getCanonicalFile();
			}
			if (!minecraft_directory.exists())
				minecraft_directory.mkdir();
		} catch (IOException e) {
			e.printStackTrace();
		}
		File manifestDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/manifests");
		if (manifestDir.exists()) {
			try {
				trace("LOAD all versions from " + manifestDir.getCanonicalPath());
				for (File manifest : manifestDir.listFiles(new FilenameFilter() {

					@Override
					public boolean accept(File arg0, String arg1) {
						return arg1.endsWith(".json");
					}

				})) {
					info("Loading version manifest file " + manifest.getName() + "...");
					String json = Files.readString(manifest.toPath());
					String manifestName = manifest.getName();
					manifestName = manifestName.substring(0, manifestName.lastIndexOf(".json"));
					versionCache.put(manifestName, JsonParser.parseString(json).getAsJsonObject());
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		initialized = true;
	}

	/**
	 * Set the Minecraft installation directory
	 * 
	 * @param directory The new Minecraft installation directory
	 * @throws IOException If the directory cannot be set
	 */
	public static void setMinecraftDirectory(File directory) throws IOException {
		MinecraftToolkit.trace("SET Minecraft Installation directory to " + directory.getCanonicalPath() + ", caller: "
				+ KDebug.getCallerClassName());
		if (initialized)
			MinecraftToolkit.warn(
					"The Minecraft installation directory was set after the toolkit was initialized, this is **bad** practice, caller: "
							+ KDebug.getCallerClassName());
		minecraft_directory = directory.getCanonicalFile();
	}

	/**
	 * Get the Minecraft installation directory, by default (except when using the
	 * gradle plugin or calling from minecraft itself), it is set to Cyan-MTK in the
	 * current directory.
	 * 
	 * @return File object that represents the installation directory.
	 */
	public static File getMinecraftDirectory() {
		return minecraft_directory;
	}

	static File minecraft_directory = null;

	/**
	 * Download a version jar, or return it if already downloaded
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return Newly downloaded file, or existing if already downloaded before
	 * @throws IOException If downloading fails.
	 */
	public static File downloadVersionJar(MinecraftVersionInfo version, CyanSide side) throws IOException {
		return downloadVersionJar(version, side, false);
	}

	/**
	 * Download a version jar
	 * 
	 * @param version   Minecraft version
	 * @param side      Which side (server or client)
	 * @param overwrite True to overwrite existing, false to return existing
	 * @return Newly downloaded file
	 * @throws IOException If downloading fails.
	 */
	public static File downloadVersionJar(MinecraftVersionInfo version, CyanSide side, boolean overwrite)
			throws IOException {
		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");
		downloadVersionManifest(version);

		trace("CREATE jars directory IF NONEXISTENT, caller: " + KDebug.getCallerClassName());
		File jarDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/jars");
		if (!jarDir.exists())
			jarDir.mkdirs();

		info("Resolving " + side + " jar of minecraft version " + version.getVersion() + "...");
		JsonObject manifest = versionCache.get(version.getVersion());
		URL u = new URL(manifest.get("downloads").getAsJsonObject().get(side.toString().toLowerCase()).getAsJsonObject()
				.get("url").getAsString());

		File output = new File(jarDir, version.getVersion() + "-" + side.toString().toLowerCase() + ".jar");
		if (output.exists() && !overwrite) {
			return output;
		} else if (output.exists()) {
			output.delete();
		}
		info("Downloading " + version.getVersion() + " into " + output.getName() + "...");
		trace("DOWNLOAD " + side + " jar of minecraft version " + version.getVersion() + " from " + u + " into "
				+ output.getCanonicalPath() + ", caller: " + KDebug.getCallerClassName());
		BufferedInputStream strm = new BufferedInputStream(u.openStream());
		Files.write(output.toPath(), strm.readAllBytes());

		return output;
	}

	/**
	 * Check if a version manifest has been saved to disk cache
	 * 
	 * @param version Minecraft version
	 * @return True if saved, false otherwise
	 */
	public static boolean isVersionManifestSaved(MinecraftVersionInfo version) {
		File manifestDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/manifests");
		File versionFile = new File(manifestDir, version.getVersion() + ".json");
		return versionFile.exists();
	}

	/**
	 * Get a version manifest, downloads into ram if not done before
	 * 
	 * @param version Minecraft version
	 * @return JsonObject representing the version manifest
	 * @throws IOException
	 */
	public static JsonObject getVersionManifest(MinecraftVersionInfo version) throws IOException {
		downloadVersionManifest(version);
		return versionCache.get(version.getVersion());
	}

	/**
	 * Save a version manifest, downloads if not done before, saved manifests are
	 * loaded on MTK initialization
	 * 
	 * @param version Minecraft version
	 * @throws IOException If saving fails
	 */
	public static void saveVersionManifest(MinecraftVersionInfo version) throws IOException {
		downloadVersionManifest(version);

		trace("CREATE manifests directory IF NONEXISTENT, caller: " + KDebug.getCallerClassName());
		File manifestDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/manifests");
		if (!manifestDir.exists())
			manifestDir.mkdirs();

		File versionFile = new File(manifestDir, version.getVersion() + ".json");
		trace("SAVE version manifest of Minecraft version " + version.getVersion() + " into "
				+ versionFile.getCanonicalPath() + ", caller: " + KDebug.getCallerClassName());
		if (versionFile.exists())
			versionFile.delete();
		Files.writeString(versionFile.toPath(), versionCache.get(version.getVersion()).toString());
	}

	static void downloadVersionManifest(MinecraftVersionInfo version) throws IOException {
		if (versionCache.containsKey(version.getVersion()))
			return;
		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");
		
		URL u = version.getManifestURL();
		info("Downloading version manifest from " + u + "...");
		trace("OPEN server connection, CREATE URL STREAM, caller: " + KDebug.getCallerClassName());
		InputStreamReader reader = new InputStreamReader(u.openStream());
		info("Parsing manifest to json...");
		trace("PARSE json from stream, caller: " + KDebug.getCallerClassName());
		JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
		trace("CLOSE server stream, caller: " + KDebug.getCallerClassName());
		reader.close();

		versionCache.put(version.getVersion(), json);
	}
//	
//	/**
//	 * Download a Minecraft version and its libraries.
//	 * 
//	 * @param version MinecraftVersionInfo object representing the version.
//	 */
//	public static void downloadVersionAndLibraries(MinecraftVersionInfo version) {
//		downloadVersionAndLibraries(version, true, true);
//	}
//
//	/**
//	 * Download a Minecraft version and its libraries.
//	 * 
//	 * @param version   MinecraftVersionInfo object representing the version.
//	 * @param overwrite True to overwrite existing files, false otherwise.
//	 */
//	public static void downloadVersionAndLibraries(MinecraftVersionInfo version, boolean overwrite) {
//		downloadVersionAndLibraries(version, overwrite, false);
//	}
//
//	/**
//	 * Download a Minecraft version and its libraries.
//	 * 
//	 * @param version   MinecraftVersionInfo object representing the version.
//	 * @param overwrite True to overwrite files, false otherwise.
//	 * @param checkHash True to compare the hashes of the local files, false to
//	 *                  download without comparing (requires overwrite set to true)
//	 */
//	public static void downloadVersionAndLibraries(MinecraftVersionInfo version, boolean overwrite, boolean checkHash) {
//		downloadVersionAndLibraries(version, overwrite, checkHash, checkHash);
//	}
//
//	/**
//	 * Download a Minecraft version and its libraries.
//	 * 
//	 * @param version         MinecraftVersionInfo object representing the version.
//	 * @param overwrite       True to overwrite files, false otherwise.
//	 * @param checkHash       True to compare the hashes of the local files, false
//	 *                        to download without comparing (requires overwrite set
//	 *                        to true)
//	 * @param checkForMissing True to download missing files. (requires overwrite
//	 *                        set to true)
//	 */
//	public static void downloadVersionAndLibraries(MinecraftVersionInfo version, boolean overwrite, boolean checkHash,
//			boolean checkForMissing) {
//		if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
//			// TODO: Migrate method in test to here
//		} else {
//			// TODO: Check hashes and report if different and that there is no connection
//		}
//	}
//
//	/**
//	 * Check if a version is available, if ignore_hash is false, the files are
//	 * compared against the stored hashes.
//	 * 
//	 * @param version            The MinecraftVersionInfo object representing the
//	 *                           version.
//	 * @param ignore_hash        Set to true to ignore hashes, only return false if
//	 *                           files are missing.
//	 * @param ignore_nonexistant Set to true ignore missing files, set to false to
//	 *                           make sure all files exist (warning: returns false
//	 *                           if version file is not found)
//	 * @return True if all check out, false otherwise.
//	 */
//	public static boolean checkVersion(MinecraftVersionInfo version, boolean ignore_hash, boolean ignore_nonexistant) {
//		// TODO
//		return false;
//	}

}
