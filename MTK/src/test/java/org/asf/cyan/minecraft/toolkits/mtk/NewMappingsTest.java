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

		MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion("1.17");
		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, GameSide.SERVER)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(version, GameSide.SERVER);
			MinecraftMappingsToolkit.saveMappingsToDisk(version, GameSide.SERVER);
		}

		Mapping<?> vanillaMappings = MinecraftMappingsToolkit.loadMappings(version, GameSide.SERVER);
		Mapping<?> paperMappings = MinecraftMappingsToolkit.downloadPaperMappings(vanillaMappings, version,
				"4e2f0be270dc4bed68357e8b4f02017146a77f4e:PB_42");
		Files.write(Path.of("/tmp/paper.ccfg"), paperMappings.toString().getBytes());

		PaperCompatibilityMappings compat = new PaperCompatibilityMappings(vanillaMappings, "paper-42", version, false,
				"4e2f0be270dc4bed68357e8b4f02017146a77f4e:PB_42");
		Files.write(Path.of("/tmp/compatibility.ccfg"), compat.toString().getBytes());

		compat = compat;
	}

}
