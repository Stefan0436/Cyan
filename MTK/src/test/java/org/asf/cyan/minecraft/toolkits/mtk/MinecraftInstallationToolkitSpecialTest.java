package org.asf.cyan.minecraft.toolkits.mtk;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;
import org.junit.Test;

public class MinecraftInstallationToolkitSpecialTest {

	@Test
	public void downloadVersionJarTest() throws IOException {
		CyanCore.setTraceLog();
		MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
		if (!CyanCore.isInitialized()) {
			MinecraftToolkit.initializeMTK();
		}
		
		File file1 = MinecraftInstallationToolkit.downloadVersionJar(MinecraftVersionToolkit.getLatestReleaseVersion(), GameSide.CLIENT, true);
		File file2 = MinecraftInstallationToolkit.downloadVersionJar(MinecraftVersionToolkit.getLatestReleaseVersion(), GameSide.SERVER, true);
		
		assertTrue(file1.exists());
		assertTrue(file2.exists());
	}

	@Test
	public void downloadVersionAndLibrariesTest() throws IOException {
		CyanCore.setTraceLog();
		MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
		if (!CyanCore.isInitialized()) {
			MinecraftToolkit.initializeMTK();
		}
		
		MinecraftInstallationToolkit.downloadVersionAndLibraries(MinecraftVersionToolkit.getLatestReleaseVersion(), true);
		MinecraftInstallationToolkit.extractNatives(MinecraftVersionToolkit.getLatestReleaseVersion());
		assertTrue(MinecraftInstallationToolkit.checkVersion(MinecraftVersionToolkit.getLatestReleaseVersion()));
	}

	@Test
	public void generateJvmArgumentsTest() throws IOException {
		CyanCore.setTraceLog();
		MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
		if (!CyanCore.isInitialized()) {
			MinecraftToolkit.initializeMTK();
		}
		
		if (!MinecraftInstallationToolkit.checkVersion(MinecraftVersionToolkit.getLatestReleaseVersion())) {
			MinecraftInstallationToolkit.downloadVersionAndLibraries(MinecraftVersionToolkit.getLatestReleaseVersion(), true);		
			assertTrue(MinecraftInstallationToolkit.checkVersion(MinecraftVersionToolkit.getLatestReleaseVersion()));
		}
		MinecraftInstallationToolkit.extractNatives(MinecraftVersionToolkit.getLatestReleaseVersion());
		System.out.println(MinecraftInstallationToolkit.generateJvmArguments(MinecraftVersionToolkit.getLatestReleaseVersion()));
	}

	@Test
	public void getMainClassTest() throws IOException {
		CyanCore.setTraceLog();
		MinecraftInstallationToolkit.setMinecraftDirectory(new File("bin/test/mtk"));
		if (!CyanCore.isInitialized()) {
			MinecraftToolkit.initializeMTK();
		}
		
		if (!MinecraftInstallationToolkit.checkVersion(MinecraftVersionToolkit.getLatestReleaseVersion())) {
			MinecraftInstallationToolkit.downloadVersionAndLibraries(MinecraftVersionToolkit.getLatestReleaseVersion(), true);		
			assertTrue(MinecraftInstallationToolkit.checkVersion(MinecraftVersionToolkit.getLatestReleaseVersion()));
		}
		System.out.println(MinecraftInstallationToolkit.getMainClass(MinecraftVersionToolkit.getLatestReleaseVersion()));
	}

}
