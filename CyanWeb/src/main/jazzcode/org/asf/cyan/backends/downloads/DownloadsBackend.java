package org.asf.cyan.backends.downloads;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.asf.cyan.webcomponents.downloads.GameVersionSelection;
import org.asf.cyan.webcomponents.downloads.Home;
import org.asf.cyan.webcomponents.downloads.ModloaderVersionSelection;
import org.asf.cyan.webcomponents.downloads.Platform;
import org.asf.jazzcode.components.FunctionInfo;
import org.asf.jazzcode.components.JWebService;
import org.asf.jazzcode.components.annotations.Function;

public class DownloadsBackend extends JWebService {

	private File compileDir = new File(System.getProperty("java.io.tmpdir"), "cyanweb/compile");

	public class CompileInfo {
		public String status;
		public String platform;
		public String platformVersion;
		public String gameVersion;
		public String cyanVersion;
		public String outputLog;
		public File dir;
	}

	private HashMap<String, CompileInfo> compilingVersions = new HashMap<String, CompileInfo>();

	private static boolean ready = false;

	private static String maven = "https://aerialworks.ddns.net/maven/";
	private static String url = maven
			+ "/org/asf/cyan/CyanVersionHolder/generic/CyanVersionHolder-generic-versions.ccfg";

	private UpdateInfo manifest;

	private void deleteDir(File dir) {
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
		if (compileDir.exists())
			deleteDir(compileDir);
		compileDir.mkdirs();

		try {
			URL u = new URL(url);
			InputStream strm = u.openStream();
			manifest = new UpdateInfo(new String(strm.readAllBytes()));
			strm.close();
		} catch (IOException e) {

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
		}
	}

	@Function
	private void refresh(FunctionInfo function) throws InterruptedException, IOException {
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
			Thread.sleep(1);
			if (i == 1000 * 60 * 10)
				break;
		}

		runFunction("refresh", function.getRequest(), function.getResponse(), function.getPagePath(),
				(str) -> function.write(str), function.variables, (obj) -> {
				}, function.getServer(), function.getContextRoot(), function.getServerContext(), function.getClient(),
				null, null, null);
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
				Map<String, String> versionMap = null;

				if (platform.equals("forge"))
					versionMap = manifest.forgeSupport;
				else if (platform.equals("fabric"))
					versionMap = manifest.fabricSupport;
				else if (platform.equals("paper"))
					versionMap = manifest.paperSupport;

