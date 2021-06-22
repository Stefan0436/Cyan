package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Map;
import java.util.Scanner;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.asf.aos.util.service.extra.slib.util.ArrayUtil;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.fluid.remapping.SimpleMappings;
import org.asf.cyan.minecraft.toolkits.mtk.internal.MappingsLoadEventProvider;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import org.apache.logging.log4j.LogManager;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 
 * Minecraft Mappings Toolkit: create Minecraft jar mappings for Fluid and
 * Cornflower. (Vanilla mappings only, look at CyanLoader compatibility mappings
 * for other mappings)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class MinecraftMappingsToolkit extends CyanComponent {

	static {
		try {
			yarnMetaDataURL = new URL("https://maven.modmuss50.me/net/fabricmc/yarn/maven-metadata.xml");
		} catch (MalformedURLException e1) {
		}
	}

	static Mapping<?> clientMappings = null;
	static Mapping<?> serverMappings = null;
	static MinecraftVersionInfo clientMappingsVersion = null;
	static MinecraftVersionInfo serverMappingsVersion = null;

	// MCP Mappings
	private static String mcpUrl = "https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/%mcver%-%mcp%/mcp_config-%mcver%-%mcp%.zip";

	// Yarn
	private static String yarnClassifierInput = "official";
	private static String yarnClassifierOutput = "intermediary";
	private static String yarnUrl = "https://maven.modmuss50.me/net/fabricmc/yarn/%version%/yarn-%version%-tiny.gz";
	private static URL yarnMetaDataURL;

	// Spigot
	private static String spigotInfoUrl = "https://hub.spigotmc.org/versions/%mcver%.json";
	private static String spigotDownloadUrl = "https://hub.spigotmc.org/stash/projects/SPIGOT/repos/builddata/raw/mappings/%mappings%?at=%commit%";
	private static String craftBukkitPOMUrl = "https://hub.spigotmc.org/stash/projects/SPIGOT/repos/craftbukkit/raw/pom.xml?at=%commit%";

	protected static void initComponent() {
		trace("INITIALIZE Minecraft Mappings Toolkit, caller: " + CallTrace.traceCallName());
	}

	/**
	 * Check if mappings are saved in cache
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return True if file exists, false otherwise.
	 */
	public static boolean areMappingsAvailable(MinecraftVersionInfo version, GameSide side) {
		return areMappingsAvailable("", "vanilla", version, side);
	}

	/**
	 * Check if mappings are saved in cache
	 * 
	 * @param suffix     The version suffix (can be empty, not null)
	 * @param identifier The identifier (vanilla for cyan, mcp for forge, yarn for
	 *                   fabric)
	 * @param version    Minecraft version
	 * @param side       Which side (server or client)
	 * @return True if file exists, false otherwise.
	 */
	public static boolean areMappingsAvailable(String suffix, String identifier, MinecraftVersionInfo version,
			GameSide side) {
		return new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/mappings/" + identifier + "-"
				+ version.getVersion() + suffix + "-" + side.toString().toLowerCase() + ".mappings.ccfg").exists();
	}

	/**
	 * Download version mappings into ram (vanilla minecraft)
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return Mapping object representing the version mappings
	 * @throws IOException If downloading fails
	 */
	public static VanillaMappings downloadVanillaMappings(MinecraftVersionInfo version, GameSide side)
			throws IOException {
		MinecraftInstallationToolkit.downloadVersionManifest(version);
		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");

		info("Resolving official " + side.toString().toLowerCase() + " mappings of minecraft version "
				+ version.getVersion() + "...");
		trace("CREATE scanner for MAPPINGS URL in VERSION JSON, caller: " + CallTrace.traceCallName());
		StringBuilder mappings_text = new StringBuilder();

		try (Scanner sc = new Scanner(
				new URL(MinecraftInstallationToolkit.getVersionManifest(version).get("downloads").getAsJsonObject()
						.get(side.toString().toLowerCase() + "_mappings").getAsJsonObject().get("url").getAsString())
								.openStream()).useDelimiter("\\A")) {
			trace("SCAN version " + side + " mappings, caller: " + CallTrace.traceCallName());
			while (sc.hasNext())
				mappings_text.append(sc.next()).append(System.lineSeparator());
			trace("CLOSE mappings scanner, caller: " + CallTrace.traceCallName());
			sc.close();
		}

		info("Mapping the " + side.toString().toLowerCase() + " jar mappings into CCFG format...");
		trace("MAP version " + side + " mappings into CCFG, caller: " + CallTrace.traceCallName());
		VanillaMappings mappings = new VanillaMappings().parseProGuardMappings(mappings_text.toString());

		if (MappingsLoadEventProvider.isAccepted()) {
			try {
				Modloader.getModloader().dispatchEvent("mtk.mappings.downloaded", "vanilla", version, side, mappings,
						version.toString());
			} catch (Exception e) {

			}
		}

		trace("SET " + side.toString().toLowerCase() + "Mappings property, caller: " + CallTrace.traceCallName());
		if (side.equals(GameSide.CLIENT)) {
			clientMappings = mappings;
			clientMappingsVersion = version;
		} else if (side.equals(GameSide.SERVER)) {
			serverMappings = mappings;
			serverMappingsVersion = version;
		}

		return mappings;
	}

	/**
	 * Retrieves the latest YARN mappings for a given minecraft version
	 * 
	 * @param version Minecraft version
	 * @return YARN version
	 * @throws IOException If downloading the version information fails
	 */
	public static String getLatestYarnVersion(MinecraftVersionInfo version) throws IOException {
		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		String mappingsVersion = "";
		try {
			String mapping = "";
			builder = factory.newDocumentBuilder();
			Document document = builder.parse(yarnMetaDataURL.openStream());
			Element root = document.getDocumentElement();
			NodeList versions = ((Element) root.getElementsByTagName("versioning").item(0))
					.getElementsByTagName("versions").item(0).getChildNodes();
			for (int i = 0; i < versions.getLength(); i++) {
				if (!versions.item(i).hasChildNodes())
					continue;
				String mapVer = versions.item(i).getFirstChild().getNodeValue();
				if (mapVer.startsWith(version + ".") || mapVer.startsWith(version + "+")) {
					mapping = mapVer;
				}
			}
			if (mapping != "") {
				mappingsVersion = mapping;
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new IOException(e);
		}
		return mappingsVersion;
	}

	/**
	 * Download version mappings into ram (Fabric Yarn)
	 * 
	 * @param version         Minecraft version
	 * @param side            Which side (server or client)
	 * @param mappingsVersion Mappings version
	 * @return Mapping object representing the version mappings
	 * @throws IOException If downloading fails
	 */
	public static YarnMappings downloadYarnMappings(MinecraftVersionInfo version, GameSide side, String mappingsVersion)
			throws IOException {
		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");

		info("Resolving YARN " + side.toString().toLowerCase() + " mappings of minecraft version " + version + "...");
		String url = yarnUrl.replace("%version%", mappingsVersion);

		trace("CREATE StringBuilder for holding the YARN mappings, caller: " + CallTrace.traceCallName());
		StringBuilder mappings_text = new StringBuilder();

		trace("CREATE GZIPInputStream with InputStream connection to URL " + url + ", caller: "
				+ CallTrace.traceCallName());
		GZIPInputStream strm = new GZIPInputStream(new URL(url).openStream());
		trace("CREATE scanner for MAPPINGS, caller: " + CallTrace.traceCallName());
		Scanner sc = new Scanner(strm);
		trace("SCAN version mappings, caller: " + CallTrace.traceCallName());
		while (sc.hasNext())
			mappings_text.append(sc.nextLine()).append(System.lineSeparator());
		trace("CLOSE mappings scanner, caller: " + CallTrace.traceCallName());
		sc.close();
		trace("CLOSE ZipInputStream, caller: " + CallTrace.traceCallName());
		strm.close();

		info("Mapping the " + side.toString().toLowerCase() + " YARN mappings into CCFG format...");
		trace("MAP version " + side + " YARN mappings into CCFG, caller: " + CallTrace.traceCallName());
		YarnMappings mappings = new YarnMappings().parseTinyV1Mappings(mappings_text.toString(), yarnClassifierInput,
				yarnClassifierOutput);
		mappings.mappingsVersion = mappingsVersion;

		if (MappingsLoadEventProvider.isAccepted()) {
			try {
				Modloader.getModloader().dispatchEvent("mtk.mappings.downloaded", "yarn", version, side, mappings,
						mappingsVersion);
			} catch (Exception e) {

			}
		}

		trace("SET " + side.toString().toLowerCase() + "Mappings property, caller: " + CallTrace.traceCallName());
		if (side.equals(GameSide.CLIENT)) {
			clientMappings = mappings;
			clientMappingsVersion = version;
		} else if (side.equals(GameSide.SERVER)) {
			serverMappings = mappings;
			serverMappingsVersion = version;
		}

		return mappings;
	}

	/**
	 * Download version mappings into ram (Minecraft Coder Pack, the one found in
	 * forge, not the original)
	 * 
	 * @param version    Minecraft version
	 * @param side       Which side (server or client)
	 * @param mcpVersion MCP version
	 * @return Mapping object representing the version mappings
	 * @throws IOException If downloading fails
	 */
	public static McpMappings downloadMCPMappings(MinecraftVersionInfo version, GameSide side, String mcpVersion)
			throws IOException {
		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");

		info("Resolving MCP mappings of minecraft version " + version + "...");
		String url = mcpUrl.replaceAll("\\%mcver\\%", version.getVersion()).replaceAll("\\%mcp\\%", mcpVersion);

		trace("CREATE StringBuilder for holding the MCP mappings, caller: " + CallTrace.traceCallName());
		StringBuilder mappings_text = new StringBuilder();

		trace("CREATE ZipInputStream with InputStream connection to URL " + url + ", caller: "
				+ CallTrace.traceCallName());
		ZipInputStream strm = new ZipInputStream(new URL(url).openStream());
		while (strm.available() != 0) {
			ZipEntry entry = strm.getNextEntry();
			if (entry.getName().equals("config/joined.tsrg")) {
				trace("CREATE scanner for MAPPINGS, caller: " + CallTrace.traceCallName());
				Scanner sc = new Scanner(strm);
				trace("SCAN version mappings of ZIP entry: " + entry.getName() + ", caller: "
						+ CallTrace.traceCallName());
				while (sc.hasNext())
					mappings_text.append(sc.nextLine()).append(System.lineSeparator());
				trace("CLOSE mappings scanner, caller: " + CallTrace.traceCallName());
				sc.close();
				break;
			}
		}
		trace("CLOSE ZipInputStream, caller: " + CallTrace.traceCallName());
		strm.close();

		info("Mapping the " + side.toString().toLowerCase() + " MCP mappings into CCFG format...");
		trace("MAP version " + side + " MCP mappings into CCFG, caller: " + CallTrace.traceCallName());
		McpMappings mappings = new McpMappings().parseTSRGMappings(mappings_text.toString());
		mappings.mappingsVersion = mcpVersion;

		if (MappingsLoadEventProvider.isAccepted()) {
			try {
				Modloader.getModloader().dispatchEvent("mtk.mappings.downloaded", "mcp", version, side, mappings,
						mcpVersion);
			} catch (Exception e) {

			}
		}

		trace("SET " + side.toString().toLowerCase() + "Mappings property, caller: " + CallTrace.traceCallName());
		if (side.equals(GameSide.CLIENT)) {
			clientMappings = mappings;
			clientMappingsVersion = version;
		} else if (side.equals(GameSide.SERVER)) {
			serverMappings = mappings;
			serverMappingsVersion = version;
		}

		return mappings;
	}

	/**
	 * Retrieves the latest SPIGOT mappings version for a given minecraft version
	 * (returns mappings:craftbukkit)
	 * 
	 * @param version Minecraft version
	 * @return Mappings commit hash and craftbukkit version
	 * @throws IOException If retrieving the version information fails.
	 */
	public static String getLatestSpigotMappings(MinecraftVersionInfo version) throws IOException {
		JsonObject refs = getRefs(version);
		String commit = refs.get("BuildData").getAsString();

		URL u = new URL(craftBukkitPOMUrl.replaceAll("\\%mcver\\%", version.getVersion()).replaceAll("\\%commit\\%",
				getRefs(version).get("CraftBukkit").getAsString()));

		String craftBukkitVersion = "";
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder;
		try {
			builder = factory.newDocumentBuilder();
			Document document = builder.parse(u.openStream());
			Element root = document.getDocumentElement();
			craftBukkitVersion = ((Element) root.getElementsByTagName("properties").item(0))
					.getElementsByTagName("minecraft_version").item(0).getChildNodes().item(0).getNodeValue();
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new IOException(e);
		}

		return commit + ":" + craftBukkitVersion;
	}

	private static JsonObject getRefs(MinecraftVersionInfo version) throws IOException {
		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");

		URL infoURL = new URL(spigotInfoUrl.replaceAll("\\%mcver\\%", version.getVersion()));

		InputStream info = infoURL.openStream();
		JsonObject obj = JsonParser.parseReader(new InputStreamReader(info)).getAsJsonObject();
		info.close();

		return obj.get("refs").getAsJsonObject();
	}

	/**
	 * Download version mappings into ram (Paper 1.17+)
	 * 
	 * @param fallback        Fallback mappings
	 * @param version         Minecraft version
	 * @param mappingsVersion Mappings hash
	 * @return Mapping object representing the version mappings
	 * @throws IOException If downloading fails
	 */
	public static SimpleMappings downloadPaperMappings(Mapping<?> fallback, MinecraftVersionInfo version,
			String mappingsVersion) throws IOException {
		info("Resolving PAPER mappings of minecraft version " + version + "...");

		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");

		String build = mappingsVersion.split(":")[1].substring(3);
		String commit = mappingsVersion.split(":")[0];

		File mappingsDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
				"caches/mappings/paper-" + version.getVersion() + "-" + build);
		if (!mappingsDir.exists())
			mappingsDir.mkdirs();

		File officialMojangYarn = new File(mappingsDir, "official-mojang+yarn.tiny");
		File mojangYarnSpigotReobf = new File(mappingsDir, "mojang+yarn-spigot-reobf.tiny");

		File tmpDir = new File(mappingsDir, "tmp");
		if (tmpDir.exists())
			deleteDir(tmpDir);

		if (!officialMojangYarn.exists() || !mojangYarnSpigotReobf.exists()) {
			info("");
			info("");
			info("As of Minecraft 1.17, Cyan will need to compile a part of the Paper server in order to generate compatible mappings.");
			info("This can be lengthy process, please wait... This process will finish automatically...");
			info("Please make sure you are running Cyan through Java JDK 16... The process will fail otherwise...");
			info("");
			info("Git needs to be installed on the path for this to work.");
			info("");
			info("");
			info("Cloning Paper sources from GitHub... (Paper is owned by PaperMC, not by us)");
			tmpDir.mkdirs();

			ProcessBuilder builder = new ProcessBuilder("git", "clone", "-n", "https://github.com/PaperMC/Paper");
			builder.directory(tmpDir);
			Process proc = builder.start();
			attachLog(proc);
			try {
				proc.waitFor();
			} catch (InterruptedException e) {
			}
			if (proc.exitValue() != 0)
				throw new IOException("Git exited with non-zero exit code: " + proc.exitValue());
			tmpDir = new File(tmpDir, "Paper");

			builder = new ProcessBuilder("git", "checkout", commit);
			builder.directory(tmpDir);
			proc = builder.start();
			attachLog(proc);
			try {
				proc.waitFor();
			} catch (InterruptedException e) {
			}
			if (proc.exitValue() != 0)
				throw new IOException("Git exited with non-zero exit code: " + proc.exitValue());

			info("");
			info("Building Paper mappings using gradle...");
			info("Applying patches...");
			File jvm = new File(ProcessHandle.current().info().command().get()).getCanonicalFile();
			String JAVA_HOME = jvm.getParentFile().getParent();
			builder = new ProcessBuilder(jvm.getCanonicalPath(), "-cp", "gradle/wrapper/gradle-wrapper.jar",
					"org.gradle.wrapper.GradleWrapperMain", "applyPatches", "--quiet");
			builder.environment().put("JAVA_HOME", JAVA_HOME);
			builder.directory(tmpDir);
			proc = builder.start();
			attachLog(proc);

			try {
				proc.waitFor();
			} catch (InterruptedException e) {
			}
			if (proc.exitValue() != 0)
				throw new IOException("Gradle exited with non-zero exit code: " + proc.exitValue());

			info("");
			info("Generating mappings...");
			builder = new ProcessBuilder(jvm.getCanonicalPath(), "-cp", "gradle/wrapper/gradle-wrapper.jar",
					"org.gradle.wrapper.GradleWrapperMain", "patchReobfMappings", "--quiet");
			builder.environment().put("JAVA_HOME", JAVA_HOME);
			builder.directory(tmpDir);
			proc = builder.start();
			attachLog(proc);

			try {
				proc.waitFor();
			} catch (InterruptedException e) {
			}
			if (proc.exitValue() != 0)
				throw new IOException("Gradle exited with non-zero exit code: " + proc.exitValue());

			info("");
			info("Copying mappings into Cyan cache...");
			File gradleMappingsDir = new File(tmpDir, ".gradle/caches/paperweight/mappings/");
			if (officialMojangYarn.exists())
				officialMojangYarn.delete();
			if (mojangYarnSpigotReobf.exists())
				mojangYarnSpigotReobf.delete();
			Files.copy(new File(gradleMappingsDir, officialMojangYarn.getName()).toPath(), officialMojangYarn.toPath());
			Files.copy(new File(gradleMappingsDir, mojangYarnSpigotReobf.getName()).toPath(),
					mojangYarnSpigotReobf.toPath());

			info("");
			info("Deleting temporary files...");
			tmpDir = new File(mappingsDir, "tmp");
			deleteDir(tmpDir);
			info("Done.");
		}

		info("Mapping the PAPER mappings into CCFG format...");
		trace("MAP version PAPER mappings into CCFG, caller: " + CallTrace.traceCallName());

		// load mojang+yarn-spigot-reobf.tiny
		String mappingsFile = new String(Files.readAllBytes(mojangYarnSpigotReobf.toPath()));
		SimpleMappings output = new SimpleMappings().parseTinyV2Mappings(mappingsFile, "mojang+yarn", "spigot");

		// load official-mojang+yarn.tiny
		mappingsFile = new String(Files.readAllBytes(officialMojangYarn.toPath()));
		SimpleMappings input = new SimpleMappings().parseTinyV2Mappings(mappingsFile, "mojang+yarn", "official");

		// generate the combined mappings
		SimpleMappings fullMappings = new SimpleMappings();
		map(input, fallback, output, fullMappings);

		trace("SET severMappings property, caller: " + CallTrace.traceCallName());
		serverMappings = fullMappings;
		serverMappingsVersion = version;

		return fullMappings;
	}

	private static void attachLog(Process proc) {
		new Thread(() -> {
			try {
				while (proc.isAlive()) {
					String buffer = "";
					while (true) {
						int b = proc.getInputStream().read();
						if (b == -1)
							return;
						char ch = (char) b;
						if (ch == '\r')
							continue;
						else if (ch == '\n')
							break;
						buffer += ch;
					}
					LogManager.getLogger("PROCESS-LOG").info(buffer);
				}
			} catch (IOException e) {

			}
		}, "Process Logger").start();
		new Thread(() -> {
			try {
				while (proc.isAlive()) {
					String buffer = "";
					while (true) {
						int b = proc.getErrorStream().read();
						if (b == -1)
							return;
						char ch = (char) b;
						if (ch == '\r')
							continue;
						else if (ch == '\n')
							break;
						buffer += ch;
					}
					LogManager.getLogger("PROCESS-LOG").warn(buffer);
				}
			} catch (IOException e) {

			}
		}, "Process Logger").start();
	}

	private static void deleteDir(File dir) {
		for (File f : dir.listFiles(t -> !t.isDirectory())) {
			f.delete();
		}
		for (File d : dir.listFiles(t -> t.isDirectory())) {
			deleteDir(d);
		}
		dir.delete();
	}

	private static void map(SimpleMappings input, Mapping<?> helper, SimpleMappings output,
			SimpleMappings fullMappings) {
		for (Mapping<?> classMapping : output.mappings) {
			String oldType = classMapping.obfuscated;
			String type = mapClass(input, classMapping.obfuscated);
			Mapping<?> map = null;
			for (Mapping<?> classMapping2 : input.mappings) {
				if (classMapping2.obfuscated.equals(classMapping.obfuscated)) {
					map = classMapping2;
					break;
				}
			}
			if (!type.equals(classMapping.obfuscated))
				classMapping.obfuscated = type;
			else {
				type = mapClass(helper, classMapping.obfuscated);
				classMapping.obfuscated = type;
			}
			if (map != null) {
				for (Mapping<?> member : map.mappings) {
					if (member.mappingType == MAPTYPE.PROPERTY) {
						if (!Stream.of(classMapping.mappings).anyMatch(t -> t.obfuscated.equals(member.obfuscated))) {
							Mapping<?> mem = new SimpleMappings();
							mem.mappingType = member.mappingType;
							mem.obfuscated = member.name;
							mem.name = member.obfuscated;
							mem.type = mapClass(input, member.type, false);
							classMapping.mappings = ArrayUtil.append(classMapping.mappings, new Mapping[] { mem });
						}
					} else if (member.mappingType == MAPTYPE.METHOD) {
						if (!Stream.of(classMapping.mappings).anyMatch(t -> t.obfuscated.equals(member.obfuscated)
								&& Arrays.equals(t.argumentTypes, member.argumentTypes))) {
							Mapping<?> mem = new SimpleMappings();
							mem.mappingType = member.mappingType;
							mem.obfuscated = member.name;
							mem.name = member.obfuscated;
							mem.type = mapClass(input, member.type, false);
							mem.argumentTypes = mapTypes(input, member.argumentTypes, false);
							classMapping.mappings = ArrayUtil.append(classMapping.mappings, new Mapping[] { mem });
						}
					}
				}
			}
			for (Mapping<?> member : classMapping.mappings) {
				if (member.mappingType == MAPTYPE.PROPERTY) {
					member.obfuscated = mapProperty(input, oldType, member.obfuscated, true);
				} else if (member.mappingType == MAPTYPE.METHOD) {
					member.obfuscated = mapMethod(input, oldType, member.obfuscated, true,
							mapTypes(input, mapTypes(output, member.argumentTypes, false)));
				}
			}
			fullMappings.add(classMapping);
		}
	}

	private static String mapProperty(Mapping<?> mappings, String classPath, String propertyName, boolean obfuscated) {
		final String pName = propertyName;
		Mapping<?> map = mappings.mapClassToMapping(classPath, t -> Stream.of(t.mappings).anyMatch(
				t2 -> t2.mappingType == MAPTYPE.PROPERTY && (!obfuscated ? t2.name : t2.obfuscated).equals(pName)),
				obfuscated);
		if (map != null) {
			map = Stream.of(map.mappings).filter(
					t2 -> t2.mappingType == MAPTYPE.PROPERTY && (!obfuscated ? t2.name : t2.obfuscated).equals(pName))
					.findFirst().get();
			if (!obfuscated)
				propertyName = map.obfuscated;
			else
				propertyName = map.name;
		}
		return propertyName;
	}

	private static String mapMethod(Mapping<?> mappings, String classPath, String methodName, boolean obfuscated,
			String... methodParameters) {
		return mapMethod(mappings, classPath, methodName, obfuscated, false, methodParameters);
	}

	private static String mapMethod(Mapping<?> mappings, String classPath, String methodName, boolean obfuscated,
			boolean getPath, String... methodParameters) {
		final String mName = methodName;
		Mapping<?> map = mappings.mapClassToMapping(classPath,
				t -> Stream.of(t.mappings)
						.anyMatch(t2 -> t2.mappingType.equals(MAPTYPE.METHOD)
								&& (!obfuscated ? t2.name : t2.obfuscated).equals(mName)
								&& Arrays.equals(t2.argumentTypes, methodParameters)),
				obfuscated);
		if (map != null) {
			classPath = map.obfuscated;
			map = Stream.of(map.mappings)
					.filter(t2 -> t2.mappingType == MAPTYPE.METHOD
							&& (!obfuscated ? t2.name : t2.obfuscated).equals(mName)
							&& Arrays.equals(t2.argumentTypes, methodParameters))
					.findFirst().get();
			if (!obfuscated)
				methodName = map.obfuscated;
			else
				methodName = map.name;
		}
		if (getPath)
			return classPath + "." + methodName;
		else
			return methodName;
	}

	private static String mapClass(Mapping<?> mp, String input) {
		return mapClass(mp, input, true);
	}

	private static String[] mapTypes(Mapping<?> input, String[] argumentTypes) {
		return mapTypes(input, argumentTypes, true);
	}

	private static String mapClass(Mapping<?> mp, String input, boolean obfuscated) {
		String suffix = "";
		if (input.contains("[]")) {
			suffix = input.substring(input.indexOf("[]"));
			input = input.substring(0, input.indexOf("[]"));
		}
		Mapping<?> map = mp.mapClassToMapping(input, t -> true, obfuscated);
		if (map != null)
			return (!obfuscated ? map.obfuscated : map.name) + suffix;
		return input + suffix;
	}

	private static String[] mapTypes(Mapping<?> input, String[] argumentTypes, boolean obfuscated) {
		String[] newTypes = new String[argumentTypes.length];
		int i = 0;
		for (String type : argumentTypes) {
			newTypes[i++] = mapClass(input, type, obfuscated);
		}
		return newTypes;
	}

	/**
	 * Download version mappings into ram (SPIGOT)
	 * 
	 * @param fallback        Fallback mappings
	 * @param version         Minecraft version
	 * @param mappingsVersion Mappings hash
	 * @return Mapping object representing the version mappings
	 * @throws IOException If downloading fails
	 */
	public static Mapping<?> downloadSpigotMappings(Mapping<?> fallback, MinecraftVersionInfo version,
			String mappingsVersion) throws IOException {
		if (mappingsVersion.split(":")[1].startsWith("PB"))
			return downloadPaperMappings(fallback, version, mappingsVersion);
		info("Resolving SPIGOT mappings of minecraft version " + version + "...");

		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");

		String commit = mappingsVersion.substring(0, mappingsVersion.indexOf(":"));
		String craftBukkitVersion = mappingsVersion.substring(mappingsVersion.indexOf(":") + 1);

		String url = spigotDownloadUrl.replaceAll("\\%commit\\%", commit);
		trace("CREATE StringBuilder for holding the SPIGOT mappings, caller: " + CallTrace.traceCallName());
		StringBuilder classMappings = new StringBuilder();
		StringBuilder memberMapppings = new StringBuilder();
		StringBuilder packageMapppings = new StringBuilder();

		trace("CREATE URL objects for the SPIGOT mappings, caller: " + CallTrace.traceCallName());
		URL classMappingsURL = new URL(url.replace("%mappings%", "bukkit-" + version + "-cl.csrg"));
		URL memberMapppingsURL = new URL(url.replace("%mappings%", "bukkit-" + version + "-members.csrg"));
		URL packageMapppingsURL = new URL(url.replace("%mappings%", "package.srg"));

		trace("DOWNLOAD CLASS MAPPINGS from " + classMappingsURL + ", caller: " + CallTrace.traceCallName());
		Scanner sc = new Scanner(classMappingsURL.openStream());
		while (sc.hasNext())
			classMappings.append(sc.nextLine()).append(System.lineSeparator());
		trace("CLOSE mappings scanner, caller: " + CallTrace.traceCallName());
		sc.close();

		trace("DOWNLOAD MEMBER MAPPINGS from " + memberMapppingsURL + ", caller: " + CallTrace.traceCallName());
		sc = new Scanner(memberMapppingsURL.openStream());
		while (sc.hasNext())
			memberMapppings.append(sc.nextLine()).append(System.lineSeparator());
		trace("CLOSE mappings scanner, caller: " + CallTrace.traceCallName());
		sc.close();

		if (Version.fromString(version.getVersion()).isLessThan(Version.fromString("1.17"))) {
			trace("DOWNLOAD PACKAGE MAPPINGS from " + packageMapppingsURL + ", caller: " + CallTrace.traceCallName());
			sc = new Scanner(packageMapppingsURL.openStream());
			while (sc.hasNext())
				packageMapppings.append(sc.nextLine()).append(System.lineSeparator());
			trace("CLOSE mappings scanner, caller: " + CallTrace.traceCallName());
			sc.close();
		}

		info("Mapping the SPIGOT mappings into CCFG format...");
		trace("MAP version SPIGOT mappings into CCFG, caller: " + CallTrace.traceCallName());
		SpigotMappings mappings = new SpigotMappings().parseMultiMappings(fallback, classMappings.toString(),
				memberMapppings.toString(), packageMapppings.toString(),
				Version.fromString(version.getVersion()).isLessThan(Version.fromString("1.17"))
						? Map.of("net.minecraft.**", "net.minecraft.server.v" + craftBukkitVersion)
						: Map.of());
		mappings.mappingsVersion = commit;

		if (MappingsLoadEventProvider.isAccepted()) {
			try {
				Modloader.getModloader().dispatchEvent("mtk.mappings.downloaded", "spigot", version, GameSide.SERVER,
						mappings, commit);
			} catch (Exception e) {

			}
		}

		trace("SET severMappings property, caller: " + CallTrace.traceCallName());
		serverMappings = mappings;
		serverMappingsVersion = version;

		return mappings;
	}

	/**
	 * Load mappings from disk
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return Mapping object representing the version mappings
	 * @throws IOException If saving fails
	 */
	public static Mapping<?> loadMappings(MinecraftVersionInfo version, GameSide side) throws IOException {
		return loadMappings("", "vanilla", version, side);
	}

	/**
	 * Load mappings from disk
	 * 
	 * @param suffix     The version suffix (can be empty, not null)
	 * @param identifier The identifier (vanilla for cyan, mcp for forge, yarn for
	 *                   fabric)
	 * @param version    Minecraft version
	 * @param side       Which side (server or client)
	 * @return Mapping object representing the version mappings
	 * @throws IOException If saving fails
	 */
	public static Mapping<?> loadMappings(String suffix, String identifier, MinecraftVersionInfo version, GameSide side)
			throws IOException {
		if (!areMappingsAvailable(suffix, identifier, version, side))
			throw new IOException("File does not exist");
		trace("LOAD version " + version + " " + side + " mappings, caller: " + CallTrace.traceCallName());
		info("Loading " + version + " " + identifier.toUpperCase() + " " + side + " mappings...");

		File mappingsFile = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
				"caches/mappings/" + identifier + "-" + version.getVersion() + suffix + "-"
						+ side.toString().toLowerCase() + ".mappings.ccfg");

		VanillaMappings mappings = new VanillaMappings().readAll(Files.readString(mappingsFile.toPath()));

		if (MappingsLoadEventProvider.isAccepted()) {
			try {
				Modloader.getModloader().dispatchEvent("mtk.mappings.loaded", identifier, suffix, version, side,
						mappings, mappingsFile);
			} catch (Exception e) {

			}
		}

		trace("SET " + side.toString().toLowerCase() + "Mappings property, caller: " + CallTrace.traceCallName());
		if (side.equals(GameSide.CLIENT)) {
			clientMappings = mappings;
			clientMappingsVersion = version;
		} else if (side.equals(GameSide.SERVER)) {
			serverMappings = mappings;
			serverMappingsVersion = version;
		}

		return mappings;
	}

	/**
	 * Save mappings to disk
	 * 
	 * @param version Minecraft version
	 * @param side    Which side (server or client)
	 * @return Mapping object representing the version mappings
	 * @throws IOException If saving fails
	 */
	public static Mapping<?> saveMappingsToDisk(MinecraftVersionInfo version, GameSide side) throws IOException {
		return saveMappingsToDisk("", "vanilla", version, side, false);
	}

	/**
	 * Save mappings to disk
	 * 
	 * @param suffix     The version suffix (can be empty, not null)
	 * @param identifier The identifier (vanilla for cyan, mcp for forge, yarn for
	 *                   fabric)
	 * @param version    Minecraft version
	 * @param side       Which side (server or client)
	 * @return Mapping object representing the version mappings
	 * @throws IOException If saving fails
	 */
	public static Mapping<?> saveMappingsToDisk(String suffix, String identifier, MinecraftVersionInfo version,
			GameSide side) throws IOException {
		return saveMappingsToDisk(suffix, identifier, version, side, false);
	}

	/**
	 * Save mappings to disk
	 * 
	 * @param version   Minecraft version
	 * @param side      Which side (server or client)
	 * @param overwrite True to overwrite existing mappings, false throws an
	 *                  exception if already saved
	 * @return Mapping object representing the version mappings
	 * @throws IOException If saving fails
	 */
	public static Mapping<?> saveMappingsToDisk(MinecraftVersionInfo version, GameSide side, boolean overwrite)
			throws IOException {
		return saveMappingsToDisk("", "vanilla", version, side, overwrite);
	}

	/**
	 * Save mappings to disk
	 * 
	 * @param suffix     The version suffix (can be empty, not null)
	 * @param identifier The identifier (vanilla for cyan, mcp for forge, yarn for
	 *                   fabric)
	 * @param version    Minecraft version
	 * @param side       Which side (server or client)
	 * @param overwrite  True to overwrite existing mappings, false throws an
	 *                   exception if already saved
	 * @return Mapping object representing the version mappings
	 * @throws IOException If saving fails
	 */
	public static Mapping<?> saveMappingsToDisk(String suffix, String identifier, MinecraftVersionInfo version,
			GameSide side, boolean overwrite) throws IOException {
		Mapping<?> mappings = null;
		if (side.equals(GameSide.CLIENT)) {
			if (!clientMappingsVersion.getVersion().equals(version.getVersion()))
				throw new IOException(
						"Cannot write " + clientMappingsVersion + " mappings to a " + version + " mappings file.");
			mappings = clientMappings;
		} else if (side.equals(GameSide.SERVER)) {
			if (!serverMappingsVersion.getVersion().equals(version.getVersion()))
				throw new IOException(
						"Cannot write " + serverMappingsVersion + " mappings to a " + version + " mappings file.");
			mappings = serverMappings;
		}

		if (!overwrite && areMappingsAvailable(suffix, identifier, version, side))
			throw new IOException("File already exists and overwrite is set to false!");

		info("Saving " + version + " " + side.toString().toLowerCase() + " mappings...");
		String mappings_file = identifier + "-" + version.getVersion() + suffix + "-" + side.toString().toLowerCase()
				+ ".mappings.ccfg";

		trace("GENERATE CCFG mappings file, caller: " + CallTrace.traceCallName());
		debug("Generating CCFG string...");
		String generated = mappings.toString();
		debug("Preparing mappings directory...");
		trace("CREATE mappings directory IF NONEXISTENT, caller: " + CallTrace.traceCallName());
		File mappingsDir = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/mappings");
		if (!mappingsDir.exists())
			mappingsDir.mkdirs();
		debug("Generating CCFG mappings file...");
		trace("WRITE mappings file into '" + mappings_file + "', caller: " + CallTrace.traceCallName());
		Files.writeString(new File(mappingsDir, mappings_file).toPath(), generated);
		info("Saved CCFG mappings to '<mtk>/caches/mappings/" + mappings_file + "'.");

		if (MappingsLoadEventProvider.isAccepted()) {
			try {
				Modloader.getModloader().dispatchEvent("mtk.mappings.saved", identifier, suffix, version,
						GameSide.SERVER, mappings);
			} catch (Exception e) {

			}
		}

		return mappings;
	}

	/**
	 * Get the version of the currently loaded mappings
	 * 
	 * @param side Which side (server or client)
	 * @return Minecraft version of the loaded mappings for the specified side
	 */
	public static MinecraftVersionInfo getLoadedMappingsVersion(GameSide side) {
		if (side.equals(GameSide.CLIENT))
			return clientMappingsVersion;
		else if (side.equals(GameSide.SERVER))
			return serverMappingsVersion;
		else
			return null; // Can't happen yet, no other sides
	}

	/**
	 * Get the mappings for the specified side
	 * 
	 * @param side Which side (server or client)
	 * @return Mapping object representing the version mappings
	 */
	public static Mapping<?> getMappings(GameSide side) {
		if (side.equals(GameSide.CLIENT))
			return clientMappings;
		else if (side.equals(GameSide.SERVER))
			return serverMappings;
		else
			return null; // Can't happen yet, no other sides
	}

}
