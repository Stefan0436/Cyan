package org.asf.cyan.minecraft.toolkits.mtk;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.minecraft.toolkits.mtk.auth.AuthenticationInfo;
import org.asf.cyan.minecraft.toolkits.mtk.auth.MinecraftAccountType;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.junit.Test;

public class MinecraftLaunchSpecialTest {

	@Test
	public void launch() throws IOException {
		CyanCore.setDebugLog();
		MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
		if (!CyanCore.isInitialized()) {
			CyanCore.initializeComponents();
			MinecraftToolkit.initializeMTK();
		}
		
		MinecraftVersionInfo version = MinecraftVersionToolkit.getLatestReleaseVersion();

		if (!MinecraftInstallationToolkit.checkVersion(version)) {
			MinecraftInstallationToolkit.downloadVersionAndLibraries(version,
					true);
			assertTrue(MinecraftInstallationToolkit.checkVersion(version));
		}
		AuthenticationInfo account = AuthenticationInfo.authenticate(MinecraftAccountType.MOJANG);
		if (account == null)
			return;

		MinecraftInstallationToolkit.setIDE();
		MinecraftInstallationToolkit.extractNatives(version);

		if (!MinecraftMappingsToolkit.areMappingsAvailable(version, GameSide.CLIENT)) {
			MinecraftMappingsToolkit.downloadVanillaMappings(version, GameSide.CLIENT);
			MinecraftMappingsToolkit.saveMappingsToDisk(version, GameSide.CLIENT);
		}

		MinecraftMappingsToolkit.loadMappings(version, GameSide.CLIENT);
		MinecraftInstallationToolkit.addDebugArguments(false, 5005);
		
		int o = MinecraftInstallationToolkit.launchInstallation(version, new File(MinecraftInstallationToolkit.getMinecraftDirectory(), "Clients/"+version), MinecraftModdingToolkit.deobfuscateJar(version, GameSide.CLIENT), account);
		assertTrue(o==0);
	}

}
