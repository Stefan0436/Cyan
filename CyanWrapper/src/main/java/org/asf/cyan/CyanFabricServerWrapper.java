package org.asf.cyan;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Scanner;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;

public class CyanFabricServerWrapper {
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
	public static void main(String[] args) throws IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException{
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
		CyanCore.setEntryMethod("CyanFabricWrapper Version " + builder.toString().trim());
		
		CyanLoader.disableVanillaMappings();
		CyanLoader.addCompatibilityMappings(CyanLoader.getFabricCompatibilityMappings(GameSide.SERVER, mappingsVersion.toString()));
		CyanLoader.initializeGame("SERVER");
		
		String wrapper = System.getProperty("cyan.launcher.server.wrapper", "net.fabricmc.loader.launch.knot.KnotServer");
		CyanCore.startGame(wrapper, args);
	}
}
