package org.asf.cyan.api.config;

import java.io.IOException;

import org.asf.cyan.api.modloader.information.mods.IBaseMod;

/**
 * 
 * Mod Configuration Manager
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class ConfigManager<T2 extends IBaseMod> {
	protected static ConfigManager<?> implementation;

	/**
	 * Retrieves the configuration manager for a given mod
	 */
	public static <T extends IBaseMod> ConfigManager<T> getFor(Class<T> mod) {
		return implementation.newInstance(mod);
	}

	/**
	 * Instantiates a new configuration manager
	 */
	public abstract <T extends IBaseMod> ConfigManager<T> newInstance(Class<T> mod);

	/**
	 * Retrieves the given configuration instance (reads it if needed)
	 * 
	 * @param <T>         Configuration type
	 * @param configClass Configuration class
	 * @return Configuration instance
	 * @throws IOException If loading fails
	 */
	public abstract <T extends ModConfiguration<T, T2>> T getConfiguration(Class<T> configClass) throws IOException;

}
