package org.asf.cyan;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Scanner;

import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.core.CyanInfo;

public class CyanServerWrapper {
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
		CyanCore.setEntryMethod("CyanWrapper Version " + builder.toString().trim());
		CyanLoader.initializeGame("SERVER");
		String defaultMain = "net.minecraft.server.Main";
		if (Version.fromString(CyanInfo.getMinecraftVersion()).isLessOrEqualTo(Version.fromString("1.15.2"))) {
			defaultMain = "net.minecraft.server.MinecraftServer";
		}
		String wrapper = System.getProperty("cyan.launcher.server.wrapper", defaultMain);
		CyanCore.startGame(wrapper, args);
	}
}
