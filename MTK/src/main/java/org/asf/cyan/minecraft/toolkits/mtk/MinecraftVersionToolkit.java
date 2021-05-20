package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

/**
 * 
 * Minecraft Version Toolkit: create and get MinecraftVersionInfo objects.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class MinecraftVersionToolkit extends CyanComponent {

	/**
	 * Initialize the toolkit, gets called from CyanCore.initializeComponents()
	 */
	protected static void initComponent() {
		trace("INITIALIZE Minecraft Version Toolkit, caller: " + CallTrace.traceCallName());
	}

	/**
	 * Create a Minecraft version info object and add it to the list of available
	 * versions.
	 * 
	 * @param mc_version   The Minecraft version string.
	 * @param version_type The Minecraft version type.
	 * @param manifest_url The Minecraft version manifest url.
	 * @param release_date The Minecraft version release date.
	 * @return New MinecraftVersionInfo object
	 */
	public static MinecraftVersionInfo createVersionInfo(String mc_version, MinecraftVersionType version_type,
			URL manifest_url, OffsetDateTime release_date) {
		MinecraftVersionInfo i = new MinecraftVersionInfo(mc_version, version_type, manifest_url, release_date);
		MinecraftToolkit.available_versions.put(mc_version, i);
		return i;
	}

	/**
	 * Create a Minecraft version info object and add it to the list of available
	 * versions.
	 * 
	 * @param mc_version   The Minecraft version string.
	 * @param version_type The Minecraft version type.
	 * @param manifest_url The Minecraft version manifest url.
	 * @param release_date The Minecraft version release date.
	 * @return New MinecraftVersionInfo object
	 */
	public static MinecraftVersionInfo createOrGetVersion(String mc_version, MinecraftVersionType version_type,
			URL manifest_url, OffsetDateTime release_date) {
		if (MinecraftToolkit.available_versions.containsKey(mc_version))
			return MinecraftToolkit.available_versions.get(mc_version);

		MinecraftVersionInfo i = new MinecraftVersionInfo(mc_version, version_type, manifest_url, release_date);
		MinecraftToolkit.available_versions.put(mc_version, i);
		return i;
	}

	/**
	 * Change the URL string used to find versions (direct link)
	 * 
	 * @param url Direct URL to version manifest (JSON) file.
	 * @throws IOException If the JSON file cannot be downloaded or parsed.
	 */
	public static void setManifesURL(String url) throws IOException {
		try {
			URL u = new URL(url);
			URLConnection uc = u.openConnection();
			uc.connect();
			uc = null;

			u = new URL(url);
			InputStreamReader reader = new InputStreamReader(u.openStream());
			JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
			reader.close();
			Gson gson = new Gson();
			if (!json.has("latest"))
				throw new IOException("Member 'latest' could not be found");
			if (!json.has("versions"))
				throw new IOException("Member 'versions' could not be found");
			if (!json.get("versions").isJsonArray())
				throw new IOException("Member 'versions' is not a JSON array.");

			@SuppressWarnings("unchecked")
			Map<String, String> latest = gson.fromJson(json.get("latest"), Map.class);
			for (String key : latest.keySet()) {
				if (!key.equals("release") && !key.equals("snapshot")) {
					throw new IOException("Map 'latest' contains unknown keys.");
				}
			}

			for (JsonElement entry : json.get("versions").getAsJsonArray()) {
				try {
					gson.fromJson(entry, Map.class);
				} catch (JsonSyntaxException ex) {
					throw new IOException("A version entry was found of incorrect type");
				}

				@SuppressWarnings("unchecked")
				Map<String, String> ver = gson.fromJson(entry, Map.class);
				if (!ver.containsKey("id"))
					throw new IOException("A version entry was found without the 'id' key");
				if (!ver.containsKey("type"))
					throw new IOException("A version entry was found without the 'type' key, ID: " + ver.get("id"));
				if (!ver.containsKey("url"))
					throw new IOException("A version entry was found without the 'url' key, ID: " + ver.get("url"));
				if (!ver.containsKey("releaseTime"))
					throw new IOException(
							"A version entry was found without the 'releaseTime' key, ID: " + ver.get("releaseTime"));
			}
			u = null;

			MinecraftToolkit.version_manifest_url = url;
		} catch (MalformedURLException e) {
			throw new MalformedURLException("Unable to query the server, URL malformed.");
		}
	}

	/**
	 * Get a list of available Minecraft versions
	 * 
	 * @return List of available Minecraft versions
	 */
	public static List<MinecraftVersionInfo> getAvailableVersions() {
		return new ArrayList<MinecraftVersionInfo>(MinecraftToolkit.available_versions.values());
	}

	/**
	 * Return the latest Minecraft release version
	 * 
	 * @return MinecraftVersionInfo object representing the latest release (null if
	 *         Latest_release is empty)
	 */
	public static MinecraftVersionInfo getLatestReleaseVersion() {
		return MinecraftToolkit.available_versions.getOrDefault("Latest_release", null);
	}

	/**
	 * Return the latest Minecraft release version
	 * 
	 * @return MinecraftVersionInfo object representing the latest release (null if
	 *         Latest_snapshot is empty)
	 */
	public static MinecraftVersionInfo getLatestSnapshotVersion() {
		return MinecraftToolkit.available_versions.getOrDefault("Latest_snapshot", null);
	}

	/**
	 * Get the version object associated with a version string
	 * 
	 * @param version the version string
	 * @return The MinecraftVersionInfo object representing the version, null if not
	 *         found
	 */
	public static MinecraftVersionInfo getVersion(String version) {
		version = version.toLowerCase();
		MinecraftVersionType type = MinecraftVersionType.UNKNOWN;

		if (version.contains("snapshot-")) {
			type = MinecraftVersionType.SNAPSHOT;
			version.replaceAll("snapshot-", "");
		} else if (version.contains("snapshot ")) {
			type = MinecraftVersionType.SNAPSHOT;
			version.replaceAll("snapshot ", "");
		} else if (version.contains("release-")) {
			type = MinecraftVersionType.RELEASE;
			version.replaceAll("release-", "");
		} else if (version.contains("release ")) {
			type = MinecraftVersionType.RELEASE;
			version.replaceAll("release ", "");
		} else if (version.contains("release-")) {
			type = MinecraftVersionType.RELEASE;
			version.replaceAll("release-", "");
		} else if (version.contains("old alpha ")) {
			type = MinecraftVersionType.UNSUPPORTED_ALPHA;
			version.replaceAll("old alpha ", "");
		} else if (version.contains("old alpha")) {
			type = MinecraftVersionType.UNSUPPORTED_ALPHA;
			version.replaceAll("old alpha", "");
		} else if (version.contains("old alpha-")) {
			type = MinecraftVersionType.UNSUPPORTED_ALPHA;
			version.replaceAll("old alpha-", "");
		} else if (version.contains("old_alpha ")) {
			type = MinecraftVersionType.UNSUPPORTED_ALPHA;
			version.replaceAll("old_alpha ", "");
		} else if (version.contains("old_alpha-")) {
			type = MinecraftVersionType.UNSUPPORTED_ALPHA;
			version.replaceAll("old_alpha-", "");
		} else if (version.contains("old_alpha_")) {
			type = MinecraftVersionType.UNSUPPORTED_ALPHA;
			version.replaceAll("old_alpha_", "");
		} else if (version.contains("old beta ")) {
			type = MinecraftVersionType.UNSUPPORTED_BETA;
			version.replaceAll("old beta ", "");
		} else if (version.contains("old beta_")) {
			type = MinecraftVersionType.UNSUPPORTED_BETA;
			version.replaceAll("old beta_", "");
		} else if (version.contains("old beta-")) {
			type = MinecraftVersionType.UNSUPPORTED_BETA;
			version.replaceAll("old beta-", "");
		} else if (version.contains("old_beta ")) {
			type = MinecraftVersionType.UNSUPPORTED_BETA;
			version.replaceAll("old_beta ", "");
		} else if (version.contains("old_beta-")) {
			type = MinecraftVersionType.UNSUPPORTED_BETA;
			version.replaceAll("old_beta-", "");
		} else if (version.contains("old_beta_")) {
			type = MinecraftVersionType.UNSUPPORTED_BETA;
			version.replaceAll("old_beta_", "");
		}

		switch (type) {
		case UNKNOWN:
			for (MinecraftVersionInfo e : MinecraftToolkit.available_versions.values()) {
				if (e.getVersion().equalsIgnoreCase(version)) {
					return e;
				}
			}
			break;
		default:
			for (MinecraftVersionInfo e : MinecraftToolkit.available_versions.values()) {
				if (e.getVersion().equalsIgnoreCase(version) && e.getVersionType() == type) {
					return e;
				}
			}
			break;
		}

		return null;
	}
}
