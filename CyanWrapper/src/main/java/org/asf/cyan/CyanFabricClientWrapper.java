package org.asf.cyan;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.asf.cyan.api.cyanloader.CyanSide;
import org.asf.cyan.core.CyanCore;

public class CyanFabricClientWrapper {
	
	/**
	 * Main initialization method
	 * @param args Arguments
	 * @throws IllegalAccessException If starting fails
	 * @throws IllegalArgumentException If starting fails
	 * @throws InvocationTargetException If starting fails
	 * @throws NoSuchMethodException If starting fails
	 * @throws SecurityException If starting fails
	 * @throws ClassNotFoundException If starting fails
	 * @throws IOException If closing the class loader fails
	 */
	public static void main(String[] args) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException, ClassNotFoundException, IOException {
		CyanLoader.disableDeobfuscator();
		CyanLoader.addCompatibilityMappings(CyanLoader.getFabricCompatibilityMappings(CyanSide.CLIENT));
		CyanLoader.initializeGame("CLIENT");
		CyanCore.startGame("net.fabricmc.loader.launch.knot.KnotClient", args);
	}
}
