package org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;

import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.McpPlatform;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.SpigotPlatform;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.VanillaPlatform;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.YarnPlatform;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.closureowners.SpigotPlatformClosureOwner;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.shared.closureowners.YarnPlatformClosureOwner;
import org.asf.cyan.cornflower.gradle.tasks.RiftJarTask;
import org.asf.cyan.cornflower.gradle.utilities.IProjectExtension;
import org.asf.cyan.cornflower.gradle.utilities.modding.ApiDependency;
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

	public static final String AerialWorksMaven = "https://aerialworks.ddns.net/maven";

	public static class PlatformConfiguration {
		public ArrayList<IPlatformConfiguration> all = new ArrayList<IPlatformConfiguration>();

		public void MCP(Closure<?> closure) {
			all.add(new McpPlatform().importClosure(PlatformClosureOwner.fromClosure(closure)));
		}

		public void SPIGOT(Closure<?> closure) {
			all.add(new SpigotPlatform().importClosure(SpigotPlatformClosureOwner.fromClosure(closure)));
		}

		public void VANILLA(Closure<?> closure) {
			all.add(new VanillaPlatform().importClosure(PlatformClosureOwner.fromClosure(closure)));
		}

		public void YARN(Closure<?> closure) {
			all.add(new YarnPlatform().importClosure(YarnPlatformClosureOwner.fromClosure(closure)));
		}
	}

	public static final int API_BASE_MODDING = 2;
	public static final int API_CORE_MODDING = 4;
	public static final int API_FLUID = 8;
	public static final int API_CYANCORE = 16;
	public static final int API_MTK = 32;
	public static final int API_CLASSTRUST = 64;

	public static final Class<org.asf.cyan.cornflower.classpath.util.SourceType> SourceType = org.asf.cyan.cornflower.classpath.util.SourceType.class;
	public static final Class<org.asf.cyan.cornflower.classpath.util.EntryType> EntryType = org.asf.cyan.cornflower.classpath.util.EntryType.class;
	public static final Class<org.asf.cyan.cornflower.classpath.util.PathPriority> PathPriority = org.asf.cyan.cornflower.classpath.util.PathPriority.class;

	public static final Class<?> EclipseLaunchGenerator = org.asf.cyan.cornflower.gradle.tasks.EclipseLaunchGenerator.class;
	public static Class<?> CtcUtil = new Supplier<Class<?>>() {

		@Override
		public Class<?> get() {
			try {
				return Class.forName("org.asf.cyan.cornflower.gradle.tasks.CtcTask");
			} catch (ClassNotFoundException e) {
			}
			return null;
		}

	}.get();

	public static final Class<?> RiftJar = org.asf.cyan.cornflower.gradle.tasks.RiftJarTask.class;

	public static final ModloaderDependency Modloader = new ModloaderDependency();
	public static final GameDependency Game = new GameDependency();
	public static final ApiDependency API = new ApiDependency();

	public static final LaunchPlatform DEOBFUSCATED = LaunchPlatform.DEOBFUSCATED;
	public static final LaunchPlatform UNKNOWN = LaunchPlatform.UNKNOWN;
	public static final LaunchPlatform VANILLA = LaunchPlatform.VANILLA;
	public static final LaunchPlatform SPIGOT = LaunchPlatform.SPIGOT;
	public static final LaunchPlatform YARN = LaunchPlatform.YARN;
	public static final LaunchPlatform MCP = LaunchPlatform.MCP;

	public static final GameSide SERVER = GameSide.SERVER;
	public static final GameSide CLIENT = GameSide.CLIENT;

	public static Configuration<?> modfileManifest(Closure<?> closure) {
		try {
			Class.forName("org.asf.cyan.cornflower.gradle.utilities.modding.CyanModfileManifestGenerator");
		} catch (Exception e) {
			throw new RuntimeException("Cannot call modfileManifest from LiteCyan.");
		}

		try {
			return (Configuration<?>) Class
					.forName("org.asf.cyan.cornflower.gradle.utilities.modding.CyanModfileManifestGenerator")
					.getMethod("fromClosure", Closure.class).invoke(null, closure);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException | ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

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

		String vanillaVersion = "undefined";
		for (IPlatformConfiguration conf : platforms.all) {
			if (conf instanceof VanillaPlatform)
				vanillaVersion = conf.getCommonMappingsVersion();

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

	public static IRiftToolchainProvider getPlatformRiftProvider(Project project, IPlatformConfiguration platform,
			GameSide side) {

		PlatformConfiguration platforms = (PlatformConfiguration) project.getExtensions().getExtraProperties()
				.get("platforms");

		String vanillaVersion = "undefined";
		for (IPlatformConfiguration conf : platforms.all) {
			if (conf instanceof VanillaPlatform)
				vanillaVersion = conf.getCommonMappingsVersion();

			if (platform == conf) {
				MinecraftVersionInfo gameVersion = MinecraftVersionToolkit.getVersion(vanillaVersion);
				if (gameVersion == null)
					gameVersion = new MinecraftVersionInfo(vanillaVersion, MinecraftVersionType.UNKNOWN, null,
							OffsetDateTime.now());

				return SimpleRiftBuilder.getProviderForPlatform(conf.getPlatform(), gameVersion, side,
						conf.getModloaderVersion(), conf.getMappingsVersion(side));
			}
		}

		return getPlatformRiftProvider(project, platform.getPlatform(), side);
	}

	public static void addPlatformRiftTasks(Project project, Closure<?> closure) {
		PlatformRiftClosureOwner.runClosure(project, closure);
	}

	@SuppressWarnings("unused")
	private static class PlatformRiftClosureOwner {
		public ArrayList<Object> sources = new ArrayList<Object>();
		public ArrayList<IRiftToolchainProvider> providers = new ArrayList<IRiftToolchainProvider>();
		public ArrayList<IPlatformConfiguration> platforms = new ArrayList<IPlatformConfiguration>();

		public void from(Object... sources) {
			for (Object source : sources)
				this.sources.add(source);
		}

		public void provider(IRiftToolchainProvider provider) {
			providers.add(provider);
		}

		public void platform(IPlatformConfiguration platform) {
			platforms.add(platform);
		}

		public void platform(IPlatformConfiguration[] platforms) {
			for (IPlatformConfiguration platform : platforms)
				this.platforms.add(platform);
		}

		public void platform(Iterable<IPlatformConfiguration> platforms) {
			for (IPlatformConfiguration platform : platforms)
				this.platforms.add(platform);
		}

		public void platform(PlatformConfiguration platforms) {
			for (IPlatformConfiguration platform : platforms.all)
				this.platforms.add(platform);
		}

		public static void runClosure(Project project, Closure<?> closure) {
			PlatformRiftClosureOwner owner = new PlatformRiftClosureOwner();
			closure.setDelegate(owner);
			closure.call();

			owner.add(project);
		}

		private void add(Project project) {
			ArrayList<RiftJarTask> tasks = new ArrayList<RiftJarTask>();
			for (IPlatformConfiguration platform : platforms) {
				String verName = platform.getCommonMappingsVersion().replaceAll("[^A-Za-z0-9.\\-]", "");
				verName = verName.toLowerCase();
				verName = verName.substring(0, 1).toUpperCase() + verName.substring(1);
				String name = platform.getPlatform().toString().toLowerCase() + "Rift" + verName;
				name = name.replaceAll("[^A-Za-z0-9]", "_");
				if (platform.getMappingsVersion(GameSide.CLIENT) != null) {
					tasks.add((RiftJarTask) project.task(Map.of("type", RiftJar), name,
							new TaskClosure(project, project, platform, GameSide.CLIENT)));
				}
				if (platform.getMappingsVersion(GameSide.SERVER) != null) {
					tasks.add((RiftJarTask) project.task(Map.of("type", RiftJar), name + "Server",
							new TaskClosure(project, project, platform, GameSide.SERVER)));
				}
			}
			project.getExtensions().getExtraProperties().set("riftTasks", tasks.toArray(new RiftJarTask[0]));
		}

		private class TaskClosure extends Closure<RiftJarTask> {
			private Project project;
			private IPlatformConfiguration config;
			private GameSide side;

			public TaskClosure(Object owner, Project project, IPlatformConfiguration config, GameSide side) {
				super(owner);
				this.project = project;
				this.config = config;
				this.side = side;
			}

			private static final long serialVersionUID = 1L;

			@Override
			public RiftJarTask call() {
				RiftJarTask tsk = (RiftJarTask) getDelegate();
				tsk.provider(getPlatformRiftProvider(project, config, side));

				tsk.mappings_identifier(config.getPlatform().toString().toLowerCase() + "-"
						+ config.getDisplayVersion().replaceAll("[!?/:\\\\]", "-") + "-"
						+ side.toString().toLowerCase());
				tsk.getArchiveClassifier()
						.set("RIFT-" + config.getPlatform().toString().toUpperCase() + "-"
								+ config.getCommonMappingsVersion().replaceAll("[!?/:\\\\]", "-")
								+ (side == GameSide.SERVER ? "-SERVER" : ""));

				tsk.from(sources);
				tsk.platform(config);
				tsk.side(side);
				providers.forEach((prov) -> tsk.provider(prov));

				project.getTasks().getByName("rift").finalizedBy(tsk);
				return tsk;
			}

		}
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
