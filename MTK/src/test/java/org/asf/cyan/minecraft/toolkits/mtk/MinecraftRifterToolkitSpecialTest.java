package org.asf.cyan.minecraft.toolkits.mtk;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.fluid.remapping.MAPTYPE;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.fluid.remapping.SimpleMappings;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class MinecraftRifterToolkitSpecialTest extends CyanComponent {

	String getLatestMCP(MinecraftVersionInfo version) {
		return getLatestFromMavenMD(
				"https://files.minecraftforge.net/maven/de/oceanlabs/mcp/mcp_config/maven-metadata.xml", version,
				"%version%-", null);
	}

	String getLatestFromMavenMD(String templateURL, MinecraftVersionInfo version, String versionTemplate,
			String versionTemplateAux) {
		try {
			URL versionURL = new URL(templateURL.replaceAll("\\%version\\%", version.toString()));

			String vers = null;
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document document = builder.parse(versionURL.openStream());
			Element root = document.getDocumentElement();
			NodeList versions = ((Element) root.getElementsByTagName("versioning").item(0))
					.getElementsByTagName("versions").item(0).getChildNodes();

			for (int i = 0; i < versions.getLength(); i++) {
				if (!versions.item(i).hasChildNodes())
					continue;

				String verNode = versions.item(i).getFirstChild().getNodeValue();
				if (verNode.startsWith(versionTemplate.replaceAll("\\%version\\%", version.toString()))) {
					vers = verNode.substring(versionTemplate.replaceAll("\\%version\\%", version.toString()).length());
				} else if (versionTemplateAux != null
						&& verNode.startsWith(versionTemplateAux.replaceAll("\\%version\\%", version.toString()))) {
					vers = verNode
							.substring(versionTemplateAux.replaceAll("\\%version\\%", version.toString()).length());
				}
			}

			return vers;
		} catch (IOException | SAXException | ParserConfigurationException ex) {
			return null;
		}
	}

	@Test
	public void generateCyanRiftTargetsTest() throws IOException {
		CyanCore.enableLog(); // trace slows down too much and debug is unreadable for this test
		if (!CyanCore.isInitialized()) {
			MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
			CyanCore.initializeComponents();
			MinecraftToolkit.initializeMTK();
		}

		MinecraftVersionInfo info = null;
		if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
			info = MinecraftVersionToolkit.getLatestReleaseVersion();
		} else {
			info = MinecraftVersionToolkit.createVersionInfo("1.16.5", MinecraftVersionType.RELEASE,
					new URL("file:////tmp"), OffsetDateTime.now());
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.CLIENT)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.CLIENT);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.CLIENT);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.SERVER);
		}

		Mapping<?> vanillaClient = MinecraftMappingsToolkit.loadMappings(info, GameSide.CLIENT);
		Mapping<?> vanillaServer = MinecraftMappingsToolkit.loadMappings(info, GameSide.SERVER);

		SimpleMappings riftClient = (SimpleMappings) MinecraftRifterToolkit.generateCyanRiftTargets(info,
				GameSide.CLIENT);
		SimpleMappings riftServer = (SimpleMappings) MinecraftRifterToolkit.generateCyanRiftTargets(info,
				GameSide.SERVER);

		testMappings(riftClient, vanillaClient, riftServer, vanillaServer);
	}

	@Test
	public void generateCyanForgeRiftTargetsTest() throws IOException {
		CyanCore.enableLog(); // trace slows down too much and debug is unreadable for this test

		if (!CyanCore.isInitialized()) {
			MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
			CyanCore.initializeComponents();
			MinecraftToolkit.initializeMTK();
		}

		MinecraftVersionInfo info = null;
		if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
			info = MinecraftVersionToolkit.getLatestReleaseVersion();
		} else {
			info = MinecraftVersionToolkit.createVersionInfo("1.16.5", MinecraftVersionType.RELEASE,
					new URL("file:////tmp"), OffsetDateTime.now());
		}

		String mcp = getLatestMCP(info);
		if (!MinecraftMappingsToolkit.areMappingsAvailable("-mtktest-" + mcp, "mcp", info, GameSide.CLIENT)) {
			MinecraftMappingsToolkit.downloadMCPMappings(info, GameSide.CLIENT, mcp);
			MinecraftMappingsToolkit.saveMappingsToDisk("-mtktest-" + mcp, "mcp", info, GameSide.CLIENT);
		}
		if (!MinecraftMappingsToolkit.areMappingsAvailable("-mtktest-" + mcp, "mcp", info, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadMCPMappings(info, GameSide.SERVER, mcp);
			MinecraftMappingsToolkit.saveMappingsToDisk("-mtktest-" + mcp, "mcp", info, GameSide.SERVER);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.CLIENT)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.CLIENT);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.CLIENT);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.SERVER);
		}

		MinecraftMappingsToolkit.loadMappings("-mtktest-" + mcp, "mcp", info, GameSide.CLIENT);
		MinecraftMappingsToolkit.loadMappings("-mtktest-" + mcp, "mcp", info, GameSide.SERVER);

		MinecraftMappingsToolkit.loadMappings(info, GameSide.CLIENT);
		MinecraftMappingsToolkit.loadMappings(info, GameSide.SERVER);

		SimpleMappings riftClient = (SimpleMappings) MinecraftRifterToolkit.generateCyanForgeRiftTargets(info,
				GameSide.CLIENT, "mtktest", mcp);
		SimpleMappings riftServer = (SimpleMappings) MinecraftRifterToolkit.generateCyanForgeRiftTargets(info,
				GameSide.SERVER, "mtktest", mcp);

		testMappings(riftClient, MinecraftRifterToolkit.getForgeClientMappings(), riftServer,
				MinecraftRifterToolkit.getForgeServerMappings());
	}

	@Test
	public void generateCyanFabricRiftTargetsTest() throws IOException {
		CyanCore.enableLog(); // trace slows down too much and debug is unreadable for this test

		if (!CyanCore.isInitialized()) {
			MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
			CyanCore.initializeComponents();
			MinecraftToolkit.initializeMTK();
		}

		MinecraftVersionInfo info = null;
		if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
			info = MinecraftVersionToolkit.getLatestReleaseVersion();
		} else {
			info = MinecraftVersionToolkit.createVersionInfo("1.16.5", MinecraftVersionType.RELEASE,
					new URL("file:////tmp"), OffsetDateTime.now());
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable("-mtktest", "yarn", info, GameSide.CLIENT)) {
			MinecraftMappingsToolkit.downloadYarnMappings(info, GameSide.CLIENT);
			MinecraftMappingsToolkit.saveMappingsToDisk("-mtktest", "yarn", info, GameSide.CLIENT);
		}
		if (!MinecraftMappingsToolkit.areMappingsAvailable("-mtktest", "yarn", info, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadYarnMappings(info, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk("-mtktest", "yarn", info, GameSide.SERVER);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.CLIENT)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.CLIENT);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.CLIENT);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.SERVER);
		}

		MinecraftMappingsToolkit.loadMappings("-mtktest", "yarn", info, GameSide.CLIENT);
		MinecraftMappingsToolkit.loadMappings("-mtktest", "yarn", info, GameSide.SERVER);

		MinecraftMappingsToolkit.loadMappings(info, GameSide.CLIENT);
		MinecraftMappingsToolkit.loadMappings(info, GameSide.SERVER);

		SimpleMappings riftClient = (SimpleMappings) MinecraftRifterToolkit.generateCyanFabricRiftTargets(info,
				GameSide.CLIENT, "mtktest");
		SimpleMappings riftServer = (SimpleMappings) MinecraftRifterToolkit.generateCyanFabricRiftTargets(info,
				GameSide.SERVER, "mtktest");

		testMappings(riftClient, MinecraftRifterToolkit.getFabricClientMappings(), riftServer,
				MinecraftRifterToolkit.getFabricServerMappings());
	}

	@Test
	public void generateCyanPaperRiftTargetsTest() throws IOException {
		CyanCore.enableLog(); // trace slows down too much and debug is unreadable for this test

		if (!CyanCore.isInitialized()) {
			MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
			CyanCore.initializeComponents();
			MinecraftToolkit.initializeMTK();
		}

		MinecraftVersionInfo info = null;
		if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
			info = MinecraftVersionToolkit.getLatestReleaseVersion();
		} else {
			info = MinecraftVersionToolkit.createVersionInfo("1.16.5", MinecraftVersionType.RELEASE,
					new URL("file:////tmp"), OffsetDateTime.now());
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable("-mtktest", "spigot", info, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadYarnMappings(info, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk("-mtktest", "spigot", info, GameSide.SERVER);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.SERVER);
		}

		MinecraftMappingsToolkit.loadMappings("-mtktest", "spigot", info, GameSide.SERVER);
		MinecraftMappingsToolkit.loadMappings(info, GameSide.SERVER);
		SimpleMappings riftServer = (SimpleMappings) MinecraftRifterToolkit.generateCyanPaperRiftTargets(info,
				"mtktest");
		testMappings(null, null, riftServer, MinecraftRifterToolkit.getPaperServerMappings());
	}

	private void testMappings(SimpleMappings riftClient, Mapping<?> client, SimpleMappings riftServer,
			Mapping<?> server) {
		info("Testing mapping sizes...");
		if (riftClient != null) {
			assertTrue(client.mappings.length == riftClient.mappings.length);
		}

		assertTrue(server.mappings.length == riftServer.mappings.length);

		if (riftClient != null) {

			info("Testing client mappings...");
			for (Mapping<?> clsMapping : client.mappings) {
				Mapping<?> riftClass = riftClient.getClassMapping(clsMapping.obfuscated);
				assertTrue(riftClass != null);
				assertTrue(riftClass.obfuscated.equals(clsMapping.name));

				for (Mapping<?> memberMapping : clsMapping.mappings) {
					assertTrue(Stream.of(riftClass.mappings).anyMatch(t -> {
						boolean match = true;
						if (!t.name.equals(memberMapping.obfuscated))
							match = false;
						else if (!t.obfuscated.equals(memberMapping.name))
							match = false;
						else if (!t.mappingType.equals(memberMapping.mappingType))
							match = false;
						else if (!t.type.equals(riftClient.mapClassToDeobfuscation(memberMapping.type)))
							match = false;
						else {
							if (t.mappingType == MAPTYPE.METHOD) {
								if (t.argumentTypes.length != memberMapping.argumentTypes.length)
									match = false;
								else {
									for (int i = 0; i < t.argumentTypes.length; i++) {
										if (!t.argumentTypes[i].equals(
												riftClient.mapClassToDeobfuscation(memberMapping.argumentTypes[i]))) {
											match = false;
											break;
										}
									}
								}
							}
						}
						return match;
					}));
				}
			}

		}

		info("Testing server mappings...");
		for (Mapping<?> clsMapping : server.mappings) {
			if (clsMapping.name.contains("package-info"))
				continue;

			Mapping<?> riftClass = riftServer.getClassMapping(clsMapping.obfuscated);
			assertTrue(riftClass != null);

			if (!riftClass.obfuscated.equals(clsMapping.name))
				error("TEST FAILED, " + riftClass.obfuscated + " should be " + clsMapping.name);

			assertTrue(riftClass.obfuscated.equals(clsMapping.name));

			for (Mapping<?> memberMapping : clsMapping.mappings) {
				assertTrue(Stream.of(riftClass.mappings).anyMatch(t -> {
					boolean match = true;
					if (!t.name.equals(memberMapping.obfuscated))
						match = false;
					else if (!t.obfuscated.equals(memberMapping.name))
						match = false;
					else if (!t.mappingType.equals(memberMapping.mappingType))
						match = false;
					else if (!t.type.equals(riftServer.mapClassToDeobfuscation(memberMapping.type)))
						match = false;
					else {
						if (t.mappingType == MAPTYPE.METHOD) {
							if (t.argumentTypes.length != memberMapping.argumentTypes.length)
								match = false;
							else {
								for (int i = 0; i < t.argumentTypes.length; i++) {
									if (!t.argumentTypes[i].equals(
											riftServer.mapClassToDeobfuscation(memberMapping.argumentTypes[i]))) {
										match = false;
										break;
									}
								}
							}
						}
					}
					return match;
				}));
			}
		}
	}
}
