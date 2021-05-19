package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * 
 * Main Minecraft toolkit: resolves the minecraft version list
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class MinecraftToolkit extends CyanComponent {

	public static String getVersion() {
		URL info = MinecraftToolkit.class.getResource("/mtk.info");
		StringBuilder builder = new StringBuilder();
		try {
			Scanner sc = new Scanner(info.openStream());
			while (sc.hasNext())
				builder.append(sc.nextLine());
			sc.close();
		} catch (IOException e) {
		}
		return builder.toString();
	}
	
	static void infoLog(String message) {
		CyanComponent.info(message);
	}

	static void warnLog(String message) {
		CyanComponent.warn(message);
	}

	static void errorLog(String message) {
		CyanComponent.error(message);
	}

	static void traceLog(String message) {
		CyanComponent.trace(message);
	}

	static void debugLog(String message) {
		CyanComponent.debug(message);
	}

	static void fatalLog(String message) {
		CyanComponent.fatal(message);
	}

	/**
	 * Progress maximum value for some wrappers such as the Cyan Client Wrapper
	 */
	public static int progressMax = 0;

	/**
	 * Progress value for some wrappers such as the Cyan Client Wrapper
	 */
	public static int progress = 0;

	/**
	 * Progress message for some wrappers such as the Cyan Client Wrapper
	 */
	public static String progressMessage = "";

	protected static void initComponent() {
		trace("INITIALIZE Main Minecraft Toolkit, caller: " + CallTrace.traceCallName());
	}

	/**
	 * Initialize the Minecraft Toolkit
	 */
	public static void initializeMTK() {
		trace("INITIALIZE Main Minecraft Toolkit, caller: " + CallTrace.traceCallName());
		resetServerConnectionState();
		resolveVersions();
	}

	static boolean _connected = false;
	static HashMap<String, MinecraftVersionInfo> available_versions = new HashMap<String, MinecraftVersionInfo>();
	static String version_manifest_url = "https://launchermeta.mojang.com/mc/game/version_manifest.json";
	static String net_test_url = "https://aerialworks.ddns.net/";

	/**
	 * Reset the 'offline' state
	 */
	public static void resetServerConnectionState() {
		trace("RESET server connection state, caller: " + CallTrace.traceCallName());
		_connected = getServerConnection();
		if (!_connected) {
			trace("STATE of connection is " + _connected + ", sending warning, caller: " + CallTrace.traceCallName());
			warn("No connection to the network test server, connection state has to be reset after connecting to the internet.");
		}
		debug("Resetted server connection state");
	}

	/**
	 * Check if there is a connection (once it returns false, it will keep returning
	 * false unless ResetServerConnectionState is called)
	 * 
	 * @return True if connected, false otherwise
	 */
	public static boolean hasMinecraftDownloadConnection() {
		trace("GET server connection, caller: " + CallTrace.traceCallName());
		if (!_connected) {
			trace("RETURN false, previous connection state was false, caller: " + CallTrace.traceCallName());
			return false;
		} else {
			trace("RETURN current connection state, check state, caller: " + CallTrace.traceCallName());
			resetServerConnectionState();
			trace("RETURN connection state " + _connected + ", caller: " + CallTrace.traceCallName());
			return _connected;
		}
	}

	/**
	 * Get the connection state with the manifest server
	 * 
	 * @return True if connected, false otherwise
	 */
	public static boolean getServerConnection() {
		try {
			trace("PARSE server connection test URL, caller: " + CallTrace.traceCallName());
			URL u = new URL(net_test_url);
			trace("OPEN connection to network test URL, caller: " + CallTrace.traceCallName());
			URLConnection uc = u.openConnection();
			uc.setConnectTimeout(5000);
			uc.setReadTimeout(5000);
			trace("CONNECT to server test URL, caller: " + CallTrace.traceCallName());
			uc.connect();
			trace("RETURN true, all passed, caller: " + CallTrace.traceCallName());
			return true;
		} catch (MalformedURLException e) {
			trace("RETURN false, MalformedURLException, message: " + e.getMessage() + ", caller: "
					+ CallTrace.traceCallName());
			return false;
		} catch (IOException e) {
			trace("RETURN false, IOException, message: " + e.getMessage() + ", caller: " + CallTrace.traceCallName());
			return false;
		}
	}

	/**
	 * Update the version map by downloading the version manifest (existing versions
	 * will be discarded from the list)
	 */
	public static void resolveVersions() {
		trace("RESOLVE version list, no arguments, caller: " + CallTrace.traceCallName());
		resolveVersions(false);
	}

	/**
	 * Update the version map by downloading the version manifest
	 * 
	 * @param noclear true to prevent clearing of the list, false otherwise
	 */
	public static void resolveVersions(boolean noclear) {
		trace("RESOLVE version list, argument noclear set to: " + noclear + ", caller: " + CallTrace.traceCallName());
		if (hasMinecraftDownloadConnection()) {
			trace("HAS server connection, resolving list, caller: " + CallTrace.traceCallName());
			try {
				if (!noclear)
					available_versions.clear();
				info("Downloading manifest from " + version_manifest_url + "...");
				trace("OPEN server connection, PARSE URL, caller: " + CallTrace.traceCallName());
				URL u = new URL(version_manifest_url);
				trace("OPEN stream to server, caller: " + CallTrace.traceCallName());
				InputStreamReader reader = new InputStreamReader(u.openStream());
				info("Parsing manifest to json...");
				trace("PARSE json from stream, caller: " + CallTrace.traceCallName());
				JsonObject json = JsonParser.parseReader(new JsonReader(reader)).getAsJsonObject();
				trace("CLOSE server stream, caller: " + CallTrace.traceCallName());
				reader.close();
				trace("CREATE Gson object, caller: " + CallTrace.traceCallName());
				Gson gson = new Gson();
				info("Reading manifest...");

				trace("CREATE map for latest version information, caller: " + CallTrace.traceCallName());
				@SuppressWarnings("unchecked")
				Map<String, String> latest = gson.fromJson(json.get("latest"), Map.class);
				String latestRelease = "";
				String latestSnapshot = "";
				trace("CHECK map for release version, caller: " + CallTrace.traceCallName());
				if (latest.containsKey("release")) {
					trace("SET latest release version to " + latest.get("release") + ", caller: "
							+ CallTrace.traceCallName());
					latestRelease = latest.get("release");
				}
				if (latest.containsKey("snapshot")) {
					trace("SET latest snapshot version to " + latest.get("snapshot") + ", caller: "
							+ CallTrace.traceCallName());
					latestSnapshot = latest.get("snapshot");
				}
				trace("CREATE DateFormat object, caller: " + CallTrace.traceCallName());
				info("Adding versions...");
				trace("LOOP through entries, caller: " + CallTrace.traceCallName());
				for (JsonElement entry : json.get("versions").getAsJsonArray()) {
					trace("CREATE map for version info, caller: " + CallTrace.traceCallName());
					@SuppressWarnings("unchecked")
					Map<String, String> ver = gson.fromJson(entry, Map.class);

					trace("SET id of version info to " + ver.get("id") + ", caller: " + CallTrace.traceCallName());
					String id = ver.get("id");

					trace("SET type string of version info to " + ver.get("type") + ", caller: "
							+ CallTrace.traceCallName());
					String type = ver.get("type");

					trace("SET url string of version info to " + ver.get("url") + ", caller: "
							+ CallTrace.traceCallName());
					String url = ver.get("url");

					trace("SET releaseTime string of version info to " + ver.get("releaseTime") + ", caller: "
							+ CallTrace.traceCallName());
					String releaseTime = ver.get("releaseTime");

					trace("PARSE MinecraftVersionInfo object, caller: " + CallTrace.traceCallName());
					MinecraftVersionInfo versionInfo = new MinecraftVersionInfo(id, MinecraftVersionType.parse(type),
							new URL(url), OffsetDateTime.parse(releaseTime));

					trace("CHECK latest release and snapshot version strings if they match " + id + ", caller: "
							+ CallTrace.traceCallName());
					if (versionInfo.getVersion().equalsIgnoreCase(latestRelease)) {
						trace("MATCHED latest release version string, caller: " + CallTrace.traceCallName());
						available_versions.put("Latest_release", versionInfo);

						info("Received latest release version: " + id);
					} else if (versionInfo.getVersion().equalsIgnoreCase(latestSnapshot)) {
						trace("MATCHED latest snapshot version string, caller: " + CallTrace.traceCallName());
						available_versions.put("Latest_snapshot", versionInfo);

						info("Received latest snapshot version: " + id);
					}

					trace("ADD MinecraftVersionInfo object to the available_versions map, version: "
							+ versionInfo.getVersion() + ", caller: " + CallTrace.traceCallName());
					available_versions.put(id, versionInfo);

					debug("Added version information object. Version: " + versionInfo.getVersion() + ", release type: "
							+ versionInfo.getVersionType().toString() + ", release date: "
							+ DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)
									.format(versionInfo.getReleaseDate())
							+ ". The item has been registered.");
				}
				
				info("Download completed, added/updated " + json.get("versions").getAsJsonArray().size() + " version"
						+ (json.get("versions").getAsJsonArray().size() == 1 ? "" : "s"));
			} catch (Exception ex) {
				warn("Downloading version list failed. Exception: " + ex.getMessage());
			}
			
			loadLocalVersions();
		} else {
			trace("NO CONNECTION to server, cancel task, caller: " + CallTrace.traceCallName());
			warn("No connection to the network test endpoint, unable to load version manifest, will try to use local files only.");
			
			loadLocalVersions();
			
			debug("Fetching latest versions...");
			MinecraftVersionInfo latestRelease = null;
			MinecraftVersionInfo latestSnapshot = null;;
			for (MinecraftVersionInfo version : available_versions.values()) {
				switch (version.getVersionType()) {
				case RELEASE:
					if (latestRelease == null || version.getReleaseDate().isAfter(latestRelease.getReleaseDate())) {
						latestRelease = version;
					}
					break;
				case SNAPSHOT:
					if (latestSnapshot == null || version.getReleaseDate().isAfter(latestSnapshot.getReleaseDate())) {
						latestSnapshot = version;
					}
					break;
				default:
					break;
				}
			}
			if (latestRelease != null) {
				available_versions.put("Latest_release", latestRelease);
				info("Received latest release version: " + latestRelease.getVersion());
			} else if (latestSnapshot != null) {
				available_versions.put("Latest_snapshot", latestSnapshot);
				info("Received latest snapshot version: " + latestSnapshot.getVersion());
			}
		}
	}
	
	static void loadLocalVersions() {
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
					trace("CREATE map for version info, caller: " + CallTrace.traceCallName());
					JsonObject ver = JsonParser.parseString(Files.readString(manifest.toPath())).getAsJsonObject();

					trace("SET id of version info to " + ver.get("id") + ", caller: "
							+ CallTrace.traceCallName());
					String id = ver.get("id").getAsString();

					trace("SET type string of version info to " + ver.get("type") + ", caller: "
							+ CallTrace.traceCallName());
					String type = ver.get("type").getAsString();

					trace("SET url string of version info to " + ver.get("url") + ", caller: "
							+ CallTrace.traceCallName());
					String url = manifest.toURI().toURL().toString();

					trace("SET releaseTime string of version info to " + ver.get("releaseTime") + ", caller: "
							+ CallTrace.traceCallName());
					String releaseTime = ver.get("releaseTime").getAsString();

					trace("PARSE MinecraftVersionInfo object, caller: " + CallTrace.traceCallName());
					MinecraftVersionInfo versionInfo = new MinecraftVersionInfo(id,
							MinecraftVersionType.parse(type), new URL(url), OffsetDateTime.parse(releaseTime));

					if (!available_versions.containsKey(id)) {
						trace("ADD MinecraftVersionInfo object to the available_versions map, version: "
								+ versionInfo.getVersion() + ", caller: " + CallTrace.traceCallName());
						available_versions.put(id, versionInfo);

						debug("Added version information object. Version: " + versionInfo.getVersion()
								+ ", release type: " + versionInfo.getVersionType().toString() + ", release date: "
								+ DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT, FormatStyle.SHORT)
										.format(versionInfo.getReleaseDate())
								+ ". The item has been registered.");
					}
				}					
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}
