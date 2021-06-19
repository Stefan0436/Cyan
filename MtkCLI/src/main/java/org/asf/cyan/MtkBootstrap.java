package org.asf.cyan;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.implementation.CyanBytecodeExporter;
import org.asf.cyan.fluid.implementation.CyanTransformer;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftModdingToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftRifterToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;

public class MtkBootstrap extends CyanComponent {
	public static File mtkDir = new File(".");

	public static void strap() {
		new MtkBootstrap();
	}

	@Override
	protected void setupComponents() {
		try {
			MinecraftInstallationToolkit.setMinecraftDirectory(mtkDir);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	protected void preInitAllComponents() {
		Configurator.setLevel("CYAN", Level.ERROR);
	}

	@Override
	protected Class<?>[] getComponentClasses() {
		return new Class<?>[] { MinecraftToolkit.class, MinecraftInstallationToolkit.class,
				MinecraftVersionToolkit.class, MinecraftMappingsToolkit.class, MinecraftModdingToolkit.class,
				MinecraftRifterToolkit.class, CyanTransformer.class, CyanBytecodeExporter.class };
	}

	@Override
	protected void finalizeComponents() {
	}

	private MtkBootstrap() {
		assignImplementation();
		initializeComponentClasses();
	}
}
