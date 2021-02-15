package org.asf.cyan.core;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.time.OffsetDateTime;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.cyanloader.CyanLoadPhase;
import org.asf.cyan.api.cyanloader.CyanSide;

public class CyanInfo extends Configuration<CyanInfo> {
	static CyanInfo info = null;
	private CyanInfo() {
		super();
		readAll(readCCFG());
	}
	
	static {
		info = new CyanInfo();
	}
	
	static final String readCCFG()
	{
		try {
			BufferedInputStream strm = new BufferedInputStream(CyanCore.getCoreClassLoader().getResource("cyan.release.ccfg").openStream());
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
	public String actualVersion;
	
	/**
	 * Get the minecraft-cyan version
	 * @return Minecraft-Cyan version
	 */
	public static String getMinecraftCyanVersion() {
		if (info.version.contains("${")) {
			return "Unknown";
		}
		return info.minecraftCyanVersion;
	}

	/**
	 * Get the minecraft-cyan version (with modloader, if another is present)
	 * @return Minecraft-Cyan version
	 */
	public static String getModloaderVersion() {
		if (info.version.contains("${")) {
			return "Unknown";
		}
		return info.actualVersion;
	}

	/**
	 * Get the minecraft version
	 * @return Minecraft version
	 */
	public static String getMinecraftVersion() {
		if (info.version.contains("${")) {
			return "Unknown";
		}
		return info.minecraftVersion;
	}

	/**
	 * Get the cyan version
	 * @return Cyan version
	 */
	public static String getCyanVersion() {
		if (info.version.contains("${")) {
			return "In-development";
		}
		return info.version;
	}

	/**
	 * Get the release date
	 * @return Release date
	 */
	public static OffsetDateTime getReleaseDate() {
		if (info.releaseDate.contains("${")) {
			return OffsetDateTime.now();
		}
		return OffsetDateTime.parse(info.releaseDate);
	}

	/**
	 * Get the development start date
	 * @return Development start date
	 */
	public static OffsetDateTime getDevStartDate() {
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
	public static CyanSide getSide() {
		return CyanCore.getSide();
	}
	
	/**
	 * Get the current loading phase
	 * @return CyanLoadPhase object representing the loading phase
	 */
	public static CyanLoadPhase getCurrentPhase() {
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
}
