package org.asf.cyan.minecraft.toolkits.mtk;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.HashMap;

import org.apache.logging.log4j.Level;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;
import org.junit.Test;

public class MinecraftModdingToolkitSpecialTest {

	@Test
	public void sourcesJar() throws IOException {
		CyanCore.enableLog();
		MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));

		CyanCore.trackLevel(Level.WARN);

		if (!CyanCore.isInitialized()) {
			MinecraftToolkit.initializeMTK();
		}
		MinecraftVersionInfo info = null;
		if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
			info = MinecraftVersionToolkit.getLatestReleaseVersion();
		} else {
			// The following needs to be updated now and then. (or saved)
			info = MinecraftVersionToolkit.createVersionInfo("1.16.5", MinecraftVersionType.RELEASE,
					new URL("file:////tmp"), OffsetDateTime.now());
		}

		File file1 = MinecraftInstallationToolkit.downloadVersionJar(info, GameSide.CLIENT);
		File file2 = MinecraftInstallationToolkit.downloadVersionJar(info, GameSide.SERVER);

		assertTrue(file1.exists());
		assertTrue(file2.exists());

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.CLIENT)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.CLIENT);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.CLIENT);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.SERVER);
		}

		MinecraftMappingsToolkit.loadMappings(info, GameSide.CLIENT);
		MinecraftMappingsToolkit.loadMappings(info, GameSide.SERVER);

		File file3 = MinecraftModdingToolkit.deobfuscateJar(info, GameSide.CLIENT);
		File file4 = MinecraftModdingToolkit.deobfuscateJar(info, GameSide.SERVER);

		assertTrue(file3.exists());
		assertTrue(file4.exists());

		File jarsC = MinecraftModdingToolkit.sourcesJar(info, GameSide.CLIENT, true);
		File jarsS = MinecraftModdingToolkit.sourcesJar(info, GameSide.SERVER, true);

		assertTrue(jarsC.exists());

		assertTrue(jarsS.exists());

		HashMap<String, Level> errors = CyanCore.stopTracking();
		errors.forEach((k, v) -> {
			System.err.println(k);
		});
		assertTrue(errors.size() == 0);
	}

	@Test
	public void deobfuscateJarTest() throws IOException {
		CyanCore.setDebugLog(); // trace slows down too much and is unreadable for this test
		MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));

		CyanCore.trackLevel(Level.WARN);

		if (!CyanCore.isInitialized()) {
			MinecraftToolkit.initializeMTK();
		}
		MinecraftVersionInfo info = null;
		if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
			info = MinecraftVersionToolkit.getLatestReleaseVersion();
		} else {
			// The following needs to be updated now and then. (or saved)
			info = MinecraftVersionToolkit.createVersionInfo("1.16.5", MinecraftVersionType.RELEASE,
					new URL("file:////tmp"), OffsetDateTime.now());
		}

		File file1 = MinecraftInstallationToolkit.downloadVersionJar(info, GameSide.CLIENT);
		File file2 = MinecraftInstallationToolkit.downloadVersionJar(info, GameSide.SERVER);

		assertTrue(file1.exists());
		assertTrue(file2.exists());

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.CLIENT)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.CLIENT);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.CLIENT);
		}

		if (!MinecraftMappingsToolkit.areMappingsAvailable(info, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(info, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk(info, GameSide.SERVER);
		}

		MinecraftMappingsToolkit.loadMappings(info, GameSide.CLIENT);
		MinecraftMappingsToolkit.loadMappings(info, GameSide.SERVER);

		File file3 = MinecraftModdingToolkit.deobfuscateJar(info, GameSide.CLIENT, true);
		File file4 = MinecraftModdingToolkit.deobfuscateJar(info, GameSide.SERVER, true);

		assertTrue(file3.exists());
		assertTrue(file4.exists());

		HashMap<String, Level> errors = CyanCore.stopTracking();
		errors.forEach((k, v) -> {
			System.err.println(k);
		});
		assertTrue(errors.size() == 0);
	}
}
