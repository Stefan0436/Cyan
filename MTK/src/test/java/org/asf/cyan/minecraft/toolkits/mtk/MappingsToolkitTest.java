package org.asf.cyan.minecraft.toolkits.mtk;

import java.io.File;
import java.io.IOException;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;
import org.junit.Test;

public class MappingsToolkitTest {

	@Test
	public void test() throws IOException {
		CyanCore.enableLog();
		MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
		if (!CyanCore.isInitialized()) {
			MinecraftToolkit.resetServerConnectionState();
			MinecraftToolkit.initializeMTK();
		}

		MinecraftMappingsToolkit.downloadPaperMappings(
				MinecraftMappingsToolkit.downloadVanillaMappings(MinecraftVersionToolkit.getLatestReleaseVersion(),
						GameSide.SERVER),
				MinecraftVersionToolkit.getLatestReleaseVersion(), "80836709e7fd3f63edb6fd8a2b85022ba90b45f7:PB_172");
		MinecraftMappingsToolkit.saveMappingsToDisk(MinecraftVersionToolkit.getLatestReleaseVersion(), GameSide.SERVER,
				true);
	}
}
