package org.asf.cyan.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.Scanner;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.api.modloader.information.modloader.LoadPhase;
import org.asf.cyan.api.modloader.information.providers.IModloaderInfoProvider;
import org.asf.cyan.api.versioning.VersionStatus;

/**
 * Cyan Information Class, contains basic information about the running
 * modloader
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CyanInfo extends Configuration<CyanInfo> {
	static CyanInfo info = null;
	static CyanInfoProvider provider;
	public static final String infoPath = "/org/asf/cyan/CyanVersionHolder/generic/CyanVersionHolder-generic-versions.ccfg";

	private CyanInfo() {
		super();
		readAll(readCCFG());

		try {
			displayAppend = "";
			CyanCore.infoLog("Connecting to " + checkSource + ", downloading version information...");
			StringBuilder conf = new StringBuilder();
			CyanCore.debugLog("Scanning version information... Using URL: " + checkSource + infoPath);
			URL u = new URL(checkSource + infoPath);
			CyanCore.debugLog("Opening connection...");
			Scanner sc = new Scanner(u.openStream());
			CyanCore.debugLog("Reading all content...");
			while (sc.hasNext())
				conf.append(sc.nextLine() + System.lineSeparator());
			sc.close();

			CyanCore.debugLog("Parsing response into CCFG object...");
			CyanUpdateInfo info = new CyanUpdateInfo(conf.toString());
			if (info.changelogs.containsKey(version)) {
				changelog = info.changelogs.get(version);
			}
			CyanCore.debugLog("Processing version information...");
			if (!version.equals(info.latestStableVersion)) {
				if (version.equals(info.latestPreviewVersion)) {
					displayAppend = "Latest PREVIEW";
					versionStatus = VersionStatus.LATEST_PREVIEW;
				} else if (version.equals(info.latestBetaVersion)) {
					displayAppend = "Latest BETA";
					versionStatus = VersionStatus.LATEST_BETA;
				} else if (version.equals(info.latestAlphaVersion)) {
					displayAppend = "Latest ALPHA";
					versionStatus = VersionStatus.LATEST_ALPHA;
				} else {
					for (String lts : info.longTermSupportVersions) {
						if (version.equals(lts)) {
							displayAppend = "LTS";
							versionStatus = VersionStatus.LTS;
						}
					}
					if (displayAppend.equals("")) {
						for (String lts : info.requiredUpgrade) {
							if (version.equals(lts)) {
								displayAppend = "UNSUPPORTED";
								versionStatus = VersionStatus.UNSUPPORTED;
							}
						}
					}
					if (displayAppend.equals("")) {
						for (String ver : info.allVersions.keySet()) {
							if (version.equals(ver)) {
								String type = info.allVersions.get(ver);
								if (type.equals("BETA") || type.equals("ALPHA") || type.equals("PREVIEW"))
									type = "Outdated " + type;
								displayAppend = type;
								if (type.equals("BETA")) {
									versionStatus = VersionStatus.OUTDATED_BETA;
								} else if (type.equals("ALPHA")) {
									versionStatus = VersionStatus.OUTDATED_ALPHA;
								} else if (type.equals("PREVIEW")) {
									versionStatus = VersionStatus.OUTDATED_PREVIEW;
								} else {
									versionStatus = VersionStatus.OUTDATED;
								}
							}
						}
					}
					if (displayAppend.equals("")) {
						displayAppend = "In-development";
					} else if (!displayAppend.contains("LTS") && !displayAppend.contains("Latest")) {
						String type = info.allVersions.get(version);
						String newversion = "";

						if (type.equals("ALPHA"))
							newversion = info.latestAlphaVersion;
						else if (type.equals("BETA"))
							newversion = info.latestBetaVersion;
						else if (type.equals("PREVIEW"))
							newversion = info.latestPreviewVersion;
						else if (type.equals("RELEASE"))
							newversion = info.latestStableVersion;

						updateChangelog = info.changelogs.get(newversion).replaceAll("\n", System.lineSeparator());
						CyanCore.infoLog("Cyan is out of date, new version: " + newversion);
						if (info.changelogs.containsKey(newversion)) {
							CyanCore.infoLog("Changelog:" + System.lineSeparator()
									+ info.changelogs.get(newversion).replaceAll("\n", System.lineSeparator()));
						}
					}
				}
			} else {
				versionStatus = VersionStatus.LATEST;
				displayAppend = "Latest RELEASE";
			}
			CyanCore.infoLog("Received version suffix: " + displayAppend);
		} catch (IOException e) {
			CyanCore.infoLog("Unable to connect to server.");
		}

		if (platform == null) {
			if (modloaderVersion.equals("") && !deobf) {
				platform = LaunchPlatform.VANILLA;
			} else if (modloaderVersion.equals("") && deobf) {
				platform = LaunchPlatform.DEOBFUSCATED;
			} else {
				if (modloaderVersion.toLowerCase().startsWith("forge-")) {
					platform = LaunchPlatform.MCP;
					modloaderName = "Forge";

					modloaderVersion = modloaderVersion.substring(modloaderVersion.indexOf("-") + 1);
				} else if (modloaderVersion.toLowerCase().startsWith("fabric-loader-")) {
					platform = LaunchPlatform.YARN;
					modloaderName = "Fabric";

					modloaderVersion = modloaderVersion.substring(modloaderVersion.indexOf("-") + 1);
					modloaderVersion = modloaderVersion.substring(modloaderVersion.indexOf("-") + 1);
				} else if (modloaderVersion.toLowerCase().startsWith("paper-")) {
					platform = LaunchPlatform.SPIGOT;
					modloaderName = "Paper";

					modloaderVersion = modloaderVersion.substring(modloaderVersion.indexOf("-") + 1);
				} else {
					platform = LaunchPlatform.UNKNOWN;
				}
			}
		}
	}

	static final String readCCFG() {
		try {
			BufferedInputStream strm = new BufferedInputStream(
					CyanCore.getCoreClassLoader().getResource("cyan.release.ccfg").openStream());
			String conf = new String(strm.readAllBytes());
			strm.close();
			return conf;
		} catch (IOException e) {
			return "{}";
		}
	}

	public String minecraftCyanVersion;
	public String minecraftVersion;
	public String devStartDate;
	public String releaseDate;
	public String version;
	public String modloaderVersion;
	public String displayAppend;
	public String checkSource;
	public String changelog;
	public LaunchPlatform platform;
	public String modloaderName = "";
	public String updateChangelog;
	private VersionStatus versionStatus = VersionStatus.UNKNOWN;

	/**
	 * Get the mod platform Cyan is running through.
	 * 
	 * @return Platform object
	 */
	public static LaunchPlatform getPlatform() {
		if (info == null)
			info = new CyanInfo();
		return info.platform;
	}

	/**
	 * Get the version suffix (display only)
	 * 
	 * @return Version suffix
	 */
	public static String getDisplayVersionSuffix() {
		if (info == null)
			info = new CyanInfo();
		return info.displayAppend;
	}

	/**
	 * Get the minecraft-cyan version
	 * 
	 * @return Minecraft-Cyan version
	 */
	public static String getMinecraftCyanVersion() {
		if (info == null)
			info = new CyanInfo();
		if (info.version.contains("${")) {
			return "Unknown";
		}
		return info.minecraftCyanVersion;
	}

	/**
	 * Get the modloader version (not cyan, returns an empty string if no other
	 * loader is present)
	 * 
	 * @return Modloader version
	 */
	public static String getModloaderVersion() {
		if (info == null)
			info = new CyanInfo();
		if (info.modloaderVersion.contains("${")) {
			return "Unknown";
		}
		return info.modloaderVersion;
	}

	/**
	 * Get the modloader name (not cyan, returns an empty string if no other loader
	 * is present)
	 * 
	 * @return Modloader name
	 */
	public static String getModloaderName() {
		if (info == null)
			info = new CyanInfo();
		return info.modloaderName;
	}
	
	/**
	 * Get the entry method used to start cyan
	 * @return Name of the Entry Method used to start Cyan.
	 */
	public static String getEntryMethod() {
		return CyanCore.getEntryMethod();
	}

	/**
	 * Get the minecraft version
	 * 
	 * @return Minecraft version
	 */
	public static String getMinecraftVersion() {
		if (info == null)
			info = new CyanInfo();
		if (info.version.contains("${")) {
			return "Unknown";
		}
		return info.minecraftVersion;
	}

	/**
	 * Get the cyan version
	 * 
	 * @return Cyan version
	 */
	public static String getCyanVersion() {
		if (info == null)
			info = new CyanInfo();
		if (info.version.contains("${")) {
			return "In-development";
		}
		return info.version;
	}

	/**
	 * Get the release date
	 * 
	 * @return Release date
	 */
	public static OffsetDateTime getReleaseDate() {
		if (info == null)
			info = new CyanInfo();
		if (info.releaseDate.contains("${")) {
			return OffsetDateTime.now();
		}
		return OffsetDateTime.parse(info.releaseDate);
	}

	/**
	 * Get the development start date
	 * 
	 * @return Development start date
	 */
	public static OffsetDateTime getDevStartDate() {
		if (info == null)
			info = new CyanInfo();
		if (info.devStartDate.contains("${")) {
			return OffsetDateTime.now();
		}
		return OffsetDateTime.parse(info.devStartDate);
	}

	/**
	 * Get the current side (CLIENT/SERVER)
	 * 
	 * @return Side name
	 */
	public static GameSide getSide() {
		if (info == null)
			info = new CyanInfo();
		return CyanCore.getSide();
	}

	/**
	 * Get the current loading phase
	 * 
	 * @return CyanLoadPhase object representing the loading phase
	 */
	public static LoadPhase getCurrentPhase() {
		if (info == null)
			info = new CyanInfo();
		return CyanCore.getCurrentPhase();
	}

	// We are a ram-only configuration
	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public static void setDeobfuscated() {
		deobf = true;
	}

	static boolean deobf = false;

	public static String getVersionChangelog() {
		if (info == null)
			info = new CyanInfo();
		return info.changelog;
	}

	public static String getVersionUpdateChangelog() {
		if (info == null)
			info = new CyanInfo();
		return info.updateChangelog;
	}

	public static VersionStatus getModloaderVersionStatus() {
		if (info == null)
			info = new CyanInfo();
		return info.versionStatus ;
	}

	public static IModloaderInfoProvider getProvider() {
		if (info == null)
			info = new CyanInfo();
		
		if (provider == null)
			provider = new CyanInfoProvider();
		
		return provider;
	}
}
