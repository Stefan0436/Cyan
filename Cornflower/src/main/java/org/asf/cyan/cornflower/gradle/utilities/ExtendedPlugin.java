package org.asf.cyan.cornflower.gradle.utilities;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

/**
 * Extension to the Gradle plugin API.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class ExtendedPlugin implements Plugin<Project> {

	static ArrayList<ExtendedPlugin> plugins = new ArrayList<ExtendedPlugin>();

	/**
	 * Get instance of plugin
	 * 
	 * @param <T>         Plugin type
	 * @param pluginClass Plugin class
	 * @return Plugin object
	 */
	@SuppressWarnings("unchecked")
	public static <T extends ExtendedPlugin> T getPluginInstance(Class<T> pluginClass) {
		if (ExtendedPlugin.class.getTypeName().equals(pluginClass.getTypeName()))
			throw new SecurityException("Cannot call GetPluginInstance on ExtendedPlugin!");
		for (ExtendedPlugin plug : plugins) {
			if (pluginClass.isInstance(plug))
				return (T) plug;
		}
		return null;
	}

	Project proj;

	/**
	 * Main plugin entrypoint
	 * 
	 * @param target Target project
	 */
	protected abstract void applyPlugin(Project target);

	/**
	 * Get the active gradle project, note, can only be used after and during the
	 * earlyInit phase (called after the constructor)
	 * 
	 * @return Project object that represents the current gradle project
	 */
	public Project getProject() {
		return proj;
	}

	/**
	 * Get the cache folder for the project.
	 * 
	 * @return File object that represents the cache folder of the project
	 */
	protected File getCacheFolder() {
		return GradleUtil.getPluginCache(this.getClass());
	}

	/**
	 * Get the cache folder for a specific cache item
	 * 
	 * @param itemname Cache item (gets created if it does not exist)
	 * @return File object that represents the cache folder of the project
	 */
	protected File getCacheFolder(String itemname) {
		return GradleUtil.getCacheFolder(this.getClass(), itemname);
	}

	/**
	 * Get the root plugin cache folder for this plugin.
	 * 
	 * @return File object that represents the cache folder of the plugin
	 */
	protected File getCacheRootFolder() {
		return GradleUtil.getPluginCache(this.getClass());
	}
	
	/**
	 * Get the shared plugin cache folder for this plugin. (shared between projects)
	 * 
	 * @return File object that represents the cache folder of the plugin
	 */
	protected File getSharedCacheFolder() {
		return GradleUtil.getSharedPluginCache(this.getClass());
	}

	/**
	 * Get the shared cache folder for a specific cache item
	 * 
	 * @param itemname Cache item (gets created if it does not exist)
	 * @return File object that represents the cache folder of the project
	 */
	protected File getSharedCacheFolder(String itemname) {
		return GradleUtil.getSharedCacheFolder(this.getClass(), itemname);
	}

	@Override
	public void apply(Project target) {
		plugins.add(this);
		proj = target;
		applyPlugin(target);
		try {
			TaskProcesssor.Load(target, this.getClass());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
			System.err.println("Task processing failed, see stacktrace. Error message: "+e.getMessage());
			e.printStackTrace();
		}
		try {
			ExtensionProcesssor.Load(target, this.getClass());
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException e) {
			System.err.println("Extension processing failed, see stacktrace. Error message: "+e.getMessage());
			e.printStackTrace();
		}
	}
}
