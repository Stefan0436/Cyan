package org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;

import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.McpPlatform;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.SpigotPlatform;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.VanillaPlatform;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.YarnPlatform;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.closureowners.SpigotPlatformClosureOwner;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.closureowners.YarnPlatformClosureOwner;
import org.asf.cyan.cornflower.gradle.utilities.IProjectExtension;
import org.asf.cyan.cornflower.gradle.utilities.modding.GameDependency;
import org.asf.cyan.cornflower.gradle.utilities.modding.IPlatformConfiguration;
import org.asf.cyan.cornflower.gradle.utilities.modding.ModloaderDependency;
import org.asf.cyan.cornflower.gradle.utilities.modding.PlatformClosureOwner;
import org.asf.cyan.fluid.bytecode.sources.FileClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.IClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.URLClassSourceProvider;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftRifterToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.rift.SimpleRiftBuilder;
import org.asf.cyan.minecraft.toolkits.mtk.rift.providers.IRiftToolchainProvider;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;
import org.gradle.api.Project;

import groovy.lang.Closure;

public class CornflowerMainExtension implements IProjectExtension {

	public static class PlatformConfiguration {
		public IPlatformConfiguration MCP;
		public IPlatformConfiguration SPIGOT;
		public IPlatformConfiguration VANILLA;
		public IPlatformConfiguration YARN;

		public ArrayList<IPlatformConfiguration> all = new ArrayList<IPlatformConfiguration>();

		public PlatformConfiguration() {
			MCP = new McpPlatform();
			SPIGOT = new SpigotPlatform();
			YARN = new YarnPlatform();
			VANILLA = new VanillaPlatform();

			all.add(MCP);
			all.add(SPIGOT);
			all.add(VANILLA);
			all.add(YARN);
		}

		public void MCP(Closure<?> closure) {
			MCP.importClosure(PlatformClosureOwner.fromClosure(closure));
		}

		public void SPIGOT(Closure<?> closure) {
			SPIGOT.importClosure(SpigotPlatformClosureOwner.fromClosure(closure));
		}

		public void VANILLA(Closure<?> closure) {
			VANILLA.importClosure(PlatformClosureOwner.fromClosure(closure));
		}

		public void YARN(Closure<?> closure) {
			YARN.importClosure(YarnPlatformClosureOwner.fromClosure(closure));
		}
	}

	public static final Class<org.asf.cyan.cornflower.classpath.util.SourceType> SourceType = org.asf.cyan.cornflower.classpath.util.SourceType.class;
	public static final Class<org.asf.cyan.cornflower.classpath.util.EntryType> EntryType = org.asf.cyan.cornflower.classpath.util.EntryType.class;
	public static final Class<org.asf.cyan.cornflower.classpath.util.PathPriority> PathPriority = org.asf.cyan.cornflower.classpath.util.PathPriority.class;

	public static final Class<?> EclipseLaunchGenerator = org.asf.cyan.cornflower.gradle.tasks.EclipseLaunchGenerator.class;
	public static final Class<?> CtcUtil = org.asf.cyan.cornflower.gradle.tasks.CtcTask.class;
	public static final Class<?> RiftJar = org.asf.cyan.cornflower.gradle.tasks.RiftJarTask.class;

	public static final ModloaderDependency Modloader = new ModloaderDependency();
	public static final GameDependency Game = new GameDependency();

	public static final LaunchPlatform DEOBFUSCATED = LaunchPlatform.DEOBFUSCATED;
	public static final LaunchPlatform UNKNOWN = LaunchPlatform.UNKNOWN;
	public static final LaunchPlatform VANILLA = LaunchPlatform.VANILLA;
	public static final LaunchPlatform SPIGOT = LaunchPlatform.SPIGOT;
	public static final LaunchPlatform YARN = LaunchPlatform.YARN;
	public static final LaunchPlatform MCP = LaunchPlatform.MCP;

	public static final GameSide SERVER = GameSide.SERVER;
	public static final GameSide CLIENT = GameSide.CLIENT;

	public static void platforms(Project proj, Closure<?> closure) {
		PlatformConfiguration config = new PlatformConfiguration();
		closure.setDelegate(config);
		closure.call();

		proj.getExtensions().getExtraProperties().set("platforms", config);
	}

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

	public static IRiftToolchainProvider getPlatformRiftProvider(Project project, LaunchPlatform platform,
			GameSide side) {

		PlatformConfiguration platforms = (PlatformConfiguration) project.getExtensions().getExtraProperties()
				.get("platforms");

		String vanillaVersion = platforms.VANILLA.getMappingsVersion(side);
		for (IPlatformConfiguration conf : platforms.all) {
			if (conf.getPlatform() == platform) {
				MinecraftVersionInfo gameVersion = MinecraftVersionToolkit.getVersion(vanillaVersion);
				if (gameVersion == null)
					gameVersion = new MinecraftVersionInfo(vanillaVersion, MinecraftVersionType.UNKNOWN, null,
							OffsetDateTime.now());

				return SimpleRiftBuilder.getProviderForPlatform(conf.getPlatform(), gameVersion, side,
						conf.getModloaderVersion(), conf.getMappingsVersion(side));
			}
		}

		return null;
	}

	@SuppressWarnings("unused")
	private static class ConfigurableRiftProvider implements IRiftToolchainProvider {

		public ArrayList<IClassSourceProvider<?>> sources = new ArrayList<IClassSourceProvider<?>>();
		public ArrayList<Mapping<?>> mappings = new ArrayList<Mapping<?>>();
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

		public void mappings(Iterable<Mapping<?>> mappings) {
			for (Mapping<?> map : mappings)
				mappings(map);
		}

		public void mappings(Mapping<?> mappings) {
			this.mappings.add(MinecraftRifterToolkit.generateRiftTargets(mappings));
		}

		public void mappings(Mapping<?>[] mappings) {
			for (Mapping<?> map : mappings)
				mappings(map);
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
			return MinecraftRifterToolkit.generateRiftTargets(mappings.toArray(new Mapping<?>[0]));
		}

	}
}
