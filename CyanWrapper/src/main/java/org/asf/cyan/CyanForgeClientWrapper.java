package org.asf.cyan;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Scanner;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;

public class CyanForgeClientWrapper {

	/**
	 * Main initialization method
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
	public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException,
			InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, IOException {
		URL info = CyanClientWrapper.class.getResource("/wrapper.info");
		StringBuilder builder = new StringBuilder();
		Scanner sc = new Scanner(info.openStream());
		while (sc.hasNext())
			builder.append(sc.nextLine());
		sc.close();

		info = CyanClientWrapper.class.getResource("/mappings.info");
		StringBuilder mcp = new StringBuilder();
		sc = new Scanner(info.openStream());
		while (sc.hasNext())
			mcp.append(sc.nextLine());
		sc.close();

		CyanLoader.setPlatformVersion(mcp.toString());
		
		CyanCore.setEntryMethod("CyanForgeWrapper Version " + builder.toString().trim() + ", MCP version " + mcp);
		CyanLoader.disableVanillaMappings();
		CyanLoader.addCompatibilityMappings(CyanLoader.getForgeCompatibilityMappings(GameSide.CLIENT, mcp.toString()));
		CyanLoader.initializeGame("CLIENT");
		String wrapper = System.getProperty("cyan.launcher.client.wrapper", "cpw.mods.modlauncher.Launcher");
		CyanCore.startGame(wrapper, args);
	}
}
