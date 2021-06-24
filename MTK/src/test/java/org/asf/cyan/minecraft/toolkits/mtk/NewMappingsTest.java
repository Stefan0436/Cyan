package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.junit.Test;

public class NewMappingsTest {

	@Test
	public void testPaper() throws IOException {
		CyanCore.enableLog();
		if (!CyanCore.isInitialized()) {
			MinecraftToolkit.resetServerConnectionState();
			MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
			MinecraftToolkit.initializeMTK();
		}

		MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion("1.16.5");
		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(version, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk(version, GameSide.SERVER);
		}

		Mapping<?> vanillaMappings = MinecraftMappingsToolkit.loadMappings(version, GameSide.SERVER);
		Mapping<?> paperMappings = MinecraftMappingsToolkit.downloadSpigotMappings(vanillaMappings, version,
				"f0a5ed1aeff8156ba4afa504e190c838dd1af50c:1_16_R3");
		Files.write(Path.of("/tmp/paper.ccfg"), paperMappings.toString().getBytes());

		PaperCompatibilityMappings compat = new PaperCompatibilityMappings(vanillaMappings, "paper-778", version, false,
				"f0a5ed1aeff8156ba4afa504e190c838dd1af50c:1_16_R3");
		Files.write(Path.of("/tmp/compatibility.ccfg"), compat.toString().getBytes());

		compat = compat;
	}

}
