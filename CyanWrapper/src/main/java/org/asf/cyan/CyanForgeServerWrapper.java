package org.asf.cyan;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Scanner;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.core.CyanCore;

public class CyanForgeServerWrapper {
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
		StringBuilder mcp = new StringBuilder();
		sc = new Scanner(info.openStream());
		while (sc.hasNext())
			mcp.append(sc.nextLine());
		sc.close();
		CyanCore.setEntryMethod("CyanForgeWrapper Version " + builder.toString().trim() + ", MCP version " + mcp);

		CyanLoader.disableVanillaMappings();
		CyanLoader.addCompatibilityMappings(CyanLoader.getForgeCompatibilityMappings(GameSide.SERVER, mcp.toString()));
		CyanLoader.initializeGame("SERVER");
		String wrapper = System.getProperty("cyan.launcher.server.wrapper", "net.minecraftforge.server.ServerMain");
		CyanCore.startGame(wrapper, args);
	}
}
