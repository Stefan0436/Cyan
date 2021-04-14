package org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import org.asf.cyan.cornflower.gradle.utilities.IProjectExtension;
import org.asf.cyan.cornflower.gradle.utilities.modding.GameDependency;
import org.asf.cyan.cornflower.gradle.utilities.modding.ModloaderDependency;
import org.asf.cyan.fluid.bytecode.sources.FileClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.IClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.URLClassSourceProvider;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftRifterToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.rift.SimpleRiftBuilder;
import org.asf.cyan.minecraft.toolkits.mtk.rift.providers.IRiftToolchainProvider;

import groovy.lang.Closure;

public class CornflowerMainExtension implements IProjectExtension {

	public static final Class<org.asf.cyan.cornflower.classpath.util.SourceType> SourceType = org.asf.cyan.cornflower.classpath.util.SourceType.class;
	public static final Class<org.asf.cyan.cornflower.classpath.util.EntryType> EntryType = org.asf.cyan.cornflower.classpath.util.EntryType.class;
	public static final Class<org.asf.cyan.cornflower.classpath.util.PathPriority> PathPriority = org.asf.cyan.cornflower.classpath.util.PathPriority.class;

	public static final Class<?> EclipseLaunchGenerator = org.asf.cyan.cornflower.gradle.tasks.EclipseLaunchGenerator.class;
	public static final Class<?> CtcUtil = org.asf.cyan.cornflower.gradle.tasks.CtcTask.class;

	public static final GameSideContainer GameSide = new GameSideContainer();
	public static final LaunchPlatformContainer LaunchPlatform = new LaunchPlatformContainer();
	
	public static final ModloaderDependency Modloader = new ModloaderDependency();
	public static final GameDependency Game = new GameDependency();

	public static String connectiveHttpURLScheme(String server, String group, String modid, String modversion,
			String trustname) {
		String url = "/cyan/trust/upload/" + group + "/" + modid + "?trustname=" + trustname + "&modversion="
				+ modversion + "&file=";

		while (url.contains("//"))
			url = url.replace("//", "/");
		while (server.endsWith("/")) {
			server = server.substring(0, server.lastIndexOf("/"));
		}

		return server + url;
	}

	public static IRiftToolchainProvider riftProvider(Closure<?> provider) {
		return ConfigurableRiftProvider.fromClosure(provider);
	}

	public static IRiftToolchainProvider riftProvider(
			org.asf.cyan.api.modloader.information.game.LaunchPlatform platform) {
		return SimpleRiftBuilder.getProviderForPlatform(platform, null, null, null, null);
	}

	@SuppressWarnings("unused")
	private static class GameSideContainer {
		public static final org.asf.cyan.api.modloader.information.game.GameSide SERVER = org.asf.cyan.api.modloader.information.game.GameSide.SERVER;
		public static final org.asf.cyan.api.modloader.information.game.GameSide CLIENT = org.asf.cyan.api.modloader.information.game.GameSide.CLIENT;
	}

	@SuppressWarnings("unused")
	private static class LaunchPlatformContainer {
		public static final org.asf.cyan.api.modloader.information.game.LaunchPlatform DEOBFUSCATED = org.asf.cyan.api.modloader.information.game.LaunchPlatform.DEOBFUSCATED;
		public static final org.asf.cyan.api.modloader.information.game.LaunchPlatform UNKNOWN = org.asf.cyan.api.modloader.information.game.LaunchPlatform.UNKNOWN;
		public static final org.asf.cyan.api.modloader.information.game.LaunchPlatform VANILLA = org.asf.cyan.api.modloader.information.game.LaunchPlatform.VANILLA;
		public static final org.asf.cyan.api.modloader.information.game.LaunchPlatform SPIGOT = org.asf.cyan.api.modloader.information.game.LaunchPlatform.SPIGOT;
		public static final org.asf.cyan.api.modloader.information.game.LaunchPlatform YARN = org.asf.cyan.api.modloader.information.game.LaunchPlatform.YARN;
		public static final org.asf.cyan.api.modloader.information.game.LaunchPlatform MCP = org.asf.cyan.api.modloader.information.game.LaunchPlatform.MCP;
	}

	@SuppressWarnings("unused")
	private static class ConfigurableRiftProvider implements IRiftToolchainProvider {

		public ArrayList<IClassSourceProvider<?>> sources = new ArrayList<IClassSourceProvider<?>>();
		public Mapping<?> mappings;
		public File mainJar;

		public static ConfigurableRiftProvider fromClosure(Closure<?> closure) {
			ConfigurableRiftProvider prov = new ConfigurableRiftProvider();
			closure.setDelegate(prov);
			closure.call();
			return prov;
		}

		public void mainJar(File jar) {
			mainJar = jar;
		}

		public void library(File lib) {
			sources.add(new FileClassSourceProvider(lib));
		}

		public void library(File[] libs) {
			for (File lib : libs)
				library(lib);
		}

		public void library(Iterable<File> libs) {
			for (File lib : libs)
				library(lib);
		}

		public void library(URL lib) {
			sources.add(new URLClassSourceProvider(lib));
		}

		public void library(URL[] libs) {
			for (URL lib : libs)
				library(lib);
		}

		public void library(IClassSourceProvider<?> lib) {
			sources.add(lib);
		}

		public void library(IClassSourceProvider<?>[] libs) {
			for (IClassSourceProvider<?> lib : libs)
				library(lib);
		}

		@Override
		public File getJar() throws IOException {
			return mainJar;
		}

		@Override
		public IClassSourceProvider<?>[] getSources() throws IOException {
			return sources.toArray(new IClassSourceProvider[0]);
		}

		@Override
		public Mapping<?> getRiftMappings() throws IOException {
			return MinecraftRifterToolkit.generateRiftTargets(mappings);
		}

	}
}
