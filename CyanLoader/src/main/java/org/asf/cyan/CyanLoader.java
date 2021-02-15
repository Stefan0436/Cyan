package org.asf.cyan;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.cyanloader.CyanSide;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.mappings.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.modifications.TitleModification;

public class CyanLoader extends CyanComponent { // for logging

	static void infoLog(String message) {
		CyanComponent.info(message);
	}
	static void warnLog(String message) {
		CyanComponent.warn(message);
	}
	static void errorLog(String message) {
		CyanComponent.error(message);
	}
	static void traceLog(String message) {
		CyanComponent.trace(message);
	}
	static void debugLog(String message) {
		CyanComponent.debug(message);
	}
	static void fatalLog(String message) {
		CyanComponent.fatal(message);
	}
	
	private static ArrayList<Mapping<?>> compatibillityMappings = new ArrayList<Mapping<?>>();
	private static Mapping<?> mappings = null;
	private static boolean deobfuscate = true;
	private static boolean loaded = false;
	private static File cyanDir;
	
	public static File getCyanDataDirectory() {
		return cyanDir;
	}
	
	public static boolean isDeobfuscatorEnabled() {
		return deobfuscate;
	}

	private static void prepare(String side) throws IOException {
		loaded = true;
		CyanCore.setSide(side);
		CyanCore.initLoader();
		cyanDir = new File(".cyan-data");
		
		if (side.equals("SERVER")) {
			// TODO: Load CYAN libraries
		}
		
		if (!cyanDir.exists())
			cyanDir.mkdirs();
		
		String cPath = cyanDir.getCanonicalPath();
		info("Starting CYAN in: " + cPath);
		MinecraftInstallationToolkit.setMinecraftDirectory(cyanDir);
		MinecraftToolkit.resetServerConnectionState();

		// TODO: Mappings loading and caching (with window)
		MinecraftVersionInfo mcVersion = new MinecraftVersionInfo(CyanInfo.getMinecraftVersion(), null, null, null);
		if (!MinecraftMappingsToolkit.areMappingsAvailable(mcVersion, CyanCore.getSide())) {
			info("First time loading, downloading " + side.toLowerCase() + " mappings...");
			MinecraftToolkit.resolveVersions();
			MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(CyanInfo.getMinecraftVersion());
			MinecraftMappingsToolkit.downloadMappings(version, CyanCore.getSide());
			MinecraftMappingsToolkit.saveMappingsToDisk(version, CyanCore.getSide());
		}

		// TODO: Pre-load registry for core modules etc

		mappings = MinecraftMappingsToolkit.loadMappings(mcVersion, CyanCore.getSide());
	}

	/**
	 * Prepare for running Cyan Components in minecraft
	 */
	public static void initializeGame(String side) {
		try {
			if (!loaded)
				prepare(side);
			
			CyanCore.registerPreLoadHook(new Runnable() {

				@Override
				public void run() {
					info("Loading FLUID mappings...");
					
					if (deobfuscate) Fluid.loadMappings(mappings);
					if (!deobfuscate && Fluid.isDeobfuscatorEnabled()) Fluid.disableDeobfuscator();
					
					for (Mapping<?> cmap : compatibillityMappings)
						Fluid.loadMappings(cmap);
					// TODO: Fluid transformers
				}

			});
			CyanCore.registerPreLoadHook(new Runnable() {

				@Override
				public void run() {
					info("Loading FLUID class load hooks...");
					
					Fluid.registerHook(new TitleModification());
				}

			});
			CyanCore.registerPreLoadHook(new Runnable() {

				@Override
				public void run() {
					info("Loading FLUID transformers...");
					
					// TODO: Fluid transformers
					
				}

			});
			CyanCore.registerPreLoadHook(new Runnable() {

				@Override
				public void run() {
					info("Loading Cyan Core Modules...");
					
					// TODO: Core modules loading
				}

			});

			info("Starting CyanCore...");
			CyanCore.initializeComponents();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Mapping<?> getFabricCompatibilityMappings(CyanSide side) {
		try {
			if (!loaded)
				prepare(side.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new FabricCompatibilityMappings(mappings, side);
	}

	public static Mapping<?> getForgeCompatibilityMappings(CyanSide side) {
		try {
			if (!loaded)
				prepare(side.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new ForgeCompatibilityMappings(mappings, side);
	}

	public static void addCompatibilityMappings(Mapping<?> mappings) {
		compatibillityMappings.add(mappings);
	}

	public static void disableDeobfuscator() {
		deobfuscate = false;
	}

}
