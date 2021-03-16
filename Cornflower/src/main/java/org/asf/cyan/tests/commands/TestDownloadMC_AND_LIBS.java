package org.asf.cyan.tests.commands;

import java.util.Arrays;
import java.util.List;

import org.asf.cyan.core.CyanCore;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.tests.InteractiveTestCommand;

public class TestDownloadMC_AND_LIBS extends InteractiveTestCommand {

	@Override
	public String getId() {
		return "test1";
	}

	@Override
	public List<String> getAliases() {
		return Arrays.asList();
	}

	@Override
	public String helpSyntax() {
		return "<version>";
	}

	@Override
	public String helpDescription() {
		return "run test 1, mc jar lib download";
	}

	@Override
	protected Boolean execute(String[] arguments) throws Exception {
		if (!CyanCore.isInitialized()) {
			MinecraftToolkit.initializeMTK();
			CyanCore.initializeComponents();
		}
		if (arguments.length != 0) {
			String version = arguments[0];
			MinecraftVersionInfo v = MinecraftVersionToolkit.getVersion(version);
			if (!MinecraftInstallationToolkit.isVersionManifestSaved(v))
				MinecraftInstallationToolkit.saveVersionManifest(v);
			
			MinecraftInstallationToolkit.downloadVersionAndLibraries(v);
			MinecraftInstallationToolkit.extractNatives(v);
			
			return true;
		} else
			return false;
	}
}
