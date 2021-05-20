package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.logging.log4j.Level;
import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.config.serializing.internal.Splitter;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.minecraft.toolkits.mtk.auth.AuthenticationInfo;
import org.asf.cyan.minecraft.toolkits.mtk.auth.MinecraftAccountType;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

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
	static boolean ide = false;

	public static boolean isIDEModeEnabled() {
		return ide;
	}

	public static void setIDE() {
		CyanCore.setIDE();
		ide = true;
	}

	private static HashMap<String, String> variableStorage = new HashMap<String, String>();
	private final static String resourcesURL = "http://resources.download.minecraft.net/%1/%2";

	/**
	 * Add debug arguments to the launching client
	 * 
	 * @param suspend True to wait until the debugger is attached.
	 * @param port    The port to listen to
	 */
	public static void addDebugArguments(boolean suspend, int port) {
		String args = "";
		if (variableStorage.containsKey("mtk.append.arguments"))
			args = variableStorage.get("mtk.append.arguments");

		if (!args.isEmpty())
			args += ";";
		args += "-Xdebug;-Xnoagent;-Djava.compiler=NONE;-Xrunjdwp:transport=dt_socket,server=y,suspend="
				+ (suspend ? "y" : "n") + ",address=" + port;

		variableStorage.put("mtk.append.arguments", args);
	}

	/**
	 * Set a variable for rule parsing
	 * 
	 * @param name  Variable name
	 * @param value Variable value
	 */
	public static void putVariable(String name, String value) {
		variableStorage.put(name, value);
	}

	/**
	 * Add all values in a map to the rule variables
	 * 
	 * @param variables Map to add
	 */
	public static void putAllVariables(Map<String, String> variables) {
		variableStorage.putAll(variables);
	}

	/**
	 * Clear all rule variables
	 */
	public static void clearVariables() {
		if (variableStorage == null)
			variableStorage = new HashMap<String, String>();
		variableStorage.clear();
		variableStorage.put("launcher_name", "Cyan-MTK");
		variableStorage.put("launcher_version",
				(MinecraftToolkit.getVersion().contains("${") ? "unknown" : MinecraftToolkit.getVersion()));
		variableStorage.put("cyan_version", CyanInfo.getCyanVersion());
	}

	// Version file cache, so the toolkit won't have to download the version file
	// with each operation
	static HashMap<String, JsonObject> versionCache = new HashMap<String, JsonObject>();

	protected static void initComponent() {
		MinecraftToolkit.trace("INITIALIZE Minecraft Installation Toolkit, caller: " + CallTrace.traceCallName());

		clearVariables();

		try {
			if (minecraft_directory == null) {
				MinecraftToolkit.trace("SET Minecraft Installation directory to "
						+ new File("./Cyan-MTK").getCanonicalPath() + ", caller: " + CallTrace.traceCallName());
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
				+ CallTrace.traceCallName());
		if (initialized)
			MinecraftToolkit.warn(
					"The Minecraft installation directory was set after the toolkit was initialized, this is **bad** practice, caller: "
							+ CallTrace.traceCallName());
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
	 * Check if a version is available, if ignore_hash is false, the files are
	 * compared against the stored hashes.
	 * 
	 * @param version     The MinecraftVersionInfo object representing the version.
	 * @param checkAssets True to check asset files
	 * @return True if all check out, false otherwise.
	 */
	public static boolean checkIntallation(MinecraftVersionInfo version, boolean checkAssets) {
		return checkIntallation(version, checkAssets, false, false);
	}

	/**
	 * Check if a version is available, if ignore_hash is false, the files are
	 * compared against the stored hashes.
	 * 
	 * @param version     The MinecraftVersionInfo object representing the version.
	 * @param checkAssets True to check asset files
	 * @param ignore_hash Set to true to ignore hashes, only return false if files
	 *                    are missing.
	 * @return True if all check out, false otherwise.
	 */
	public static boolean checkIntallation(MinecraftVersionInfo version, boolean checkAssets, boolean ignore_hash) {
		return checkIntallation(version, checkAssets, ignore_hash, false);
	}

	/**
	 * Generate the JVM arguments for the client, please check the installation
	 * first with <code>checkVersion(version)</code> function, also make sure the
	 * natives have been extracted.
	 * 
	 * @param version The MinecraftVersionInfo object representing the version.
	 * @return Array of arguments
	 * @throws IOException If loading the natives or the manifest fails
	 */
	public static String[] generateJvmArguments(MinecraftVersionInfo version) throws IOException {
		return generateJvmArguments(version, null);
	}

	/**
	 * Generate the JVM arguments for the client, please check the installation
	 * first with <code>checkVersion(version)</code> function, also make sure the
	 * natives have been extracted.
	 * 
	 * @param version        The MinecraftVersionInfo object representing the
	 *                       version.
	 * @param extraArguments Extra JVM arguments
	 * @return Array of arguments
	 * @throws IOException If loading the natives or the manifest fails
	 */
	public static String[] generateJvmArguments(MinecraftVersionInfo version, ArrayList<String> extraArguments)
			throws IOException {
		return generateJvmArguments(version, extraArguments, getLibraries(version));
	}

	/**
	 * Generate the JVM arguments for the client, please check the installation
	 * first with <code>checkVersion(version)</code> function, also make sure the
	 * natives have been extracted.
	 * 
	 * @param version        The MinecraftVersionInfo object representing the
	 *                       version.
	 * @param extraArguments Extra JVM arguments
	 * @param libraries      The libraries to include
	 * @return Array of arguments
	 * @throws IOException If loading the natives or the manifest fails
	 */
	public static String[] generateJvmArguments(MinecraftVersionInfo version, ArrayList<String> extraArguments,
			File[] libraries) throws IOException {
		return generateJvmArguments(version, extraArguments, libraries, getNativesDirectory(version));
	}

	/**
	 * Generate the JVM arguments for the client, please check the installation
	 * first with <code>checkVersion(version)</code> function, also make sure the
	 * natives have been extracted.
	 * 
	 * @param version        The MinecraftVersionInfo object representing the
	 *                       version.
	 * @param extraArguments Extra JVM arguments
	 * @param libraries      The libraries to include
	 * @param nativesDir     The natives directory
	 * @return Array of arguments
	 * @throws IOException If loading the manifest fails
	 */
	public static String[] generateJvmArguments(MinecraftVersionInfo version, ArrayList<String> extraArguments,
			File[] libraries, File nativesDir) throws IOException {
		return generateJvmArguments(version, extraArguments, libraries, nativesDir,
				getVersionJar(version, GameSide.CLIENT));
	}

	/**
	 * Generate the JVM arguments for the client, please check the installation
	 * first with <code>checkVersion(version)</code> function, also make sure the
	 * natives have been extracted.
	 * 
	 * @param version        The MinecraftVersionInfo object representing the
	 *                       version.
	 * @param extraArguments Extra JVM arguments
	 * @param libraries      The libraries to include
	 * @param nativesDir     The natives directory
	 * @param mainJar        The main client jar
	 * @return Array of arguments
	 * @throws IOException If loading the manifest fails
	 */
	public static String[] generateJvmArguments(MinecraftVersionInfo version, ArrayList<String> extraArguments,
			File[] libraries, File nativesDir, File mainJar) throws IOException {
		return generateJvmArguments(version, extraArguments, libraries, nativesDir, mainJar, null);
	}

	/**
	 * Generate the JVM arguments for the client, please check the installation
	 * first with <code>checkVersion(version)</code> function, also make sure the
	 * natives have been extracted.
	 * 
	 * @param version        The MinecraftVersionInfo object representing the
	 *                       version.
	 * @param extraArguments Extra JVM arguments
	 * @param libraries      The libraries to include
	 * @param nativesDir     The natives directory
	 * @param mainJar        The main client jar
	 * @param logConf        The logging configuration path
	 * @return Array of arguments
	 * @throws IOException If loading the manifest fails
	 */
	public static String[] generateJvmArguments(MinecraftVersionInfo version, ArrayList<String> extraArguments,
			File[] libraries, File nativesDir, File mainJar, String logConf) throws IOException {
		return generateJvmArguments(version, extraArguments, libraries, nativesDir, mainJar, logConf, true);
	}

	/**
	 * Generate the JVM arguments for the client, please check the installation
	 * first with <code>checkVersion(version)</code> function, also make sure the
	 * natives have been extracted.
	 * 
	 * @param version        The MinecraftVersionInfo object representing the
	 *                       version.
	 * @param extraArguments Extra JVM arguments
	 * @param libraries      The libraries to include
	 * @param nativesDir     The natives directory
	 * @param mainJar        The main client jar
	 * @param logConf        The logging configuration path
	 * @param addCp          True to add the classpath, false otherwise
	 * @return Array of arguments
	 * @throws IOException If loading the manifest fails
	 */
	public static String[] generateJvmArguments(MinecraftVersionInfo version, ArrayList<String> extraArguments,
			File[] libraries, File nativesDir, File mainJar, String logConf, boolean addCp) throws IOException {
		return generateJvmArguments(version, extraArguments, libraries, nativesDir, mainJar, getAssetsRoot(),
				getAssetId(version), logConf, addCp);
	}

	/**
	 * Generate the JVM arguments for the client, please check the installation
	 * first with <code>checkVersion(version)</code> function, also make sure the
	 * natives have been extracted.
	 * 
	 * @param version        The MinecraftVersionInfo object representing the
	 *                       version.
	 * @param extraArguments Extra JVM arguments
	 * @param libraries      The libraries to include
	 * @param nativesDir     The natives directory
	 * @param mainJar        The main client jar
	 * @param assetDir       The asset root directory
	 * @param assetsIndex    The asset index id
	 * @param logConf        The logging configuration path
	 * @param addCp          True to add the classpath, false otherwise
	 * @return Array of arguments
	 * @throws IOException If loading the manifest fails
	 */
	public static String[] generateJvmArguments(MinecraftVersionInfo version, ArrayList<String> extraArguments,
			File[] libraries, File nativesDir, File mainJar, File assetDir, String assetsIndex, String logConf,
			boolean addCp) throws IOException {
		ArrayList<String> arguments = new ArrayList<String>();
		String cp = "";
		HashMap<String, String> keys = new HashMap<String, String>();
		keys.putAll(variableStorage);

		if (addCp) {
			for (File lib : libraries) {
				if (cp.isEmpty())
					cp = lib.getCanonicalPath();
				else
					cp += ":" + lib.getCanonicalPath();
			}
			if (keys.containsKey("mtk.append.classpath")) {
				for (String lib : Splitter.split(keys.get("mtk.append.classpath"), ':')) {
					if (cp.isEmpty())
						cp = lib;
					else
						cp += ":" + lib;
				}
			}
		}
		if (keys.containsKey("mtk.append.arguments")) {
			if (extraArguments == null)
				extraArguments = new ArrayList<String>();
			for (String arg : Splitter.split(keys.get("mtk.append.arguments"), ';')) {
				extraArguments.add(arg);
			}
		}
		if (addCp) {
			if (cp.isEmpty())
				cp = mainJar.getCanonicalPath();
			else
				cp += ":" + mainJar.getCanonicalPath();
		}

		keys.put("version_name", version.getVersion());
		keys.put("version_type", version.getVersionType().toString().toLowerCase());
		keys.put("assets_root", assetDir.getCanonicalPath());
		keys.put("assets_index_name", assetsIndex);
		if (addCp) {
			keys.put("classpath", cp);
		}
		keys.put("natives_directory", nativesDir.getCanonicalPath());

		JsonObject manifest = getVersionManifest(version).deepCopy();

		info("Generating " + version.getVersion() + " JVM arguments...");
		Gson gson = new Gson();
		recurseInheritsFrom(manifest, manifest, gson);

		for (JsonElement element : manifest.get("arguments").getAsJsonObject().get("jvm").getAsJsonArray()) {
			if (element.isJsonPrimitive()) {
				debug("Adding argument: " + element.getAsString());
				arguments.add(element.getAsString());
			} else {
				JsonObject obj = element.getAsJsonObject();
				String[] values = new String[0];
				if (obj.has("value")) {
					JsonElement e = obj.get("value");
					if (e.isJsonArray()) {
						values = new String[e.getAsJsonArray().size()];
						int i = 0;
						for (JsonElement e2 : e.getAsJsonArray()) {
							values[i++] = e2.getAsString();
						}
					} else if (e.isJsonPrimitive())
						values = new String[] { e.getAsString() };
				}
				boolean allow = true;
				if (obj.has("rules")) {
					for (JsonElement r : obj.get("rules").getAsJsonArray()) {
						JsonObject rule = r.getAsJsonObject();
						Map<?, ?> rules = gson.fromJson(rule, Map.class);
						allow = evaluate(rules, keys, gson, null, "allow");
					}
				}
				if (allow) {
					for (String str : values) {
						arguments.add(str);
					}
				}
			}
		}

		if (manifest.has("logging")) {
			if (manifest.get("logging").getAsJsonObject().has("client")) {
				JsonObject logInfo = manifest.get("logging").getAsJsonObject().get("client").getAsJsonObject();
				debug("Loading logging information...");
				String path = null;
				String argument = "-Dlog4j.configurationFile=${path}";
				if (logInfo.has("argument")) {
					argument = logInfo.get("argument").getAsString();
				}
				String mode = "log4j2-xml";
				if (logInfo.has("type")) {
					mode = logInfo.get("type").getAsString();
				}

				JsonObject fileInfo = null;
				if (logInfo.has("file")) {
					fileInfo = logInfo.get("file").getAsJsonObject();
					String sha1 = fileInfo.get("sha1").getAsString();
					String url = fileInfo.get("url").getAsString();
					long size = fileInfo.get("size").getAsLong();

					File logFile = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
							"caches/assets/logging/" + fileInfo.get("id").getAsString());

					if (!logFile.getParentFile().exists()) {
						logFile.getParentFile().mkdirs();
					}

					try {
						if (logFile.exists() && !sha1HEX(Files.readAllBytes(logFile.toPath())).equals(sha1)) {
							logFile.delete();
						}
					} catch (NoSuchAlgorithmException | IOException e) {
						logFile.delete();
					}
					if (!logFile.exists()) {
						if (!url.equals("")) {
							info("Downloading logging context...");
							try {
								MinecraftInstallationToolkit.Download(logFile, new URL(url), size, sha1, false, false,
										true, true);
							} catch (NoSuchAlgorithmException e) {
							}
						}
					}

					if (logFile.exists())
						path = logFile.getCanonicalPath();
				}

				if (mode.equals("log4j2-xml") || mode.equals("log4j-xml") || fileInfo == null || path == null) {
					path = MinecraftInstallationToolkit.class.getResource("/log4j2" + (ide ? "-ide" : "-game") + ".xml")
							.toString();
				}

				if (logConf != null) {
					path = logConf;
				}

				argument = argument.replace("${path}", path);
				arguments.add(argument);
			}
		}

		if (extraArguments != null) {
			for (String argument : extraArguments) {
				debug("Adding argument: " + argument);
				arguments.add(argument);
			}
		}

		String[] args = new String[arguments.size() - (addCp ? 0 : 2)];
		int ind = 0;
		boolean skip = false;
		for (String argument : arguments) {
			if (!addCp && argument.equals("-cp") || argument.equals("-classpath") || skip) {
				if ((argument.equals("-cp") || argument.equals("-classpath")) && !skip)
					skip = true;
				else
					skip = false;

				continue;
			}
			for (String key : keys.keySet()) {
				argument = argument.replace("${" + key + "}", keys.get(key));
			}
			debug("Processing argument: " + argument);
			args[ind++] = argument;
		}

		return args;
	}

	/**
	 * Get the main class of a version, please check the installation first with
	 * <code>checkVersion(version)</code>
	 * 
	 * @param version The MinecraftVersionInfo object representing the version.
	 * @return Main class name
	 */
	public static String getMainClass(MinecraftVersionInfo version) {
		JsonObject manifest;
		try {
			manifest = getVersionManifest(version).deepCopy();
		} catch (IOException e) {
			return null;
		}
		Gson gson = new Gson();
		recurseInheritsFrom(manifest, manifest, gson);
		return manifest.get("mainClass").getAsString();
	}

	/**
	 * Generate the java program arguments for the client, please check the
	 * installation first with <code>checkVersion(version)</code> function, also
	 * make sure the natives have been extracted.
	 * 
	 * @param version       The MinecraftVersionInfo object representing the
	 *                      version.
	 * @param account       The account to use
	 * @param gameDirectory Game directory
	 * @return Array of arguments
	 * @throws IOException If loading the manifest fails
	 */
	public static String[] generateGameArguments(MinecraftVersionInfo version, AuthenticationInfo account,
			File gameDirectory) throws IOException {
		return generateGameArguments(version, null, account.getPlayerName(), account.getUUID(),
				account.getAccessToken(), account.getAccountType(), gameDirectory);
	}

	/**
	 * Get the JVM path
	 * 
	 * @return Java executable file
	 */
	public static File getJVM() {
		if (variableStorage.containsKey("jvm.executable"))
			return new File(variableStorage.get("jvm.executable"));
		else
			return new File(ProcessHandle.current().info().command().get());
	}

	/**
	 * Generate the java program arguments for the client, please check the
	 * installation first with <code>checkVersion(version)</code> function, also
	 * make sure the natives have been extracted.
	 * 
	 * @param version        The MinecraftVersionInfo object representing the
	 *                       version.
	 * @param extraArguments Extra JVM arguments
	 * @param account        The account to use
	 * @param gameDirectory  Game directory
	 * @return Array of arguments
	 * @throws IOException If loading the manifest fails
	 */
	public static String[] generateGameArguments(MinecraftVersionInfo version, ArrayList<String> extraArguments,
			AuthenticationInfo account, File gameDirectory) throws IOException {
		return generateGameArguments(version, extraArguments, account.getPlayerName(), account.getUUID(),
				account.getAccessToken(), account.getAccountType(), gameDirectory);
	}

	/**
	 * Generate the java program arguments for the client, please check the
	 * installation first with <code>checkVersion(version)</code> function, also
	 * make sure the natives have been extracted.
	 * 
	 * @param version        The MinecraftVersionInfo object representing the
	 *                       version.
	 * @param extraArguments Extra JVM arguments
	 * @param playerName     Authenticated player name
	 * @param playerUUID     Authenticated player UUID
	 * @param accessToken    Access token
	 * @param userType       Account type
	 * @param gameDirectory  Game directory
	 * @return Array of arguments
	 * @throws IOException If loading the manifest fails
	 */
	public static String[] generateGameArguments(MinecraftVersionInfo version, ArrayList<String> extraArguments,
			String playerName, UUID playerUUID, String accessToken, MinecraftAccountType userType, File gameDirectory)
			throws IOException {
		return generateGameArguments(version, extraArguments, playerName, playerUUID, accessToken, userType,
				gameDirectory, getAssetsRoot(), getAssetId(version));
	}

	/**
	 * Generate the java program arguments for the client, please check the
	 * installation first with <code>checkVersion(version)</code> function, also
	 * make sure the natives have been extracted.
	 * 
	 * @param version        The MinecraftVersionInfo object representing the
	 *                       version.
	 * @param extraArguments Extra JVM arguments
	 * @param playerName     Authenticated player name
	 * @param playerUUID     Authenticated player UUID
	 * @param accessToken    Access token
	 * @param userType       Account type
	 * @param gameDirectory  Game directory
	 * @param assetDir       The asset root directory
	 * @param assetsIndex    The asset index id
	 * @return Array of arguments
	 * @throws IOException If loading the manifest fails
	 */
	public static String[] generateGameArguments(MinecraftVersionInfo version, ArrayList<String> extraArguments,
			String playerName, UUID playerUUID, String accessToken, MinecraftAccountType userType, File gameDirectory,
			File assetDir, String assetsIndex) throws IOException {
		ArrayList<String> arguments = new ArrayList<String>();

		HashMap<String, String> keys = new HashMap<String, String>();
		keys.putAll(variableStorage);

		keys.put("auth_player_name", playerName);
		keys.put("auth_access_token", accessToken);
		keys.put("auth_uuid", playerUUID.toString().replaceAll("-", ""));
		keys.put("user_type", userType.toString().toLowerCase());

		keys.put("game_directory", gameDirectory.getCanonicalPath());

		keys.put("version_name", version.getVersion());
		keys.put("version_type", version.getVersionType().toString().toLowerCase());
		keys.put("assets_root", assetDir.getCanonicalPath());
		keys.put("assets_index_name", assetsIndex);

		JsonObject manifest = getVersionManifest(version).deepCopy();

		info("Generating " + version.getVersion() + " JVM arguments...");
		Gson gson = new Gson();
		recurseInheritsFrom(manifest, manifest, gson);

		for (JsonElement element : manifest.get("arguments").getAsJsonObject().get("game").getAsJsonArray()) {
			if (element.isJsonPrimitive()) {
				debug("Adding argument: " + element.getAsString());
				arguments.add(element.getAsString());
			} else {
				JsonObject obj = element.getAsJsonObject();
				String[] values = new String[0];
				if (obj.has("value")) {
					JsonElement e = obj.get("value");
					if (e.isJsonArray()) {
						values = new String[e.getAsJsonArray().size()];
						int i = 0;
						for (JsonElement e2 : e.getAsJsonArray()) {
							values[i++] = e2.getAsString();
						}
					} else if (e.isJsonPrimitive())
						values = new String[] { e.getAsString() };
				}
				boolean allow = true;
				if (obj.has("rules")) {
					for (JsonElement r : obj.get("rules").getAsJsonArray()) {
						JsonObject rule = r.getAsJsonObject();
						Map<?, ?> rules = gson.fromJson(rule, Map.class);
						allow = evaluate(rules, keys, gson, null, "allow");
					}
				}
				if (allow) {
					for (String str : values) {
						arguments.add(str);
					}
				}
			}
		}

		if (extraArguments != null) {
			for (String argument : extraArguments) {
				debug("Adding argument: " + argument.replace(accessToken, "*************"));
				arguments.add(argument);
			}
		}

		String[] args = new String[arguments.size()];
		int ind = 0;
		for (String argument : arguments) {
			for (String key : keys.keySet()) {
				argument = argument.replace("${" + key + "}", keys.get(key));
			}
			debug("Processing argument: " + argument.replace(accessToken, "*************"));
			args[ind++] = argument;
		}

		return args;
	}

	/**
	 * Check if a version is available, if ignore_hash is false, the files are
	 * compared against the stored hashes.
	 * 
	 * @param version            The MinecraftVersionInfo object representing the
	 *                           version.
	 * @param checkAssets        True to check asset files
	 * @param ignore_hash        Set to true to ignore hashes, only return false if
	 *                           files are missing.
	 * @param ignore_nonexistent Set to true ignore missing files, set to false to
	 *                           make sure all files exist.
	 * @return True if all check out, false otherwise.
	 */
	public static boolean checkIntallation(MinecraftVersionInfo version, boolean checkAssets, boolean ignore_hash,
			boolean ignore_nonexistent) {
		return checkIntallation(version, checkAssets, ignore_hash, ignore_nonexistent, false);
	}

	/**
	 * Check if a version is available, if ignore_hash is false, the files are
	 * compared against the stored hashes.
	 * 
	 * @param version            The MinecraftVersionInfo object representing the
	 *                           version.
	 * @param checkAssets        True to check asset files
	 * @param ignore_hash        Set to true to ignore hashes, only return false if
	 *                           files are missing.
	 * @param ignore_nonexistent Set to true ignore missing files, set to false to
	 *                           make sure all files exist.
	 * @param removeRift         True to remove rift identifiers (remapped jars, use
	 *                           this for deobfuscated environments)
	 * @return True if all check out, false otherwise.
	 */
	public static boolean checkIntallation(MinecraftVersionInfo version, boolean checkAssets, boolean ignore_hash,
			boolean ignore_nonexistent, boolean removeRift) {
		return checkVersionMissingFiles(version, checkAssets, ignore_hash, ignore_nonexistent, removeRift) == 0;
	}

	@SuppressWarnings("unchecked")
	private static int checkVersionMissingFiles(MinecraftVersionInfo version, boolean checkAssets, boolean ignore_hash,
			boolean ignore_nonexistent, boolean removeRift) {
		JsonObject manifest;
		int incorrect = 0;
		try {
			manifest = getVersionManifest(version).deepCopy();
		} catch (IOException e) {
			return -1;
		}

		info("Validating version " + version.getVersion() + "...");
		File f3 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/libraries");
		File f4 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
				"caches/natives/" + version.getVersion());
		File f5 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/assets/indexes");
		File f6 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/assets/objects");

		if (!ignore_nonexistent) {
			if (!f3.exists())
				incorrect++;
			if (!f4.exists())
				incorrect++;
			if (!f5.exists())
				incorrect++;
			if (!f6.exists())
				incorrect++;
		}

		Gson gson = new Gson();
		recurseInheritsFrom(manifest, manifest, gson);

		if (checkAssets) {
			info("Validating assets...");
			JsonObject assetIndexObj = manifest.get("assetIndex").getAsJsonObject();
			String assetId = assetIndexObj.get("id").getAsString();
			String assetIndexSha = assetIndexObj.get("sha1").getAsString();
			File assetIndex = new File(f5, assetId + ".json");
			if (!fileCheck(assetIndex, assetIndexSha, ignore_hash, ignore_nonexistent)) {
				debug("File " + assetIndex.getName() + " did not pass validation.");
				incorrect++;
			} else {
				try {
					JsonObject assets = JsonParser.parseString(Files.readString(assetIndex.toPath())).getAsJsonObject();
					Map<String, Map<String, ?>> assetMap = gson.fromJson(assets.get("objects"), Map.class);
					for (Map<String, ?> val : assetMap.values()) {
						String hash = val.get("hash").toString();
						File output = new File(f6, hash.substring(0, 2) + "/" + hash);
						if (!fileCheck(output, hash, ignore_hash, ignore_nonexistent)) {
							debug("File " + output.getName() + " did not pass validation.");
							incorrect++;
						}
					}
				} catch (JsonSyntaxException | IOException e1) {
					debug("File " + assetIndex.getName()
							+ " did not pass validation as an error has occurred parsing it: " + e1.getMessage() + ".");
					incorrect++;
				}
			}
		}

		MinecraftToolkit.info("Validating version libraries...");
		for (JsonElement o : manifest.get("libraries").getAsJsonArray()) {
			JsonObject obj = o.getAsJsonObject();
			boolean allow = true;
			if (obj.has("rules")) {
				for (JsonElement r : obj.get("rules").getAsJsonArray()) {
					JsonObject rule = r.getAsJsonObject();
					Map<?, ?> rules = gson.fromJson(rule, Map.class);
					allow = evaluate(rules, variableStorage, gson, null, "allow");
				}
			}
			if (!allow)
				continue;

			ArrayList<String> downloadNativesList = new ArrayList<String>();
			if (obj.has("natives")) {
				Map<?, ?> natives = gson.fromJson(obj.get("natives"), Map.class);
				if (natives.containsKey(OsInfo.getCurrent().toString())) {
					String arch = System.getProperty("os.arch").toLowerCase();
					if (arch.equals("i386"))
						arch = "32";
					else if (arch.equals("x86") || arch.equals("amd64"))
						arch = "64";

					String nativesId = natives.get(OsInfo.getCurrent().toString()).toString()
							.replaceAll("\\$\\{arch\\}", arch);
					downloadNativesList.add(nativesId);

					if (gson.fromJson(obj.get("downloads").getAsJsonObject().get("classifiers"), Map.class)
							.containsKey("javadoc"))
						downloadNativesList.add("javadoc");

					if (gson.fromJson(obj.get("downloads").getAsJsonObject().get("classifiers"), Map.class)
							.containsKey("sources"))
						downloadNativesList.add("sources");
				}
			}
			if (obj.has("downloads")) {
				Map<String, Map<?, ?>> m = gson.fromJson(obj.get("downloads"), Map.class);
				for (String k : m.keySet()) {
					if (k.equals("artifact")) {
						Map<?, ?> object = m.get(k);
						String sha1 = object.get("sha1").toString();
						String path = object.get("path").toString();
						File f = new File(f3, path);
						if (!fileCheck(f, sha1, ignore_hash, ignore_nonexistent)) {
							debug("File " + f.getName() + " did not pass validation.");
							incorrect++;
						}
					} else if (k.equals("classifiers")) {
						Map<?, ?> ob = m.get(k);
						for (Object k2 : ob.keySet()) {
							Map<?, ?> object = (Map<?, ?>) ob.get(k2);
							String sha1 = object.get("sha1").toString();
							String path = object.get("path").toString();
							if (downloadNativesList.size() != 0) {
								if (!downloadNativesList.contains(k2.toString()))
									continue;
							}

							File f = new File(f3, path);
							File nativeOut = new File(f4, f.getName());
							if (!fileCheck(f, sha1, ignore_hash, ignore_nonexistent)) {
								debug("File " + f.getName() + " did not pass validation.");
								incorrect++;
							}
							if (!fileCheck(nativeOut, sha1, ignore_hash, ignore_nonexistent)) {
								debug("File " + f.getName() + " did not pass validation.");
								incorrect++;
							}
						}
					}
				}
			} else if (obj.has("name")) {
				String urlRoot = "https://libraries.minecraft.net/"; // someday, it might change
				String path = "";
				String[] info = obj.get("name").getAsString().split(":");

				String libname = info[1];
				String libversion = info[2];
				if (removeRift) {
					if (libversion.contains("-RIFT"))
						libversion = libversion.substring(0, libversion.indexOf("-RIFT"));
					if (libname.contains("-RIFT"))
						libname = libname.substring(0, libname.indexOf("-RIFT"));
					String grp = info[0];
					info = new String[] { grp, libname, libversion };
				}

				if (info.length == 3) {
					path = info[0].replaceAll("\\.", "/") + "/" + info[1] + "/" + info[2] + "/" + info[1] + "-"
							+ info[2] + ".jar";
				} else if (info.length == 2) {
					path = info[0] + "/" + info[1] + "/" + info[0] + "-" + info[1] + ".jar";
				} else if (info.length == 1) {
					path = info[0] + "/" + info[0] + ".jar";
				}
				if (obj.has("url")) {
					urlRoot = obj.get("url").getAsString();
				}
				if (!urlRoot.endsWith("/"))
					urlRoot += "/";
				String download = urlRoot + path;

				if (!path.equals("")) {
					String sha1 = null;
					try {
						InputStream strm = new URL(download + ".sha1").openStream();
						sha1 = new String(strm.readAllBytes());
						strm.close();
					} catch (IOException e) {
						warn("Failed to download hash for file " + path + ", must assume it is good...");
					}
					File f = new File(f3, path);
					if (!fileCheck(f, sha1, ignore_hash, ignore_nonexistent)) {
						debug("File " + f.getName() + " did not pass validation.");
						incorrect++;
					}
				}
			}
		}

		if (incorrect == 0)
			info("All files validated successfully.");
		else
			info("Validation output: " + incorrect + " or more missing files.");

		return incorrect;
	}

	/**
	 * Get the array of library files needed to run the game, NOTE: does NOT add the
	 * version jar
	 * 
	 * @param version The MinecraftVersionInfo object representing the version.
	 *                (might not have all installed, use checkVersion to verify)
	 * @return Array of library files
	 */
	public static File[] getLibraries(MinecraftVersionInfo version) {
		return getLibraries(version, false);
	}

	/**
	 * Get the array of library files needed to run the game, NOTE: does NOT add the
	 * version jar
	 * 
	 * @param version    The MinecraftVersionInfo object representing the version.
	 *                   (might not have all installed, use checkVersion to verify)
	 * @param removeRift True to remove rift identifiers (remapped jars, use this
	 *                   for deobfuscated environments)
	 * @return Array of library files
	 */
	@SuppressWarnings("unchecked")
	public static File[] getLibraries(MinecraftVersionInfo version, boolean removeRift) {
		ArrayList<File> libraries = new ArrayList<File>();
		File f3 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/libraries");

		JsonObject manifest;
		try {
			manifest = getVersionManifest(version).deepCopy();
		} catch (IOException e) {
			return new File[0];
		}

		Gson gson = new Gson();
		recurseInheritsFrom(manifest, manifest, gson);
		MinecraftToolkit.info("Resolving version libraries...");
		for (JsonElement o : manifest.get("libraries").getAsJsonArray()) {
			JsonObject obj = o.getAsJsonObject();
			String name = obj.get("name").getAsString();
			boolean allow = true;
			if (obj.has("rules")) {
				for (JsonElement r : obj.get("rules").getAsJsonArray()) {
					JsonObject rule = r.getAsJsonObject();
					Map<?, ?> rules = gson.fromJson(rule, Map.class);
					allow = evaluate(rules, variableStorage, gson, null, "allow");
				}
			}
			if (!allow)
				continue;
			if (obj.has("downloads")) {
				Map<String, Map<?, ?>> m = gson.fromJson(obj.get("downloads"), Map.class);
				for (String k : m.keySet()) {
					if (k.equals("artifact")) {
						Map<?, ?> object = m.get(k);
						String path = object.get("path").toString();
						libraries.add(new File(f3, path));
					} else if (k.equals("classifiers")) {
					} else {
						MinecraftToolkit.warn("No implementation for downloads." + k
								+ "! Unable to parse it, name of library: " + name + ".");
					}
				}
			} else if (obj.has("name")) {
				String path = "";
				String[] info = obj.get("name").getAsString().split(":");
				String libname = info[1];
				String libversion = info[2];
				if (removeRift) {
					if (libversion.contains("-RIFT"))
						libversion = libversion.substring(0, libversion.indexOf("-RIFT"));
					if (libname.contains("-RIFT"))
						libname = libname.substring(0, libname.indexOf("-RIFT"));
					String grp = info[0];
					info = new String[] { grp, libname, libversion };
				}

				if (info.length == 3) {
					path = info[0].replaceAll("\\.", "/") + "/" + info[1] + "/" + info[2] + "/" + info[1] + "-"
							+ info[2] + ".jar";
				} else if (info.length == 2) {
					path = info[0] + "/" + info[1] + "/" + info[0] + "-" + info[1] + ".jar";
				} else if (info.length == 1) {
					path = info[0] + "/" + info[0] + ".jar";
				}

				libraries.add(new File(f3, path));
			}
			for (String key : obj.keySet()) {
				if (key.equals("downloads") || key.equals("url") || key.equals("name") || key.equals("rules")
						|| key.equals("natives") || key.equals("extract"))
					continue;

				MinecraftToolkit.warn(
						"No implementation for " + key + ".*! Unable to parse it, name of library: " + name + ".");
			}
		}

		return libraries.toArray(t -> new File[t]);
	}

	/**
	 * Get the array of library files needed to run the game in maven format, NOTE:
	 * does NOT add the version jar
	 * 
	 * @param version    The MinecraftVersionInfo object representing the version.
	 *                   (might not have all installed, use checkVersion to verify)
	 * @param removeRift True to remove rift identifiers (remapped jars, use this
	 *                   for deobfuscated environments)
	 * @return Array of library files
	 */
	public static String[] getLibrariesMavenFormat(MinecraftVersionInfo version, boolean removeRift) {
		ArrayList<String> libraries = new ArrayList<String>();

		JsonObject manifest;
		try {
			manifest = getVersionManifest(version).deepCopy();
		} catch (IOException e) {
			return new String[0];
		}

		Gson gson = new Gson();
		recurseInheritsFrom(manifest, manifest, gson);
		MinecraftToolkit.info("Resolving version libraries...");
		for (JsonElement o : manifest.get("libraries").getAsJsonArray()) {
			JsonObject obj = o.getAsJsonObject();
			String name = obj.get("name").getAsString();
			boolean allow = true;
			if (obj.has("rules")) {
				for (JsonElement r : obj.get("rules").getAsJsonArray()) {
					JsonObject rule = r.getAsJsonObject();
					Map<?, ?> rules = gson.fromJson(rule, Map.class);
					allow = evaluate(rules, variableStorage, gson, null, "allow");
				}
			}
			if (!allow)
				continue;
			if (obj.has("name")) {
				String group = "";
				String libname = "";
				String libversion = "";

				String[] info = obj.get("name").getAsString().split(":");
				if (info.length == 3) {
					group = info[0];
					libname = info[1];
					libversion = info[2];
				} else if (info.length == 2) {
					libname = info[0];
					libversion = info[1];
				} else if (info.length == 1) {
					libname = info[0];
				}

				if (removeRift) {
					if (libversion.contains("-RIFT"))
						libversion = libversion.substring(0, libversion.indexOf("-RIFT"));
					if (libname.contains("-RIFT"))
						libname = libname.substring(0, libname.indexOf("-RIFT"));
				}

				libraries.add(group + ":" + libname + ":" + libversion);
			}
			for (String key : obj.keySet()) {
				if (key.equals("downloads") || key.equals("url") || key.equals("name") || key.equals("rules")
						|| key.equals("natives") || key.equals("extract"))
					continue;

				MinecraftToolkit.warn(
						"No implementation for " + key + ".*! Unable to parse it, name of library: " + name + ".");
			}
		}

		return libraries.toArray(t -> new String[t]);
	}

	/**
	 * Extract the natives, run after validating and before each time you start the
	 * game
	 * 
	 * @param version MinecraftVersionInfo object representing the version.
	 * @throws IOException If extracting fails
	 */
	public static void extractNatives(MinecraftVersionInfo version) throws IOException {
		File natives = getNativesDirectory(version);
		info("Extracting natives for version " + version + "...");
		for (File jar : natives.listFiles(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				return arg0.getName().endsWith(".jar") && !arg0.getName().endsWith("-sources.jar")
						&& !arg0.getName().endsWith("-javadoc.jar");
			}

		})) {
			ZipFile file = new ZipFile(jar);
			Enumeration<? extends ZipEntry> entries = file.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.isDirectory())
					continue;

				String path = entry.getName().replaceAll("\\\\", "/");
				if (path.startsWith("META-INF/") || path.endsWith(".git") || path.endsWith(".sha1")
						|| path.endsWith(".md5"))
					continue;

				File output = new File(natives, path);
				if (output.exists())
					output.delete();

				FileOutputStream outstrm = new FileOutputStream(output);
				InputStream strm = file.getInputStream(entry);
				strm.transferTo(outstrm);
				strm.close();
				outstrm.close();

				info(jar.getName() + " -> " + output.getName());
			}
			file.close();
		}
	}

	/**
	 * Get the assets root directory
	 * 
	 * @return File object representing the assets root directory
	 */
	public static File getAssetsRoot() {
		return new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/assets");
	}

	/**
	 * Get the asset index file that belongs to a specific minecraft version
	 * 
	 * @param version MinecraftVersionInfo object representing the version.
	 * @return Asset id as string
	 * @throws IOException If the manifest does not exist and cannot be downloaded
	 */
	public static String getAssetId(MinecraftVersionInfo version) throws IOException {
		JsonObject versionJson = getVersionManifest(version).deepCopy();
		recurseInheritsFrom(versionJson, versionJson, new Gson());
		JsonObject assetIndexObj = versionJson.get("assetIndex").getAsJsonObject();
		String assetId = assetIndexObj.get("id").getAsString();
		return assetId;
	}

	/**
	 * Get the asset index file that belongs to a specific minecraft version
	 * 
	 * @return File object representing the index file
	 * @throws IOException If the file cannot be found or if the version manifest is
	 *                     not saved and cannot be downloaded
	 */
	public static File getAssetIndex(MinecraftVersionInfo version) throws IOException {
		File f5 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/assets/indexes");
		File assetIndex = new File(f5, getAssetId(version) + ".json");
		if (!assetIndex.exists())
			throw new IOException("Asset index file for version " + version.getVersion() + " does not exist.");

		return assetIndex;
	}

	/**
	 * Get the natives folder for a minecraft version
	 * 
	 * @param version The MinecraftVersionInfo object representing the version.
	 * @return File object representing the version natives directory
	 * @throws IOException If the natives folder does not exist
	 */
	public static File getNativesDirectory(MinecraftVersionInfo version) throws IOException {
		File f4 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
				"caches/natives/" + version.getVersion());
		if (!f4.exists())
			throw new IOException("Natives folder for version " + version.getVersion() + " does not exist.");
		return f4;
	}

	/**
	 * Get the jar of a version and side
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return File object if existing, null if the file was not found
	 */
	public static File getVersionJar(MinecraftVersionInfo version, GameSide side) {
		trace("CREATE jars directory IF NONEXISTENT, caller: " + CallTrace.traceCallName());
		File jarDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/jars");
		if (!jarDir.exists())
			jarDir.mkdirs();
		File output = new File(jarDir, side.toString().toLowerCase() + "-" + version.getVersion() + ".jar");
		if (output.exists()) {
			return output;
		} else
			return null;
	}

	/**
	 * Download a version jar, or return it if already downloaded
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return Newly downloaded file, or existing if already downloaded before
	 * @throws IOException If downloading fails.
	 */
	public static File downloadVersionJar(MinecraftVersionInfo version, GameSide side) throws IOException {
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
	public static File downloadVersionJar(MinecraftVersionInfo version, GameSide side, boolean overwrite)
			throws IOException {
		trace("CREATE jars directory IF NONEXISTENT, caller: " + CallTrace.traceCallName());
		File jarDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/jars");
		if (!jarDir.exists())
			jarDir.mkdirs();

		File output = new File(jarDir, side.toString().toLowerCase() + "-" + version.getVersion() + ".jar");
		if (output.exists() && !overwrite) {
			return output;
		} else if (output.exists()) {
			output.delete();
		}

		downloadVersionManifest(version);

		info("Resolving " + side + " jar of minecraft version " + version.getVersion() + "...");
		JsonObject manifest = versionCache.get(version.getVersion()).deepCopy();
		recurseInheritsFrom(manifest, manifest, new Gson());
		URL u = new URL(manifest.get("downloads").getAsJsonObject().get(side.toString().toLowerCase()).getAsJsonObject()
				.get("url").getAsString());

		info("Downloading " + version.getVersion() + " into " + output.getName() + "...");
		trace("DOWNLOAD " + side + " jar of minecraft version " + version.getVersion() + " from " + u + " into "
				+ output.getCanonicalPath() + ", caller: " + CallTrace.traceCallName());
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
	 * @throws IOException If the manifest does not exist and cannot be downloaded
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

		trace("CREATE manifests directory IF NONEXISTENT, caller: " + CallTrace.traceCallName());
		File manifestDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/manifests");
		if (!manifestDir.exists())
			manifestDir.mkdirs();

		File versionFile = new File(manifestDir, version.getVersion() + ".json");
		trace("SAVE version manifest of Minecraft version " + version.getVersion() + " into "
				+ versionFile.getCanonicalPath() + ", caller: " + CallTrace.traceCallName());
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
		trace("OPEN server connection, CREATE URL STREAM, caller: " + CallTrace.traceCallName());
		InputStreamReader reader = new InputStreamReader(u.openStream());
		info("Parsing manifest to json...");
		trace("PARSE json from stream, caller: " + CallTrace.traceCallName());
		JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
		trace("CLOSE server stream, caller: " + CallTrace.traceCallName());
		reader.close();

		versionCache.put(version.getVersion(), json);
	}

	/**
	 * Download a Minecraft version and its libraries.
	 * 
	 * @param version        MinecraftVersionInfo object representing the version.
	 * @param downloadAssets True to download asset files
	 * @throws IOException If downloading fails
	 */
	public static void downloadVersionFiles(MinecraftVersionInfo version, boolean downloadAssets) throws IOException {
		downloadVersionFiles(version, downloadAssets, true, true, false);
	}

	/**
	 * Download a Minecraft version and its libraries.
	 * 
	 * @param version        MinecraftVersionInfo object representing the version.
	 * @param downloadAssets True to download asset files
	 * @param overwrite      True to overwrite existing files, false otherwise.
	 * @throws IOException If downloading fails
	 */
	public static void downloadVersionFiles(MinecraftVersionInfo version, boolean downloadAssets, boolean overwrite)
			throws IOException {
		downloadVersionFiles(version, downloadAssets, overwrite, true, false);
	}

	public static enum OsInfo {
		windows, linux, osx, other;

		public static OsInfo getCurrent() {
			String info = System.getProperty("os.name").toLowerCase();
			if (info.startsWith("windows"))
				return windows;
			else if (info.startsWith("mac") || info.startsWith("osx") // Just in case
					|| info.startsWith("darwin"))
				return osx;
			else if (info.startsWith("linux"))
				return linux;
			else
				return other;
		}
	}

	static boolean evaluate(Map<?, ?> rules, HashMap<String, String> variables, Gson gson, String last, String action) {
		for (Object keyObj : rules.keySet()) {
			String key = keyObj.toString();
			Object value = rules.get(keyObj);
			if (key.equals("action")) {
				action = value.toString();
			} else {
				if (value instanceof Map) {
					return evaluate((Map<?, ?>) value, variables, gson, (last != null ? last + "." : "") + key, action);
				} else {
					String prop = (last != null ? last + "." : "") + key;
					boolean output = true;

					if (variables.containsKey(prop)) {
						output = variables.get(prop).equals(value.toString());
					} else {
						if (System.getProperty(prop) == null)
							output = false;
						if (prop.equals("os.name"))
							output = OsInfo.getCurrent().toString().equals(value.toString());
						else if (prop.equals("os.arch")) {
							String arch = System.getProperty("os.arch").toLowerCase();

							switch (value.toString()) {
							case "i386":
								output = arch.equals("i386");
								break;
							case "x86":
								output = arch.equals("x86") || arch.equals("amd64");
								break;
							default:
								output = arch.matches(value.toString());
								break;
							}
						} else if (System.getProperty(prop) != null) {
							output = System.getProperty(prop).equals(value.toString());
						}
					}

					if (!(action.equals("allow") ? output : !output))
						return false;
				}
			}
		}
		return true;
	}

	/**
	 * Launches the Minecraft client, please validate the installation before
	 * calling.
	 * 
	 * @param version Minecraft version
	 * @param gameDir Game directory
	 * @param account Minecraft account
	 * @return Exit code
	 * @throws IOException If loading arguments fails
	 */
	public static int launchInstallation(MinecraftVersionInfo version, File gameDir, AuthenticationInfo account)
			throws IOException {
		return launchInstallation(version, gameDir, generateJvmArguments(version),
				generateGameArguments(version, account, gameDir));
	}

	/**
	 * Launches the Minecraft client, please validate the installation before
	 * calling.
	 * 
	 * @param version Minecraft version
	 * @param gameDir Game directory
	 * @param mainJar Client jarfile
	 * @param account Minecraft account
	 * @return Exit code
	 * @throws IOException If loading arguments fails
	 */
	public static int launchInstallation(MinecraftVersionInfo version, File gameDir, File mainJar,
			AuthenticationInfo account) throws IOException {
		return launchInstallation(version, gameDir,
				generateJvmArguments(version, null, getLibraries(version), getNativesDirectory(version), mainJar),
				generateGameArguments(version, account, gameDir));
	}

	/**
	 * Launches the Minecraft client, please validate the installation before
	 * calling.
	 * 
	 * @param version   Minecraft version
	 * @param gameDir   Game directory
	 * @param mainJar   Client jarfile
	 * @param account   Minecraft account
	 * @param mainClass Main class
	 * @return Exit code
	 * @throws IOException If loading arguments fails
	 */
	public static int launchInstallation(MinecraftVersionInfo version, File gameDir, File mainJar,
			AuthenticationInfo account, String mainClass) throws IOException {
		return launchInstallation(version, gameDir,
				generateJvmArguments(version, null, getLibraries(version), getNativesDirectory(version), mainJar),
				generateGameArguments(version, account, gameDir), mainClass);
	}

	/**
	 * Launches the Minecraft client, please validate the installation before
	 * calling.
	 * 
	 * @param version Minecraft version
	 * @param gameDir Game directory
	 * @param jvm     JVM arguments
	 * @param game    Program arguments
	 * @return Exit code
	 */
	public static int launchInstallation(MinecraftVersionInfo version, File gameDir, String[] jvm, String[] game) {
		return launchInstallation(version, gameDir, jvm, game, getMainClass(version));
	}

	/**
	 * Launches the Minecraft client, please validate the installation before
	 * calling.
	 * 
	 * @param version   Minecraft version
	 * @param gameDir   Game directory
	 * @param jvm       JVM arguments
	 * @param game      Program arguments
	 * @param mainClass Main class
	 * @return Exit code
	 */
	public static int launchInstallation(MinecraftVersionInfo version, File gameDir, String[] jvm, String[] game,
			String mainClass) {
		return launchInstallation(version, gameDir, jvm, game, mainClass, getJVM().getAbsolutePath());
	}

	/**
	 * Launches the Minecraft client, please validate the installation before
	 * calling.
	 * 
	 * @param version    Minecraft version
	 * @param gameDir    Game directory
	 * @param jvm        JVM arguments
	 * @param game       Program arguments
	 * @param mainClass  Main class
	 * @param executable The java virtual machine executable to use
	 * @return Exit code
	 */
	public static int launchInstallation(MinecraftVersionInfo version, File gameDir, String[] jvm, String[] game,
			String mainClass, String executable) {
		ProcessBuilder builder = new ProcessBuilder();
		builder.command(ArrayUtil.buildArray(executable, jvm, mainClass, game));
		builder.directory(gameDir);
		builder.inheritIO();
		if (!gameDir.exists())
			gameDir.mkdirs();
		Process proc;
		try {
			proc = builder.start();
		} catch (IOException e) {
			error("Failed to launch the game", e);
			return 1;
		}
		try {
			proc.waitFor();
		} catch (InterruptedException e) {
			return 1;
		}
		return proc.exitValue();
	}

	/**
	 * Download a Minecraft version and its libraries. (including assets)
	 * 
	 * @param version        MinecraftVersionInfo object representing the version.
	 * @param downloadAssets True to download asset files
	 * @param overwrite      True to overwrite files, false otherwise.
	 * @param checkHash      True to compare the hashes of the local files, false to
	 *                       download without comparing (requires overwrite set to
	 *                       true) *
	 * @param removeRift     True to remove rift identifiers (remapped jars, use
	 *                       this for deobfuscated environments)
	 * @throws IOException If downloading fails
	 */
	@SuppressWarnings("unchecked")
	public static void downloadVersionFiles(MinecraftVersionInfo version, boolean downloadAssets, boolean overwrite,
			boolean checkHash, boolean removeRift) throws IOException {
		if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
			CyanCore.trackLevel(Level.WARN);
			File f3 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/libraries");
			File f4 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
					"caches/natives/" + version.getVersion());
			File f5 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/assets/indexes");
			File f6 = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/assets/objects");

			if (!f3.exists())
				f3.mkdirs();
			if (!f4.exists())
				f4.mkdirs();
			if (!f5.exists())
				f5.mkdirs();
			if (!f6.exists())
				f6.mkdirs();

			debug("Set libraries folder: " + f3.getCanonicalPath());
			debug("Set natives folder: " + f4.getCanonicalPath());

			Gson gson = new Gson();
			JsonObject versionJson = getVersionManifest(version);
			if (!isVersionManifestSaved(version) || overwrite)
				saveVersionManifest(version);
			versionJson = versionJson.deepCopy();

			recurseInheritsFrom(versionJson, versionJson, gson);
			MinecraftInstallationToolkit.downloadVersionJar(version, GameSide.CLIENT, overwrite);

			if (downloadAssets) {
				MinecraftToolkit.info("Downloading assets...");
				JsonObject assetIndexObj = versionJson.get("assetIndex").getAsJsonObject();
				String assetId = assetIndexObj.get("id").getAsString();
				String assetIndexSha = assetIndexObj.get("sha1").getAsString();
				String assetIndexSize = assetIndexObj.get("size").getAsString();
				String assetIndexUrl = assetIndexObj.get("url").getAsString();

				File assetIndex = new File(f5, assetId + ".json");
				if (!assetIndex.getParentFile().exists())
					assetIndex.getParentFile().mkdirs();

				try {
					if (checkFile(assetIndex, overwrite, assetIndexSha, checkHash)) {
						Download(assetIndex, new URL(assetIndexUrl), Double.valueOf(assetIndexSize).longValue(),
								assetIndexSha, overwrite, checkHash, true, true);
					}
				} catch (NoSuchAlgorithmException e) {
				}

				JsonObject assets = JsonParser.parseString(Files.readString(assetIndex.toPath())).getAsJsonObject();
				Map<String, Map<String, ?>> assetMap = gson.fromJson(assets.get("objects"), Map.class);
				for (Map<String, ?> val : assetMap.values()) {
					String hash = val.get("hash").toString();
					long size = Double.valueOf(val.get("size").toString()).longValue();
					File output = new File(f6, hash.substring(0, 2) + "/" + hash);
					if (!output.getParentFile().exists())
						output.getParentFile().mkdirs();

					try {
						Download(output, new URL(resourcesURL.replace("%1", hash.substring(0, 2)).replace("%2", hash)),
								size, hash, overwrite, checkHash, true, true);
					} catch (NoSuchAlgorithmException e) {
					}
				}
			}

			MinecraftToolkit.info("Downloading version libraries...");
			for (JsonElement o : versionJson.get("libraries").getAsJsonArray()) {
				JsonObject obj = o.getAsJsonObject();
				String name = obj.get("name").getAsString();

				if (removeRift) {
					String[] info = name.split(":");
					String group = info[0];
					String libname = info[1];
					String libversion = info[2];
					if (libversion.contains("-RIFT"))
						libversion = libversion.substring(0, libversion.indexOf("-RIFT"));
					if (libname.contains("-RIFT"))
						libname = libname.substring(0, libname.indexOf("-RIFT"));
					name = group + ":" + libname + ":" + libversion;
				}

				boolean allow = true;
				if (obj.has("rules")) {
					for (JsonElement r : obj.get("rules").getAsJsonArray()) {
						JsonObject rule = r.getAsJsonObject();
						Map<?, ?> rules = gson.fromJson(rule, Map.class);
						allow = evaluate(rules, variableStorage, gson, null, "allow");
					}
				}
				if (!allow)
					continue;
				File nativePath = null;
				String nativeId = null;
				boolean extractLater = false;
				ArrayList<String> downloadNativesList = new ArrayList<String>();
				if (obj.has("natives")) {
					Map<?, ?> natives = gson.fromJson(obj.get("natives"), Map.class);
					if (natives.containsKey(OsInfo.getCurrent().toString())) {
						String arch = System.getProperty("os.arch").toLowerCase();
						if (arch.equals("i386"))
							arch = "32";
						else if (arch.equals("x86") || arch.equals("amd64"))
							arch = "64";

						String nativesId = natives.get(OsInfo.getCurrent().toString()).toString()
								.replaceAll("\\$\\{arch\\}", arch);
						downloadNativesList.add(nativesId);
						nativeId = nativesId;

						if (gson.fromJson(obj.get("downloads").getAsJsonObject().get("classifiers"), Map.class)
								.containsKey("javadoc"))
							downloadNativesList.add("javadoc");

						if (gson.fromJson(obj.get("downloads").getAsJsonObject().get("classifiers"), Map.class)
								.containsKey("sources"))
							downloadNativesList.add("sources");
					}
				}
				if (obj.has("downloads")) {
					Map<String, Map<?, ?>> m = gson.fromJson(obj.get("downloads"), Map.class);
					for (String k : m.keySet()) {
						if (k.equals("artifact")) {
							Map<?, ?> object = m.get(k);
							String download = object.get("url").toString();
							String sha1 = object.get("sha1").toString();
							Long size = ((Double) object.get("size")).longValue();
							String path = object.get("path").toString();

							File f = new File(f3, path);
							if (!download.equals("")) {
								try {
									URL url = new URL(download);
									if (!f.getParentFile().exists())
										f.getParentFile().mkdirs();

									try {
										Download(f, url, size, sha1, overwrite, checkHash, true, true);
									} catch (Exception ex) {
										MinecraftToolkit.error("Failed to download file, file name: '" + f.getName()
												+ "', exception: " + ex.toString());
									}
								} catch (MalformedURLException ex) {
									MinecraftToolkit.error("Failed to download file, URL malformed, URL: " + download
											+ ", exception: " + ex.toString());
								}
							} else {
								try {
									if (checkFile(f, overwrite, sha1, checkHash))
										MinecraftToolkit
												.warn("Skipped download of library '" + name + "', no url specified.");
								} catch (NoSuchAlgorithmException | IOException e) {
								}
							}
						} else if (k.equals("classifiers")) {
							Map<?, ?> ob = m.get(k);
							for (Object k2 : ob.keySet()) {
								Map<?, ?> object = (Map<?, ?>) ob.get(k2);
								String download = object.get("url").toString();
								String sha1 = object.get("sha1").toString();
								Long size = ((Double) object.get("size")).longValue();
								String path = object.get("path").toString();
								if (downloadNativesList.size() != 0) {
									if (!downloadNativesList.contains(k2.toString()))
										continue;
								}

								File f = new File(f3, path);
								if (!download.equals("")) {
									try {
										URL url = new URL(download);
										if (!f.getParentFile().exists())
											f.getParentFile().mkdirs();
										try {
											Download(f, url, size, sha1, overwrite, checkHash, true, true);
											if (nativeId != null && nativeId.equals(k2.toString()))
												nativePath = f;

											File nativeOut = new File(f4, f.getName());
											if (!nativeOut.getParentFile().exists())
												nativeOut.getParentFile().mkdirs();
											if (checkFile(nativeOut, overwrite, sha1, checkHash)) {
												Files.copy(f.toPath(), nativeOut.toPath());
											}
										} catch (Exception ex) {
											MinecraftToolkit.error("Failed to download file, file name: '" + f.getName()
													+ "', exception: " + ex.toString());
										}
									} catch (MalformedURLException ex) {
										MinecraftToolkit.error("Failed to download file, URL malformed, URL: "
												+ download + ", exception: " + ex.toString());
									}
								} else {
									try {
										if (checkFile(f, overwrite, sha1, checkHash))
											MinecraftToolkit.warn(
													"Skipped download of library '" + name + "', no url specified.");
									} catch (NoSuchAlgorithmException | IOException e) {
									}
								}
							}
						} else {
							MinecraftToolkit.warn("No implementation for downloads." + k
									+ "! Unable to parse it, name of library: " + name + ".");
						}
					}
				} else if (obj.has("name")) {
					String urlRoot = "https://libraries.minecraft.net/"; // someday, it might change
					String path = "";
					String[] info = obj.get("name").getAsString().split(":");

					if (removeRift) {
						String group = info[0];
						String libname = info[1];
						String libversion = info[2];
						if (libversion.contains("-RIFT"))
							libversion = libversion.substring(0, libversion.indexOf("-RIFT"));
						if (libname.contains("-RIFT"))
							libname = libname.substring(0, libname.indexOf("-RIFT"));
						info = new String[] { group, libname, libversion };
					}

					if (info.length == 3) {
						path = info[0].replaceAll("\\.", "/") + "/" + info[1] + "/" + info[2] + "/" + info[1] + "-"
								+ info[2] + ".jar";
					} else if (info.length == 2) {
						path = info[0] + "/" + info[1] + "/" + info[0] + "-" + info[1] + ".jar";
					} else if (info.length == 1) {
						path = info[0] + "/" + info[0] + ".jar";
					}
					if (obj.has("url")) {
						urlRoot = obj.get("url").getAsString();
					}
					if (!urlRoot.endsWith("/"))
						urlRoot += "/";
					String download = urlRoot + path;

					if (!path.equals("")) {
						String sha1 = null;

						try {
							InputStream strm = new URL(download + ".sha1").openStream();
							sha1 = new String(strm.readAllBytes());
							strm.close();
						} catch (IOException e) {
							warn("Failed to download hash for file " + path + ", must assume it is good...");
						}
						try {
							URL url = new URL(download);
							File f = new File(f3, path);
							if (!f.getParentFile().exists())
								f.getParentFile().mkdirs();

							try {
								Download(f, url, -1, sha1, overwrite, checkHash, true, false);
							} catch (Exception ex) {
								MinecraftToolkit.error("Failed to download file, file name: '" + f.getName()
										+ "', exception: " + ex.toString());
							}
						} catch (MalformedURLException ex) {
							MinecraftToolkit.error("Failed to download file, URL malformed, URL: " + download
									+ ", exception: " + ex.toString());
						}
					} else {
						MinecraftToolkit.warn("Skipped download of library '" + name + "', no url specified.");
					}
				} else {
					MinecraftToolkit.info("Skipped download of library '" + name + "', no downloads map.");
				}
				if ((obj.has("extract") || extractLater) && nativePath != null) {
					extractLater = false;
					ArrayList<String> exclude = new ArrayList<String>();
					ArrayList<String> include = new ArrayList<String>();
					if (obj.get("extract").getAsJsonObject().entrySet().size() != 0) {
						Map<?, ?> rules = gson.fromJson(obj.get("extract"), Map.class);
						for (Object key : rules.keySet()) {
							if (key.equals("exclude")) {
								for (Object str : (ArrayList<Object>) rules.get(key)) {
									exclude.add(str.toString());
								}
							} else if (key.equals("include")) {
								for (Object str : (ArrayList<Object>) rules.get(key)) {
									include.add(str.toString());
								}
							}
						}
					}
					ZipFile file = new ZipFile(nativePath);
					Enumeration<? extends ZipEntry> entries = file.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						if (entry.isDirectory())
							continue;

						// Just in case, can't find it anymore, but it is possible windows uses
						// backslashes in zips.
						String path = entry.getName().replaceAll("\\\\", "/");

						if (path.startsWith("/"))
							path = path.substring(1);
						boolean incl = true;
						if (exclude.size() != 0) {
							for (String excludeName : exclude) {
								if (excludeName.endsWith("/") && path.startsWith(excludeName)) {
									incl = false;
									break;
								} else {
									String nm = new File(path).getName();
									if (nm.equals(excludeName)) {
										incl = false;
										break;
									}
								}
							}
						}
						if (!incl)
							continue;

						if (include.size() != 0) {
							incl = false;
							for (String includeName : include) {
								if (includeName.endsWith("/") && path.startsWith(includeName)) {
									incl = true;
									break;
								} else {
									if (includeName.contains("/")) {
										if (includeName.equals(path)) {
											incl = true;
											break;
										}
									} else {
										String nm = new File(path).getName();
										if (nm.equals(includeName)) {
											incl = true;
											break;
										}
									}
								}
							}
						}

						if (incl) {
							String fullname = new File(nativePath.getName()).getName();
							File output = new File(f4, path);
							if (!output.getParentFile().exists())
								output.getParentFile().mkdirs();

							if (!output.exists()) {
								info("Extracting file " + output.getName() + " of archive " + fullname + "...");
								BufferedInputStream strm1 = new BufferedInputStream(file.getInputStream(entry));
								FileOutputStream strm = new FileOutputStream(output);
								strm1.transferTo(strm);
								strm1.close();
								strm.close();
								debug("Saving -> OK");
							} else
								MinecraftToolkit.info("Skipped file " + output.getName() + " as it already exists.");
						}
					}
					file.close();
					nativePath = null;
				} else if (obj.has("extract")) {
					extractLater = true;
				}
				for (String key : obj.keySet()) {
					if (key.equals("downloads") || key.equals("url") || key.equals("name") || key.equals("rules")
							|| key.equals("natives") || key.equals("extract"))
						continue;

					MinecraftToolkit.warn(
							"No implementation for " + key + ".*! Unable to parse it, name of library: " + name + ".");
				}
			}
			TreeMap<String, Level> log = new TreeMap<String, Level>(CyanCore.stopTracking());
			if (log.keySet().size() != 0)
				MinecraftToolkit.warn("\nWarnings and/or errors were logged during download:");
			for (String itm : log.keySet()) {
				String msg = itm;
				Level lv = log.get(itm);
				if (lv.equals(Level.WARN))
					MinecraftToolkit.warn(msg);
				else if (lv.equals(Level.ERROR))
					MinecraftToolkit.error(msg);
				else if (lv.equals(Level.FATAL))
					MinecraftToolkit.fatal(msg);
			}
		} else

		{
			int missing = checkVersionMissingFiles(version, downloadAssets, false, false, removeRift);
			if (missing == -1)
				throw new IOException("Could not download the version manifest, device is offline.");

			throw new IOException("No connection, unable to download, missing " + missing + " libraries.");
		}
	}

	private static boolean checkFile(File f, boolean overwrite, String sha1, boolean checkHash)
			throws NoSuchAlgorithmException, IOException {
		if (f.exists() && overwrite) {
			if (checkHash && sha1 != null) {
				MinecraftToolkit.debug("File " + f.getName() + " already exists, comparing hashes...");
				FileInputStream strm1 = new FileInputStream(f);
				String hash = sha1HEX(strm1.readAllBytes());
				strm1.close();
				if (hash.equals(sha1)) {
					MinecraftToolkit.info("Skipped file " + f.getName() + " as it matches the expected hash.");
					return false;
				} else {
					MinecraftToolkit.debug("Hash mismatch, removing existing file...");
					f.delete();
				}
			} else {
				MinecraftToolkit.debug("File " + f.getName() + " already exists, removing existing file...");
				f.delete();
			}
		}
		return true;
	}

	private static boolean fileCheck(File f, String sha1, boolean ignore_hash, boolean ignore_nonexistent) {
		if (f.exists()) {
			if (!ignore_hash && sha1 != null) {
				MinecraftToolkit.debug("File " + f.getName() + " exists, comparing hashes...");
				try {
					FileInputStream strm1 = new FileInputStream(f);
					String hash = sha1HEX(strm1.readAllBytes());
					strm1.close();
					if (hash.equals(sha1)) {
						return true;
					}
				} catch (IOException | NoSuchAlgorithmException e) {
					if (ignore_nonexistent)
						return true;
					else
						return false;
				}
			} else {
				return true;
			}
		} else if (ignore_nonexistent)
			return true;
		return false;
	}

	static void recurseInheritsFrom(JsonObject root, JsonObject versionJson, Gson gson) {
		if (versionJson.has("inheritsFrom")) {
			try {
				String ver = versionJson.get("inheritsFrom").getAsString();
				JsonObject inheritsJson = getVersionManifest(MinecraftVersionToolkit.getVersion(ver));

				@SuppressWarnings("unchecked")
				Map<String, ?> entries = gson.fromJson(inheritsJson, Map.class);
				for (String key : entries.keySet()) {
					Object value = entries.get(key);
					if (key.equals("inheritsFrom"))
						recurseInheritsFrom(root, inheritsJson, gson);
					else {
						processValues(root, root, versionJson, gson, key,
								(versionJson.has(key) ? versionJson.get(key) : null), value);
					}
				}
			} catch (IOException e) {
			}
		}
	}

	static void processValues(JsonObject root, JsonObject element1, JsonObject versionJson, Gson gson, String key,
			JsonElement element, Object value) {
		if (element1.has(key) && !element1.get(key).isJsonArray()) {
			if (!(value instanceof String)) {
				if (value instanceof Map) {
					for (Object newKey : ((Map<?, ?>) value).keySet()) {
						Object newValue = ((Map<?, ?>) value).get(newKey);
						processValues(root, element1.get(key).getAsJsonObject(), versionJson, gson, newKey.toString(),
								element1.get(key), newValue);
					}
				}
			}
		} else {
			JsonObject r = root;
			JsonElement e = gson.toJsonTree(value);
			if (element1.has(key) && element1.get(key).isJsonArray()) {
				JsonArray a = element1.get(key).getAsJsonArray();
				for (JsonElement element2 : e.getAsJsonArray()) {
					if (!a.contains(element2))
						a.add(element2);
				}
			} else {
				if (element != null)
					r = element.getAsJsonObject();
				r.add(key, e);
			}
		}
	}

	private static String sha1HEX(byte[] array) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] sha = digest.digest(array);
		StringBuilder result = new StringBuilder();
		for (byte aByte : sha) {
			result.append(String.format("%02x", aByte));
		}
		return result.toString();
	}

	private static void Download(File f, URL url, long size, String sha1, boolean overwrite, boolean checkSHAifExists,
			boolean enforceOnlineHash, boolean enforceOnlineSize) throws IOException, NoSuchAlgorithmException {
		MinecraftToolkit.debug("Preparing to download...");

		if (!checkFile(f, overwrite, sha1, checkSHAifExists))
			return;

		MinecraftToolkit.info("Downloading " + url.toString() + " -> " + f.getName() + "...");
		BufferedInputStream strm = new BufferedInputStream(url.openStream());
		byte[] file = strm.readAllBytes();
		strm.close();

		String hash = sha1HEX(file);
		MinecraftToolkit.debug("Downloaded file size: " + file.length); // TODO: make pretty
		if (size != -1)
			MinecraftToolkit.debug("Expected file size: " + size); // TODO: make pretty
		if (size != -1 && file.length != size && enforceOnlineSize)
			throw new SecurityException("Local stored size of " + f.getName() + " does not match online file!");

		if (enforceOnlineSize)
			MinecraftToolkit.debug("Size check -> OK");

		MinecraftToolkit.debug("Downloaded file hash: " + hash);
		if (sha1 != null) {
			MinecraftToolkit.debug("Expected file hash: " + sha1);
			if (!hash.equals(sha1) && enforceOnlineHash)
				throw new SecurityException("Local hash of " + f.getName() + " does not match online file!");
			if (enforceOnlineHash)
				MinecraftToolkit.debug("Hash check -> OK");
		}

		MinecraftToolkit.debug("Saving file to " + f.getCanonicalPath() + "...");
		FileOutputStream st = new FileOutputStream(f);
		st.write(file);
		st.close();
		MinecraftToolkit.debug("Saving -> OK");
	}

}
