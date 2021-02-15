package org.asf.cyan;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.asf.cyan.core.CyanCore;

public class CyanServerWrapper {
	/**
	 * Main server initialization method
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
		CyanLoader.initializeGame("SERVER");
		CyanCore.startGame("net.minecraft.server.Main", args);
	}
}
