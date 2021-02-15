package org.asf.cyan;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.asf.cyan.api.cyanloader.CyanSide;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.fluid.mappings.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;

/**
 * 
 * Loads the MCP mappings to allow forge to run Cyan, credits to Forge and the
 * developers of the Minecraft Coder Pack
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
class ForgeCompatibilityMappings extends CompatibilityMappings {
	public static File mcpMappingsOutput = null;
	public static String url = "https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/%mcver%/mcp_config-%mcver%.zip";

	public ForgeCompatibilityMappings(Mapping<?> mappings, CyanSide side) {
		mcpMappingsOutput = new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "caches/mappings/");

		try {
			CyanLoader.infoLog("Loading forge support... Preparing MCP mappings for compatibility...");
			if (!mcpMappingsOutput.exists())
				mcpMappingsOutput.mkdirs();

			if (!areMappingsAvailable("mcp", CyanInfo.getModloaderVersion(), side)) {
				CyanLoader.infoLog("First time loading with forge support for version " + CyanInfo.getModloaderVersion()
						+ ", downloading MCP mappings...");

				McpMappings newMappings = downloadMCPMappings(CyanInfo.getMinecraftVersion());
				saveMappingsToDisk("mcp", CyanInfo.getModloaderVersion(), newMappings, side);
			}

			Mapping<?> MCPMappings = loadMappings("mcp", CyanInfo.getModloaderVersion(), side, McpMappings.class);
			combine("MCP", mappings, MCPMappings);
		} catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
				| NoSuchMethodException | SecurityException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static McpMappings downloadMCPMappings(String version) throws IOException {
		if (!MinecraftToolkit.hasMinecraftDownloadConnection())
			throw new IOException("No network connection");

		CyanLoader.infoLog("Resolving MCP mappings of minecraft version " + version + "...");
		String url = ForgeCompatibilityMappings.url.replaceAll("\\%mcver\\%", version);

		CyanLoader.traceLog("CREATE StringBuilder for holding the MCP mappings");
		StringBuilder mappings_text = new StringBuilder();

		CyanLoader.traceLog("CREATE ZipInputStream with InputStream connection to URL " + url);
		ZipInputStream strm = new ZipInputStream(new URL(url).openStream());
		while (strm.available() != 0) {
			ZipEntry entry = strm.getNextEntry();
			if (entry.getName().equals("config/joined.tsrg")) {
				CyanLoader.traceLog("CREATE scanner for MAPPINGS");
				Scanner sc = new Scanner(strm);
				CyanLoader.traceLog("SCAN version mappings of ZIP entry: " + entry.getName());
				while (sc.hasNext())
					mappings_text.append(sc.nextLine()).append(System.lineSeparator());
				CyanLoader.traceLog("CLOSE mappings scanner");
				sc.close();
				break;
			}
		}
		CyanLoader.traceLog("CLOSE ZipInputStream");
		strm.close();

		CyanLoader.infoLog("Mapping the " + version + " MCP mappings into CCFG format...");
		CyanLoader.traceLog("MAP version " + version + " MCP mappings into CCFG");
		return new McpMappings().parseTSRGMappings(mappings_text.toString());
	}
}
