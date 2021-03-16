package org.asf.cyan.modifications._1_15_2.client;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.asf.cyan.CyanLoader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Erase;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.modifications._1_15_2.typereplacers.CrashReportMock;
import org.asf.cyan.modifications._1_15_2.typereplacers.MinecraftMock;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.Minecraft")
public class MinecraftModification {

	private static final Logger LOGGER = null;

	@Reflect
	@TargetType(target = "net.minecraft.client.Minecraft")
	public static MinecraftMock getInstance() {
		return null;
	}

	@Erase
	public static void crash(@TargetType(target = "net.minecraft.CrashReport") CrashReportMock report) {
		// using regular format to keep support with some programs, else it would have
		// been dd-MM-yyyy HH.mm.ss
		
		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
		
		File crashFile = new File(getInstance().gameDirectory,
				"crash-reports/crash-" + dateFormat.format(new Date()) + "-client.txt");
		
		Marker m = MarkerManager.getMarker("CrashHandler");
		
		CyanLoader.getSystemOutputStream().println(report.getFriendlyReport());
		if (report.getSaveFile() != null) {
			try {
				LOGGER.fatal(m, "!ALERT! Game has crashed! Crash report has been saved to: " + report.getSaveFile().getCanonicalPath());
			} catch (IOException e) {
				LOGGER.fatal(m, "!ALERT! Game has crashed! Crash report has been saved to: " + report.getSaveFile().getAbsolutePath());
			}
		} else if (report.saveToFile(crashFile)) {
			try {
				LOGGER.fatal(m, "!ALERT! Game has crashed! Crash report has been saved to: " + crashFile.getCanonicalPath());
			} catch (IOException e) {
				LOGGER.fatal(m, "!ALERT! Game has crashed! Crash report has been saved to: " + crashFile.getAbsolutePath());
			}
		} else {
			LOGGER.fatal(m, "!ALERT! Game has crashed! ERROR: Could not save crash report, unknown error.");
		}
		
		System.exit(-1);
	}

}
