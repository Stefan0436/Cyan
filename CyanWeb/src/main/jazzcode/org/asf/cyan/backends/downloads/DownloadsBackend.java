package org.asf.cyan.backends.downloads;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.asf.cyan.api.versioning.Version;

import org.asf.cyan.backends.downloads.DownloadsBackend.ZipInfo.ZipStatus;
import org.asf.cyan.webcomponents.downloads.DownloadPage;
import org.asf.cyan.webcomponents.downloads.GameVersionSelection;
import org.asf.cyan.webcomponents.downloads.Home;
import org.asf.cyan.webcomponents.downloads.ModloaderVersionSelection;
import org.asf.cyan.webcomponents.downloads.Platform;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.JWebService;
import org.asf.jazzcode.components.annotations.Function;

public class DownloadsBackend extends JWebService {

	private File zipDir = new File(System.getProperty("java.io.tmpdir"), "cyanweb/sourceziptmp");

	public static class ZipInfo {
		public ZipStatus status;

		public static enum ZipStatus {
			DONE, IN_PROGRESS, NEVER_STARTED, FAILURE
		}

		public String platform;
		public String platformVersion;
		public String gameVersion;
		public String cyanVersion;

		public String id;
	}

	private ArrayList<ZipInfo> zipQueue = new ArrayList<ZipInfo>();
	private HashMap<String, ZipInfo> zippingVersions = new HashMap<String, ZipInfo>();

	private boolean selecting = false;

	@Function
	private void sourceZipper(FunctionInfo function) throws InterruptedException {
		while (true) {
			while (selecting) {
				Thread.sleep(100);
			}
			selecting = true;

			if (this.zipQueue.size() != 0) {
				ZipInfo info = zipQueue.remove(0);
				selecting = false;

				File sources = new File(function.getServerContext().getSourceDirectory(),
						"cyanfiles/" + info.cyanVersion + "/" + info.gameVersion + "/" + info.platform + "/"
								+ info.platformVersion + "/cyan-sources-" + info.cyanVersion + ".zip");

				if (sources.exists()) {
					info.status = ZipStatus.DONE;
					while (true) {
						try {
							if (zippingVersions.containsKey(info.id))
								zippingVersions.remove(info.id);
							break;
						} catch (ConcurrentModificationException ex) {
						}
					}
				}

				File tmp = new File(zipDir, info.id + "-" + System.currentTimeMillis());
				while (tmp.exists()) {
					deleteDir(tmp);
					tmp = new File(zipDir, info.id + "-" + System.currentTimeMillis());
				}

				tmp.mkdirs();
				try {
					if (runProcess(new String[] { "git", "clone", git }, tmp) != 0) {
						throw new IOException("Non-zero git exit code");
					}

					File output = new File(tmp, "cyan-sources-" + info.cyanVersion + ".zip");
					FileOutputStream strmO = new FileOutputStream(output);
					ZipOutputStream zip = new ZipOutputStream(strmO);
					zip(new File(tmp, "Cyan"), "", zip);
					zip.close();
					strmO.close();

					File target = new File(function.getServerContext().getSourceDirectory(),
							"cyanfiles/" + info.cyanVersion + "/" + info.gameVersion + "/" + info.platform + "/"
									+ info.platformVersion + "/cyan-sources-" + info.cyanVersion + ".zip");
					if (!target.getParentFile().exists())
						target.mkdirs();

					Files.copy(output.toPath(), target.toPath());
					info.status = ZipStatus.DONE;
					Thread.sleep(30 * 1000);

					deleteDir(tmp);
					while (true) {
						try {
							if (zippingVersions.containsKey(info.id))
								zippingVersions.remove(info.id);
							break;
						} catch (ConcurrentModificationException ex) {
						}
					}
				} catch (IOException | InterruptedException e) {
					e.printStackTrace();
					info.status = ZipStatus.FAILURE;
					Thread.sleep(30 * 1000);

					deleteDir(tmp);
					while (true) {
						try {
							if (zippingVersions.containsKey(info.id))
								zippingVersions.remove(info.id);
							break;
						} catch (ConcurrentModificationException ex) {
						}
					}
				}
			}
			selecting = false;
			Thread.sleep(10000);
		}
	}

