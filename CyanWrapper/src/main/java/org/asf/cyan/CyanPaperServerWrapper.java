package org.asf.cyan;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Scanner;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.fluid.Fluid;

public class CyanPaperServerWrapper extends CyanComponent {

	/**
	 * Main server initialization method
	 * 
	 * @param args Arguments
	 * @throws IllegalAccessException    If starting fails
	 * @throws IllegalArgumentException  If starting fails
	 * @throws InvocationTargetException If starting fails
	 * @throws NoSuchMethodException     If starting fails
	 * @throws SecurityException         If starting fails
	 * @throws ClassNotFoundException    If starting fails
	 * @throws IOException               If closing the class loader fails
	 */
	public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException,
			SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		URL info = CyanClientWrapper.class.getResource("/wrapper.info");
		StringBuilder builder = new StringBuilder();
		Scanner sc = new Scanner(info.openStream());
		while (sc.hasNext())
			builder.append(sc.nextLine());
		sc.close();

		info = CyanClientWrapper.class.getResource("/mappings.info");
		StringBuilder mappingsVersion = new StringBuilder();
		sc = new Scanner(info.openStream());
		while (sc.hasNext())
			mappingsVersion.append(sc.nextLine());
		sc.close();

		CyanLoader.setPlatformVersion(mappingsVersion.toString());

		Fluid.addAgent("io.papermc.paperclip.Agent", "premain");
		CyanCore.setEntryMethod("CyanPaperWrapper Version " + builder.toString().trim());

		CyanLoader.setupModloader("SERVER");
		CyanLoader.disableVanillaMappings();
		CyanLoader.addCompatibilityMappings(CyanLoader.getPaperCompatibilityMappings(mappingsVersion.toString()));
		CyanLoader.initializeGame("SERVER");
		String wrapper = System.getProperty("cyan.launcher.server.wrapper", "io.papermc.paperclip.Paperclip");
		System.setProperty("net.kyori.adventure.text.warnWhenLegacyFormattingDetected", "false");
		CyanCore.startGame(wrapper, args);
	}

}
