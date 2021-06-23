package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.closureowners;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.Scanner;

import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.core.CyanUpdateInfo;
import org.asf.cyan.cornflower.gradle.Cornflower;
import org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions.CornflowerMainExtension;
import org.asf.cyan.cornflower.gradle.utilities.GradleUtil;
import org.asf.cyan.cornflower.gradle.utilities.modding.PlatformClosureOwner;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import groovy.lang.Closure;

public class IntermediaryPlatformClosureOwner extends PlatformClosureOwner {

	private File infoDir = GradleUtil.getSharedCacheFolder(Cornflower.class, "platforms");
	private CyanUpdateInfo versions;

	public static PlatformClosureOwner fromClosure(Closure<?> closure) {
		IntermediaryPlatformClosureOwner owner = new IntermediaryPlatformClosureOwner();

		String config = "";
		File manifest = new File(owner.infoDir, "manifest.ccfg");
		try {
			StringBuilder conf = new StringBuilder();
			URL u = new URL(CornflowerMainExtension.AerialWorksMaven + CyanInfo.infoPath);
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
		try {
			if (!manifest.getParentFile().exists())
				manifest.getParentFile().mkdirs();
			Files.write(manifest.toPath(), config.getBytes());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		closure.setDelegate(owner);
		closure.call();
		return owner;
	}

	public String getSupportedFabricVersion(String gameVersion, String cyanVersion) throws IOException {
		String ver = getSupportedStableFabricVersion(gameVersion, cyanVersion);
		if (ver == null)
			ver = getSupportedLatestFabricVersion(gameVersion, cyanVersion);
		if (ver == null)
			ver = getSupportedTestingFabricVersion(gameVersion, cyanVersion);
		if (ver == null)
			return getLatestFabricVersion(gameVersion);
		return ver;
	}

	public String getSupportedStableFabricVersion(String gameVersion, String cyanVersion) {
		return versions.fabricSupport.get("stable-cyan-" + gameVersion + "-" + cyanVersion);
	}

	public String getSupportedTestingFabricVersion(String gameVersion, String cyanVersion) {
		return versions.fabricSupport.get("testing-cyan-" + gameVersion + "-" + cyanVersion);
	}

	public String getSupportedLatestFabricVersion(String gameVersion, String cyanVersion) {
		return versions.fabricSupport.get("latest-cyan-" + gameVersion + "-" + cyanVersion);
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
