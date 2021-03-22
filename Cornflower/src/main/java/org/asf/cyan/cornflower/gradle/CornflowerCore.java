package org.asf.cyan.cornflower.gradle;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.cornflower.gradle.utilities.Log4jToGradleAppender;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.gradle.api.Project;

/**
 * Cornflower Plugin Core Class, DO NOT USE OUTSIDE OF GRADLE
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CornflowerCore {
	public static Logger LOGGER = null;
	public static Marker MAIN = MarkerManager.getMarker("MAIN");

	static void load(Project target, File fld) {
		CyanCore.enableCornflowerSupport();
		Log4jToGradleAppender.setGradleLogger(target.getLogger());
		System.setProperty("log4j2.disable.jmx", "true");

		try {
			Class.forName("org.asf.cyan.tests.TestCommand");
			System.setProperty("cyan.cornflower.mtkdir", fld.getCanonicalPath());
		} catch (ClassNotFoundException | IOException e) {
		}
		
		if (!CyanCore.isInitialized()) {
			CyanCore.simpleInit();
			try {
				MinecraftInstallationToolkit.setMinecraftDirectory(fld);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			MinecraftToolkit.initializeMTK();
			CyanCore.initializeComponents();
		}

		LOGGER = LogManager.getLogger("Cornflower");
		MAIN = MarkerManager.getMarker("MAIN");
	}
}
