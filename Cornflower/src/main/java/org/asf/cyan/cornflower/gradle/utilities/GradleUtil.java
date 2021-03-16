package org.asf.cyan.cornflower.gradle.utilities;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

/**
 * Gradle utility class
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class GradleUtil {
	
	/**
	 * Get the gradle cache root
	 * 
	 * @param plugin The plugin class
	 * @return File object that represents the gradle global scache directory
	 */
	public static File getCacheRoot(Class<? extends ExtendedPlugin> plugin) {
		ExtendedPlugin plugin_instance = ExtendedPlugin.getPluginInstance(plugin);
		try {
			return plugin_instance.getProject().getGradle().getGradleUserHomeDir().listFiles(new FilenameFilter() {

				@Override
				public boolean accept(File file, String name) {
					if (name.equals("caches"))
						return true;
					return false;
				}

			})[0].getCanonicalFile();
		} catch (IOException ex) {
			return null;
		}
	}

	/**
	 * Get the main plugin cache directory
	 * 
	 * @param plugin The plugin class
	 * @return File object that represents the cache directory
	 */
	public static File getPluginCacheRoot(Class<? extends ExtendedPlugin> plugin) {
		File f = new File(getCacheRoot(plugin), plugin.getSimpleName());
		if (!f.exists())
			f.mkdir();
		return f;
	}

	/**
	 * Get the plugin cache directory (shared across projects)
	 * 
	 * @param plugin The plugin class
	 * @return File object that represents the cache directory
	 */
	public static File getSharedPluginCache(Class<? extends ExtendedPlugin> plugin) {
		File f = new File(getPluginCacheRoot(plugin), "shared");
		if (!f.exists())
			f.mkdir();
		return f;
	}

	/**
	 * Get the project cache directory for a specified plugin
	 * 
	 * @param plugin The plugin class
	 * @return File object that represents the cache directory
	 */
	public static File getPluginCache(Class<? extends ExtendedPlugin> plugin) {
		ExtendedPlugin plugin_instance = ExtendedPlugin.getPluginInstance(plugin);
		Object group = plugin_instance.getProject().getGroup();
		File f = new File(getPluginCacheRoot(plugin), (!group.equals("") ? group + "." : "")+plugin_instance.getProject().getName());
		if (!f.exists())
			f.mkdir();

		return f;
	}

	/**
	 * Get or create a cache directory for a project
	 * 
	 * @param plugin     The plugin class
	 * @param folderName The name of the cache folder
	 * @return File object that represents the cache directory
	 */
	public static File getCacheFolder(Class<? extends ExtendedPlugin> plugin, String folderName) {
		File f = new File(getPluginCache(plugin), folderName);
		if (!f.exists())
			f.mkdir();
		return f;
	}
	
	/**
	 * Get or create a cache directory shared across projects
	 * 
	 * @param plugin     The plugin class
	 * @param folderName The name of the cache folder
	 * @return File object that represents the cache directory
	 */
	public static File getSharedCacheFolder(Class<? extends ExtendedPlugin> plugin, String folderName) {
		File f = new File(getSharedPluginCache(plugin), folderName);
		if (!f.exists())
			f.mkdir();
		return f;
	}
}