	private void zip(File input, String prefix, ZipOutputStream output) throws IOException {
		if (input.isDirectory()) {
			for (File f : input.listFiles(t -> t.isDirectory())) {
				ZipEntry entry = new ZipEntry(prefix + f.getName() + "/");
				output.putNextEntry(entry);
				output.closeEntry();
			}
			for (File f : input.listFiles(t -> !t.isDirectory()))
				zip(f, prefix, output);
			return;
		}
		ZipEntry entry = new ZipEntry(prefix + input.getName() + (input.isDirectory() ? "/" : ""));
		output.putNextEntry(entry);
		FileInputStream strm = new FileInputStream(input);
		strm.transferTo(output);
		strm.close();
		output.closeEntry();
	}

	private int runProcess(String[] process, File dir) throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(process);
		builder.directory(dir);
		builder.inheritIO();
		Process proc = builder.start();
		proc.waitFor();
		return proc.exitValue();
	}

	@Function
	public File getSourceZip(FunctionInfo function) {
		if (function.parameters.length != 4) {
			return null;
		}

		String platform = function.parameters[0];
		String game = function.parameters[2];
		String version = function.parameters[3];
		String cyan = getCyanVersion(new FunctionInfo(function).setParams(function.parameters));
		if (cyan == null)
			return null;

		File zip = new File(function.getServerContext().getSourceDirectory(),
				"cyanfiles/" + cyan + "/" + game + "/" + platform + "/" + version + "/cyan-sources-" + cyan + ".zip");
		if (!zip.exists())
			return null;

		return zip;
	}

	@Function
	public ZipStatus getZipStatus(FunctionInfo function) {
		if (function.parameters.length != 4) {
			return null;
		}

		String platform = function.parameters[0];
		String repository = function.parameters[1];
		String game = function.parameters[2];
		String version = function.parameters[3];

		if (!zippingVersions.containsKey(game + "-" + platform + "-" + version + "-" + repository))
			if (getSourceZip(function) != null)
				return ZipStatus.DONE;
			else
				return ZipStatus.NEVER_STARTED;

		return zippingVersions.get(game + "-" + platform + "-" + version + "-" + repository).status;
	}

	@Function
	public void queueZip(FunctionInfo function) {
		setup(function);
		if (function.parameters.length != 4) {
			return;
		}

		String platform = function.parameters[0];
		String repository = function.parameters[1];
		String gameversion = function.parameters[2];
		String modloader = function.parameters[3];

		if (getZipStatus(function) == ZipStatus.NEVER_STARTED) {
			ZipInfo info = new ZipInfo();

			info.status = ZipStatus.IN_PROGRESS;
			info.gameVersion = gameversion;
			info.cyanVersion = getCyanVersion(function);
			if (info.cyanVersion == null)
				return;

			info.platform = platform;
			info.platformVersion = modloader;
			if (modloader == null) {
				modloader = gameversion;
			}
			info.id = gameversion + "-" + platform + "-" + modloader + "-" + repository;

			zipQueue.add(info);
			zippingVersions.put(gameversion + "-" + platform + "-" + modloader + "-" + repository, info);
		}
	}

	public static String jcEncodeParam(String input) {
		return input.replace("%", "%25").replace(":", "%3A").replace(",", "%2C");
	}

	public static String jcEncode(String input) throws UnsupportedEncodingException {
		return URLEncoder.encode(input.replace("%", "%25").replace("?", "%3F").replace("&", "%26").replace("/", "%2F"),
				"UTF-8");
	}

	private static boolean ready = false;

	private static String git = "https://aerialworks.ddns.net/ASF/Cyan.git";
	private static String maven = "https://aerialworks.ddns.net/maven/";
	private static String releases = "https://aerialworks.ddns.net/cyan/releases/";
	private static String url = maven
			+ "/org/asf/cyan/CyanVersionHolder/generic/CyanVersionHolder-generic-versions.ccfg";

	public static String getMavenRepo() {
		return maven;
	}

	private UpdateInfo manifest;

	public void deleteDir(File dir) {
		for (File f : dir.listFiles(t -> !t.isDirectory())) {
			f.delete();
		}
		for (File d : dir.listFiles(t -> t.isDirectory())) {
			deleteDir(d);
		}
		dir.delete();
	}

	public static boolean isReady() {
		return ready;
	}

	@Function
	public UpdateInfo getManifest(FunctionInfo function) {
		return manifest;
	}

	@Override
	protected void startService() {
		if (zipDir.exists())
			deleteDir(zipDir);
		zipDir.mkdirs();

		try {
			URL u = new URL(url);
			InputStream strm = u.openStream();
			manifest = new UpdateInfo(new String(strm.readAllBytes()));
			strm.close();
		} catch (IOException e) {
			manifest = new UpdateInfo(new String(""));
		}

		ready = true;
	}

	private static boolean refreshed = false;
	private static boolean refreshing = false;

	private void setup(FunctionInfo function) {
		if (!refreshed) {
			refreshed = true;
			runFunction("refresh", function.getRequest(), function.getResponse(), function.getPagePath(),
					(str) -> function.write(str), function.variables, (obj) -> {
					}, function.getServer(), function.getContextRoot(), function.getServerContext(),
					function.getClient(), null, null, null);
			while (refreshing) {
			}

			runFunction("sourceZipper", function.getRequest(), function.getResponse(), function.getPagePath(),
					(str) -> function.write(str), function.variables, (obj) -> {
					}, function.getServer(), function.getContextRoot(), function.getServerContext(),
					function.getClient(), null, null, null);

			runFunction("sourceZipper", function.getRequest(), function.getResponse(), function.getPagePath(),
					(str) -> function.write(str), function.variables, (obj) -> {
					}, function.getServer(), function.getContextRoot(), function.getServerContext(),
					function.getClient(), null, null, null);

			runFunction("sourceZipper", function.getRequest(), function.getResponse(), function.getPagePath(),
					(str) -> function.write(str), function.variables, (obj) -> {
					}, function.getServer(), function.getContextRoot(), function.getServerContext(),
					function.getClient(), null, null, null);
		}
	}

	@Function
	public boolean hasHighSupport(FunctionInfo function) {
		setup(function);
		if (function.parameters.length != 4) {
			return false;
		}

		String platform = function.parameters[0];
		String repository = function.parameters[1];
		String gameVersion = function.parameters[2];
		String loaderVersion = function.parameters[3];

		Map<String, String> versions = getVersionMap(platform);
		if (repository.equals("testing")) {
			if (checkVersions(versions, platform, gameVersion, loaderVersion,
					new String[] { "testing", "latest", "stable", "lts" }))
				return true;
		} else if (repository.equals("latest")) {
			if (checkVersions(versions, platform, gameVersion, loaderVersion,
					new String[] { "latest", "stable", "lts" }))
				return true;
		} else if (repository.equals("stable")) {
			if (checkVersions(versions, platform, gameVersion, loaderVersion, new String[] { "stable", "lts" }))
				return true;
		} else if (repository.startsWith("lts-") || repository.equals("lts")) {
			if (checkVersions(versions, platform, gameVersion, loaderVersion, new String[] { "lts" }))
				return true;
		}

		return false;
	}

	@Function
	public String createInstallerDownloadURL(FunctionInfo function) {
		setup(function);
		if (function.parameters.length != 4) {
			return null;
		}

		String platform = function.parameters[0];
		String gameVersion = function.parameters[2];
		String loaderVersion = function.parameters[3];
		String cyanVersion = getCyanVersion(function);

		String identifier = gameVersion;
		if (!platform.equals("vanilla")) {
			identifier += "-" + platform + "-" + loaderVersion;
		}

		return releases + cyanVersion + "/installers/" + gameVersion + "/" + identifier + "-cyan-" + cyanVersion
				+ "-installer.jar";
	}

	private boolean checkVersions(Map<String, String> versions, String platform, String gameVersion,
			String loaderVersion, String[] repositories) {
		for (String repo : repositories) {
			if (repo.equals("lts")) {
				for (String version : versions.keySet().stream()
						.filter(t -> t.startsWith("lts-") && t.endsWith("-" + platform + "-" + gameVersion))
						.toArray(t -> new String[t])) {
					if (version.equals(loaderVersion))
						return true;
				}
			}
			if (versions.containsKey(repo + "-" + platform + "-" + gameVersion)
					&& loaderVersion.equals(versions.get(repo + "-" + platform + "-" + gameVersion))) {
				return true;
			}
		}
		return false;
	}

	@Function
	private void refresh(FunctionInfo function) throws InterruptedException, IOException {
		while (true) {
			refreshing = true;
			try {
				URL u = new URL(url);
				InputStream strm = u.openStream();
				manifest = new UpdateInfo(new String(strm.readAllBytes()));
				strm.close();
			} catch (IOException e) {

			}
			refreshing = false;
			int i = 0;
			while (true) {
				i++;
				Thread.sleep(1000);
				if (i == 600)
					break;
			}
		}
	}

	@Function
	public String getFirstType(FunctionInfo function) {
		setup(function);
		String version = manifest.latestStableVersion;
		if (version.equals("")) {
			version = manifest.latestPreviewVersion;
		} else {
			return "stable";
		}
		if (version.equals("")) {
			version = manifest.latestBetaVersion;
		} else {
			return "latest";
		}
		if (version.equals("")) {
			version = manifest.latestAlphaVersion;
		} else {
			return "testing";
		}
		if (version.equals("")) {
			return null;
		} else {
			return "testing";
		}
	}

	@Function
	public void setupWizard(FunctionInfo function) throws InvocationTargetException, IOException {
		if (isDown())
			return;
		setup(function);
		String page = function.namedParameters.getOrDefault("page", "home");

		if (page.equals("home"))
			new Home().install(function);
		else if (page.equals("gameversion"))
			new GameVersionSelection().install(function);
		else if (page.equals("platform"))
			new Platform().install(function);
		else if (page.equals("modloaderversions"))
			new ModloaderVersionSelection().install(function);
		else if (page.equals("downloads"))
			new DownloadPage().install(function);
	}

	@Function
	public List<String> getVersions(FunctionInfo function) {
		setup(function);
		ArrayList<String> versions = new ArrayList<String>();
		if (function.parameters.length == 2) {
			String platform = function.parameters[0];
			String repository = function.parameters[1];

			if (platform.equals("vanilla")) {
				for (String version : manifest.byGameVersions.keySet()) {
					if (version.endsWith("-" + repository)) {
						versions.add(version.substring(0, version.indexOf("-" + repository)));
					}
				}
			} else {
				Map<String, String> versionMap = getVersionMap(platform);
				for (String version : versionMap.keySet()) {
					if (version.startsWith(repository + "-" + platform + "-")) {
						String ver = version.substring((repository + "-" + platform + "-").length());
						if (!versions.contains("ver"))
							versions.add(ver);
					}
				}
			}
		}

		versions.sort((a, b) -> a.compareTo(b));
		versions.sort((a, b) -> -Version.fromString(a).compareTo(Version.fromString(b)));
		return versions;
	}

	private Map<String, String> getVersionMap(String platform) {
		Map<String, String> versionMap = null;

		if (platform.equals("forge"))
			versionMap = manifest.forgeSupport;
		else if (platform.equals("fabric"))
			versionMap = manifest.fabricSupport;
		else if (platform.equals("paper"))
			versionMap = manifest.paperSupport;
		else if (platform.equals("vanilla"))
			versionMap = manifest.byGameVersions;

		return versionMap;
	}

	@Function
	public List<String> getCyanLTSVersions(FunctionInfo function) {
		setup(function);
		ArrayList<String> versions = new ArrayList<String>();
		for (String version : manifest.longTermSupportVersions) {
			versions.add(version);
		}
		versions.sort((a, b) -> a.compareTo(b));
		versions.sort((a, b) -> -Version.fromString(a).compareTo(Version.fromString(b)));
		return versions;
	}

	@Function
	public File getVersionDir(FunctionInfo function) {
		if (function.parameters.length != 4) {
			return null;
		}

		String platform = function.parameters[0];
		String game = function.parameters[2];
		String version = function.parameters[3];
		String cyan = getCyanVersion(new FunctionInfo(function).setParams(function.parameters));
		if (cyan == null)
			return null;

		File dir = new File(function.getServerContext().getSourceDirectory(),
				"cyanfiles/" + cyan + "/" + game + "/" + platform + "/" + version);
		return dir;
	}

	@Function
	public String getVersionURI(FunctionInfo function) {
		if (function.parameters.length != 4) {
			return null;
		}

		String platform = function.parameters[0];
		String game = function.parameters[2];
		String version = function.parameters[3];
		String cyan = getCyanVersion(new FunctionInfo(function).setParams(function.parameters));

		String root = function.getContextRoot();
		if (!root.endsWith("/"))
			root += "/";
		return root + "cyanfiles/" + cyan + "/" + game + "/" + platform + "/" + version;
	}

	@Function
	public String getCyanVersion(FunctionInfo function) {
		setup(function);
		if (function.parameters.length != 4) {
			return null;
		}

		String platform = function.parameters[0];
		String repository = function.parameters[1];
		String gameversion = function.parameters[2];
		String modloader = function.parameters[3];

		if (platform.equals("vanilla")) {
			for (String version : manifest.byGameVersions.keySet()) {
				if (version.endsWith("-" + repository)) {
					String ver = version.substring(0, version.indexOf("-" + repository));
					if (ver.equals(gameversion))
						return manifest.byGameVersions.get(version);
				}
			}
		} else {
			Map<String, String> versionMap = null;

			if (platform.equals("forge"))
				versionMap = manifest.forgeSupport;
			else if (platform.equals("fabric"))
				versionMap = manifest.fabricSupport;
			else if (platform.equals("paper"))
				versionMap = manifest.paperSupport;

			for (String version : versionMap.keySet()) {
				if (version.startsWith(gameversion + "-") && version.endsWith("-" + repository)) {
					String ver = version.substring((gameversion + "-").length(), version.indexOf("-" + repository));
					if (ver.equals(modloader))
						return versionMap.get(version);
				}
			}
		}

		return null;
	}

	@Function
	public List<String> getModloaderVersions(FunctionInfo function) {
		setup(function);
		ArrayList<String> versions = new ArrayList<String>();
		if (function.parameters.length == 3) {
			String gameversion = function.parameters[0];
			String platform = function.parameters[2];
			String repository = function.parameters[1];

			if (!platform.equals("vanilla")) {
				Map<String, String> versionMap = null;

				if (platform.equals("forge"))
					versionMap = manifest.forgeSupport;
				else if (platform.equals("fabric"))
					versionMap = manifest.fabricSupport;
				else if (platform.equals("paper"))
					versionMap = manifest.paperSupport;
				if (versionMap == null) {
					return versions;
				}

				for (String version : versionMap.keySet()) {
					if (version.startsWith(gameversion + "-") && version.endsWith("-" + repository)) {
						String ver = version.substring((gameversion + "-").length(), version.indexOf("-" + repository));
						if (!versions.contains("ver"))
							versions.add(ver);
					}
				}
			}
		}

		versions.sort((a, b) -> a.compareTo(b));
		versions.sort((a, b) -> -Version.fromString(a).compareTo(Version.fromString(b)));
		return versions;
	}

	public boolean isDown() {
		return new File("cyanserver.shutdownbackend").exists();
	}

	@Function
	public String createModKitDownloadURL(FunctionInfo function) {
		setup(function);
		if (function.parameters.length != 5) {
			return null;
		}

		String gameVersion = function.parameters[2];
		String cyanVersion = getCyanVersion(new FunctionInfo(function).setParams(function.parameters[0],
				function.parameters[1], function.parameters[2], function.parameters[3]));
		String type = function.parameters[4];

		return releases + cyanVersion + "/" + type + "s/" + gameVersion + "/" + type + "-" + gameVersion + "-"
				+ cyanVersion + ".zip";
	}

}
