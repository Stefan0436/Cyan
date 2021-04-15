package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.closureowners;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.time.OffsetDateTime;

import org.asf.cyan.cornflower.gradle.Cornflower;
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

	public static PlatformClosureOwner fromClosure(Closure<?> closure) {
		YarnPlatformClosureOwner owner = new YarnPlatformClosureOwner();
		closure.setDelegate(owner);
		closure.call();
		return owner;
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
