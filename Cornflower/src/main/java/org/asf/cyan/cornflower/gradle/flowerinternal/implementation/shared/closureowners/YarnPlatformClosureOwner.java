package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.closureowners;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.OffsetDateTime;
import java.util.Scanner;

import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.core.CyanUpdateInfo;
import org.asf.cyan.cornflower.gradle.Cornflower;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.CyanModloader;
import org.asf.cyan.cornflower.gradle.utilities.GradleUtil;
import org.asf.cyan.cornflower.gradle.utilities.modding.PlatformClosureOwner;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import groovy.lang.Closure;

public class YarnPlatformClosureOwner extends PlatformClosureOwner {

	private File infoDir = GradleUtil.getSharedCacheFolder(Cornflower.class, "platforms");
	private CyanUpdateInfo versions;

	public static PlatformClosureOwner fromClosure(Closure<?> closure) {
		YarnPlatformClosureOwner owner = new YarnPlatformClosureOwner();

		String config = "";
		File manifest = new File(owner.infoDir, "manifest.ccfg");
		try {
			StringBuilder conf = new StringBuilder();
			URL u = new URL(CyanModloader.maven + CyanInfo.infoPath);
			Scanner sc = new Scanner(u.openStream());
			while (sc.hasNext())
				conf.append(sc.nextLine() + System.lineSeparator());
			sc.close();

			config = conf.toString();
		} catch (IOException e) {
			if (manifest.exists())
				try {
					config = new String(Files.readAllBytes(manifest.toPath()));
				} catch (IOException e1) {
					throw new RuntimeException(e);
				}
			else
				throw new RuntimeException(e);
		}
		owner.versions = new CyanUpdateInfo(config);

		closure.setDelegate(owner);
		closure.call();
		return owner;
	}

	public String getSupportedYarnVersion(String gameVersion, String cyanVersion) throws IOException {
		File yarnData = new File(infoDir, "yarn-" + gameVersion + "-" + cyanVersion + ".info");

		String ver = null;
		try {
			ver = versions.yarnMappings.get("cyan-" + gameVersion + "-" + cyanVersion);
			if (ver == null) {
				MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(gameVersion);
				if (version == null)
					version = new MinecraftVersionInfo(gameVersion, MinecraftVersionType.UNKNOWN, null,
							OffsetDateTime.now());

				ver = MinecraftMappingsToolkit.getLatestYarnVersion(version);
			}
		} catch (IOException ex) {
			if (yarnData.exists()) {
				ver = new String(Files.readAllBytes(yarnData.toPath()));
			} else {
				throw ex;
			}
		}

		Files.write(yarnData.toPath(), ver.getBytes());
		return ver;
	}

	public String getSupportedFabricVersion(String gameVersion, String cyanVersion) throws IOException {
		File loaderData = new File(infoDir, "fabric-" + gameVersion + "-" + cyanVersion + ".info");

		String ver = null;
		try {
			ver = versions.fabricSupport.get("stable-fabric-" + gameVersion);
			if (ver == null)
				ver = versions.fabricSupport.get("latest-fabric-" + gameVersion);
			if (ver == null)
				ver = versions.fabricSupport.get("testing-fabric-" + gameVersion);
			if (ver == null)
				return getLatestFabricVersion(gameVersion);
		} catch (IOException ex) {
			if (loaderData.exists()) {
				ver = new String(Files.readAllBytes(loaderData.toPath()));
			} else {
				throw ex;
			}
		}

		Files.write(loaderData.toPath(), ver.getBytes());
		return ver;
	}

	public String getYarnVersion(String gameVersion) throws IOException {
		File yarnData = new File(infoDir, "yarn-" + gameVersion + ".info");

		String ver = null;
		try {
			MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(gameVersion);
			if (version == null)
				version = new MinecraftVersionInfo(gameVersion, MinecraftVersionType.UNKNOWN, null,
						OffsetDateTime.now());

			ver = MinecraftMappingsToolkit.getLatestYarnVersion(version);
		} catch (IOException ex) {
			if (yarnData.exists()) {
				ver = new String(Files.readAllBytes(yarnData.toPath()));
			} else {
				throw ex;
			}
		}

		Files.write(yarnData.toPath(), ver.getBytes());
		return ver;
	}

	public String getLatestFabricVersion(String gameVersion) throws IOException {
		File fabricData = new File(infoDir, "fabric-" + gameVersion + ".info");
		String ver = null;
		try {
			URL data = new URL("https://meta.fabricmc.net/v2/versions/loader/" + gameVersion);
			InputStream strm = data.openStream();
			JsonObject json = JsonParser.parseString(new String(strm.readAllBytes())).getAsJsonArray().get(0)
					.getAsJsonObject();
			ver = json.get("loader").getAsJsonObject().get("version").getAsString();
		} catch (IOException ex) {
			if (fabricData.exists()) {
				ver = new String(Files.readAllBytes(fabricData.toPath()));
			} else {
				throw ex;
			}
		}

		Files.write(fabricData.toPath(), ver.getBytes());
		return ver;
	}

}
