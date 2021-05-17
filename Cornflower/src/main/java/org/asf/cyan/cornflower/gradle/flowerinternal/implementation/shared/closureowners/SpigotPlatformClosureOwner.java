package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.closureowners;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Scanner;
import java.util.TimeZone;

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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import groovy.lang.Closure;

public class SpigotPlatformClosureOwner extends PlatformClosureOwner {

	private File infoDir = GradleUtil.getSharedCacheFolder(Cornflower.class, "cyanmanifests");
	private CyanUpdateInfo versions;

	public static PlatformClosureOwner fromClosure(Closure<?> closure) {
		SpigotPlatformClosureOwner owner = new SpigotPlatformClosureOwner();

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

		try {
			Files.write(manifest.toPath(), config.getBytes());
		} catch (IOException e1) {
			throw new RuntimeException(e1);
		}

		closure.setDelegate(owner);
		closure.call();
		return owner;
	}

	public String getRecommendedSpigotMappings(String gameVersion) {
		return versions.spigotStableMappings.get(gameVersion);
	}

	public String getLatestSpigotMappings(String gameVersion) {
		return versions.spigotLatestMappings.get(gameVersion);
	}

	public String getTestingSpigotMappings(String gameVersion) {
		return versions.spigotTestingMappings.get(gameVersion);
	}

	public String getSpigotMappingsByPaperVersion(String gameVersion, String paperVersion) {
		return versions.spigotMappings.get("paper-" + gameVersion + "-" + paperVersion);
	}

	public String getSupportedPaperVersion(String gameVersion, String cyanVersion) {
		String ver = getSupportedStablePaperVersion(gameVersion, cyanVersion);
		if (ver == null)
			ver = getSupportedLatestPaperVersion(gameVersion, cyanVersion);
		if (ver == null)
			ver = getSupportedTestingPaperVersion(gameVersion, cyanVersion);
		return ver;
	}

	public String getSupportedStablePaperVersion(String gameVersion, String cyanVersion) {
		return versions.paperSupport.get("stable-cyan-" + gameVersion + "-" + cyanVersion);
	}

	public String getSupportedTestingPaperVersion(String gameVersion, String cyanVersion) {
		return versions.paperSupport.get("testing-cyan-" + gameVersion + "-" + cyanVersion);
	}

	public String getSupportedLatestPaperVersion(String gameVersion, String cyanVersion) {
		return versions.paperSupport.get("latest-cyan-" + gameVersion + "-" + cyanVersion);
	}

	public String getRemoteSpigotMappingsVersion(String gameVersion) throws IOException {
		File infoDir = GradleUtil.getSharedCacheFolder(Cornflower.class, "platforms");
		File spigotData = new File(infoDir, "spigot-" + gameVersion + ".info");

		String ver = null;
		try {
			MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(gameVersion);
			if (version == null)
				version = new MinecraftVersionInfo(gameVersion, MinecraftVersionType.UNKNOWN, null,
						OffsetDateTime.now());

			ver = MinecraftMappingsToolkit.getLatestSpigotMappings(version);
		} catch (IOException ex) {
			if (spigotData.exists()) {
				ver = new String(Files.readAllBytes(spigotData.toPath()));
			} else {
				throw ex;
			}
		}

		Files.write(spigotData.toPath(), ver.getBytes());
		return ver;
	}

	public String getPaperVersion(String mappings) {
		return versions.paperByMappings.get(mappings);
	}

	public String getPaperVersionByDate(String version, String gameVersion) throws ParseException, IOException {
		if (versions.paperByMappings.containsKey(version))
			return versions.paperByMappings.get(version);

		String commitHash = version.substring(0, version.indexOf(":"));
		version = version.substring(version.indexOf(":") + 1);

		File infoDir = GradleUtil.getSharedCacheFolder(Cornflower.class, "platforms");
		File spigotData = new File(infoDir,
				"spigot-" + gameVersion + "-autoget-" + commitHash + "-" + version + ".info");

		String ver = "";
		try {
			URL url = new URL("https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/commits");
			InputStream strm = url.openStream();
			String data = new String(strm.readAllBytes());
			strm.close();

			ArrayList<String> commits = new ArrayList<String>();
			String[] chunks = data.split("<tr data-commitid=\"");
			chunks = Arrays.copyOfRange(chunks, 1, chunks.length);

			int loc = 0;
			int index = 0;
			for (String chunk : chunks) {
				String hash = chunk.substring(0, chunk.indexOf("\""));
				if (hash.equals(commitHash)) {
					loc = index;
				}
				commits.add(hash);
				index++;
			}

			if (loc == 0) {
				url = new URL("https://papermc.io/api/v2/projects/paper/versions/" + gameVersion);
				strm = url.openStream();
				JsonArray arr = JsonParser.parseString(new String(strm.readAllBytes())).getAsJsonObject().get("builds")
						.getAsJsonArray();
				strm.close();

				int i = arr.size() - 1;
				return arr.get(i--).getAsString();
			}
			strm.close();

			url = new URL(
					"https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/commits/" + commits.get(loc - 1));
			strm = url.openStream();
			data = new String(strm.readAllBytes());
			strm.close();

			SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
			fmt.setTimeZone(TimeZone.getTimeZone("GMT"));

			String dateStr = data.substring(data.indexOf("datetime=\"") + "datetime=\"".length());
			dateStr = dateStr.substring(0, dateStr.indexOf("\""));

			ver = getBuild(gameVersion, fmt.parse(dateStr));
		} catch (IOException e) {
			if (spigotData.exists()) {
				ver = new String(Files.readAllBytes(spigotData.toPath()));
			} else {
				throw e;
			}
		}

		Files.write(spigotData.toPath(), ver.getBytes());
		return ver;
	}

	private String getBuild(String gameVersion, Date date) throws IOException, ParseException {
		SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
		fmt.setTimeZone(TimeZone.getTimeZone("GMT"));

		URL url = new URL("https://papermc.io/api/v2/projects/paper/versions/" + gameVersion);
		InputStream strm = url.openStream();
		JsonArray arr = JsonParser.parseString(new String(strm.readAllBytes())).getAsJsonObject().get("builds")
				.getAsJsonArray();
		strm.close();

		if (arr.size() == 1) {
			return arr.get(0).getAsString();
		}

		int i = arr.size() - 1;
		String build = arr.get(i--).getAsString();
		String selectedBuild = build;

		while (true) {
			url = new URL("https://papermc.io/api/v2/projects/paper/versions/" + gameVersion + "/builds/" + build);
			strm = url.openStream();
			JsonObject obj = JsonParser.parseString(new String(strm.readAllBytes())).getAsJsonObject();
			strm.close();

			Date releaseDatePrev = null;
			try {
				String buildprev = arr.get(i--).getAsString();
				url = new URL(
						"https://papermc.io/api/v2/projects/paper/versions/" + gameVersion + "/builds/" + buildprev);
				strm = url.openStream();
				JsonObject obj2 = JsonParser.parseString(new String(strm.readAllBytes())).getAsJsonObject();
				strm.close();
				releaseDatePrev = fmt.parse(obj2.get("time").getAsString());
			} catch (Exception e) {

			}

			Date releaseDate = fmt.parse(obj.get("time").getAsString());
			if (releaseDate.before(date)) {
				break;
			}

			if (releaseDatePrev != null && releaseDatePrev.after(date)) {
				build = arr.get(i--).getAsString();
				selectedBuild = arr.get(i).getAsString();
			} else {
				break;
			}
		}

		return selectedBuild;
	}

}
