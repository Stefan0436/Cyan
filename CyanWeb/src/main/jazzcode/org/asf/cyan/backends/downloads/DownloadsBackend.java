package org.asf.cyan.backends.downloads;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import org.asf.cyan.webcomponents.downloads.DownloadPage;
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

		public String id;
		public File dir;
	}

	public static String jcEncodeParam(String input) throws UnsupportedEncodingException {
		return input.replace("%", "%25").replace(":", "%3A").replace(",", "%2C");
	}

	public static String jcEncode(String input) throws UnsupportedEncodingException {
		return URLEncoder.encode(input.replace("%", "%25").replace("?", "%3F").replace("&", "%26").replace("/", "%2F"),
				"UTF-8");
	}

	private ArrayList<CompileInfo> compileQueue = new ArrayList<CompileInfo>();
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

			runFunction("compiler", function.getRequest(), function.getResponse(), function.getPagePath(),
					(str) -> function.write(str), function.variables, (obj) -> {
					}, function.getServer(), function.getContextRoot(), function.getServerContext(),
					function.getClient(), null, null, null);

			runFunction("compiler", function.getRequest(), function.getResponse(), function.getPagePath(),
					(str) -> function.write(str), function.variables, (obj) -> {
					}, function.getServer(), function.getContextRoot(), function.getServerContext(),
					function.getClient(), null, null, null);

			runFunction("compiler", function.getRequest(), function.getResponse(), function.getPagePath(),
					(str) -> function.write(str), function.variables, (obj) -> {
					}, function.getServer(), function.getContextRoot(), function.getServerContext(),
					function.getClient(), null, null, null);
		}
	}

	private boolean selecting = false;

	class CompilerOutputStream extends OutputStream {

		private CompileInfo info;

		public CompilerOutputStream(CompileInfo compile) {
			this.info = compile;
		}

		@Override
		public void write(int arg0) throws IOException {
			info.outputLog += (char) arg0;
		}

		public void writeLine(String line) throws IOException {
			write((line + System.lineSeparator()).getBytes());
		}

	}

	@Function
	private void compiler(FunctionInfo function) throws InterruptedException {
		while (true) {
			while (selecting) {
				Thread.sleep(100);
			}
			selecting = true;

			if (this.compileQueue.size() != 0) {
				CompileInfo info = compileQueue.remove(0);
				selecting = false;

				File dir = new File(function.getServerContext().getSourceDirectory(), "cyan/versions/"
						+ info.cyanVersion + "/" + info.gameVersion + "/" + info.platform + "/" + info.platformVersion);
				if (dir.exists()) {
					info.status = "Done";
					while (true) {
						try {
							if (compilingVersions.containsKey(info.id))
								compilingVersions.remove(info.id);
							break;
						} catch (ConcurrentModificationException ex) {
						}
					}
				}

				try {
					info.outputLog += System.lineSeparator();
					CompilerOutputStream output = new CompilerOutputStream(info);
					output.writeLine("Preparing to compile...");
					info.status = "Preparing...";

					runCompiler(info, output, function);

					output.close();
				} catch (Exception e) {
					info.outputLog += "Compilation process failed!" + System.lineSeparator();
					info.outputLog += "Exception was thrown: " + e.getClass().getTypeName() + ": " + e.getMessage()
							+ System.lineSeparator();
					for (StackTraceElement ele : e.getStackTrace()) {
						info.outputLog += "    at " + ele + System.lineSeparator();
					}
					info.status = "Fatal Error";
					Thread.sleep(6000);
					while (true) {
						try {
							if (compilingVersions.containsKey(info.id))
								compilingVersions.remove(info.id);
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

	private void runCompiler(CompileInfo info, CompilerOutputStream output, FunctionInfo function)
			throws IOException, InterruptedException {
		File tmp = new File(compileDir, info.id + "-" + System.currentTimeMillis());
		while (tmp.exists()) {
			deleteDir(tmp);
			tmp = new File(compileDir, info.id + "-" + System.currentTimeMillis());
		}
		tmp.mkdirs();
		try {
			output.writeLine("Compile ID: " + tmp.getName());
			UpdateInfo manifest = null;

			info.status = "Downloading manifest...";
			String url = maven + "/org/asf/cyan/CyanVersionHolder/" + info.cyanVersion + "/CyanVersionHolder-"
					+ info.cyanVersion + "-versions.ccfg";
			ByteArrayOutputStream strm = new ByteArrayOutputStream();
			download(new URL(url), strm, info, output);
			manifest = new UpdateInfo(new String(strm.toByteArray()));

			info.status = "Downloading sources...";
			url = maven + "/org/asf/cyan/CyanLoader-Sources/" + info.cyanVersion + "/CyanLoader-Sources-"
					+ info.cyanVersion + "-full.zip";
			download(new URL(url), new File(tmp, "sources.zip"), info, output);

			info.status = "Extracting sources...";
			Unzipper zip = new Unzipper(info, new File(tmp, "sources.zip"), new File(tmp, "sources"), output);
			zip.start();

			info.status = "Preparing build command...";
			output.writeLine("Generating build command...");
			ArrayList<String> cmd = new ArrayList<String>();
			cmd.add("bash");
			cmd.add("buildlocal.sh");
			cmd.add("--version");
			cmd.add(info.gameVersion);
			if (!info.platform.equals("vanilla")) {
				cmd.add("--modloader");
				cmd.add(info.platform);
				cmd.add("--modloader-version");
				cmd.add(info.platformVersion);
				if (info.platform.equals("paper")) {
					Version target = Version.fromString(info.platformVersion);
					Version mappingsMin = null;
					Version mappingsMax = null;

					HashMap<String, String> mappings = manifest.paperByMappings;
					ArrayList<String> keys = new ArrayList<String>(manifest.paperByMappings.keySet());
					keys.sort((a, b) -> {
						String ver1 = mappings.get(a);
						String ver2 = mappings.get(b);
						return Version.fromString(ver1).compareTo(Version.fromString(ver2));
					});

					Version last = null;
					for (String ver : keys) {
						String paper = manifest.paperByMappings.get(ver);
						Version paperVer = Version.fromString(paper);
						if (last != null && last.isGreaterThan(target) && paperVer.isGreaterThan(last)) {
							mappingsMax = last;
							break;
						} else if (last == null && paperVer.isGreaterThan(target)) {
							last = paperVer;
						}
					}

					keys.sort((a, b) -> {
						String ver1 = mappings.get(a);
						String ver2 = mappings.get(b);
						return -Version.fromString(ver1).compareTo(Version.fromString(ver2));
					});

					last = null;
					for (String ver : keys) {
						String paper = manifest.paperByMappings.get(ver);
						Version paperVer = Version.fromString(paper);
						if (paperVer.isLessThan(target)) {
							mappingsMin = last;
							break;
						} else if (mappingsMax == null || paperVer.isLessOrEqualTo(mappingsMax)) {
							last = paperVer;
						}
					}
					if (mappingsMin == null && last != null)
						mappingsMin = last;
					else if (mappingsMin == null)
						mappingsMin = target;

					for (String ver : keys) {
						String paper = manifest.paperByMappings.get(ver);
						if (paper.equals(mappingsMin.toString())) {
							cmd.add("--mappings-version");
							cmd.add(ver);
							break;
						}
					}
				}
			}

			output.writeLine("Compiling...");
			info.status = "Compiling...";
			File sourcesDir = new File(tmp, "sources");
			new File(sourcesDir, "CyanLoader/nogit").createNewFile();

			runProcess(new String[] { "chmod", "+x", "gradlew" }, output, sourcesDir);
			int exit = runProcess(cmd.toArray(t -> new String[t]), output, sourcesDir);
			if (exit != 0)
				throw new IOException("Non-zero exit code of build process	");

			output.writeLine("Installing build...");
			info.status = "Installing...";
			File outputBase = new File(function.getServerContext().getSourceDirectory(), "cyan/versions/"
					+ info.cyanVersion + "/" + info.gameVersion + "/" + info.platform + "/" + info.platformVersion);
			outputBase.mkdirs();

			try {
				File buildDir = new File(sourcesDir, "build/Wrapper");
				File modkit = new File(sourcesDir, "modkit-" + info.gameVersion);
				File coremodkit = new File(sourcesDir, "coremodkit-" + info.gameVersion);
				File server = new File(buildDir, "Server jars");
				server = server.listFiles(f -> f.isDirectory())[0];

				File client = new File(buildDir, manifest.libraryVersions.get("CyanWrapper") + "/.minecraft");
				if (server.exists()) {
					File serverZip = new File(outputBase, "cyan-server-" + info.gameVersion + "-" + info.platform + "-"
							+ info.platformVersion + "-" + info.cyanVersion + ".zip");
					Zipper zipper = new Zipper(info, server, serverZip, output);
					zipper.start();
				}
				if (client.exists()) {
					deleteDir(new File(client, "versions")
							.listFiles(f -> f.isDirectory() && f.getName().endsWith("-dbg"))[0]);

					File clientZip = new File(outputBase, "cyan-client-" + info.gameVersion + "-" + info.platform + "-"
							+ info.platformVersion + "-" + info.cyanVersion + ".zip");
					Zipper zipper = new Zipper(info, client, clientZip, output);
					zipper.start();
				}
				if (modkit.exists()) {
					File modkitZip = new File(outputBase, "cyan-modkit-" + info.gameVersion + "-" + info.platform + "-"
							+ info.platformVersion + "-" + info.cyanVersion + ".zip");
					Zipper zipper = new Zipper(info, modkit, modkitZip, output);
					zipper.start();
				}
				if (coremodkit.exists()) {
					File modkitZip = new File(outputBase, "cyan-coremodkit-" + info.gameVersion + "-" + info.platform
							+ "-" + info.platformVersion + "-" + info.cyanVersion + ".zip");
					Zipper zipper = new Zipper(info, coremodkit, modkitZip, output);
					zipper.start();
				}

				info.dir = outputBase;
				info.status = "Done";
				Thread.sleep(6000);
				while (true) {
					try {
						if (compilingVersions.containsKey(info.id))
							compilingVersions.remove(info.id);
						break;
					} catch (ConcurrentModificationException ex) {
					}
				}
			} catch (Exception e) {
				deleteDir(outputBase);
				throw e;
			}
		} finally {
			deleteDir(tmp);
		}
	}

	private void download(URL u, File outputFile, CompileInfo info, CompilerOutputStream output) throws IOException {
		FileOutputStream fileOut = new FileOutputStream(outputFile);
		Downloader downloader = new Downloader(u.openConnection(), fileOut, info);
		downloader.download();
		fileOut.close();
		output.writeLine("");
	}

	private void download(URL u, OutputStream outStrm, CompileInfo info, CompilerOutputStream output)
			throws IOException {
		Downloader downloader = new Downloader(u.openConnection(), outStrm, info);
		downloader.download();
		output.writeLine("");
	}

	private void readAll(InputStream inp, CompilerOutputStream output, Process proc) {
		new Thread(() -> {
			while (true) {
				try {
					if (!proc.isAlive())
						break;
					int i = inp.read();
					if (i == -1)
						break;

					output.write(i);
				} catch (IOException e) {
					break;
				}
			}
		}).start();
	}

	private int runProcess(String[] process, CompilerOutputStream output, File dir)
			throws IOException, InterruptedException {
		ProcessBuilder builder = new ProcessBuilder(process);
		builder.directory(dir);
		Process proc = builder.start();
		readAll(proc.getErrorStream(), output, proc);
		readAll(proc.getInputStream(), output, proc);
		proc.waitFor();
		output.writeLine("");
		return proc.exitValue();
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
				Thread.sleep(1);
				if (i == 1000 * 60 * 10)
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

		versions.sort((a, b) -> -Version.fromString(a).compareTo(Version.fromString(b)));
		return versions;
	}

	@Function
	public List<String> getCyanLTSVersions(FunctionInfo function) {
		setup(function);
		ArrayList<String> versions = new ArrayList<String>();
		for (String version : manifest.longTermSupportVersions) {
			versions.add(version);
		}
		versions.sort((a, b) -> -Version.fromString(a).compareTo(Version.fromString(b)));
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
		if (cyan == null)
			return null;

		File dir = new File(function.getServerContext().getSourceDirectory(),
				"cyan/versions/" + cyan + "/" + game + "/" + platform + "/" + version);
		if (!dir.exists())
			return null;
		return dir;
	}

	@Function
	public String getCompiledVersionURI(FunctionInfo function) {
		if (function.parameters.length != 4) {
			return null;
		}

		String platform = function.parameters[0];
		String game = function.parameters[2];
		String version = function.parameters[3];
		String cyan = getCyanVersion(new FunctionInfo(function).setParams(function.parameters));

		return "/cyan/versions/" + cyan + "/" + game + "/" + platform + "/" + version;
	}

	@Function
	public void queueCompilation(FunctionInfo function) {
		setup(function);
		if (function.parameters.length != 4) {
			return;
		}

		String platform = function.parameters[0];
		String repository = function.parameters[1];
		String gameversion = function.parameters[2];
		String modloader = function.parameters[3];

		if (getCompiledVersion(function) == null && getCompilingVersion(function) == null) {
			CompileInfo info = new CompileInfo();
			info.status = "In Queue...";
			info.gameVersion = gameversion;
			info.cyanVersion = getCyanVersion(function);
			if (info.cyanVersion == null)
				return;

			info.platform = platform;
			info.platformVersion = modloader;
			info.outputLog = "In queue... Waiting for compiler slot...";
			if (modloader == null) {
				modloader = gameversion;
			}
			info.id = gameversion + "-" + platform + "-" + modloader + "-" + repository;

			compileQueue.add(info);
			compilingVersions.put(gameversion + "-" + platform + "-" + modloader + "-" + repository, info);
		}
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
			if (isEqualTo(other))
				return false;
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
			if (isEqualTo(other))
				return false;

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

	public abstract static class ProgressUtil {

		private String lastLine = "";

		public ProgressUtil(CompileInfo info) {
			this.info = info;
		}

		private CompileInfo info;
		protected String progressMessage = "";

		protected void setProgress(long value, long size) {
			progress(progressMessage, value, size);
		}

		protected void progress(String message, double value, double max) {
			info.outputLog = info.outputLog.substring(0, info.outputLog.length() - lastLine.length());

			StringBuilder progressMsg = new StringBuilder();
			progressMsg.append(message + "  ");
			for (int i = message.length(); i < 130 - 62; i++) {
				progressMsg.append(" ");
			}
			String msg = (int) ((100 / max) * value) + "";

			progressMsg.append(msg);
			for (int i = msg.length(); i < 3; i++) {
				progressMsg.append(" ");
			}
			progressMsg.append(" %  ");
			progressMsg.append(" ");

			progressMsg.append("[");
			double step = max / 50;
			double i = 0;

			double v = (value / step);
			for (i = 0; i < v; i++) {
				progressMsg.append("=");
			}
			for (double i2 = i; i2 < 50; i2++) {
				progressMsg.append(" ");
			}
			progressMsg.append("]");

			info.outputLog += progressMsg.toString();
			lastLine = progressMsg.toString();
		}

		protected void setProgressMessage(String message) {
			if (message.length() >= 130 - 62) {
				message = message.substring(0, 130 - 67);
				message += "...";
			}
			progressMessage = message;
		}

	}

	public static class Zipper extends ProgressUtil {

		private File input;
		private ZipOutputStream output;
		private CompilerOutputStream log;

		private int count(File dir) {
			int i = 0;
			File[] listFiles = dir.listFiles(t -> !t.isDirectory());
			for (int j = 0; j < listFiles.length; j++) {
				i++;
			}
			for (File d : dir.listFiles(t -> t.isDirectory())) {
				i += count(d);
				i++;
			}
			return i;
		}

		public Zipper(CompileInfo info, File input, File output, CompilerOutputStream log)
				throws ZipException, IOException {
			super(info);
			this.output = new ZipOutputStream(new FileOutputStream(output));
			this.input = input;
			this.log = log;
		}

		public void start() throws IOException {
			setProgressMessage("Zipping " + input.getName() + "...");
			int val = 0;
			int max = count(input);
			setProgress(val++, max);

			zip(input, "", val, max);

			setProgress(max, max);
			output.close();
			log.writeLine("");
		}

		private int zip(File input, String prefix, int val, int max) throws IOException {
			if (input.isDirectory()) {
				for (File f : input.listFiles(t -> t.isDirectory())) {
					ZipEntry entry = new ZipEntry(prefix + f.getName() + "/");
					output.putNextEntry(entry);
					output.closeEntry();
					val = zip(f, prefix + f.getName() + "/", val, max);
				}
				for (File f : input.listFiles(t -> !t.isDirectory()))
					val = zip(f, prefix, val, max);
				return val;
			}
			ZipEntry entry = new ZipEntry(prefix + input.getName() + (input.isDirectory() ? "/" : ""));
			output.putNextEntry(entry);
			FileInputStream strm = new FileInputStream(input);
			strm.transferTo(output);
			strm.close();
			output.closeEntry();
			setProgress(val++, max);
			return val;
		}

	}

	public static class Unzipper extends ProgressUtil {

		private ZipFile input;
		private File output;
		private CompilerOutputStream log;

		public Unzipper(CompileInfo info, File input, File output, CompilerOutputStream log)
				throws ZipException, IOException {
			super(info);
			this.input = new ZipFile(input);
			this.output = output;
			this.log = log;
		}

		public void start() throws IOException {
			setProgressMessage("Unzipping " + input.getName() + "...");
			int val = 0;
			int max = input.size();
			setProgress(val++, max);

			output.mkdirs();
			Enumeration<? extends ZipEntry> entries = input.entries();
			while (entries.hasMoreElements()) {
				ZipEntry entry = entries.nextElement();
				if (entry.getName().replace("\\", "/").endsWith("/")) {
					File dir = new File(output, entry.getName());
					dir.mkdirs();
					setProgress(val++, max);
					continue;
				}

				InputStream strm = input.getInputStream(entry);
				FileOutputStream outStrm = new FileOutputStream(new File(output, entry.getName()));
				strm.transferTo(outStrm);
				outStrm.close();
				setProgress(val++, max);
			}

			setProgress(max, max);
			input.close();
			log.writeLine("");
		}

	}

	public static class Downloader extends ProgressUtil {

		private String fileName = "";
		private boolean done = false;
		private boolean completedThread = false;

		private long value = 0;
		private long size = 0;

		private OutputStream output;
		private InputStream input;

		private long start = System.currentTimeMillis();

		private long[] durations = new long[15];
		private long eta = -1;

		private int checkNum = 0;

		private Thread downloadThread = new Thread(() -> {
			while (!done) {
				checkDownload();
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					break;
				}
			}
			completedThread = true;
		}, "Download progress thread");

		public Downloader(URLConnection conn, OutputStream output, CompileInfo info) throws IOException {
			super(info);
			fileName = new File(URLDecoder.decode(conn.getURL().getFile(), "UTF-8")).getName();
			setProgressMessage("Downloading '" + fileName + "' (? KB/s)");
			setProgress(value, size);
			this.output = output;
			input = conn.getInputStream();
			size = conn.getContentLengthLong();
			downloadThread.start();
		}

		private long valueLast = 0;

		public void checkDownload() {
			long end = System.currentTimeMillis();

			if ((end - start) > 1000) {
				long val = value - valueLast;
				valueLast = value;

				long bS = val;
				String unit = "B";
				if (val > 1024) {
					val = val / 1024;
					unit = "KB";
					if (val > 1024) {
						val = val / 1024;
						unit = "MB";
						if (val > 1024) {
							val = val / 1024;
							unit = "GB";
							if (val > 1024) {
								val = val / 1024;
								unit = "TB";
							}
						}
					}
				}

				start = System.currentTimeMillis();
				long remainingSec = 0;
				if (bS != 0)
					remainingSec = (size - value) / bS;

				if (checkNum == 15) {
					checkNum = 0;
					eta = LongStream.of(durations).sorted().findFirst().getAsLong();
					for (int i = 0; i < durations.length; i++)
						durations[i] = 0;
				} else {
					durations[checkNum] = remainingSec;
					checkNum++;
				}

				if (eta != -1) {
					setProgressMessage("Downloading '" + fileName + "' (" + val + " " + unit + "/s - ETA: "
							+ String.format("%d:%02d:%02d", eta / 3600, (eta % 3600) / 60, (eta % 60)) + ")");
					if (eta != 0 && bS != 0)
						eta--;
				} else {
					setProgressMessage(
							"Downloading '" + fileName + "' (" + val + " " + unit + "/s - ETA: CALCULATING)");
				}
			}

			setProgress(value, size);
		}

		public void download() throws IOException {
			while (!done) {
				try {
					int b = input.read();
					switch (b) {
					case -1:
						done = true;
						break;
					default:
						output.write(b);
						value++;
					}
				} catch (IOException e) {
					done = true;
					throw e;
				}
			}
			while (!completedThread) {
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
				}
			}
			progress(progressMessage, 100, 100);
		}
	}

}