				for (String version : versionMap.keySet()) {
					if (version.startsWith(repository + "-" + platform + "-")) {
						String ver = version.substring((repository + "-" + platform + "-").length());
						if (!versions.contains("ver"))
							versions.add(ver);
					}
				}
			}
		}
		return versions;
	}

	@Function
	public CompileInfo getCompilingVersion(FunctionInfo function) {
		if (function.parameters.length != 4) {
			return null;
		}

		String platform = function.parameters[0];
		String repository = function.parameters[1];
		String game = function.parameters[2];
		String version = function.parameters[3];
		return compilingVersions.get(game + "-" + platform + "-" + version + "-" + repository);
	}

	@Function
	public File getCompiledVersion(FunctionInfo function) {
		if (function.parameters.length != 4) {
			return null;
		}

		String platform = function.parameters[0];
		String game = function.parameters[2];
		String version = function.parameters[3];
		String cyan = getCyanVersion(new FunctionInfo(function).setParams(function.parameters));

		File dir = new File(function.getServerContext().getSourceDirectory(),
				"cyan/versions/" + cyan + "/" + game + "/" + platform + "/" + version);
		if (!dir.exists())
			return null;
		return dir;
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

				for (String version : versionMap.keySet()) {
					if (version.startsWith(gameversion + "-") && version.endsWith("-" + repository)) {
						String ver = version.substring((gameversion + "-").length(), version.indexOf("-" + repository));
						if (!versions.contains("ver"))
							versions.add(ver);
					}
				}
			}
		}

		versions.sort((a, b) -> -Version.fromString(a).compareTo(Version.fromString(b)));
		return versions;
	}

	public static class Version {

		private ArrayList<VersionSegment> segments = new ArrayList<VersionSegment>();

		protected static class VersionSegment {
			public String data = null;
			public int value = -1;

			public int separator = -1;
			public boolean hasSeparator = false;

			@Override
			public String toString() {
				return data + (separator != -1 ? (char) separator : "");
			}
		}

		protected Version() {
		}

		/**
		 * Parses the given string into a version wrapper instance.
		 * 
		 * @param version Version String
		 * @return Version instance.
		 */
		public static Version fromString(String version) {
			Version ver = new Version();
			return ver.parse(version);
		}

		private Version parse(String version) {
			segments.clear();

			boolean lastWasAlpha = false;
			boolean first = true;

			VersionSegment last = new VersionSegment();
			segments.add(last);

			for (char ch : version.toCharArray()) {
				if (!first) {
					if (last.data != null) {
						if ((Character.isAlphabetic(ch) && !lastWasAlpha) || (Character.isDigit(ch) && lastWasAlpha)) {
							if (last.data.matches("^[0-9]+$"))
								last.value = Integer.valueOf(last.data);

							last.hasSeparator = true;
							if (last.value == -1 && last.data != null && last.data.length() > 0
									&& Character.isAlphabetic(last.data.charAt(0)))
								last.value = last.data.charAt(0);

							last = new VersionSegment();
							segments.add(last);
						}
					}
				}

				if (!Character.isDigit(ch) && !Character.isAlphabetic(ch)) {
					if (first) {
						continue;
					}

					if (last.data != null) {
						if (last.data.matches("^[0-9]+$"))
							last.value = Integer.valueOf(last.data);

						last.separator = ch;
						last.hasSeparator = true;
						last = new VersionSegment();
						segments.add(last);
					}
					continue;
				}

				if (Character.isAlphabetic(ch) && lastWasAlpha) {
					if (last.value == -1)
						last.value = last.data.charAt(0);
					last.data += ch;
					continue;
				}

				if (last.data == null) {
					last.data = "";
				}
				last.data += ch;

				lastWasAlpha = Character.isAlphabetic(ch);
				first = false;
			}

			if (last.data != null && last.data.matches("^[0-9]+$"))
				last.value = Integer.valueOf(last.data);
			if (last.value == -1 && last.data != null && last.data.length() > 0
					&& Character.isAlphabetic(last.data.charAt(0)))
				last.value = last.data.charAt(0);

			return this;
		}

		/**
		 * Compares this version to another
		 * 
		 * @param other Version to compare to
		 * @return 1 if greater, 0 if equal and -1 if less.
		 */
		public int compareTo(Version other) {
			if (isEqualTo(other))
				return 0;
			if (isGreaterThan(other))
				return 1;
			if (isLessThan(other))
				return -1;

			return 0;
		}

		public boolean isEqualTo(Version other) {
			if (other.segments.size() != segments.size())
				return false;

			int i = 0;
			for (VersionSegment segment : segments) {
				VersionSegment otherSegment = other.segments.get(i);
				if (segment.value != otherSegment.value)
					return false;
				i++;
			}

			return true;
		}

		public boolean isGreaterThan(Version other) {
			int i = 0;
			for (VersionSegment segment : segments) {
				if (i >= other.segments.size())
					return true;

				VersionSegment otherSegment = other.segments.get(i);
				if (isSnapshot(otherSegment) && !isSnapshot(segment))
					return true;
				else if (!isSnapshot(segment) && isSnapshot(otherSegment))
					return false;

				if (segment.value < otherSegment.value)
					return false;
				i++;
			}

			return true;
		}

		private boolean isSnapshot(VersionSegment t) {
			return t.toString().toLowerCase().contains("snapshot") || t.toString().toLowerCase().contains("beta")
					|| t.toString().toLowerCase().contains("alpha") || t.toString().toLowerCase().contains("pre");
		}

		public boolean isLessThan(Version other) {
			int i = 0;
			for (VersionSegment segment : segments) {
				if (i >= other.segments.size()) {
					if (isSnapshot(segment) && !other.segments.stream().anyMatch(t -> isSnapshot(t))) {
						break;
					}
					return false;
				}

				VersionSegment otherSegment = other.segments.get(i);
				if (isSnapshot(otherSegment) && !isSnapshot(segment))
					return false;
				else if (!isSnapshot(otherSegment) && isSnapshot(segment))
					return true;

				if (segment.value > otherSegment.value)
					return false;
				i++;
			}

			return true;
		}

		public boolean isGreaterOrEqualTo(Version other) {
			int comp = compareTo(other);
			return comp == 1 || comp == 0;
		}

		public boolean isLessOrEqualTo(Version other) {
			int comp = compareTo(other);
			return comp == -1 || comp == 0;
		}

		@Override
		public String toString() {
			String str = "";
			for (VersionSegment segment : segments) {
				str += segment.toString();
			}
			return str;
		}

	}
}
