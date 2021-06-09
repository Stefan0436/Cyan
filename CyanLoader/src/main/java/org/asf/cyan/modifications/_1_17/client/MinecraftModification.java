package org.asf.cyan.modifications._1_17.client;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.modloader.LoadPhase;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.Erase;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.modifications._1_17.typereplacers.CrashReportMock;
import org.asf.cyan.modifications._1_17.typereplacers.MinecraftMock;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.Minecraft")
public class MinecraftModification {

	private static boolean firstLoad;
	private static final Logger LOGGER = null;

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	public static void clinit() {
		firstLoad = true;
	}

	@Reflect
	@TargetType(target = "net.minecraft.client.Minecraft")
	public static MinecraftMock getInstance() {
		return null;
	}

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	public static void ctor1(@TargetType(target = "net.minecraft.client.main.GameConfig") Object conf) {
		CyanCore.setPhase(LoadPhase.RUNTIME);
		if (firstLoad) {
			Modloader.getModloader().dispatchEvent("mods.runtimestart");

			firstLoad = false;
		}
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "setupDefaultState(int,int,int,int)", targetOwner = "com.mojang.blaze3d.systems.RenderSystem")
	public static void ctor2(@TargetType(target = "net.minecraft.client.main.GameConfig") Object conf) {
		if (firstLoad) {
			CyanCore.setPhase(LoadPhase.INIT);
			Modloader.getModloader().dispatchEvent("mods.init");
		}
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD, targetCall = "createDefault()", targetOwner = "net.minecraft.client.color.block.BlockColors")
	public static void ctor3(@TargetType(target = "net.minecraft.client.main.GameConfig") Object conf) {
		CyanCore.setPhase(LoadPhase.POSTINIT);
		if (firstLoad) {
			Modloader.getModloader().dispatchEvent("mods.postinit");
		}
	}

	@Constructor
	@InjectAt(location = InjectLocation.HEAD)
	public static void ctor4(@TargetType(target = "net.minecraft.client.main.GameConfig") Object conf) {
		CyanCore.setPhase(LoadPhase.PRELOAD);
		if (firstLoad) {
			Modloader.getModloader().dispatchEvent("mods.preinit");
		}
	}

	@Erase
	public static void crash(@TargetType(target = "net.minecraft.CrashReport") CrashReportMock report) {
		CyanLoader.crash();

		// using regular format to keep support with some programs, else it would have
		// been dd-MM-yyyy HH.mm.ss

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");

		File dir = null;
		if (getInstance() == null || getInstance().gameDirectory == null)
			dir = MinecraftInstallationToolkit.getMinecraftDirectory();
		else
			dir = getInstance().gameDirectory;

		File crashFile = new File(dir, "crash-reports/crash-" + dateFormat.format(new Date()) + "-client.txt");

		Marker m = MarkerManager.getMarker("CrashHandler");

		CyanLoader.getSystemOutputStream().println(report.getFriendlyReport());
		if (report.getSaveFile() != null) {
			try {
				LOGGER.fatal(m, "!ALERT! Game has crashed! Crash report has been saved to: "
						+ report.getSaveFile().getCanonicalPath());
			} catch (IOException e) {
				LOGGER.fatal(m, "!ALERT! Game has crashed! Crash report has been saved to: "
						+ report.getSaveFile().getAbsolutePath());
			}
		} else if (report.saveToFile(crashFile)) {
			try {
				LOGGER.fatal(m,
						"!ALERT! Game has crashed! Crash report has been saved to: " + crashFile.getCanonicalPath());
			} catch (IOException e) {
				LOGGER.fatal(m,
						"!ALERT! Game has crashed! Crash report has been saved to: " + crashFile.getAbsolutePath());
			}
		} else {
			LOGGER.fatal(m, "!ALERT! Game has crashed! ERROR: Could not save crash report, unknown error.");
		}

		System.exit(-1);
	}

}
