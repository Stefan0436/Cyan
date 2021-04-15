package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;
import java.util.Scanner;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.internal.MappingsLoadEventProvider;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

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
			yarnMetaDataURL = new URL("http://maven.modmuss50.me/net/fabricmc/yarn/maven-metadata.xml");
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
	private static String yarnUrl = "http://maven.modmuss50.me/net/fabricmc/yarn/%version%/yarn-%version%-tiny.gz";
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
	 * Download version mappings into ram (SPIGOT)
	 * 
	 * @param fallback        Fallback mappings
	 * @param version         Minecraft version
	 * @param mappingsVersion Mappings hash
	 * @return Mapping object representing the version mappings
	 * @throws IOException If downloading fails
	 */
	public static SpigotMappings downloadSpigotMappings(Mapping<?> fallback, MinecraftVersionInfo version,
			String mappingsVersion) throws IOException {
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

		trace("DOWNLOAD PACKAGE MAPPINGS from " + packageMapppingsURL + ", caller: " + CallTrace.traceCallName());
		sc = new Scanner(packageMapppingsURL.openStream());
		while (sc.hasNext())
			packageMapppings.append(sc.nextLine()).append(System.lineSeparator());
		trace("CLOSE mappings scanner, caller: " + CallTrace.traceCallName());
		sc.close();

		info("Mapping the SPIGOT mappings into CCFG format...");
		trace("MAP version SPIGOT mappings into CCFG, caller: " + CallTrace.traceCallName());
		SpigotMappings mappings = new SpigotMappings().parseMultiMappings(fallback, classMappings.toString(),
				memberMapppings.toString(), packageMapppings.toString(),
				Map.of("net.minecraft.**", "net.minecraft.server.v" + craftBukkitVersion));
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
