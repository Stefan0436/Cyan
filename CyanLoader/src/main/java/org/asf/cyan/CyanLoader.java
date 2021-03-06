package org.asf.cyan;

import java.awt.GraphicsEnvironment;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.serializing.internal.Splitter;
import org.asf.cyan.api.events.IEventProvider;
import org.asf.cyan.api.events.core.EventBusFactory;
import org.asf.cyan.api.events.extended.IExtendedEvent;
import org.asf.cyan.api.fluid.annotations.LoaderVersionGreaterThan;
import org.asf.cyan.api.fluid.annotations.LoaderVersionLessThan;
import org.asf.cyan.api.fluid.annotations.PlatformExclude;
import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.fluid.annotations.SideOnly;
import org.asf.cyan.api.fluid.annotations.VersionRegex;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.api.modloader.information.modloader.LoadPhase;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.modloader.information.providers.IModProvider;
import org.asf.cyan.api.reports.ReportBuilder;
import org.asf.cyan.api.reports.ReportCategory;
import org.asf.cyan.api.reports.ReportNode;
import org.asf.cyan.api.versioning.StringVersionProvider;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.core.SimpleModloader;
import org.asf.cyan.core.StartupWindow;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.FluidAgent;
import org.asf.cyan.fluid.api.ClassLoadHook;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.information.metadata.TransformerMetadata;
import org.asf.cyan.fluid.implementation.CyanReportBuilder;
import org.asf.cyan.fluid.implementation.CyanTransformerMetadata;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.internal.KickStartConfig.KickStartInstallation;
import org.asf.cyan.internal.LegacyModKitSupportHook;
import org.asf.cyan.internal.ManifestUtils;
import org.asf.cyan.internal.modkitimpl.info.Protocols;
import org.asf.cyan.internal.modkitimpl.util.EventUtilImpl;
import org.asf.cyan.loader.configs.ModUpdateChannel;
import org.asf.cyan.loader.configs.ModUpdateChannelConfig;
import org.asf.cyan.loader.configs.ModUpdateConfiguration;
import org.asf.cyan.loader.configs.SecurityConfiguration;
import org.asf.cyan.loader.eventbus.CyanEventBridge;

import org.asf.cyan.minecraft.toolkits.mtk.FabricCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.ForgeCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.PaperCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.mods.AbstractMod;
import org.asf.cyan.mods.ICoremod;
import org.asf.cyan.mods.IMod;
import org.asf.cyan.mods.config.CyanModfileManifest;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.internal.BaseEventController;
import org.asf.cyan.mods.internal.IAcceptableComponent;
import org.asf.cyan.mods.internal.ModInfoCache;
import org.asf.cyan.security.TrustContainer;

import modkit.protocol.ModKitModloader;
import modkit.threading.ThreadManager;
import modkit.util.CheckString;

/**
 * 
 * CyanLoader Minecraft Modloader.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CyanLoader extends ModKitModloader
		implements IModProvider, ModKitModloader.ModKitProtocolRules, IEventListenerContainer {

	private static File[] extraClassPath = new File[0];

	private CyanLoader() {
		mavenRepositories.put("AerialWorks", "https://aerialworks.ddns.net/maven");
		mavenRepositories.put("Maven Central", "https://repo1.maven.org/maven2");
		RootModloader.participate(this, RootRules.defaultRules().level(5));
	}

	protected static String getMarker() {
		return "MODLOADER";
	}

	public static void appendCyanInfo(BiConsumer<String, Object> setDetail1, BiConsumer<String, Object> setDetail2) {
		int mods = Modloader.getModloader(CyanLoader.class).getLoadedCoremods().length;
		if (mods != 0) {
			setDetail1.accept("Coremods", mods);
			for (IModManifest coremod : Modloader.getModloader(CyanLoader.class).getLoadedCoremods()) {
				setDetail1.accept(coremod.id(), displayMod(coremod, true));
			}
		} else
			setDetail1.accept("Mods", "No mods loaded");

		mods = Modloader.getModloader(CyanLoader.class).getLoadedMods().length;
		if (mods != 0) {
			setDetail2.accept("Mods", mods);
			for (IModManifest mod : Modloader.getModloader(CyanLoader.class).getLoadedMods()) {
				setDetail2.accept(mod.id(), displayMod(mod, false));
			}
		} else
			setDetail2.accept("Mods", "No mods loaded");
	}

	public static String displayMod(IModManifest mod, boolean coremod) {
		try {
			HashMap<String, Object> entries = new HashMap<String, Object>();
			entries.put("Version", mod.version());
			entries.put("Display Name", mod.displayName());
			entries.put("Dependencies", (mod.dependencies().length + mod.optionalDependencies().length) + " ("
					+ mod.optionalDependencies().length + " optional)");

			long allThreads = 0;
			long suspendedThreads = 0;
			long savedThreads = 0;
			long repeatingThreads = 0;
			for (ThreadManager manager : ThreadManager.getThreadManagers(mod)) {
				allThreads += manager.getThreadList().size();
				allThreads += manager.getSuspendedThreadList().size();

				savedThreads += manager.getSavedThreads().length;
				repeatingThreads += manager.getThreadList().stream().filter(t -> t.isRepeating()).count();
				suspendedThreads += manager.getSuspendedThreadList().size();

			}
			entries.put("Mod Threads", allThreads + " (" + suspendedThreads + " suspended, " + savedThreads
					+ " in memory, " + repeatingThreads + " repeating)");

			if (coremod) {
				int transformers = 0;
				int applied = 0;
				for (String[] data : CyanLoader.transformers.keySet()) {
					if (data[1].equals(mod.id()))
						transformers++;
				}

				for (TransformerMetadata md : TransformerMetadata.getLoadedTransformers()) {
					if (md.getTransfomerOwner().equals(mod.id()))
						applied++;
				}

				entries.put("Transformers", transformers + " (" + applied + " applied)");
			}

			StringBuilder value = new StringBuilder();
			for (String name : entries.keySet()) {
				value.append("\n\t\t");
				value.append(name);
				value.append(": ");
				value.append(entries.get(name));
			}

			return value.toString();
		} catch (Exception e) {
			return "~~ ERROR ~~ " + e.getMessage();
		}
	}

	private HashMap<String, String> mavenRepositories = new HashMap<String, String>();
	private HashMap<String, Version> coremodMavenDependencies = new HashMap<String, Version>();
	private HashMap<String, Version> modMavenDependencies = new HashMap<String, Version>();

	private static String platformVersion = "";

	/**
	 * Sets the platform version (can only be done ONCE)
	 * 
	 * @param version Platform version
	 */
	public static void setPlatformVersion(String version) {
		if (!platformVersion.isEmpty())
			return;
		platformVersion = version;
	}

	private static boolean developerMode = false;
	private static SecurityConfiguration securityConf;
	private static ArrayList<TrustContainer> trust = new ArrayList<TrustContainer>();
	private static final PrintStream defaultOutputStream = System.out;
	private static ArrayList<Mapping<?>> compatibilityMappings = new ArrayList<Mapping<?>>();
	private static String[] allowedComponentPackages = new String[0];
	private static Mapping<?> mappings = null;
	private static boolean vanillaMappings = true;
	private static boolean loaded = false;
	private static File cyanDir;

	private HashMap<String, CyanModfileManifest> modManifests = new HashMap<String, CyanModfileManifest>();
	private HashMap<String, CyanModfileManifest> coreModManifests = new HashMap<String, CyanModfileManifest>();

	private ArrayList<IMod> mods = new ArrayList<IMod>();
	private ArrayList<ICoremod> coremods = new ArrayList<ICoremod>();

	public static boolean isDeveloperModeEnabled() {
		return developerMode;
	}

	private static HashMap<String, HashMap<String, byte[]>> classesMap = new HashMap<String, HashMap<String, byte[]>>();

	private HashMap<String, IAcceptableComponent> loadedComponents = new HashMap<String, IAcceptableComponent>();

	private static String[] acceptableProviders = new String[] { "transformers", "transformer-packages", "class-hooks",
			"class-hook-packages", "auto.init", "mod.id" };

	public static File getCyanDataDirectory() {
		return cyanDir;
	}

	public static boolean areVanillaMappingsEnabled() {
		return vanillaMappings;
	}

	/**
	 * Enables component whitelisting.<br/>
	 * <br/>
	 * <b>WARNING:</b> using this exposes the coremodule loading mechanism, do not
	 * use this outside of development environments!<br/>
	 * <br/>
	 * This method must be called BEFORE the modloader is prepared, only top-level
	 * wrappers can use this.
	 */
	public static void enableDeveloperMode() {
		developerMode = true;
	}

	private static void prepare(String side) throws IOException {
		Configuration.setLoggers(str -> warn(str), str -> error(str));
		setupModloader(side);
		loaded = true;

		int progressMax = 0;
		progressMax++; // set dir
		progressMax++; // set dump
		progressMax++; // del backtrace
		progressMax++; // reset server connection
		progressMax++; // load version

		if (!MinecraftMappingsToolkit.areMappingsAvailable(
				new MinecraftVersionInfo(CyanInfo.getMinecraftVersion(), null, null, null), CyanCore.getSide())
				&& vanillaMappings) {
			progressMax++; // resolve
			progressMax++; // download version
			progressMax++; // download mappings
			progressMax++; // save mappings
		}

		// Base loading
		if (vanillaMappings) {
			progressMax++; // load mappings
		}
		progressMax++; // load postponed components
		progressMax++; // load events
		progressMax++; // load coremods
		progressMax++; // finish load

		// Mod loading
		progressMax++;
		progressMax++;
		progressMax++;
		progressMax++;
		progressMax++;

		// Ignore for Forge
		if (CyanInfo.getPlatform() != LaunchPlatform.MCP) {
			progressMax++;
			progressMax++;
			progressMax++;
		}

		// Final loading
		progressMax++;
		progressMax++;

		cyanDir = new File(".cyan-data");
		if (!cyanDir.exists())
			cyanDir.mkdirs();

		String cPath = cyanDir.getCanonicalPath();
		info("Starting CYAN in: " + cPath);

		StartupWindow.WindowAppender.addMax(progressMax);

		MinecraftInstallationToolkit.setMinecraftDirectory(cyanDir);
		StartupWindow.WindowAppender.increaseProgress();

		Fluid.setDumpDir(MinecraftInstallationToolkit.getMinecraftDirectory());
		StartupWindow.WindowAppender.increaseProgress();

		if (new File(cyanDir, "transformer-backtrace").exists())
			deleteDir(new File(cyanDir, "transformer-backtrace"));
		StartupWindow.WindowAppender.increaseProgress();

		MinecraftToolkit.resetServerConnectionState();
		StartupWindow.WindowAppender.increaseProgress();

		MinecraftVersionInfo mcVersion = new MinecraftVersionInfo(CyanInfo.getMinecraftVersion(), null, null, null);
		StartupWindow.WindowAppender.increaseProgress();

		if (!MinecraftMappingsToolkit.areMappingsAvailable(mcVersion, CyanCore.getSide()) && vanillaMappings) {
			info("First time loading, downloading " + side.toLowerCase() + " mappings...");

			MinecraftToolkit.resolveVersions();
			StartupWindow.WindowAppender.increaseProgress();

			MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(CyanInfo.getMinecraftVersion());
			StartupWindow.WindowAppender.increaseProgress();

			MinecraftMappingsToolkit.downloadVanillaMappings(version, CyanCore.getSide());
			StartupWindow.WindowAppender.increaseProgress();

			MinecraftMappingsToolkit.saveMappingsToDisk(version, CyanCore.getSide());
			StartupWindow.WindowAppender.increaseProgress();
		}

		if (vanillaMappings) {
			mappings = MinecraftMappingsToolkit.loadMappings(mcVersion, CyanCore.getSide());
			StartupWindow.WindowAppender.increaseProgress();
		}
	}

	private static boolean setup = false;

	/**
	 * Prepare CyanLoader, called automatically.
	 * 
	 * @param side Side to use
	 * @throws IOException If setting up the loader fails.
	 */
	public static void setupModloader(String side) throws IOException {
		if (setup)
			return;
		CyanCore.addToPackageScan("modkit");
		CyanCore.addAllowedPackage("modkit");

		if (developerMode) {
			if (System.getProperty("authorizeDebugPackages") != null)
				allowedComponentPackages = System.getProperty("authorizeDebugPackages").split(":");

			System.err.println("");
			System.err.println("");
			System.err.println("DANGER!");
			System.err.println("Jar and coremod loading mechanisms have been released to the command line!");
			System.err.println("Shut down the program if you are not running in a development environment!");
			System.err.println("");
			System.err.println("");

			ArrayList<File> paths = new ArrayList<File>();
			if (System.getProperty("cyan.load.classpath") != null) {
				for (String str : Splitter.split(System.getProperty("cyan.load.classpath"), File.pathSeparator)) {
					if (!str.isEmpty())
						paths.add(new File(str));
				}
			}
			extraClassPath = paths.toArray(t -> new File[t]);
		} else {
			if (System.getProperty("authorizeDebugPackages") != null) {
				System.err.println(
						"Cyan is not running in a development environment, you cannot use authorizeDebugPackages outside of it.");
				System.exit(-1);
			}
		}

		if (side.equals("SERVER")) {
			URL url = CyanLoader.class.getResource("/log4j2-server.xml");
			if (MinecraftInstallationToolkit.isIDEModeEnabled()) {
				url = CyanLoader.class.getResource("/log4j2-server-ide.xml");
			}
			System.setProperty("log4j2.configurationFile", url.toString());
			System.setProperty("log4j.configurationFile", url.toString());
			if (System.getProperty("cyanDevEnv.noDisable.Agent") == null)
				CyanCore.disableAgent();
		}

		CyanCore.setSide(side);
		if (CyanCore.getCoreClassLoader() == null)
			CyanCore.initLoader();

		CyanCore.simpleInit();

		if (LOG == null)
			CyanCore.initLogger();

		if (side.equals("CLIENT") || !GraphicsEnvironment.isHeadless())
			StartupWindow.WindowAppender.showWindow();

		if (cyanDir == null) {
			cyanDir = new File(".cyan-data");

			if (!cyanDir.exists())
				cyanDir.mkdirs();

			if (MinecraftInstallationToolkit.getMinecraftDirectory() == null) {
				MinecraftInstallationToolkit.setMinecraftDirectory(cyanDir);
			}
		}
		securityConf = new SecurityConfiguration(cyanDir.getAbsolutePath());
		securityConf.readAll();

		File coremods = new File(cyanDir, "coremods");
		File versionCoremods = new File(coremods, CyanInfo.getMinecraftVersion());

		Thread.currentThread().setUncaughtExceptionHandler((t, ex) -> {
			StringBuilder buffer = new StringBuilder();
			ex.printStackTrace(new PrintStream(new OutputStream() {

				@Override
				public void write(int arg0) throws IOException {
					buffer.append((char) arg0);
				}

			}));
			LOG.fatal(buffer.toString().trim());
			StartupWindow.WindowAppender.fatalError(ex.getClass().getSimpleName() + ": " + ex.getLocalizedMessage());
			buildCrashReport(t, ex);
			System.exit(1);
		});

		CyanLoader ld = new CyanLoader();
		ld.addInformationProvider(CyanInfo.getProvider());
		ld.addInformationProvider(ld);
		appendImplementation(ld);

		if (System.getProperty("debugModfileManifests") != null) {
			String[] files = null;

			if (System.getProperty("os.name").toLowerCase().contains("win")
					&& !System.getProperty("os.name").toLowerCase().contains("darwin"))
				files = System.getProperty("debugModfileManifests").split(";");
			else
				files = System.getProperty("debugModfileManifests").split(":");

			for (String file : files) {
				if (file.startsWith("CM//") && developerMode == true) {
					CyanModfileManifest mod = new CyanModfileManifest();
					mod.readAll(new String(Files.readAllBytes(new File(file.substring(4)).toPath())));
					ld.coreModManifests.put(mod.modGroup + ":" + mod.modId, mod);
					ld.coreModManifests.put(mod.modClassPackage + "." + mod.modClassName, mod);
					CyanCore.addAllowedPackage(mod.modClassPackage);
					classesMap.put(mod.modGroup + ":" + mod.modId, new HashMap<String, byte[]>());
					try {
						Class<?> cls = Class.forName(mod.modClassPackage + "." + mod.modClassName);
						String base = cls.getProtectionDomain().getCodeSource().getLocation().toString();
						if (base.toString().startsWith("jar:"))
							base = base.substring(0, base.lastIndexOf("!"));
						else if (base.endsWith("/" + cls.getTypeName().replace(".", "/") + ".class")) {
							base = base.substring(0,
									base.length() - ("/" + cls.getTypeName().replace(".", "/") + ".class").length());
						}
						if (!base.startsWith("jar:") && (base.endsWith(".jar") || base.endsWith(".zip")))
							base = "jar:" + base + "!/";
						mod.source = base;
						CyanCore.addAdditionalClass(cls);
						CyanCore.addToPackageScan(mod.modClassPackage);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				} else if (file.startsWith("M//")) {
					CyanModfileManifest mod = new CyanModfileManifest();
					mod.readAll(new String(Files.readAllBytes(new File(file.substring(3)).toPath())));
					ld.modManifests.put(mod.modClassPackage + "." + mod.modClassName, mod);
					ld.modManifests.put(mod.modGroup + ":" + mod.modId, mod);
					CyanCore.addAllowedPackage(mod.modClassPackage);
				}
			}
		}
		MinecraftToolkit.resetServerConnectionState();

		if (!coremods.exists())
			coremods.mkdirs();
		if (versionCoremods.exists()) {
			ld.importCoremods(versionCoremods);
		}
		ld.importCoremods(coremods);

		File trustContainers = new File(cyanDir, "trust");
		if (!trustContainers.exists())
			trustContainers.mkdirs();
		importTrust(trustContainers);

		Modloader.addModloaderImplementation(ld);
		if (!CyanInfo.getModloaderName().isEmpty() || !CyanInfo.getModloaderVersion().isEmpty()) {
			String name = CyanInfo.getModloaderName();
			String version = CyanInfo.getModloaderVersion();
			if (CyanInfo.getModloaderName().isEmpty()) {
				name = CyanInfo.getModloaderVersion();
				version = "";
			}

			SimpleModloader modloader = new SimpleModloader(name, name, new StringVersionProvider(version));
			Modloader.addModloaderImplementation(modloader);
		}

		setup = true;
	}

	private static void buildCrashReport(Thread thread, Throwable ex) {
		warn("Game is crashing before it started! Building pre-start Cyan crash report...");
		if (ReportBuilder.getImplementationInstance() == null)
			CyanReportBuilder.initComponent();
		if (TransformerMetadata.getImplementationInstance() == null)
			CyanTransformerMetadata.initComponent();

		ReportBuilder builder = ReportBuilder.create("Cyan Early Crash Report");

		ReportCategory head = builder.newCategory("Head");

		ReportNode generalDetails = builder.newNode(head, "General Details");
		generalDetails.add("Time", new Date());
		generalDetails.add("Thread Name", thread.getName());

		ReportNode exceptionDetails = builder.newNode(head, "Exception Details");
		exceptionDetails.add("Exception Type", () -> {
			return ex.getClass().getTypeName();
		});
		exceptionDetails.add("Message", () -> {
			return ex.getMessage();
		});
		builder.newNode(head, "Stacktrace").add(() -> {
			StringBuilder stacktrace = new StringBuilder();
			Throwable exception = ex;
			boolean first = true;
			while (exception != null) {
				if (!first) {
					stacktrace.append("\n");
				}
				first = false;
				stacktrace.append("Caused by: " + exception.getClass().getTypeName() + ": " + exception.getMessage());
				for (StackTraceElement e : exception.getStackTrace()) {
					stacktrace.append("\n");
					first = false;
					stacktrace.append("\tat ").append(e);
				}
				exception = exception.getCause();
			}
			return stacktrace;
		});

		ReportCategory details = builder.newCategory("Installation Details");

		ReportNode node = builder.newNode(details, "Version Information");
		node.add("Game Version", CyanInfo.getMinecraftVersion());
		node.add("Full Version", CyanInfo.getMinecraftCyanVersion());

		String modloaders = "";
		String loaderversions = "";
		ReportNode loaderInfo = builder.newNode(details, "Modloader(s)");
		for (Modloader modloader : Modloader.getAllModloaders()) {
			if (!modloaders.isEmpty()) {
				modloaders += ", ";
				loaderversions += ", ";
			}

			modloaders += modloader.getSimpleName();
			loaderversions += modloader.getName() + "; "
					+ (modloader.getVersion() == null ? "Generic" : modloader.getVersion());
		}
		if (modloaders.isEmpty())
			modloaders = "None loaded";
		if (loaderversions.isEmpty())
			loaderversions = "None loaded";

		loaderInfo.add("Running Modloader(s)", modloaders);
		loaderInfo.add("Modloader Version(s)", loaderversions);
		loaderInfo.add("Modloader Phase", CyanCore.getCurrentPhase());
		for (Modloader modloader : Modloader.getAllModloaders()) {
			if (modloader.supportsMods())
				loaderInfo.add("Loaded " + modloader.getSimpleName().toUpperCase() + " Mods",
						modloader.getLoadedMods().length);
			if (modloader.supportsCoreMods())
				loaderInfo.add("Loaded " + modloader.getSimpleName().toUpperCase() + " Coremods",
						modloader.getLoadedCoremods().length);
		}

		if (Modloader.getModloader(CyanLoader.class) != null) {
			ReportCategory cyanMods = builder.newCategory("Installed CYAN Mods");
			ReportNode coremodsCategoryCyanLoader = builder.newNode(cyanMods, "Loaded CYAN Coremods");
			ReportNode modsCategoryCyanLoader = builder.newNode(cyanMods, "Loaded CYAN Mods");
			CyanLoader.appendCyanInfo(
					(str, obj) -> coremodsCategoryCyanLoader.add(str, obj).toString().replace("\n\t", "\n"),
					(str, obj) -> modsCategoryCyanLoader.add(str, obj.toString().replace("\n\t", "\n")));
		}

		ReportCategory systemDetails = builder.newCategory("System Details");

		ReportNode system = builder.newNode(systemDetails, "Operating System");
		system.add("Name", System.getProperty("os.name"));
		system.add("Version", System.getProperty("os.version"));
		system.add("Architecture", System.getProperty("os.arch"));

		ReportNode runtime = builder.newNode(systemDetails, "Runtime Details");
		runtime.add("CPUs", Runtime.getRuntime().availableProcessors());
		runtime.add("RAM", Runtime.getRuntime().totalMemory() + " / " + Runtime.getRuntime().maxMemory() + " ("
				+ Runtime.getRuntime().freeMemory() + " free)");

		ReportCategory javaDetails = builder.newCategory("Java Installation Details");

		ReportNode java = builder.newNode(javaDetails, "Java Details");
		java.add("Java Version", System.getProperty("java.version"));
		java.add("Java Vendor", System.getProperty("java.vendor"));

		ReportNode javaSpec = builder.newNode(javaDetails, "Java Specification");
		javaSpec.add("Java Specification Name", System.getProperty("java.specification.name"));
		javaSpec.add("Java Specification Version", System.getProperty("java.specification.version"));

		ReportNode javaRuntime = builder.newNode(javaDetails, "Java Runtime");
		javaRuntime.add("Java Runtime Name", System.getProperty("java.runtime.name"));
		javaRuntime.add("Java Runtime Version", System.getProperty("java.runtime.version"));

		ReportNode javaVM = builder.newNode(javaDetails, "Java VM Details");
		javaVM.add("Java VM Vendor", System.getProperty("java.vm.vendor"));
		javaVM.add("Java VM Name", System.getProperty("java.vm.name"));
		javaVM.add("Java VM Version", System.getProperty("java.vm.version"));
		javaVM.add("Java VM Specification", System.getProperty("java.vm.specification.name"));

		StringBuilder report = new StringBuilder();
		builder.build(report);

		LOG.info("Saving crash report...");

		CyanLoader.getSystemOutputStream().println(report.toString());

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss");
		File file = new File(MinecraftInstallationToolkit.getMinecraftDirectory(),
				"crash-reports/crash-" + dateFormat.format(new Date()) + "-"
						+ Modloader.getModloaderGameSide().toString().toLowerCase() + ".txt");
		if (!file.getParentFile().exists())
			file.getParentFile().mkdirs();
		try {
			Files.write(file.toPath(), report.toString().getBytes());
			try {
				LOG.fatal("!ALERT! Game has crashed! Crash report has been saved to: " + file.getCanonicalPath());
			} catch (IOException e) {
				LOG.fatal("!ALERT! Game has crashed! Crash report has been saved to: " + file.getAbsolutePath());
			}
		} catch (IOException e) {
			LOG.fatal("!ALERT! Game has crashed! ERROR: Could not save crash report, unknown error.");
		}
	}

	private void loadCoreMods(ClassLoader loader) {
		coreModManifests.keySet().stream().sorted((t1, t2) -> t1.compareTo(t2)).forEach(k -> {
			CyanModfileManifest manifest = coreModManifests.get(k);
			if (k.contains(":") && !manifest.loaded)
				loadMod(true, manifest, new ArrayList<String>(), loader);
		});
	}

	private void loadMod(boolean coremod, CyanModfileManifest manifest, ArrayList<String> loadingMods,
			ClassLoader loader) {
		IMod[] mods = new IMod[this.mods.size() + coremods.size()];

		int i = 0;
		for (IMod mod : this.mods)
			mods[i++] = mod;
		for (IMod mod : coremods)
			mods[i++] = mod;

		if (loadingMods.contains(manifest.modGroup + ":" + manifest.modId)) {
			fatal("Mod dependency cicle detected! Currently loading id: " + manifest.modGroup + ":" + manifest.modId);
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}
		loadingMods.add(manifest.modGroup + ":" + manifest.modId);

		debug("Preparing to load mod " + manifest.modGroup + ":" + manifest.modId + "... (" + manifest.displayName
				+ ")");

		final Collection<CyanModfileManifest> allManifests;
		if (coremod) {
			allManifests = coreModManifests.values();
		} else {
			allManifests = modManifests.values();
		}

		manifest.mavenRepositories.forEach((name, url) -> {
			mavenRepositories.putIfAbsent(name, url);
		});

		manifest.dependencies.forEach((id, version) -> {
			Optional<CyanModfileManifest> optManifest = allManifests.stream()
					.filter(t -> id.equals(t.modGroup + ":" + t.modId)).findFirst();
			if (optManifest.isEmpty() && !Stream.of(mods).anyMatch(t -> t.getManifest().id().equals(id))) {
				fatal("Missing mod dependency for " + manifest.displayName + ": " + id);
				StartupWindow.WindowAppender.fatalError();
				System.exit(-1);
			}
			String cVersion = (optManifest.isEmpty()
					? Stream.of(mods).filter(t -> t.getManifest().id().equals(id)).findFirst().get().getManifest()
							.version().toString()
					: optManifest.get().version);

			Version ver = Version.fromString(cVersion);
			checkDependencyVersion(version, ver, "Missing mod dependency for " + manifest.displayName + ": " + id);

			if (!optManifest.isEmpty() && !Stream.of(mods).anyMatch(
					t -> t.getManifest().id().equals(optManifest.get().modGroup + ":" + optManifest.get().modId))) {
				loadMod(coremod, optManifest.get(), loadingMods, loader);
			}
		});

		manifest.optionalDependencies.forEach((id, version) -> {
			Optional<CyanModfileManifest> optManifest = allManifests.stream()
					.filter(t -> id.equals(t.modGroup + ":" + t.modId)).findFirst();

			if (!optManifest.isEmpty() && !Stream.of(mods).anyMatch(
					t -> t.getManifest().id().equals(optManifest.get().modGroup + ":" + optManifest.get().modId))) {

				String cVersion = (optManifest.isEmpty()
						? Stream.of(mods).filter(t -> t.getManifest().id().equals(id)).findFirst().get().getManifest()
								.version().toString()
						: optManifest.get().version);

				Version ver = Version.fromString(cVersion);
				checkDependencyVersion(version, ver, "Missing mod dependency for " + manifest.displayName + ": " + id);

				loadMod(coremod, optManifest.get(), loadingMods, loader);
			}
		});

		IMod mod = getMod(manifest.modClassPackage + "." + manifest.modClassName, coremod, loader);
		if (mod == null) {
			if (coremod) {
				fatal("Failed to load coremod " + manifest.modGroup + ":" + manifest.modId
						+ " as it was not accepted by the modloader!");
				StartupWindow.WindowAppender.fatalError();
			} else {
				fatal("Failed to load mod " + manifest.modGroup + ":" + manifest.modId + ", unknown error!");
				StartupWindow.WindowAppender.fatalError();
			}
			System.exit(-1);
		}

		if (coremod) {
			loadCoremod((ICoremod) mod, manifest, classesMap.get(manifest.modGroup + ":" + manifest.modId));
		} else {
			loadMod(mod, manifest);
		}
	}

	private void checkDependencyVersion(String check, Version version, String message) {
		String error = CheckString.validateCheckString(check, version, message, false);
		if (error != null) {
			fatal(error);
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}
	}

	/**
	 * Loads mod classes (must be done during CORELOAD)
	 * 
	 * @param mod         Mod to load
	 * @param modManifest Mod manifest
	 */
	public void loadMod(IMod mod, CyanModfileManifest modManifest) {
		if (CyanCore.getCurrentPhase().equals(LoadPhase.CORELOAD)) {
			mods.add(mod);
			dispatchEvent("mod.loaded", mod);

			for (URL url : CyanCore.getAddedUrls()) {
				String base = url.toString();
				if (!base.startsWith("jar:") && (base.endsWith(".jar") || base.endsWith(".zip")))
					base = "jar:" + base + "!";
				try {
					URL newURL = new URL(base + "/" + mod.getClass().getTypeName().replace(".", "/") + ".class");
					newURL.openStream().close();
					modManifest.source = base;
				} catch (Exception e) {
				}
			}
			if (modManifest.source == null && mod.getClass().getProtectionDomain() != null
					&& mod.getClass().getProtectionDomain().getCodeSource() != null
					&& mod.getClass().getProtectionDomain().getCodeSource().getLocation() != null) {
				String base = mod.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
				if (base.toString().startsWith("jar:"))
					base = base.substring(0, base.lastIndexOf("!"));
				else if (base.endsWith("/" + mod.getClass().getTypeName().replace(".", "/") + ".class")) {
					base = base.substring(0,
							base.length() - ("/" + mod.getClass().getTypeName().replace(".", "/") + ".class").length());
				}
				if (!base.startsWith("jar:") && (base.endsWith(".jar") || base.endsWith(".zip")))
					base = "jar:" + base + "!";
				modManifest.source = base;
			} else if (modManifest.source == null)
				throw new RuntimeException(
						"Could not determine mod source, mod: " + modManifest.modGroup + ":" + modManifest.modId);

			info("Loading mod " + modManifest.modGroup + ":" + modManifest.modId + "... (" + modManifest.displayName
					+ ")");

			modManifest.incompatibilities.forEach((id, ver) -> {
				if (coreModManifests.containsKey(id)) {
					if (CheckString.validateCheckString(ver, Version.fromString(coreModManifests.get(id).version))) {
						if (!ver.equals("*"))
							ver = "";

						throw new RuntimeException("Mod " + mod.getManifest().id() + " is not compatible with " + id
								+ (ver.isEmpty() ? "" : " (" + ver + ")"));
					}
				}
				if (modManifests.containsKey(id)) {
					if (CheckString.validateCheckString(ver, Version.fromString(modManifests.get(id).version))) {
						if (!ver.equals("*"))
							ver = "";

						throw new RuntimeException("Mod " + mod.getManifest().id() + " is not compatible with " + id
								+ (ver.isEmpty() ? "" : " (" + ver + ")"));
					}
				}
			});
			mod.setup(getModloader(), getGameSide(), modManifest);

			modManifest.loaded = true;
			modManifests.put(mod.getManifest().id(), modManifest);
			modManifests.put(mod.getClass().getTypeName(), modManifest);

			if (mod instanceof AbstractMod) {
				modManifest.dependencies.forEach((id, ver) -> {
					Optional<? extends IMod> depmod = coremods.stream().filter(t -> t.getManifest().id().equals(id))
							.findFirst();
					if (!depmod.isPresent())
						depmod = mods.stream().filter(t -> t.getManifest().id().equals(id)).findFirst();

					if (depmod.isPresent() && depmod.get() instanceof AbstractMod) {
						((AbstractMod) mod).enableModSupport((AbstractMod) depmod.get());
					}
				});
				modManifest.optionalDependencies.forEach((id, ver) -> {
					Optional<? extends IMod> depmod = coremods.stream().filter(t -> t.getManifest().id().equals(id))
							.findFirst();
					if (!depmod.isPresent())
						depmod = mods.stream().filter(t -> t.getManifest().id().equals(id)).findFirst();

					if (depmod.isPresent() && depmod.get() instanceof AbstractMod) {
						((AbstractMod) mod).enableModSupport((AbstractMod) depmod.get());
					}
				});
			}
		} else
			throw new IllegalStateException("Already past CORELOAD");
	}

	/**
	 * Loads coremod classes (must be done during CORELOAD)
	 * 
	 * @param mod         Coremod to load
	 * @param modManifest Mod manifest
	 * @param classes     Class map
	 */
	public void loadCoremod(ICoremod mod, CyanModfileManifest modManifest, HashMap<String, byte[]> classes) {
		if (CyanCore.getCurrentPhase().equals(LoadPhase.NOT_READY)
				|| CyanCore.getCurrentPhase().equals(LoadPhase.CORELOAD)) {
			for (String clsName : classes.keySet()) {
				try {
					ByteArrayInputStream strm = new ByteArrayInputStream(classes.get(clsName));
					this.checkTrust("mod class", strm, clsName);
					strm.close();
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			}

			for (URL url : CyanCore.getAddedUrls()) {
				String base = url.toString();
				if (!base.startsWith("jar:") && (base.endsWith(".jar") || base.endsWith(".zip")))
					base = "jar:" + base + "!";
				try {
					URL newURL = new URL(base + "/" + mod.getClass().getTypeName().replace(".", "/") + ".class");
					newURL.openStream().close();
					modManifest.source = base;
				} catch (Exception e) {
				}
			}
			if (modManifest.source == null && mod.getClass().getProtectionDomain() != null
					&& mod.getClass().getProtectionDomain().getCodeSource() != null
					&& mod.getClass().getProtectionDomain().getCodeSource().getLocation() != null) {
				String base = mod.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
				if (base.toString().startsWith("jar:"))
					base = base.substring(0, base.lastIndexOf("!"));
				else if (base.endsWith("/" + mod.getClass().getTypeName().replace(".", "/") + ".class")) {
					base = base.substring(0,
							base.length() - ("/" + mod.getClass().getTypeName().replace(".", "/") + ".class").length());
				}
				if (!base.startsWith("jar:") && (base.endsWith(".jar") || base.endsWith(".zip")))
					base = "jar:" + base + "!";
				modManifest.source = base;
			} else if (modManifest.source == null)
				throw new RuntimeException(
						"Could not determine mod source, mod: " + modManifest.modGroup + ":" + modManifest.modId);

			coremods.add(mod);
			dispatchEvent("mod.loaded", mod);

			info("Loading coremod " + modManifest.modGroup + ":" + modManifest.modId + "... (" + modManifest.displayName
					+ ")");

			modManifest.incompatibilities.forEach((id, ver) -> {
				if (coreModManifests.containsKey(id)) {
					if (CheckString.validateCheckString(ver, Version.fromString(coreModManifests.get(id).version))) {
						if (!ver.equals("*"))
							ver = "";

						throw new RuntimeException("Mod " + mod.getManifest().id() + " is not compatible with " + id
								+ (ver.isEmpty() ? "" : " (" + ver + ")"));
					}
				}
				if (modManifests.containsKey(id)) {
					if (CheckString.validateCheckString(ver, Version.fromString(modManifests.get(id).version))) {
						if (!ver.equals("*"))
							ver = "";

						throw new RuntimeException("Mod " + mod.getManifest().id() + " is not compatible with " + id
								+ (ver.isEmpty() ? "" : " (" + ver + ")"));
					}
				}
			});

			mod.setup(getModloader(), getGameSide(), modManifest);
			modManifest.loaded = true;
			coreModManifests.put(mod.getManifest().id(), modManifest);
			coreModManifests.put(mod.getClass().getTypeName(), modManifest);

			if (mod instanceof AbstractMod) {
				modManifest.dependencies.forEach((id, ver) -> {
					Optional<? extends IMod> depmod = coremods.stream().filter(t -> t.getManifest().id().equals(id))
							.findFirst();
					if (!depmod.isPresent())
						depmod = mods.stream().filter(t -> t.getManifest().id().equals(id)).findFirst();

					if (depmod.isPresent() && depmod.get() instanceof AbstractMod) {
						((AbstractMod) mod).enableModSupport((AbstractMod) depmod.get());
					}
				});
				modManifest.optionalDependencies.forEach((id, ver) -> {
					Optional<? extends IMod> depmod = coremods.stream().filter(t -> t.getManifest().id().equals(id))
							.findFirst();
					if (!depmod.isPresent())
						depmod = mods.stream().filter(t -> t.getManifest().id().equals(id)).findFirst();

					if (depmod.isPresent() && depmod.get() instanceof AbstractMod) {
						((AbstractMod) mod).enableModSupport((AbstractMod) depmod.get());
					}
				});
			}
		} else
			throw new IllegalStateException("Already past CORELOAD");
	}

	@SuppressWarnings("unchecked")
	private <T extends IMod> T getMod(String className, boolean coremod, ClassLoader loader) {
		if (coremod) {
			if (this.loadedComponents.get(className) instanceof IMod)
				return (T) this.loadedComponents.get(className);
			else
				return null;
		}

		try {
			Class<?> cls;
			try {
				cls = loader.loadClass(className);
			} catch (ClassNotFoundException e) {
				try {
					cls = CyanCore.getCoreClassLoader().loadClass(className);
				} catch (ClassNotFoundException e2) {
					try {
						cls = CyanCore.getClassLoader().loadClass(className);
					} catch (ClassNotFoundException e3) {
						cls = getClass().getClassLoader().loadClass(className);
					}
				}
			}
			Constructor<IMod> ctor = (Constructor<IMod>) cls.getConstructor();
			IMod mod = ctor.newInstance();
			return (T) mod;
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			error("Could not instanciate mod class: " + className, e);
		}
		return null;
	}

	private void importCoremods(File coremodsDirectory) {
		for (File ccmf : coremodsDirectory.listFiles((t) -> !t.isDirectory() && t.getName().endsWith(".ccmf"))) {
			try {
				importCoremod(ccmf);
			} catch (IOException e) {
				fatal("Importing coremod failed, mod: " + ccmf.getName());
				StartupWindow.WindowAppender.fatalError();
				fatal("Exception was thrown in the mod loading process.", e);
				System.exit(-1);
			}
		}
	}

	private ArrayList<File> addToSystemLater = new ArrayList<File>();

	private void importCoremod(File ccmf) throws IOException {
		String ccfg = null;
		HashMap<String, byte[]> modClasses = new HashMap<String, byte[]>();

		try {
			FileInputStream strmI = new FileInputStream(ccmf);
			ZipInputStream zIn = new ZipInputStream(strmI);
			ZipEntry ent = zIn.getNextEntry();
			while (ent != null) {
				String pth = ent.getName();
				if (pth.startsWith("/"))
					pth = pth.substring(1);
				if (pth.equals("mod.manifest.ccfg")) {
					ccfg = new String(zIn.readAllBytes());
					break;
				}
				ent = zIn.getNextEntry();
			}
			zIn.close();
			strmI.close();
		} catch (IOException e) {
			throw new IOException("Invalid mod file, failed to load manifest.", e);
		}

		if (ccfg == null) {
			throw new IOException("Invalid mod file, missing manifest.");
		}

		CyanModfileManifest manifest = new CyanModfileManifest().readAll(ccfg);
		if (manifest.updateserver != null) {
			scanModFiles();
			checkUpdates(ccmf, manifest, otherModFileManifests, otherModFileArray);

			ccfg = null;
			try {
				FileInputStream strmI = new FileInputStream(ccmf);
				ZipInputStream zIn = new ZipInputStream(strmI);
				ZipEntry ent = zIn.getNextEntry();
				while (ent != null) {
					String pth = ent.getName();
					if (pth.startsWith("/"))
						pth = pth.substring(1);
					if (pth.equals("mod.manifest.ccfg")) {
						ccfg = new String(zIn.readAllBytes());
						break;
					}
					ent = zIn.getNextEntry();
				}
				zIn.close();
				strmI.close();
			} catch (IOException e) {
				throw new IOException("Invalid mod file, failed to load manifest.", e);
			}

			if (ccfg == null) {
				throw new IOException("Invalid mod file, missing manifest.");
			}
			manifest = new CyanModfileManifest().readAll(ccfg);
		}

		for (String k : new ArrayList<String>(manifest.jars.keySet())) {
			if (!k.startsWith("/")) {
				manifest.jars.put("/" + k, manifest.jars.get(k));
				manifest.jars.remove(k);
			}
		}

		info("Importing coremod " + manifest.modGroup + ":" + manifest.modId + "...");

		if (manifest.gameVersionRegex != null && manifest.gameVersionMessage != null
				&& !CyanInfo.getMinecraftVersion().matches(manifest.gameVersionRegex)) {
			fatal("Incompatible game version '" + CyanInfo.getMinecraftVersion() + "', coremod " + manifest.displayName
					+ " wants " + manifest.gameVersionMessage);
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}

		if (manifest.supportedModLoaders.size() != 0 && (!manifest.supportedModLoaders.containsKey("cyanloader")
				|| !CheckString.validateCheckString(manifest.supportedModLoaders.get("cyanloader"), getVersion()))) {
			if (!manifest.supportedModLoaders.containsKey("cyanloader")) {
				fatal("The mod '" + manifest.displayName + "' does not support Cyan.");
			} else {
				fatal("The mod '" + manifest.displayName + "' does not support Cyan version "
						+ CyanInfo.getCyanVersion() + ".");
			}
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}

		if (manifest.incompatibleLoaderVersions.size() != 0) {
			for (String loader : manifest.incompatibleLoaderVersions.keySet()) {
				Modloader ld = Modloader.getModloader(loader);
				if (ld != null && CheckString.validateCheckString(manifest.incompatibleLoaderVersions.get(loader),
						ld.getVersion())) {
					fatal("The mod '" + manifest.displayName + "' does not support " + ld.getSimpleName() + " version "
							+ ld.getVersion() + ".");
					StartupWindow.WindowAppender.fatalError();
					System.exit(-1);
				}
			}
		}

		if (CyanInfo.getPlatform() != LaunchPlatform.DEOBFUSCATED && CyanInfo.getPlatform() != LaunchPlatform.VANILLA
				&& CyanInfo.getPlatform() != LaunchPlatform.UNKNOWN) {
			if (!manifest.platforms.containsKey(CyanInfo.getPlatform().toString())) {
				boolean first = true;
				StringBuilder platforms = new StringBuilder();
				for (String platform : manifest.platforms.keySet()) {
					if (!first)
						platforms.append(", ");
					first = false;
					platforms.append(platform);
				}
				fatal("Incompatible platform '" + CyanInfo.getPlatform() + "', coremod " + manifest.displayName
						+ " only supports the following platforms: " + platforms);
				StartupWindow.WindowAppender.fatalError();
				System.exit(-1);
			}

			String platformVersion = manifest.platforms.get(CyanInfo.getPlatform().toString());
			String cVersion = CyanLoader.platformVersion;

			if (cVersion == null)
				cVersion = CyanInfo.getModloaderVersion();

			this.checkDependencyVersion(platformVersion, Version.fromString(cVersion), "Incompatible platform '"
					+ CyanInfo.getPlatform() + "' for coremod '" + manifest.displayName + "'");
		}

		ZipInputStream strm = new ZipInputStream(new FileInputStream(ccmf));
		boolean cacheOutOfDate = false;
		ModInfoCache info = new ModInfoCache();
		File cache = new File(cyanDir, "caches/coremods/" + manifest.modGroup + "/" + manifest.modId);
		File modCache = new File(cyanDir, "caches/coremods/" + manifest.modGroup + "/" + manifest.modId + "/mod.cache");

		if (!modCache.exists()) {
			cache.mkdirs();
			cacheOutOfDate = true;
		} else {
			info.readAll(Files.readString(modCache.toPath()));
			if (!info.modVersion.equals(manifest.version)) {
				cacheOutOfDate = true;
			} else if (!CyanInfo.getMinecraftVersion().equals(info.gameVersion)) {
				cacheOutOfDate = true;
			} else if (!CyanInfo.getPlatform().toString().equals(info.platform)) {
				cacheOutOfDate = true;
			} else if (!(CyanInfo.getModloaderName() + "-" + CyanInfo.getModloaderVersion().toString())
					.equals(info.platformVersion)) {
				cacheOutOfDate = true;
			} else if (manifest.trustContainers.size() != info.trustContainers.size()) {
				cacheOutOfDate = true;
			} else {
				for (String key : manifest.trustContainers.keySet()) {
					if (!info.trustContainers.containsKey(key)
							|| !info.trustContainers.get(key).equals(manifest.trustContainers.get(key))) {
						cacheOutOfDate = true;
						break;
					}
				}
				if (!cacheOutOfDate) {
					for (String key : info.trustContainers.keySet()) {
						if (!manifest.trustContainers.containsKey(key)
								|| !info.trustContainers.get(key).equals(manifest.trustContainers.get(key))) {
							cacheOutOfDate = true;
							break;
						}
					}
				}
			}
		}

		if (cacheOutOfDate) {
			info("(Re)building coremod cache...");
			CyanLoader.deleteDir(cache);
			cache.mkdirs();
			info.gameVersion = CyanInfo.getMinecraftVersion();
			info.platform = CyanInfo.getPlatform().toString();
			info.platformVersion = CyanInfo.getModloaderName() + "-" + CyanInfo.getModloaderVersion().toString();
			info.modVersion = manifest.version;
			info.trustContainers.clear();
			info.trustContainers.putAll(manifest.trustContainers);

			info("Game version: " + CyanInfo.getMinecraftVersion());
			info("Mod version: " + manifest.version);
			info("Platform: " + CyanInfo.getPlatform().toString());
			if (!CyanInfo.getModloaderName().isEmpty())
				info("Platform version: " + CyanInfo.getModloaderName() + "-"
						+ CyanInfo.getModloaderVersion().toString());

			Files.writeString(modCache.toPath(), info.toString());
		}

		manifest.mavenDependencies.forEach((group, item) -> {
			item.forEach((name, version) -> {
				if (!coremodMavenDependencies.containsKey(group + ":" + name) || Version.fromString(version)
						.isGreaterThan(coremodMavenDependencies.get(group + ":" + name))) {
					coremodMavenDependencies.put(group + ":" + name, Version.fromString(version));
				}
			});
		});

		info("Loading mod jars...");
		ZipEntry ent = strm.getNextEntry();
		while (ent != null) {
			String path = ent.getName().replace("\\", "/");
			if (!path.startsWith("/"))
				path = "/" + path;

			if (!path.endsWith("/")) {
				if (manifest.jars.containsKey(path)) {
					File output = new File(cache, path);
					if (!output.getParentFile().exists())
						output.getParentFile().mkdirs();

					if (!output.exists()) {
						boolean allow = false;

						for (String type : manifest.jars.get(path).split(" & ")) {
							type = type.trim();

							if (type.startsWith("platform:")) {
								String platform = type.substring("platform:".length());
								if (CyanInfo.getPlatform().toString().equalsIgnoreCase(platform)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase()
											.endsWith("-" + platform.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + platform.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("gameversion:")) {
								String gameversion = type.substring("gameversion:".length());
								if (CyanInfo.getMinecraftVersion().toString().equalsIgnoreCase(gameversion)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase().endsWith(
											"-" + gameversion.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + gameversion.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("side:")) {
								String side = type.substring("side:".length());
								if (CyanInfo.getSide().toString().equalsIgnoreCase(side)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase()
											.endsWith("-" + side.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0, path.toLowerCase().indexOf("-" + side.toLowerCase()))
												+ "." + extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("loaderversion:")) {
								String loaderversion = type.substring("loaderversion:".length());
								if (CyanInfo.getCyanVersion().toString().equalsIgnoreCase(loaderversion)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase().endsWith(
											"-" + loaderversion.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + loaderversion.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("mappingsversion:")) {
								String version = type.substring("mappingsversion:".length());
								boolean found = false;
								for (Mapping<?> mapping : Fluid.getMappings()) {
									if (mapping.mappingsVersion != null && mapping.mappingsVersion.equals(version)) {
										found = true;
										break;
									}
								}
								if (found)
									allow = true;
							} else if (type.equals("any")) {
								allow = true;
							}
						}

						if (allow) {
							info("Installing mod jar: " + path + "...");
							FileOutputStream outputStrm = new FileOutputStream(output);
							strm.transferTo(outputStrm);
							outputStrm.close();
						} else {
							ent = strm.getNextEntry();
							continue;
						}
					}

					ZipFile modJar = new ZipFile(output);
					Enumeration<? extends ZipEntry> entries = modJar.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						String entryPath = entry.getName().replace("\\", "/");
						if (entryPath.startsWith("/")) {
							entryPath = entryPath.substring(1);
						}
						if (entryPath.endsWith(".class")) {
							String cls = entryPath.replace("/", ".").substring(0, entryPath.lastIndexOf(".class"));
							InputStream strmI = modJar.getInputStream(entry);
							modClasses.put(cls, strmI.readAllBytes());
							strmI.close();
						}
					}
					modJar.close();
					CyanCore.addCoreUrl(output.toURI().toURL());
					addToSystemLater.add(output);
				}
			}

			ent = strm.getNextEntry();
		}

		final CyanModfileManifest mf = manifest;
		manifest.trustContainers.forEach((name, location) -> {
			HashMap<String, String> rewrites = new HashMap<String, String>();
			try {
				if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
					URL u = new URL(securityConf.modSecurityService + "/request-server-location/" + mf.modGroup + "/"
							+ mf.modId);
					InputStream in = u.openStream();
					location = new String(in.readAllBytes()).replace("\r", "").split("\n")[0];
					in.close();
					rewrites.put("location", location);
				}
			} catch (IOException ex) {
			}

			String version = "latest";
			if (name.contains("@")) {
				version = name.substring(name.lastIndexOf("@") + 1);
				name = name.substring(0, name.lastIndexOf("@"));
			} else {
				try {
					URL latestInfo = new URL(location + "/" + name.replace(".", "/") + ".latest");
					InputStream in = latestInfo.openStream();
					version = new String(in.readAllBytes()).replace("\r", "").split("\n")[0];
					in.close();
					rewrites.put("version", version);
				} catch (IOException ex) {

				}
			}

			try {
				if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
					URL u = new URL(securityConf.modSecurityService + "/request-trust-container-name/" + mf.modGroup
							+ "/" + mf.modId + "/" + name);
					InputStream in = u.openStream();
					name = new String(in.readAllBytes()).replace("\r", "").split("\n")[0];
					in.close();
					rewrites.put("name", name);
				}
			} catch (IOException ex) {
			}

			try {
				if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
					URL u = new URL(securityConf.modSecurityService + "/request-trust-container-version/" + mf.modGroup
							+ "/" + mf.modId + "/" + name + "/" + version);
					InputStream in = u.openStream();
					version = new String(in.readAllBytes()).replace("\r", "").split("\n")[0];
					in.close();
					rewrites.put("version", version);
				}
			} catch (IOException ex) {
			}

			File rewriteFile = new File(cache, "rewrites.data");
			if (rewriteFile.exists()) {
				try {
					for (String line : Files.readAllLines(rewriteFile.toPath())) {
						if (!line.isEmpty()) {
							String key = line.substring(0, line.indexOf("="));
							String value = line.substring(line.indexOf("=") + 1);
							if (!rewrites.containsKey(key))
								rewrites.put(key, value);
						}
					}
				} catch (IOException e) {
				}
			}

			if (rewrites.containsKey("location"))
				location = rewrites.get("location");
			if (rewrites.containsKey("name"))
				name = rewrites.get("name");
			if (rewrites.containsKey("version"))
				version = rewrites.get("version");

			StringBuilder rewriteFileCont = new StringBuilder();
			rewrites.forEach((k, v) -> {
				rewriteFileCont.append(k).append("=").append(v).append("\n");
			});
			try {
				Files.writeString(rewriteFile.toPath(), rewriteFileCont.toString());
			} catch (IOException e1) {
			}

			boolean update = false;
			File trust = new File(new File(cyanDir, "trust"), name.replace(".", "/") + ".ctc");
			if (!trust.getParentFile().exists())
				trust.getParentFile().mkdirs();

			if (trust.exists()) {
				try {
					TrustContainer ctc = TrustContainer.importContainer(trust);
					if (!ctc.getVersion().equals(version) && !version.equals("latest")
							&& version.matches("^[0-9]+-[0-9]+$")) {
						update = true;
						trust.delete();
					} else {
						try {
							URL remote = new URL(
									location + "/" + name.replace(".", "/") + "-" + version + ".ctc.sha256");

							InputStream inp = remote.openStream();
							String sha = new String(inp.readAllBytes()).replaceAll("\t", " ").replaceAll("\r", "");
							inp.close();
							if (sha.contains("\n")) {
								sha = sha.substring(0, sha.indexOf("\n"));
							}
							if (sha.contains(" ")) {
								sha = sha.substring(0, sha.indexOf(" "));
							}

							String localhash = sha256HEX(Files.readAllBytes(trust.toPath()));
							if (!localhash.equals(sha)) {
								fatal("Trust container " + name + " for coremod '" + mf.displayName
										+ "' has been tampered with!");
								fatal("Will not start to protect the end user!");
								StartupWindow.WindowAppender
										.fatalError("Trust container has been tampered with!\nPlease check the log!");
								System.exit(-1);
							}
						} catch (IOException ex) {
						}
					}
				} catch (IOException e) {
					update = true;
					trust.delete();
				} catch (IllegalArgumentException e) {
				}
			} else {
				update = true;
			}

			if (update) {
				info("Downloading coremod trust container " + name + " for coremod '" + mf.displayName + "'...");
				try {
					URL remote = new URL(location + "/" + name.replace(".", "/") + "-" + version + ".ctc");
					InputStream in = remote.openStream();
					FileOutputStream out = new FileOutputStream(trust);
					in.transferTo(out);
					out.close();
					in.close();
				} catch (IOException e) {
					fatal("Unable to download trust container " + name + " for coremod '" + mf.displayName + "'");
					StartupWindow.WindowAppender.fatalError();
					System.exit(-1);
				}
			}
		});

		if (coreModManifests.containsKey(manifest.modGroup + ":" + manifest.modId)) {
			fatal("Coremod conflict!");
			fatal("Coremod path '" + manifest.modGroup + ":" + manifest.modId + "' was imported twice!");
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}

		CyanCore.addAllowedPackage(manifest.modClassPackage);
		coreModManifests.put(manifest.modGroup + ":" + manifest.modId, manifest);
		coreModManifests.put(manifest.modClassPackage + "." + manifest.modClassName, manifest);

		strm.close();
		classesMap.put(manifest.modGroup + ":" + manifest.modId, modClasses);
	}

	private void importMods(File modsDirectory) {
		for (File cmf : modsDirectory.listFiles((t) -> !t.isDirectory() && t.getName().endsWith(".cmf"))) {
			try {
				importMod(cmf);
			} catch (IOException e) {
				fatal("Importing mod failed, mod: " + cmf.getName());
				StartupWindow.WindowAppender.fatalError();
				fatal("Exception was thrown in the mod loading process.", e);
				System.exit(-1);
			}
		}
	}

	@Override
	protected ClassLoader getComponentClassLoader() {
		return CyanCore.getCoreClassLoader();
	}

	private File[] otherModFileArray;
	private HashMap<String, CyanModfileManifest> otherModFileManifests;

	private void scanModFiles() {
		if (otherModFileManifests == null) {
			otherModFileManifests = new HashMap<String, CyanModfileManifest>();
			HashMap<String, File> modFiles = new HashMap<String, File>();

			File mods = new File(cyanDir, "mods");
			File versionMods = new File(mods, CyanInfo.getMinecraftVersion());
			File coremods = new File(cyanDir, "coremods");
			File versionCoremods = new File(coremods, CyanInfo.getMinecraftVersion());

			if (versionMods.exists()) {
				for (File cmf : versionMods.listFiles((t) -> !t.isDirectory() && t.getName().endsWith(".cmf"))) {
					String ccfg2 = null;
					try {
						FileInputStream strmI = new FileInputStream(cmf);
						ZipInputStream zIn = new ZipInputStream(strmI);
						ZipEntry ent = zIn.getNextEntry();
						while (ent != null) {
							String pth = ent.getName();
							if (pth.startsWith("/"))
								pth = pth.substring(1);
							if (pth.equals("mod.manifest.ccfg")) {
								ccfg2 = new String(zIn.readAllBytes());
								break;
							}
							ent = zIn.getNextEntry();
						}
						zIn.close();
						strmI.close();
					} catch (IOException e) {
					}

					if (ccfg2 != null) {
						CyanModfileManifest manifest2 = new CyanModfileManifest().readAll(ccfg2);
						if (!otherModFileManifests.containsKey(manifest2.modGroup + ":" + manifest2.modId)) {
							otherModFileManifests.put(manifest2.modGroup + ":" + manifest2.modId, manifest2);
							modFiles.put(manifest2.modGroup + ":" + manifest2.modId, cmf);
						}
					}
				}
			}
			if (mods.exists()) {
				for (File cmf : mods.listFiles((t) -> !t.isDirectory() && t.getName().endsWith(".cmf"))) {
					String ccfg2 = null;
					try {
						FileInputStream strmI = new FileInputStream(cmf);
						ZipInputStream zIn = new ZipInputStream(strmI);
						ZipEntry ent = zIn.getNextEntry();
						while (ent != null) {
							String pth = ent.getName();
							if (pth.startsWith("/"))
								pth = pth.substring(1);
							if (pth.equals("mod.manifest.ccfg")) {
								ccfg2 = new String(zIn.readAllBytes());
								break;
							}
							ent = zIn.getNextEntry();
						}
						zIn.close();
						strmI.close();
					} catch (IOException e) {
					}

					if (ccfg2 != null) {
						CyanModfileManifest manifest2 = new CyanModfileManifest().readAll(ccfg2);
						if (!otherModFileManifests.containsKey(manifest2.modGroup + ":" + manifest2.modId)) {
							otherModFileManifests.put(manifest2.modGroup + ":" + manifest2.modId, manifest2);
							modFiles.put(manifest2.modGroup + ":" + manifest2.modId, cmf);
						}
					}
				}
			}
			if (versionCoremods.exists()) {
				for (File cmf : versionCoremods.listFiles((t) -> !t.isDirectory() && t.getName().endsWith(".ccmf"))) {
					String ccfg2 = null;
					try {
						FileInputStream strmI = new FileInputStream(cmf);
						ZipInputStream zIn = new ZipInputStream(strmI);
						ZipEntry ent = zIn.getNextEntry();
						while (ent != null) {
							String pth = ent.getName();
							if (pth.startsWith("/"))
								pth = pth.substring(1);
							if (pth.equals("mod.manifest.ccfg")) {
								ccfg2 = new String(zIn.readAllBytes());
								break;
							}
							ent = zIn.getNextEntry();
						}
						zIn.close();
						strmI.close();
					} catch (IOException e) {
					}

					if (ccfg2 != null) {
						CyanModfileManifest manifest2 = new CyanModfileManifest().readAll(ccfg2);
						if (!otherModFileManifests.containsKey(manifest2.modGroup + ":" + manifest2.modId)) {
							otherModFileManifests.put(manifest2.modGroup + ":" + manifest2.modId, manifest2);
							modFiles.put(manifest2.modGroup + ":" + manifest2.modId, cmf);
						}
					}
				}
			}
			if (coremods.exists()) {
				for (File cmf : coremods.listFiles((t) -> !t.isDirectory() && t.getName().endsWith(".ccmf"))) {
					String ccfg2 = null;
					try {
						FileInputStream strmI = new FileInputStream(cmf);
						ZipInputStream zIn = new ZipInputStream(strmI);
						ZipEntry ent = zIn.getNextEntry();
						while (ent != null) {
							String pth = ent.getName();
							if (pth.startsWith("/"))
								pth = pth.substring(1);
							if (pth.equals("mod.manifest.ccfg")) {
								ccfg2 = new String(zIn.readAllBytes());
								break;
							}
							ent = zIn.getNextEntry();
						}
						zIn.close();
						strmI.close();
					} catch (IOException e) {
					}

					if (ccfg2 != null) {
						CyanModfileManifest manifest2 = new CyanModfileManifest().readAll(ccfg2);
						if (!otherModFileManifests.containsKey(manifest2.modGroup + ":" + manifest2.modId)) {
							otherModFileManifests.put(manifest2.modGroup + ":" + manifest2.modId, manifest2);
							modFiles.put(manifest2.modGroup + ":" + manifest2.modId, cmf);
						}
					}
				}
			}

			otherModFileArray = new File[modFiles.size()];

			int i = 0;
			for (String key : otherModFileManifests.keySet()) {
				otherModFileArray[i++] = modFiles.get(key);
			}
		}
	}

	private void importMod(File cmf) throws IOException {
		String ccfg = null;
		try {
			FileInputStream strmI = new FileInputStream(cmf);
			ZipInputStream zIn = new ZipInputStream(strmI);
			ZipEntry ent = zIn.getNextEntry();
			while (ent != null) {
				String pth = ent.getName();
				if (pth.startsWith("/"))
					pth = pth.substring(1);
				if (pth.equals("mod.manifest.ccfg")) {
					ccfg = new String(zIn.readAllBytes());
					break;
				}
				ent = zIn.getNextEntry();
			}
			zIn.close();
			strmI.close();
		} catch (IOException e) {
			throw new IOException("Invalid mod file, failed to load manifest.", e);
		}

		if (ccfg == null) {
			throw new IOException("Invalid mod file, missing manifest.");
		}

		CyanModfileManifest manifest = new CyanModfileManifest().readAll(ccfg);

		if (manifest.updateserver != null) {
			scanModFiles();
			checkUpdates(cmf, manifest, otherModFileManifests, otherModFileArray);

			ccfg = null;
			try {
				FileInputStream strmI = new FileInputStream(cmf);
				ZipInputStream zIn = new ZipInputStream(strmI);
				ZipEntry ent = zIn.getNextEntry();
				while (ent != null) {
					String pth = ent.getName();
					if (pth.startsWith("/"))
						pth = pth.substring(1);
					if (pth.equals("mod.manifest.ccfg")) {
						ccfg = new String(zIn.readAllBytes());
						break;
					}
					ent = zIn.getNextEntry();
				}
				zIn.close();
				strmI.close();
			} catch (IOException e) {
				throw new IOException("Invalid mod file, failed to load manifest.", e);
			}

			if (ccfg == null) {
				throw new IOException("Invalid mod file, missing manifest.");
			}
			manifest = new CyanModfileManifest().readAll(ccfg);
		}

		info("Importing mod " + manifest.modGroup + ":" + manifest.modId + "...");
		for (String k : new ArrayList<String>(manifest.jars.keySet())) {
			if (!k.startsWith("/")) {
				manifest.jars.put("/" + k, manifest.jars.get(k));
				manifest.jars.remove(k);
			}
		}

		if (manifest.gameVersionRegex != null && manifest.gameVersionMessage != null
				&& !CyanInfo.getMinecraftVersion().matches(manifest.gameVersionRegex)) {
			fatal("Incompatible game version '" + CyanInfo.getMinecraftVersion() + "', mod " + manifest.displayName
					+ " wants " + manifest.gameVersionMessage);
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}

		if (manifest.supportedModLoaders.size() != 0 && (!manifest.supportedModLoaders.containsKey("cyanloader")
				|| !CheckString.validateCheckString(manifest.supportedModLoaders.get("cyanloader"), getVersion()))) {
			if (!manifest.supportedModLoaders.containsKey("cyanloader")) {
				fatal("The mod '" + manifest.displayName + "' does not support Cyan.");
			} else {
				fatal("The mod '" + manifest.displayName + "' does not support Cyan version "
						+ CyanInfo.getCyanVersion() + ".");
			}
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}

		if (manifest.incompatibleLoaderVersions.size() != 0) {
			for (String loader : manifest.incompatibleLoaderVersions.keySet()) {
				Modloader ld = Modloader.getModloader(loader);
				if (ld != null && CheckString.validateCheckString(manifest.incompatibleLoaderVersions.get(loader),
						ld.getVersion())) {
					fatal("The mod '" + manifest.displayName + "' does not support " + ld.getSimpleName() + " version "
							+ ld.getVersion() + ".");
					StartupWindow.WindowAppender.fatalError();
					System.exit(-1);
				}
			}
		}

		if (CyanInfo.getPlatform() != LaunchPlatform.DEOBFUSCATED && CyanInfo.getPlatform() != LaunchPlatform.VANILLA
				&& CyanInfo.getPlatform() != LaunchPlatform.UNKNOWN) {
			if (!manifest.platforms.containsKey(CyanInfo.getPlatform().toString())) {
				boolean first = true;
				StringBuilder platforms = new StringBuilder();
				for (String platform : manifest.platforms.keySet()) {
					if (!first)
						platforms.append(", ");
					first = false;
					platforms.append(platform);
				}
				fatal("Incompatible platform '" + CyanInfo.getPlatform() + "', mod " + manifest.displayName
						+ " only supports the following platforms: " + platforms);
				StartupWindow.WindowAppender.fatalError();
				System.exit(-1);
			}

			String platformVersion = manifest.platforms.get(CyanInfo.getPlatform().toString());
			String cVersion = CyanLoader.platformVersion;

			if (cVersion == null)
				cVersion = CyanInfo.getModloaderVersion();

			this.checkDependencyVersion(platformVersion, Version.fromString(cVersion),
					"Incompatible platform '" + CyanInfo.getPlatform() + "' for mod '" + manifest.displayName + "'");
		}

		ZipInputStream strm = new ZipInputStream(new FileInputStream(cmf));
		boolean cacheOutOfDate = false;
		ModInfoCache info = new ModInfoCache();
		File cache = new File(cyanDir, "caches/mods/" + manifest.modGroup + "/" + manifest.modId);
		File modCache = new File(cyanDir, "caches/mods/" + manifest.modGroup + "/" + manifest.modId + "/mod.cache");

		if (!modCache.exists()) {
			cache.mkdirs();
			cacheOutOfDate = true;
		} else {
			info.readAll(Files.readString(modCache.toPath()));
			if (!info.modVersion.equals(manifest.version)) {
				cacheOutOfDate = true;
			} else if (!CyanInfo.getMinecraftVersion().equals(info.gameVersion)) {
				cacheOutOfDate = true;
			} else if (!CyanInfo.getPlatform().toString().equals(info.platform)) {
				cacheOutOfDate = true;
			} else if (!(CyanInfo.getModloaderName() + "-" + CyanInfo.getModloaderVersion().toString())
					.equals(info.platformVersion)) {
				cacheOutOfDate = true;
			}
		}

		if (cacheOutOfDate) {
			info("(Re)building mod cache...");
			CyanLoader.deleteDir(cache);
			cache.mkdirs();
			info.gameVersion = CyanInfo.getMinecraftVersion();
			info.platform = CyanInfo.getPlatform().toString();
			info.platformVersion = CyanInfo.getModloaderName() + "-" + CyanInfo.getModloaderVersion().toString();
			info.modVersion = manifest.version;

			info("Game version: " + CyanInfo.getMinecraftVersion());
			info("Mod version: " + manifest.version);
			info("Platform: " + CyanInfo.getPlatform().toString());
			if (!CyanInfo.getModloaderName().isEmpty())
				info("Platform version: " + CyanInfo.getModloaderName() + "-"
						+ CyanInfo.getModloaderVersion().toString());

			Files.writeString(modCache.toPath(), info.toString());
		}

		manifest.mavenDependencies.forEach((group, item) -> {
			item.forEach((name, version) -> {
				if (!modMavenDependencies.containsKey(group + ":" + name)
						|| Version.fromString(version).isGreaterThan(modMavenDependencies.get(group + ":" + name))) {
					modMavenDependencies.put(group + ":" + name, Version.fromString(version));
				}
			});
		});

		info("Loading mod jars...");
		ZipEntry ent = strm.getNextEntry();
		while (ent != null) {
			String path = ent.getName().replace("\\", "/");
			if (!path.startsWith("/"))
				path = "/" + path;

			if (!path.endsWith("/")) {
				if (manifest.jars.containsKey(path)) {
					File output = new File(cache, path);
					if (!output.getParentFile().exists())
						output.getParentFile().mkdirs();

					if (!output.exists()) {
						boolean allow = false;

						for (String type : manifest.jars.get(path).split(" & ")) {
							type = type.trim();

							if (type.startsWith("platform:")) {
								String platform = type.substring("platform:".length());
								if (CyanInfo.getPlatform().toString().equalsIgnoreCase(platform)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase()
											.endsWith("-" + platform.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + platform.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("gameversion:")) {
								String gameversion = type.substring("gameversion:".length());
								if (CyanInfo.getMinecraftVersion().toString().equalsIgnoreCase(gameversion)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase().endsWith(
											"-" + gameversion.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + gameversion.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("side:")) {
								String side = type.substring("side:".length());
								if (CyanInfo.getSide().toString().equalsIgnoreCase(side)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase()
											.endsWith("-" + side.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0, path.toLowerCase().indexOf("-" + side.toLowerCase()))
												+ "." + extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("loaderversion:")) {
								String loaderversion = type.substring("loaderversion:".length());
								if (CyanInfo.getCyanVersion().toString().equalsIgnoreCase(loaderversion)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase().endsWith(
											"-" + loaderversion.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + loaderversion.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("mappingsversion:")) {
								String version = type.substring("mappingsversion:".length());
								boolean found = false;
								for (Mapping<?> mapping : Fluid.getMappings()) {
									if (mapping.mappingsVersion != null && mapping.mappingsVersion.equals(version)) {
										found = true;
										break;
									}
								}
								if (found)
									allow = true;
							} else if (type.equals("any")) {
								allow = true;
							}
						}

						if (allow) {
							info("Installing mod jar: " + path + "...");
							FileOutputStream outputStrm = new FileOutputStream(output);
							strm.transferTo(outputStrm);
							outputStrm.close();
						} else {
							ent = strm.getNextEntry();
							continue;
						}
					}
					CyanCore.addUrl(output.toURI().toURL());
					FluidAgent.addToClassPath(output);
				}
			}

			ent = strm.getNextEntry();
		}

		if (modManifests.containsKey(manifest.modGroup + ":" + manifest.modId)) {
			fatal("Mod conflict!");
			fatal("Mod path '" + manifest.modGroup + ":" + manifest.modId + "' was imported twice!");
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}

		CyanCore.addAllowedPackage(manifest.modClassPackage);
		modManifests.put(manifest.modGroup + ":" + manifest.modId, manifest);
		modManifests.put(manifest.modClassPackage + "." + manifest.modClassName, manifest);

		strm.close();
	}

	private ArrayList<File> updatedMods = new ArrayList<File>();

	private void checkUpdates(File cmf, CyanModfileManifest manifest, HashMap<String, CyanModfileManifest> otherMods,
			File[] otherModFiles) throws IOException {
		if (updatedMods.contains(cmf))
			return;

		updatedMods.add(cmf);
		ModUpdateConfiguration config = new ModUpdateConfiguration(
				new File(new File(cyanDir, "config/"), manifest.modGroup + "/" + manifest.modId)).readAll();
		if (config.updates) {
			info("Checking for mod updates... Mod: " + manifest.displayName);
			String server = manifest.updateserver;
			if (!config.server.equals("@default"))
				server = config.server;

			String path = manifest.modGroup + "/" + manifest.modId + "/mod.channels.ccfg";
			if (!server.endsWith("/"))
				path = "/" + path;

			try {
				URL url = new URL(server + path);
				InputStream strm = url.openStream();
				ModUpdateChannelConfig channels = new ModUpdateChannelConfig().readAll(new String(strm.readAllBytes()));
				strm.close();
				String channel = channels.channels.getOrDefault("@default", "stable");
				if (!config.channel.equals("@default"))
					channel = config.channel;

				if (!channels.channels.containsValue(channel)) {
					warn("Update channel '" + channel + "' not found in mod channel list.");
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
					}
				} else {
					ModUpdateChannel ch = null;
					ModUpdateChannel urlCh = null;

					try {
						path = manifest.modGroup + "/" + manifest.modId + "/channels/" + channel + ".ccfg";
						if (!server.endsWith("/"))
							path = "/" + path;
						url = new URL(server + path);
						strm = url.openStream();
						ch = new ModUpdateChannel().readAll(new String(strm.readAllBytes()));
						strm.close();
					} catch (IOException e) {
						warn("Update channel '" + channel + "' was not found.");
						try {
							Thread.sleep(2000);
						} catch (InterruptedException e2) {
						}
					}

					if (ch != null) {
						if (ch.versions.containsKey(CyanInfo.getMinecraftVersion())) {

							urlCh = ch;

							Version oldVer = Version.fromString(manifest.version);
							String version = ch.versions.get(CyanInfo.getMinecraftVersion());
							Version newVer = Version.fromString(version);

							debug("Local version: " + oldVer);
							debug("Remote version: " + newVer);
							if (newVer.isGreaterThan(oldVer)) {
								info("Trying to update '" + manifest.displayName + "' to " + newVer + "...");

								String templateURL = null;
								if (urlCh.urls.containsKey(CyanInfo.getMinecraftVersion())) {
									templateURL = urlCh.urls.get(CyanInfo.getMinecraftVersion());
								} else if (urlCh.urls.containsKey("@fallback")) {
									templateURL = urlCh.urls.get("@fallback");
								} else {
									try {
										path = manifest.modGroup + "/" + manifest.modId + "/channels/@fallback.ccfg";
										if (!server.endsWith("/"))
											path = "/" + path;
										url = new URL(server + path);
										strm = url.openStream();
										urlCh = new ModUpdateChannel().readAll(new String(strm.readAllBytes()));
										strm.close();

										if (urlCh.urls.containsKey(CyanInfo.getMinecraftVersion())) {
											templateURL = urlCh.urls.get(CyanInfo.getMinecraftVersion());
										} else if (urlCh.urls.containsKey("@fallback")) {
											templateURL = urlCh.urls.get("@fallback");
										} else {
											throw new IOException("No match");
										}
									} catch (IOException e) {
										warn("Could not find any matching URL for mod update.");
										try {
											Thread.sleep(2000);
										} catch (InterruptedException e2) {
										}
									}
								}

								if (templateURL != null) {
									templateURL = templateURL.replace("%v", version);
									templateURL = templateURL.replace("%gv", CyanInfo.getMinecraftVersion());
									templateURL = templateURL.replace("%cv", CyanInfo.getCyanVersion());

									try {
										String ccfg2 = "";
										FileInputStream strmI = new FileInputStream(cmf);
										ZipInputStream zIn = new ZipInputStream(strmI);
										ZipEntry ent = zIn.getNextEntry();
										while (ent != null) {
											String pth = ent.getName();
											if (pth.startsWith("/"))
												pth = pth.substring(1);
											if (pth.equals("mod.manifest.ccfg")) {
												ccfg2 = new String(zIn.readAllBytes());
												break;
											}
											ent = zIn.getNextEntry();
										}
										zIn.close();
										strmI.close();
										CyanModfileManifest man = new CyanModfileManifest().readAll(ccfg2);

										boolean fail = false;
										if (man.gameVersionRegex != null
												&& !CyanInfo.getMinecraftVersion().matches(man.gameVersionRegex)) {
											info("No update available (update is incompatible with game version)");
											try {
												Thread.sleep(500);
											} catch (InterruptedException e2) {
											}
											fail = true;
										}

										if (!fail) {
											if (man.supportedModLoaders.size() != 0
													&& (!man.supportedModLoaders.containsKey("cyanloader")
															|| !CheckString.validateCheckString(
																	manifest.supportedModLoaders.get("cyanloader"),
																	getVersion()))) {
												info("No update available (update is incompatible with the current Cyan version)");
												try {
													Thread.sleep(500);
												} catch (InterruptedException e2) {
												}
												fail = true;
											}
										}

										if (!fail) {
											if (man.incompatibleLoaderVersions.size() != 0) {
												for (String loader : man.incompatibleLoaderVersions.keySet()) {
													Modloader ld = Modloader.getModloader(loader);
													if (ld != null && CheckString.validateCheckString(
															man.incompatibleLoaderVersions.get(loader),
															ld.getVersion())) {
														info("No update available (update is incompatible with current version of "
																+ ld.getName() + ")");
														try {
															Thread.sleep(500);
														} catch (InterruptedException e2) {
														}
														fail = true;
														break;
													}
												}
											}
										}

										if (!fail) {
											for (String id : man.incompatibilities.keySet()) {
												String ver = man.incompatibilities.get(id);
												String currentVer = null;

												if (otherMods.containsKey(id)) {
													currentVer = otherMods.get(id).version;
												}

												if (currentVer != null && CheckString.validateCheckString(ver,
														Version.fromString(currentVer))) {
													info("No update available (update is incompatible with current mods)");
													try {
														Thread.sleep(500);
													} catch (InterruptedException e2) {
													}
													fail = true;
													break;
												}
											}
										}

										if (!fail) {
											for (CyanModfileManifest man2 : otherMods.values()) {
												for (String id : man2.incompatibilities.keySet()) {
													String ver = man2.incompatibilities.get(id);
													String currentVer = man.version;

													if (currentVer != null && CheckString.validateCheckString(ver,
															Version.fromString(currentVer))) {
														info("No update available (other mod is incompatible)");
														try {
															Thread.sleep(500);
														} catch (InterruptedException e2) {
														}
														fail = true;
														break;
													}
												}
											}
										}

										if (!fail) {
											if (CyanInfo.getPlatform() != LaunchPlatform.DEOBFUSCATED
													&& CyanInfo.getPlatform() != LaunchPlatform.VANILLA
													&& CyanInfo.getPlatform() != LaunchPlatform.UNKNOWN) {
												if (!man.platforms.containsKey(CyanInfo.getPlatform().toString())) {
													boolean first = true;
													StringBuilder platforms = new StringBuilder();
													for (String platform : man.platforms.keySet()) {
														if (!first)
															platforms.append(", ");
														first = false;
														platforms.append(platform);
													}

													info("No update available (update is incompatible with current platform)");
													try {
														Thread.sleep(500);
													} catch (InterruptedException e2) {
													}
													fail = true;
												} else {
													String platformVersion = man.platforms
															.get(CyanInfo.getPlatform().toString());
													String cVersion = CyanLoader.platformVersion;

													if (cVersion == null)
														cVersion = CyanInfo.getModloaderVersion();

													if (!CheckString.validateCheckString(platformVersion,
															Version.fromString(cVersion))) {
														info("No update available (update is incompatible with current platform)");
														try {
															Thread.sleep(500);
														} catch (InterruptedException e2) {
														}
														fail = true;
													}
												}
											}
										}

										if (!fail) {
											for (String dep : man.dependencies.keySet()) {
												String ver = man.dependencies.get(dep);

												int i = 0;
												boolean found = false;
												for (String id : otherMods.keySet()) {
													if (id.equals(dep)) {
														found = true;

														CyanModfileManifest man2 = otherMods.get(id);
														checkUpdates(otherModFiles[i], man2, otherMods, otherModFiles);

														ccfg2 = "";
														strmI = new FileInputStream(cmf);
														zIn = new ZipInputStream(strmI);
														ent = zIn.getNextEntry();
														while (ent != null) {
															String pth = ent.getName();
															if (pth.startsWith("/"))
																pth = pth.substring(1);
															if (pth.equals("mod.manifest.ccfg")) {
																ccfg2 = new String(zIn.readAllBytes());
																break;
															}
															ent = zIn.getNextEntry();
														}
														zIn.close();
														strmI.close();
														man2 = new CyanModfileManifest().readAll(ccfg2);

														String cVersion = man2.version;
														for (String id2 : man2.incompatibilities.keySet()) {
															if (id2.equals(man.modGroup + ":" + man.modId)) {
																String ver2 = man2.incompatibilities.get(id);
																String currentVer = man.version;

																if (CheckString.validateCheckString(ver2,
																		Version.fromString(currentVer))) {
																	info("No update available (update is incompatible with current mods)");
																	try {
																		Thread.sleep(500);
																	} catch (InterruptedException e2) {
																	}
																	fail = true;
																	break;
																}
															}
														}
														if (!CheckString.validateCheckString(ver,
																Version.fromString(cVersion))) {
															info("No update available (incompatible dependency version for: "
																	+ dep + ")");
															try {
																Thread.sleep(500);
															} catch (InterruptedException e2) {
															}
															fail = true;
														}
														break;
													}
													i++;
												}
												if (!found) {
													info("No update available (missing dependency: " + dep + ")");
													try {
														Thread.sleep(500);
													} catch (InterruptedException e2) {
													}
													fail = true;
												}
											}
										}

										if (!fail) {
											info("Downloading new modfile...");
											info("Backing up old modfile...");
											File backup = new File(cmf.getAbsolutePath() + ".bak");
											Files.copy(cmf.toPath(), backup.toPath());

											info("Downloading...");
											try {
												url = new URL(templateURL);
												strm = url.openStream();
												FileOutputStream output = new FileOutputStream(cmf);
												strm.transferTo(output);
												strm.close();

												String ccfg = null;
												try {
													strmI = new FileInputStream(cmf);
													zIn = new ZipInputStream(strmI);
													ent = zIn.getNextEntry();
													while (ent != null) {
														String pth = ent.getName();
														if (pth.startsWith("/"))
															pth = pth.substring(1);
														if (pth.equals("mod.manifest.ccfg")) {
															ccfg = new String(zIn.readAllBytes());
															break;
														}
														ent = zIn.getNextEntry();
													}
													zIn.close();
													strmI.close();
													strm.close();
												} catch (IOException e) {
													throw new IOException("Invalid mod file, failed to load manifest.",
															e);
												}

												if (ccfg == null) {
													throw new IOException("Invalid mod file, missing manifest.");
												}
												manifest = new CyanModfileManifest().readAll(ccfg);

												backup.delete();
												info("Updated '" + man.displayName + "' to " + man.version);
											} catch (IOException e) {
												error("Exception thrown! Rolling back...");
												cmf.delete();
												Files.copy(backup.toPath(), cmf.toPath());
												backup.delete();
												try {
													Thread.sleep(2000);
												} catch (InterruptedException e2) {
												}
												error("Update failed", e);
											}
										}
									} catch (IOException e) {
										warn("Failed to download update modfile.", e);
										try {
											Thread.sleep(2000);
										} catch (InterruptedException e2) {
										}
									}
								}
							} else {
								if (newVer.isLessThan(oldVer))
									info("No update available (local version is newer)");
								else
									info("No update available (matching versions)");
							}
						} else {
							info("No update available (game version not found in manifest)");
						}
					}
				}
			} catch (IOException e) {
				warn("Unable to check for updates, could not reach mod update channel file.");
			}
		}
	}

	private static String sha256HEX(byte[] array) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		byte[] sha = digest.digest(array);
		StringBuilder result = new StringBuilder();
		for (byte aByte : sha) {
			result.append(String.format("%02x", aByte));
		}
		return result.toString();
	}

	private static void importTrust(File trustContainers) {
		for (File ctc : trustContainers.listFiles((f) -> f.getName().endsWith(".ctc") && !f.isDirectory())) {
			try {
				trust.add(TrustContainer.importContainer(ctc));
			} catch (IOException e) {
				error("Trust container " + ctc.getName() + " failed to import.");
			} catch (IllegalArgumentException e) {
				fatal("");
				try {
					fatal("Trust container is incompatible with Cyan " + CyanInfo.getCyanVersion() + "."
							+ "\n\nContainer file path:\n" + ctc.getCanonicalPath());
				} catch (IOException e1) {
					fatal("Trust container is incompatible with Cyan " + CyanInfo.getCyanVersion() + "."
							+ "\n\nContainer file path:\n" + ctc.getAbsolutePath());
				}

				fatal("Please update or remove the trust file and its coremod.");
				fatal("If the mod is incompatible, you could attempt to downgrade Cyan.");
				StartupWindow.WindowAppender.fatalError("Trust container '" + ctc.getName()
						+ "' is incompatible with Cyan " + CyanInfo.getCyanVersion());
				System.exit(1);
			}
		}
		for (File dir : trustContainers.listFiles((f) -> f.isDirectory())) {
			importTrust(dir);
		}
	}

	private static void deleteDir(File file) {
		for (File f : file.listFiles(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				return !arg0.isDirectory();
			}

		})) {
			f.delete();
		}
		for (File f : file.listFiles(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				return arg0.isDirectory();
			}

		})) {
			deleteDir(f);
		}
		file.delete();
	}

	/**
	 * Prepare for running Cyan Components in minecraft
	 */
	public static void initializeGame(String side) {
		try {
			if (!loaded)
				prepare(side);
			Modloader.getModloader(CyanLoader.class).startCore();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Mapping<?> getFabricCompatibilityMappings(GameSide side) {
		try {
			if (!loaded)
				prepare(side.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (mappings == null) {
			try {
				MinecraftVersionInfo mcVersion = new MinecraftVersionInfo(CyanInfo.getMinecraftVersion(), null, null,
						null);
				if (!MinecraftMappingsToolkit.areMappingsAvailable(mcVersion, CyanCore.getSide())) {
					info("First time loading, downloading " + side.toString().toLowerCase() + " mappings...");
					MinecraftToolkit.resolveVersions();
					MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(CyanInfo.getMinecraftVersion());
					MinecraftMappingsToolkit.downloadVanillaMappings(version, CyanCore.getSide());
					MinecraftMappingsToolkit.saveMappingsToDisk(version, CyanCore.getSide());
				}
				mappings = MinecraftMappingsToolkit.loadMappings(mcVersion, CyanCore.getSide());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return new FabricCompatibilityMappings(mappings, side);
	}

	public static Mapping<?> getForgeCompatibilityMappings(GameSide side, String mcpVersion) {
		try {
			if (!loaded)
				prepare(side.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (mappings == null) {
			try {
				MinecraftVersionInfo mcVersion = new MinecraftVersionInfo(CyanInfo.getMinecraftVersion(), null, null,
						null);
				if (!MinecraftMappingsToolkit.areMappingsAvailable(mcVersion, CyanCore.getSide())) {
					info("First time loading, downloading " + side.toString().toLowerCase() + " mappings...");
					MinecraftToolkit.resolveVersions();
					MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(CyanInfo.getMinecraftVersion());
					MinecraftMappingsToolkit.downloadVanillaMappings(version, CyanCore.getSide());
					MinecraftMappingsToolkit.saveMappingsToDisk(version, CyanCore.getSide());
				}
				mappings = MinecraftMappingsToolkit.loadMappings(mcVersion, CyanCore.getSide());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return new ForgeCompatibilityMappings(mappings, side, mcpVersion);
	}

	public static Mapping<?> getPaperCompatibilityMappings(String mappingsVersion) {
		try {
			if (!loaded)
				prepare(GameSide.SERVER.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		if (mappings == null) {
			try {
				MinecraftVersionInfo mcVersion = new MinecraftVersionInfo(CyanInfo.getMinecraftVersion(), null, null,
						null);
				if (!MinecraftMappingsToolkit.areMappingsAvailable(mcVersion, CyanCore.getSide())) {
					info("First time loading, downloading server mappings...");
					MinecraftToolkit.resolveVersions();
					MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(CyanInfo.getMinecraftVersion());
					MinecraftMappingsToolkit.downloadVanillaMappings(version, CyanCore.getSide());
					MinecraftMappingsToolkit.saveMappingsToDisk(version, CyanCore.getSide());
				}
				mappings = MinecraftMappingsToolkit.loadMappings(mcVersion, CyanCore.getSide());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return new PaperCompatibilityMappings(mappings, mappingsVersion);
	}

	public static void addCompatibilityMappings(Mapping<?> mappings) {
		compatibilityMappings.add(mappings);
	}

	public static void disableVanillaMappings() {
		vanillaMappings = false;
	}

	@Override
	public IModManifest[] getKnownNormalMods() {
		return modManifests.values().stream().map(t -> new IModManifest() {

			@Override
			public String id() {
				return t.modGroup + ":" + t.modId;
			}

			@Override
			public String displayName() {
				return t.displayName;
			}

			@Override
			public Version version() {
				return Version.fromString(t.version);
			}

			@Override
			public String[] dependencies() {
				return t.dependencies.keySet().toArray(t -> new String[t]);
			}

			@Override
			public String[] optionalDependencies() {
				return t.optionalDependencies.keySet().toArray(t -> new String[t]);
			}

			@Override
			public String description() {
				return t.fallbackDescription;
			}

		}).toArray(t -> new IModManifest[t]);
	}

	@Override
	public IModManifest[] getKnownCoreMods() {
		return coreModManifests.values().stream().map(t -> new IModManifest() {

			@Override
			public String id() {
				return t.modGroup + ":" + t.modId;
			}

			@Override
			public String displayName() {
				return t.displayName;
			}

			@Override
			public Version version() {
				return Version.fromString(t.version);
			}

			@Override
			public String[] dependencies() {
				return t.dependencies.keySet().toArray(t -> new String[t]);
			}

			@Override
			public String[] optionalDependencies() {
				return t.optionalDependencies.keySet().toArray(t -> new String[t]);
			}

			@Override
			public String description() {
				return t.fallbackDescription;
			}

		}).toArray(t -> new IModManifest[t]);
	}

	@Override
	public IModManifest[] getLoadedNormalMods() {
		return mods.stream().map(t -> t.getManifest()).toArray(t -> new IModManifest[t]);
	}

	@Override
	public IModManifest[] getLoadedCoreMods() {
		return coremods.stream().map(t -> t.getManifest()).toArray(t -> new IModManifest[t]);
	}

	@Override
	public int getAllKnownModsLength() {
		return (int) modManifests.keySet().stream().filter(t -> t.contains(":")).count()
				+ (int) coreModManifests.keySet().stream().filter(t -> t.contains(":")).count();
	}

	/**
	 * Gets the actual System output stream, assigned before the game starts.
	 */
	public static PrintStream getSystemOutputStream() {
		return defaultOutputStream;
	}

	@Override
	protected String getImplementationName() {
		return "Cyan";
	}

	@Override
	public String getName() {
		return "CyanLoader";
	}

	@Override
	public String getSimpleName() {
		return "Cyan";
	}

	private ArrayList<String> events = new ArrayList<String>();
	private ArrayList<IExtendedEvent<?>> extEvents = new ArrayList<IExtendedEvent<?>>();
	private CyanEventBridge bridger = null;

	private static ArrayList<String> classHookPackages = new ArrayList<String>();
	private static ArrayList<ClassLoadHook> classHooks = new ArrayList<ClassLoadHook>();

	private static HashMap<String[], URL> transformerPackages = new HashMap<String[], URL>();
	private static HashMap<String[], URL> transformers = new HashMap<String[], URL>();

	@Override
	protected boolean presentComponent(Class<IModloaderComponent> component) {
		if (CyanEventBridge.class.isAssignableFrom(component)) {
			return true;
		} else if (IEventProvider.class.isAssignableFrom(component) || IExtendedEvent.class.isAssignableFrom(component)
				|| IPostponedComponent.class.isAssignableFrom(component)) {
			return true;
		} else if (CyanErrorHandlers.class.isAssignableFrom(component)) {
			return true;
		} else if (BaseEventController.class.getTypeName().equals(component.getTypeName())) {
			return true;
		} else if (IAcceptableComponent.class.isAssignableFrom(component)) {
			try {
				return checkTrust("component", component);
			} catch (Exception ex) {
				fatal("Failed to authenticate component: " + component.getTypeName());
				fatal("Will not continue as it is way too risky.");
				StartupWindow.WindowAppender.fatalError("Failed to authenticate component: " + component.getTypeName());
				System.exit(-1);
				return false;
			}
		}
		return false;
	}

	private boolean checkTrust(String type, Class<?> component) throws IOException {
		if (Stream.of(allowedComponentPackages).anyMatch(
				t -> t.equals(component.getPackageName()) || component.getPackageName().startsWith(t + "."))) {
			return true;
		}

		boolean found = false;
		for (TrustContainer container : trust) {
			int result = container.validateClass(component);
			if (result == 1) {
				fatal("");
				fatal("");
				fatal("----------------------- COREMOD MIGHT HAVE BEEN TAMPERED WITH! -----------------------");
				fatal("A " + type + " did not pass security checks, Cyan will shut down to protect the end-user.");
				fatal("Make sure you download content from official sources and not third-parties. If you are");
				fatal("certain the content is authentic, you might need to clear the trust container storage.");
				fatal("");
				fatal(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase() + ": "
						+ component.getTypeName());
				fatal("");
				fatal("");
				StartupWindow.WindowAppender.fatalError("A coremod might have been tampered with!\nPlease check log!");
				System.exit(-1);
			} else if (result == 0) {
				found = true;
				break;
			}
		}

		if (!found) {
			fatal("");
			fatal("Starting failed as a " + type + " was not present in any trust container.");
			fatal("");
			fatal("Make sure you have the component trust container installed.");
			fatal("Most components should automatically download from a trust server, if the server is");
			fatal("down, you will need to manually copy the component trust container to .cyan-data/trust.");
			fatal("");
			fatal(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase() + ": "
					+ component.getTypeName());
			info("");
			info("");
			info("");
			info("Note for coremod developers: as it is impossible to authenticate mods from development");
			info("environments, you need to instruct the security system to allow your coremod classes.");
			info("");
			info("You will need to be running development cyan wrappers. If you are, you can use the");
			info("-DauthorizeDebugPackages=<package> argument to whitelist your component.");
			info("(use -DauthorizeDebugPackages=<package1>:<package2> for multiple)");
			StartupWindow.WindowAppender.fatalError("Unauthorized component.\nPlease check log.");
			System.exit(-1);
		}

		return true;
	}

	private boolean checkTrust(String type, InputStream component, String name) throws IOException {
		String pkgN = "";
		if (name.contains(".")) {
			pkgN = name.substring(0, name.lastIndexOf("."));
		}
		final String pkgF = pkgN;
		if (Stream.of(allowedComponentPackages).anyMatch(t -> t.equals(pkgF) || pkgF.startsWith(t + "."))) {
			return true;
		}

		boolean found = false;
		for (TrustContainer container : trust) {
			int result = container.validateClass(component, name);
			if (result == 1) {
				fatal("");
				fatal("");
				fatal("----------------------- COREMOD MIGHT HAVE BEEN TAMPERED WITH! -----------------------");
				fatal("A " + type + " did not pass security checks, Cyan will shut down to protect the end-user.");
				fatal("Make sure you download content from official sources and not third-parties. If you are");
				fatal("certain the content is authentic, you might need to clear the trust container storage.");
				fatal("");
				fatal(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase() + ": " + name);
				fatal("");
				fatal("");
				StartupWindow.WindowAppender.fatalError("A coremod might have been tampered with!\nPlease check log!");
				System.exit(-1);
			} else if (result == 0) {
				found = true;
				break;
			}
		}

		if (!found) {
			fatal("");
			fatal("Starting failed as a " + type + " was not present in any trust container.");
			fatal("");
			fatal("Make sure you have the component trust container installed.");
			fatal("Most components should automatically download from a trust server, if the server is");
			fatal("down, you will need to manually copy the component trust container to .cyan-data/trust.");
			fatal("");
			fatal(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase() + ": " + name);
			info("");
			info("");
			info("");
			info("Note for coremod developers: as it is impossible to authenticate mods from development");
			info("environments, you need to instruct the security system to allow your coremod classes.");
			info("");
			info("You will need to be running development cyan wrappers. If you are, you can use the");
			info("-DauthorizeDebugPackages=<package> argument to whitelist your component.");
			info("(use -DauthorizeDebugPackages=<package1>:<package2> for multiple)");
			StartupWindow.WindowAppender.fatalError("Unauthorized component.\nPlease check log.");
			System.exit(-1);
		}

		return true;
	}

	@Override
	protected boolean execComponent(IModloaderComponent component) {
		if (component instanceof CyanEventBridge) {
			bridger = (CyanEventBridge) component;
			return true;
		} else if (component instanceof IEventProvider) {
			if (events.contains(((IEventProvider) component).getChannelName()))
				return false;

			events.add(((IEventProvider) component).getChannelName());

			return true;
		} else if (component instanceof IExtendedEvent) {
			if (events.contains(((IExtendedEvent<?>) component).channelName()))
				return false;

			IExtendedEvent<?> event = (IExtendedEvent<?>) component;
			extEvents.add(event);
			events.add(event.channelName());

			return true;
		} else if (component instanceof IPostponedComponent) {
			((IPostponedComponent) component).initComponent();
			return true;
		}
		if (component instanceof CyanErrorHandlers) {
			((CyanErrorHandlers) component).attach();
			return true;
		} else if (component instanceof BaseEventController) {
			BaseEventController controller = (BaseEventController) component;
			controller.assign();
			controller.attachListenerRegistry((channel, listener) -> {
				try {
					attachEventListener(channel, listener);
				} catch (IllegalStateException e) {
					error("Failed to attach event listener " + listener.getListenerName() + " to event " + channel
							+ ", event not recognized.");
				}
			}, eventType -> {
				for (IExtendedEvent<?> ev : extEvents) {
					if (eventType.isAssignableFrom(ev.getClass())
							|| eventType.getTypeName().equals(ev.getClass().getTypeName()))
						return ev.channelName();
				}

				return null;
			}, eventType -> {
				for (IExtendedEvent<?> ev : extEvents) {
					if (eventType.isAssignableFrom(ev.getClass())
							|| eventType.getTypeName().equals(ev.getClass().getTypeName()))
						return ev.requiresSynchronizedListeners();
				}

				return false;
			});
			return true;
		} else if (component instanceof IAcceptableComponent) {
			IAcceptableComponent cp = (IAcceptableComponent) component;

			URL location = cp.getClass().getProtectionDomain().getCodeSource().getLocation();
			String pref = location.toString();
			if (pref.startsWith("jar:")) {
				pref = pref.substring("jar:".length(), pref.lastIndexOf("!/"));
			} else if (pref.contains(".class")) {
				pref = pref.substring(0, pref.lastIndexOf(cp.getClass().getTypeName().replace(".", "/") + ".class"));
			}

			for (String prov : cp.providers()) {
				if (!Stream.of(acceptableProviders).anyMatch(t -> t.equals(prov))) {
					return false;
				}
			}

			for (String rq : cp.infoRequests()) {
				if (rq.equals("mod.manifest")) {
					cp.provideInfo(coreModManifests.get(cp.getClass().getTypeName()), rq);
				}
			}

			String modid = cp.getClass().getSimpleName();
			for (String prov : cp.providers()) {
				if (prov.equals("auto.init")) {
					cp.provide("auto.init");
				} else if (prov.equals("mod.id")) {
					modid = cp.provide(prov).toString();
				}
			}

			for (String prov : cp.providers()) {
				if (prov.equals("transformers")) {
					String[] transformers = (String[]) cp.provide(prov);
					for (String transformer : transformers) {
						try {
							CyanLoader.transformers.put(new String[] { transformer, modid }, new URL(pref));
						} catch (IllegalStateException | MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				} else if (prov.equals("transformer-packages")) {
					String[] transformerPackages = (String[]) cp.provide(prov);
					for (String transformer : transformerPackages) {
						try {
							CyanCore.addAllowedPackage(transformer);
							CyanLoader.transformerPackages.put(new String[] { transformer, modid }, new URL(pref));
						} catch (IllegalStateException | MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				} else if (prov.equals("class-hook-packages")) {
					String[] classHookPackages = (String[]) cp.provide(prov);
					for (String hook : classHookPackages) {
						try {
							CyanCore.addAllowedPackage(hook);
							CyanLoader.classHookPackages.add(hook);
						} catch (IllegalStateException e) {
							throw new RuntimeException(e);
						}
					}
				} else if (prov.equals("class-hooks")) {
					ClassLoadHook[] classHooks = (ClassLoadHook[]) cp.provide(prov);
					for (ClassLoadHook hook : classHooks) {
						try {
							CyanLoader.classHooks.add(hook);
						} catch (IllegalStateException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}

			loadedComponents.put(cp.getClass().getTypeName(), cp);
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	protected EventBusFactory<?> createEventBusFactory() {
		if (bridger == null) {
			error("createEventBusFactory was called in CyanLoader, but no CyanEventBridge was presented yet!");
			return null;
		}
		return bridger.getNewFactory();
	}

	@Override
	protected boolean acceptsAnonymousComponent() {
		return true;
	}

	@Override
	protected boolean postLoadOnlyComponent(Class<IModloaderComponent> component) {
		if (IExtendedEvent.class.isAssignableFrom(component) || IPostponedComponent.class.isAssignableFrom(component)) {
			return true;
		}
		return false;
	}

	private void startCore() {
		CyanCore.registerPreLoadHook(new Runnable() {

			@Override
			public void run() {

				info("Loading FLUID mappings...");

				if (vanillaMappings)
					Fluid.loadMappings(mappings);

				for (Mapping<?> cmap : compatibilityMappings)
					Fluid.loadMappings(cmap);
			}

		});
		CyanCore.registerPreLoadHook(new Runnable() {

			@Override
			public void run() {
				info("Loading FLUID internal class hooks...");
				Fluid.registerHook(new LegacyModKitSupportHook());
			}

		});
		CyanCore.registerPreLoadHook(new Runnable() {

			@Override
			public void run() {
				info("Loading FLUID class load hooks...");
				for (ClassLoadHook hook : classHooks) {
					Class<?> cls = hook.getClass();
					if (cls.isAnnotationPresent(SideOnly.class)
							&& cls.getAnnotation(SideOnly.class).value() != getModloaderGameSide()) {
						continue;
					} else if (cls.isAnnotationPresent(PlatformOnly.class)
							&& cls.getAnnotation(PlatformOnly.class).value() != getModloaderLaunchPlatform()) {
						continue;
					} else if (cls.isAnnotationPresent(PlatformExclude.class)
							&& cls.getAnnotation(PlatformOnly.class).value() == getModloaderLaunchPlatform()) {
						continue;
					} else if (cls.isAnnotationPresent(VersionRegex.class)
							&& !cls.getAnnotation(VersionRegex.class).modloaderVersion()
							&& !getModloaderGameVersion().matches(cls.getAnnotation(VersionRegex.class).value())) {
						continue;
					} else if (cls.isAnnotationPresent(VersionRegex.class)
							&& cls.getAnnotation(VersionRegex.class).modloaderVersion() && !getModloaderVersion()
									.toString().matches(cls.getAnnotation(VersionRegex.class).value())) {
						continue;
					} else if (cls.isAnnotationPresent(LoaderVersionGreaterThan.class)) {
						LoaderVersionGreaterThan anno = cls.getAnnotation(LoaderVersionGreaterThan.class);
						String[] loaders = anno.name().split("\\|");
						String[] versions = anno.version().split("\\|");
						String[] gameVersions = anno.gameVersionList().split("\\|");

						boolean valid = true;
						if (gameVersions.length > 0) {
							valid = false;
							for (String v : gameVersions) {
								if (CheckString.validateCheckString(v,
										Version.fromString(CyanInfo.getMinecraftVersion()))) {
									valid = true;
									break;
								}
							}
						}
						if (versions.length > 0 && valid) {
							int i = 0;
							Version version = Version.fromString(versions[0]);

							boolean end = false;
							for (String loader : loaders) {
								if (i > versions.length)
									version = Version.fromString(versions[0]);
								else
									version = Version.fromString(versions[i]);

								Modloader ld = Modloader.getModloader(loader);
								if (ld != null && version.isGreaterThan(ld.getVersion())) {
									end = true;
									break;
								}

								i++;
							}

							if (end)
								continue;
						}
					} else if (cls.isAnnotationPresent(LoaderVersionLessThan.class)) {
						LoaderVersionLessThan anno = cls.getAnnotation(LoaderVersionLessThan.class);
						String[] loaders = anno.name().split("\\|");
						String[] versions = anno.version().split("\\|");
						String[] gameVersions = anno.gameVersionList().split("\\|");

						boolean valid = true;
						if (gameVersions.length > 0) {
							valid = false;
							for (String v : gameVersions) {
								if (CheckString.validateCheckString(v,
										Version.fromString(CyanInfo.getMinecraftVersion()))) {
									valid = true;
									break;
								}
							}
						}
						if (versions.length > 0 && valid) {
							int i = 0;
							Version version = Version.fromString(versions[0]);

							boolean end = false;
							for (String loader : loaders) {
								if (i > versions.length)
									version = Version.fromString(versions[0]);
								else
									version = Version.fromString(versions[i]);

								Modloader ld = Modloader.getModloader(loader);
								if (ld != null && version.isLessThan(ld.getVersion())) {
									end = true;
									break;
								}

								i++;
							}

							if (end)
								continue;
						}
					}

					Fluid.registerHook(hook);
				}
				for (Class<?> hook : findClasses(getMainImplementation(), ClassLoadHook.class,
						CyanCore.getCoreClassLoader(), getClass().getClassLoader())) {
					if (hook.isAnnotationPresent(SideOnly.class)
							&& hook.getAnnotation(SideOnly.class).value() != getModloaderGameSide()) {
						continue;
					} else if (hook.isAnnotationPresent(PlatformOnly.class)
							&& hook.getAnnotation(PlatformOnly.class).value() != getModloaderLaunchPlatform()) {
						continue;
					} else if (hook.isAnnotationPresent(PlatformExclude.class)
							&& hook.getAnnotation(PlatformExclude.class).value() == getModloaderLaunchPlatform()) {
						continue;
					} else if (hook.isAnnotationPresent(VersionRegex.class)
							&& !hook.getAnnotation(VersionRegex.class).modloaderVersion()
							&& !getModloaderGameVersion().matches(hook.getAnnotation(VersionRegex.class).value())) {
						continue;
					} else if (hook.isAnnotationPresent(VersionRegex.class)
							&& hook.getAnnotation(VersionRegex.class).modloaderVersion() && !getModloaderVersion()
									.toString().matches(hook.getAnnotation(VersionRegex.class).value())) {
						continue;
					} else if (hook.isAnnotationPresent(LoaderVersionGreaterThan.class)) {
						LoaderVersionGreaterThan anno = hook.getAnnotation(LoaderVersionGreaterThan.class);
						String[] loaders = anno.name().split("\\|");
						String[] versions = anno.version().split("\\|");
						String[] gameVersions = anno.gameVersionList().split("\\|");

						boolean valid = true;
						if (gameVersions.length > 0) {
							valid = false;
							for (String v : gameVersions) {
								if (CheckString.validateCheckString(v,
										Version.fromString(CyanInfo.getMinecraftVersion()))) {
									valid = true;
									break;
								}
							}
						}
						if (versions.length > 0 && valid) {
							int i = 0;
							Version version = Version.fromString(versions[0]);

							boolean end = false;
							for (String loader : loaders) {
								if (i > versions.length)
									version = Version.fromString(versions[0]);
								else
									version = Version.fromString(versions[i]);

								Modloader ld = Modloader.getModloader(loader);
								if (ld != null && version.isGreaterThan(ld.getVersion())) {
									end = true;
									break;
								}

								i++;
							}

							if (end)
								continue;
						}
					} else if (hook.isAnnotationPresent(LoaderVersionLessThan.class)) {
						LoaderVersionLessThan anno = hook.getAnnotation(LoaderVersionLessThan.class);
						String[] loaders = anno.name().split("\\|");
						String[] versions = anno.version().split("\\|");
						String[] gameVersions = anno.gameVersionList().split("\\|");

						boolean valid = true;
						if (gameVersions.length > 0) {
							valid = false;
							for (String v : gameVersions) {
								if (CheckString.validateCheckString(v,
										Version.fromString(CyanInfo.getMinecraftVersion()))) {
									valid = true;
									break;
								}
							}
						}
						if (versions.length > 0 && valid) {
							int i = 0;
							Version version = Version.fromString(versions[0]);

							boolean end = false;
							for (String loader : loaders) {
								if (i > versions.length)
									version = Version.fromString(versions[0]);
								else
									version = Version.fromString(versions[i]);

								Modloader ld = Modloader.getModloader(loader);
								if (ld != null && version.isLessThan(ld.getVersion())) {
									end = true;
									break;
								}

								i++;
							}
							if (end)
								continue;
						}
					}

					classHookPackages.forEach((pkg) -> {
						if (hook.getPackageName().equals(pkg) || hook.getPackageName().startsWith(pkg + ".")) {
							try {
								Fluid.registerHook((ClassLoadHook) hook.getConstructor().newInstance());
							} catch (IllegalStateException | InstantiationException | IllegalAccessException
									| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
									| SecurityException e) {
							}
						}
					});
				}
			}

		});
		CyanCore.registerPreLoadHook(new Runnable() {
			@Override
			public void run() {
				info("Loading FLUID transformers...");
				Version minecraft = Version.fromString(CyanInfo.getMinecraftVersion());
				Version last = Version.fromString("0.0.0");

				String selectedPackage = "";
				Class<?>[] classes = findAnnotatedClasses(getMainImplementation(), FluidTransformer.class);
				for (Class<?> cls : classes) {
					String pkg = cls.getPackageName();
					if (!pkg.startsWith("org.asf.cyan.modifications."))
						continue;

					if (pkg.contains(".common")) {
						pkg = pkg.substring(0, pkg.lastIndexOf(".common"));
					} else if (pkg.contains(".client")) {
						pkg = pkg.substring(0, pkg.lastIndexOf(".client"));
					} else if (pkg.contains(".server")) {
						pkg = pkg.substring(0, pkg.lastIndexOf(".server"));
					}

					String version = pkg.substring(pkg.lastIndexOf(".") + 1).substring(1).replace("_", ".");
					if (Version.fromString(version).isGreaterThan(minecraft)
							|| Version.fromString(version).isLessThan(last))
						continue;

					selectedPackage = pkg;
					last = Version.fromString(version);
				}

				try {
					for (Class<?> transformer : classes) {
						String pkg = transformer.getPackageName();
						if (!pkg.startsWith("org.asf.cyan.modifications.")
								|| (!pkg.equals(selectedPackage) && !pkg.startsWith(selectedPackage + ".")))
							continue;

						if (pkg.contains(".client") && CyanInfo.getSide() != GameSide.CLIENT) {
							continue;
						} else if (pkg.contains(".server") && CyanInfo.getSide() != GameSide.SERVER) {
							continue;
						}

						if (transformer.isAnnotationPresent(SideOnly.class)
								&& transformer.getAnnotation(SideOnly.class).value() != getModloaderGameSide()) {
							continue;
						} else if (transformer.isAnnotationPresent(PlatformOnly.class) && transformer
								.getAnnotation(PlatformOnly.class).value() != getModloaderLaunchPlatform()) {
							continue;
						} else if (transformer.isAnnotationPresent(PlatformExclude.class) && transformer
								.getAnnotation(PlatformExclude.class).value() == getModloaderLaunchPlatform()) {
							continue;
						} else if (transformer.isAnnotationPresent(VersionRegex.class)
								&& !transformer.getAnnotation(VersionRegex.class).modloaderVersion()
								&& !getModloaderGameVersion()
										.matches(transformer.getAnnotation(VersionRegex.class).value())) {
							continue;
						} else if (transformer.isAnnotationPresent(VersionRegex.class)
								&& transformer.getAnnotation(VersionRegex.class).modloaderVersion()
								&& !getModloaderVersion().toString()
										.matches(transformer.getAnnotation(VersionRegex.class).value())) {
							continue;
						} else if (transformer.isAnnotationPresent(LoaderVersionGreaterThan.class)) {
							LoaderVersionGreaterThan anno = transformer.getAnnotation(LoaderVersionGreaterThan.class);
							String[] loaders = anno.name().split("\\|");
							String[] versions = anno.version().split("\\|");
							String[] gameVersions = anno.gameVersionList().split("\\|");

							boolean valid = true;
							if (gameVersions.length > 0) {
								valid = false;
								for (String v : gameVersions) {
									if (CheckString.validateCheckString(v,
											Version.fromString(CyanInfo.getMinecraftVersion()))) {
										valid = true;
										break;
									}
								}
							}
							if (versions.length > 0 && valid) {
								int i = 0;
								Version version = Version.fromString(versions[0]);

								boolean end = false;
								for (String loader : loaders) {
									if (i > versions.length)
										version = Version.fromString(versions[0]);
									else
										version = Version.fromString(versions[i]);

									Modloader ld = Modloader.getModloader(loader);
									if (ld != null && version.isGreaterThan(ld.getVersion())) {
										end = true;
										break;
									}

									i++;
								}
								if (end)
									continue;
							}
						} else if (transformer.isAnnotationPresent(LoaderVersionLessThan.class)) {
							LoaderVersionLessThan anno = transformer.getAnnotation(LoaderVersionLessThan.class);
							String[] loaders = anno.name().split("\\|");
							String[] versions = anno.version().split("\\|");
							String[] gameVersions = anno.gameVersionList().split("\\|");

							boolean valid = true;
							if (gameVersions.length > 0) {
								valid = false;
								for (String v : gameVersions) {
									if (CheckString.validateCheckString(v,
											Version.fromString(CyanInfo.getMinecraftVersion()))) {
										valid = true;
										break;
									}
								}
							}
							if (versions.length > 0 && valid) {
								int i = 0;
								Version version = Version.fromString(versions[0]);

								boolean end = false;
								for (String loader : loaders) {
									if (i > versions.length)
										version = Version.fromString(versions[0]);
									else
										version = Version.fromString(versions[i]);

									Modloader ld = Modloader.getModloader(loader);
									if (ld != null && version.isLessThan(ld.getVersion())) {
										end = true;
										break;
									}

									i++;
								}
								if (end)
									continue;
							}
						}

						Fluid.registerTransformer(transformer.getTypeName(),
								CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
					}
				} catch (IllegalStateException | ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}

		});
		CyanCore.registerPreLoadHook(new Runnable() {

			@Override
			public void run() {
				info("Loading Coremod Transformers...");
				for (String[] transformerInfo : transformers.keySet()) {
					String transformer = transformerInfo[0];
					String owner = transformerInfo[1];
					URL source = transformers.get(transformerInfo);
					try {
						Class<?> cls = Class.forName(transformer, false, CyanCore.getCoreClassLoader());
						if (cls.isAnnotationPresent(SideOnly.class)
								&& cls.getAnnotation(SideOnly.class).value() != getModloaderGameSide()) {
							continue;
						} else if (cls.isAnnotationPresent(PlatformOnly.class)
								&& cls.getAnnotation(PlatformOnly.class).value() != getModloaderLaunchPlatform()) {
							continue;
						} else if (cls.isAnnotationPresent(PlatformExclude.class)
								&& cls.getAnnotation(PlatformExclude.class).value() == getModloaderLaunchPlatform()) {
							continue;
						} else if (cls.isAnnotationPresent(VersionRegex.class)
								&& !cls.getAnnotation(VersionRegex.class).modloaderVersion()
								&& !getModloaderGameVersion().matches(cls.getAnnotation(VersionRegex.class).value())) {
							continue;
						} else if (cls.isAnnotationPresent(VersionRegex.class)
								&& cls.getAnnotation(VersionRegex.class).modloaderVersion() && !getModloaderVersion()
										.toString().matches(cls.getAnnotation(VersionRegex.class).value())) {
							continue;
						} else if (cls.isAnnotationPresent(LoaderVersionGreaterThan.class)) {
							LoaderVersionGreaterThan anno = cls.getAnnotation(LoaderVersionGreaterThan.class);
							String[] loaders = anno.name().split("\\|");
							String[] versions = anno.version().split("\\|");
							String[] gameVersions = anno.gameVersionList().split("\\|");

							boolean valid = true;
							if (gameVersions.length > 0) {
								valid = false;
								for (String v : gameVersions) {
									if (CheckString.validateCheckString(v,
											Version.fromString(CyanInfo.getMinecraftVersion()))) {
										valid = true;
										break;
									}
								}
							}
							if (versions.length > 0 && valid) {
								int i = 0;
								Version version = Version.fromString(versions[0]);

								for (String loader : loaders) {
									if (i > versions.length)
										version = Version.fromString(versions[0]);
									else
										version = Version.fromString(versions[i]);

									Modloader ld = Modloader.getModloader(loader);
									if (ld != null && version.isGreaterThan(ld.getVersion())) {
										continue;
									}

									i++;
								}
							}
						} else if (cls.isAnnotationPresent(LoaderVersionLessThan.class)) {
							LoaderVersionLessThan anno = cls.getAnnotation(LoaderVersionLessThan.class);
							String[] loaders = anno.name().split("\\|");
							String[] versions = anno.version().split("\\|");
							String[] gameVersions = anno.gameVersionList().split("\\|");

							boolean valid = true;
							if (gameVersions.length > 0) {
								valid = false;
								for (String v : gameVersions) {
									if (CheckString.validateCheckString(v,
											Version.fromString(CyanInfo.getMinecraftVersion()))) {
										valid = true;
										break;
									}
								}
							}
							if (versions.length > 0 && valid) {
								int i = 0;
								Version version = Version.fromString(versions[0]);

								for (String loader : loaders) {
									if (i > versions.length)
										version = Version.fromString(versions[0]);
									else
										version = Version.fromString(versions[i]);

									Modloader ld = Modloader.getModloader(loader);
									if (ld != null && version.isLessThan(ld.getVersion())) {
										continue;
									}

									i++;
								}
							}
						}

						Fluid.registerTransformer(transformer, owner, source);
					} catch (IllegalStateException | ClassNotFoundException e) {
					}
				}

				for (Class<?> transformer : findAnnotatedClasses(getMainImplementation(), FluidTransformer.class)) {
					if (transformer.isAnnotationPresent(SideOnly.class)
							&& transformer.getAnnotation(SideOnly.class).value() != getModloaderGameSide()) {
						continue;
					} else if (transformer.isAnnotationPresent(PlatformOnly.class)
							&& transformer.getAnnotation(PlatformOnly.class).value() != getModloaderLaunchPlatform()) {
						continue;
					} else if (transformer.isAnnotationPresent(PlatformExclude.class) && transformer
							.getAnnotation(PlatformExclude.class).value() == getModloaderLaunchPlatform()) {
						continue;
					} else if (transformer.isAnnotationPresent(VersionRegex.class)
							&& !transformer.getAnnotation(VersionRegex.class).modloaderVersion()
							&& !getModloaderGameVersion()
									.matches(transformer.getAnnotation(VersionRegex.class).value())) {
						continue;
					} else if (transformer.isAnnotationPresent(VersionRegex.class)
							&& transformer.getAnnotation(VersionRegex.class).modloaderVersion()
							&& !getModloaderVersion().toString()
									.matches(transformer.getAnnotation(VersionRegex.class).value())) {
						continue;
					} else if (transformer.isAnnotationPresent(LoaderVersionGreaterThan.class)) {
						LoaderVersionGreaterThan anno = transformer.getAnnotation(LoaderVersionGreaterThan.class);
						String[] loaders = anno.name().split("\\|");
						String[] versions = anno.version().split("\\|");
						String[] gameVersions = anno.gameVersionList().split("\\|");

						boolean valid = true;
						if (gameVersions.length > 0) {
							valid = false;
							for (String v : gameVersions) {
								if (CheckString.validateCheckString(v,
										Version.fromString(CyanInfo.getMinecraftVersion()))) {
									valid = true;
									break;
								}
							}
						}
						if (versions.length > 0 && valid) {
							int i = 0;
							Version version = Version.fromString(versions[0]);

							for (String loader : loaders) {
								if (i > versions.length)
									version = Version.fromString(versions[0]);
								else
									version = Version.fromString(versions[i]);

								Modloader ld = Modloader.getModloader(loader);
								if (ld != null && version.isGreaterThan(ld.getVersion())) {
									continue;
								}

								i++;
							}
						}
					} else if (transformer.isAnnotationPresent(LoaderVersionLessThan.class)) {
						LoaderVersionLessThan anno = transformer.getAnnotation(LoaderVersionLessThan.class);
						String[] loaders = anno.name().split("\\|");
						String[] versions = anno.version().split("\\|");
						String[] gameVersions = anno.gameVersionList().split("\\|");

						boolean valid = true;
						if (gameVersions.length > 0) {
							valid = false;
							for (String v : gameVersions) {
								if (CheckString.validateCheckString(v,
										Version.fromString(CyanInfo.getMinecraftVersion()))) {
									valid = true;
									break;
								}
							}
						}
						if (versions.length > 0 && valid) {
							int i = 0;
							Version version = Version.fromString(versions[0]);

							for (String loader : loaders) {
								if (i > versions.length)
									version = Version.fromString(versions[0]);
								else
									version = Version.fromString(versions[i]);

								Modloader ld = Modloader.getModloader(loader);
								if (ld != null && version.isLessThan(ld.getVersion())) {
									continue;
								}

								i++;
							}
						}
					}

					transformerPackages.forEach((pkgInfo, source) -> {
						String pkg = pkgInfo[0];
						String owner = pkgInfo[1];
						if (transformer.getPackageName().equals(pkg)
								|| transformer.getPackageName().startsWith(pkg + ".")) {
							try {
								transformers.put(new String[] { transformer.getTypeName(), owner }, source);
								Fluid.registerTransformer(transformer.getTypeName(), owner, source);
							} catch (IllegalStateException | ClassNotFoundException e) {
							}
						}
					});
				}
			}
		});

		EventUtilImpl.init();
		BaseEventController.addEventContainer(this);

		try {
			registerPath(getClass());
		} catch (MalformedURLException e) {
		}

		createEventChannel("mods.setuploader");
		createEventChannel("mods.aftermodloader");
		createEventChannel("mods.prestartgame");
		createEventChannel("mod.loaded");
		createEventChannel("mods.all.loaded");
		createEventChannel("modloader.register.path");
		createEventChannel("mods.load.regular.start");

		BaseEventController.work();

		info("Starting CyanCore...");
		CyanCore.addAllowedTransformerAutoDetectClass("org.asf.cyan.api.internal.CyanAPIComponent");
		CyanCore.initializeComponents();

		info("Adding coremod paths to system classloader...");
		addToSystemLater.forEach(t -> {
			try {
				FluidAgent.addToClassPath(t);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});
		addToSystemLater.clear();

		info("Reloading reflections...");
		CyanCore.reinitReflections();

		info("Downloading coremod maven dependencies...");
		downloadMavenDependencies(coremodMavenDependencies);

		File mods = new File(cyanDir, "mods");
		File versionMods = new File(mods, CyanInfo.getMinecraftVersion());
		StartupWindow.WindowAppender.increaseProgress();

		info("Scanning CMF mod files...");
		if (!mods.exists())
			mods.mkdirs();
		StartupWindow.WindowAppender.increaseProgress();

		if (versionMods.exists()) {
			info("Importing version-specific mods...");
			importMods(versionMods);
		}
		StartupWindow.WindowAppender.increaseProgress();

		info("Importing regular mods...");
		for (File f : extraClassPath) {
			try {
				CyanCore.addUrl(f.toURI().toURL());
				FluidAgent.addToClassPath(f);
			} catch (IOException e) {
			}
		}
		importMods(mods);
		StartupWindow.WindowAppender.increaseProgress();

		info("Downloading mod maven dependencies...");
		downloadMavenDependencies(modMavenDependencies);
		StartupWindow.WindowAppender.increaseProgress();
	}

	private static ArrayList<Path> paths = new ArrayList<Path>();

	@AttachEvent(value = "modloader.register.path", synchronize = true)
	public void registerPath(Class<?> owner) throws MalformedURLException {
		URL u = owner.getProtectionDomain().getCodeSource().getLocation();
		if (u.toString().startsWith("jar:"))
			u = new URL(u.toString().substring(4, u.toString().lastIndexOf("!/")));
		else if (u.toString().endsWith(owner.getTypeName().replace(".", "/") + ".class")) {
			u = new URL(u.toString().substring(0,
					u.toString().lastIndexOf(owner.getTypeName().replace(".", "/") + ".class")));
		}
		try {
			paths.add(Path.of(u.toURI()));
		} catch (URISyntaxException e) {
		}
	}

	private boolean gameLoad = false;

	@AttachEvent(value = "mods.prestartgame", synchronize = true)
	private void beforeGame(ClassLoader loader) throws ClassNotFoundException {
		if (gameLoad)
			return;
		gameLoad = true;

		info("Loading coremods...");
		loadCoreMods(loader);
		StartupWindow.WindowAppender.increaseProgress();

		info("Finishing bootstrap... Loading postponed components...");
		loadPostponedComponents(loader, CyanCore.getCoreClassLoader(), CyanCore.getClassLoader());
		StartupWindow.WindowAppender.increaseProgress();

		info("Loading final events...");
		loadEvents();
		StartupWindow.WindowAppender.increaseProgress();

		dispatchEvent("mods.aftermodloader", loader);
		BaseEventController.work();
		StartupWindow.WindowAppender.increaseProgress();
	}

	@Override
	protected void postRegister() {
		loadEvents();
	}

	private void loadEvents() {
		for (String event : events) {
			try {
				Optional<IExtendedEvent<?>> optEvent = extEvents.stream().filter(t -> t.channelName().equals(event))
						.findFirst();
				if (optEvent.isPresent()) {
					IExtendedEvent<?> extEvent = optEvent.get();

					extEvent.afterInstantiation();
					extEvent.assign(createEventChannel(event));
				} else {
					createEventChannel(event);
				}
			} catch (IllegalStateException e) {
			}
		}
		events.clear();
	}

	private void downloadMavenDependencies(HashMap<String, Version> mavenDependencies) {
		mavenDependencies.forEach((dep, version) -> {
			String group = dep.split(":")[0];
			String name = dep.split(":")[1];

			String path = group.replace(".", "/") + "/" + name + "/" + version.toString() + "/" + name + "-"
					+ version.toString() + ".jar";
			File libFile = new File("libraries/" + path);
			if (!libFile.getParentFile().exists())
				libFile.getParentFile().mkdirs();

			if (!libFile.exists()) {
				boolean downloaded = false;
				for (String repo : mavenRepositories.keySet()) {
					String url = mavenRepositories.get(repo);
					info("Trying to download " + group + ":" + name + " from " + repo + " (" + url + ")...");
					try {
						URL u = new URL(url + "/" + path);
						debug("Full URL: " + u);

						InputStream strm = u.openStream();
						FileOutputStream libOut = new FileOutputStream(libFile);
						strm.transferTo(libOut);
						strm.close();
						libOut.close();
						downloaded = true;
						info("Done.");
						break;
					} catch (IOException e) {
					}
				}
				if (!downloaded) {
					fatal("Could not download dependency " + group + ":" + name + " from ANY maven repository!");
					StartupWindow.WindowAppender.fatalError();
					System.exit(1);
				}
			}

			try {
				CyanCore.addCoreUrl(libFile.toURI().toURL());
				CyanCore.addUrl(libFile.toURI().toURL());
				FluidAgent.addToClassPath(libFile);
			} catch (IOException e) {
				fatal("Could not load dependency " + group + ":" + name);
				StartupWindow.WindowAppender.fatalError();
				fatal("Exception was thrown during dependency loading.", e);
				System.exit(1);
			}
		});
	}

	public IMod[] getAllModInstances() {
		ArrayList<IMod> mods = new ArrayList<IMod>();
		mods.addAll(this.mods);
		mods.addAll(this.coremods);
		return mods.toArray(t -> new IMod[t]);
	}

	private boolean loadedMods = false;
	private boolean bootstrap = false;

	@AttachEvent(value = "mods.setuploader", synchronize = true)
	private void preloadMods(ClassLoader loader) throws IOException {
		if (getLaunchPlatform() == LaunchPlatform.MCP) {
			bootstrap = true;
			loadMods(loader);
		}
	}

	@AttachEvent(value = "mods.load.regular.start", synchronize = true)
	private void loadMods(ClassLoader loader) throws IOException {
		if (getLaunchPlatform() == LaunchPlatform.MCP) {
			if (!bootstrap) {
				return;
			}
			if (loadedMods)
				return;
			info("Cyan will now load its mods, unlike standalone Cyan, paper and fabric, Cyan mods need to be loaded AFTER forge mods.");
		}

		if (loadedMods)
			return;
		loadedMods = true;

		info("Loading regular mods...");
		loadModClasses(loader);
		StartupWindow.WindowAppender.increaseProgress();

		info("Looking for the KickStart CYAN Installer...");
		String dir = System.getenv("APPDATA");
		if (dir == null)
			dir = System.getProperty("user.home");
		File installs = new File(dir, ".kickstart-installer.ccfg");
		if (installs.exists()) {
			info("Adding current installation to manifest...");
			ManifestUtils man = ManifestUtils.getDefault();

			String platVer = platformVersion;
			if (Modloader.getModloaderLaunchPlatform() == LaunchPlatform.VANILLA)
				platVer = Modloader.getModloaderGameVersion();

			if (!cyanDir.exists())
				cyanDir.mkdirs();
			KickStartInstallation install = man.addInstallation(null, cyanDir.getCanonicalFile().getParentFile(),
					Modloader.getModloaderGameVersion(), Modloader.getModloaderLaunchPlatform().toString(), platVer);
			install.rootLoader = getModKitRootModloader().getName().toLowerCase();
			install.clearLoaders();
			for (Modloader ld : Modloader.getAllModloaders()) {
				install.appendLoader(ld.getName().toLowerCase(), ld.getSimpleName(), ld.getVersion().toString());
			}
			install.getLoader("cyanloader").modInstallDir = ".cyan-data/mods";
			install.getLoader("cyanloader").coreModInstallDir = ".cyan-data/mods";

			man.write();
		}
		StartupWindow.WindowAppender.increaseProgress();

		info("Reloading reflections...");
		CyanCore.reinitReflections();

		dispatchEvent("mods.all.loaded");
		dispatchEvent("mods.all.loaded", loader);

		if (getLaunchPlatform() == LaunchPlatform.MCP)
			info("Returning to game code...");

		StartupWindow.WindowAppender.increaseProgress();
	}

	private void loadModClasses(ClassLoader loader) {
		modManifests.keySet().stream().sorted((t1, t2) -> t1.compareTo(t2)).forEach(k -> {
			CyanModfileManifest manifest = modManifests.get(k);
			if (k.contains(":") && !manifest.loaded)
				loadMod(false, manifest, new ArrayList<String>(), loader);
		});
	}

	/**
	 * Retrieves a mod instance by its class
	 * 
	 * @param modClass Mod class
	 * @return Mod instance or null
	 */
	@SuppressWarnings("unchecked")
	public <T extends IBaseMod> T getModByClass(Class<T> modClass) {
		for (IMod md : mods) {
			if (md.getClass().isAssignableFrom(modClass) || md.getClass().getTypeName().equals(modClass.getTypeName()))
				return (T) md;
		}
		for (IMod md : coremods) {
			if (md.getClass().isAssignableFrom(modClass) || md.getClass().getTypeName().equals(modClass.getTypeName()))
				return (T) md;
		}
		return null;
	}

	public static void addCyanPaths(ArrayList<Path> paths) {
		for (Path pth : CyanCore.getAddedPaths()) {
			paths.add(pth);
		}
		paths.addAll(CyanLoader.paths);
	}

	public static void crash() {
		CyanLoader.getModloader(CyanLoader.class);
	}

	static String[] coremodTypes = null;
	static String[] cyanClasses = new String[] { "org.asf.cyan.CyanLoader",
			"org.asf.cyan.mods.events.IEventListenerContainer", "org.asf.cyan.mods.internal.BaseEventController",
			"org.asf.cyan.mods.IMod", "org.asf.cyan.mods.ICoreMod", "org.asf.cyan.mods.AbstractCoremod",
			"org.asf.cyan.mods.AbstractMod", "org.asf.cyan.mods.IBaseMod", "org.asf.cyan.core.CyanCore",
			"org.asf.cyan.api.modloader.Modloader", "org.asf.cyan.api.common.CyanComponent",
			"org.asf.cyan.api.config.Configuration", "modkit.util.EventUtil", "modkit.util.ContainerConditions",
			"org.asf.cyan.api.internal.CyanAPIComponent", "org.asf.cyan.mods.config.CyanModfileManifest",
			"org.asf.cyan.internal.modkitimpl.info.Protocols", "org.asf.cyan.internal.modkitimpl.util.EventUtilImpl",
			"org.asf.cyan.api.events.extended.IExtendedEvent", "org.asf.cyan.api.events.extended.EventObject",
			"modkit.protocol.ModKitModloader", "modkit.protocol.ModKitProtocol",
			"modkit.protocol.handshake.HandshakeRule", "modkit.protocol.IncompatibleProtocolException",
			"modkit.protocol.ModKitModloader$ModKitProtocolRules" };
	static String[] dntPackages = new String[] { "com.google.gson", "org.apache.logging", "com.google.common",
			"org.asf.cyan.api.packet", "org.asf.cyan.fluid" };

	public static boolean doNotTransform(String name) {
		if (coremodTypes == null) {
			coremodTypes = CyanLoader.getModloader(CyanLoader.class).coremods.stream()
					.map(t -> t.getClass().getTypeName()).toArray(t -> new String[t]);
		}

		return Stream.of(cyanClasses).anyMatch(t -> t.equals(name))
				|| Stream.of(coremodTypes).anyMatch(t -> t.equals(name))
				|| Stream.of(dntPackages).anyMatch(t -> name.startsWith(t + "."));
	}

	public static void setCallTraceClassLoader(ClassLoader loader) {
		CallTrace.setCallTraceClassLoader(loader);
	}

	private static ArrayList<String> locations = null;

	private static CyanLoader cyanModloader = null;

	public static InputStream getClassStream(String name) throws MalformedURLException {
		if (doNotTransform(name))
			return null;
		if (getClassData(name) == null)
			return null;
		return (InputStream) getClassData(name)[0];
	}

	public static Object[] getClassData(String name) throws MalformedURLException {
		if (doNotTransform(name))
			return null;
		if (cyanModloader == null)
			cyanModloader = CyanLoader.getModloader(CyanLoader.class);

		if (locations == null) {
			locations = new ArrayList<String>();
			for (Path pth : CyanCore.getAddedPaths()) {
				String path = pth.toString();
				if (path.endsWith(".jar") || path.endsWith(".zip"))
					path = "jar:" + pth.toUri().toURL() + "!/";
				if (!path.endsWith("/"))
					path += "/";
				locations.add(path);
			}
			for (Path pth : paths) {
				String path = pth.toString();
				if (path.endsWith(".jar") || path.endsWith(".zip"))
					path = "jar:" + pth.toUri().toURL() + "!/";
				if (!path.endsWith("/"))
					path += "/";
				locations.add(path);
			}
		}

		for (String loc : locations) {
			try {
				URL u = new URL(loc + name.replace(".", "/") + ".class");
				InputStream strm = u.openStream();
				String location = loc;
				if (location.startsWith("jar:"))
					location = location.substring(4, location.lastIndexOf("!/"));
				return new Object[] { strm, new URL(location) };
			} catch (IOException e) {
			}
		}

		return null;
	}

	public static String getModSourceBase(Class<? extends IMod> modType) {
		String name = modType.getTypeName();

		if (cyanModloader == null)
			cyanModloader = CyanLoader.getModloader(CyanLoader.class);

		if (cyanModloader.modManifests.containsKey(name))
			return cyanModloader.modManifests.get(name).source;
		else if (cyanModloader.coreModManifests.containsKey(name))
			return cyanModloader.coreModManifests.get(name).source;

		return null;
	}

	//
	// ModKit Protocol Versions

	@Override
	public double modloaderProtocol() {
		return Protocols.LOADER_PROTOCOL;
	}

	@Override
	public double modloaderMinProtocol() {
		return Protocols.MIN_LOADER;
	}

	@Override
	public double modloaderMaxProtocol() {
		return Protocols.MAX_LOADER;
	}

	@Override
	public double modkitProtocolVersion() {
		return Protocols.MODKIT_PROTOCOL;
	}

	public String getLoaders() {
		String modloaders = "";
		for (Modloader modloader : Modloader.getAllModloaders()) {
			if (!modloaders.isEmpty()) {
				modloaders += ", ";
			}

			modloaders += modloader.getSimpleName();
		}
		return modloaders;
	}

	public String getLoaderVersions() {
		String loaderversions = "";

		for (Modloader modloader : Modloader.getAllModloaders()) {
			if (!loaderversions.isEmpty()) {
				loaderversions += ", ";
			}
			loaderversions += modloader.getName() + "; "
					+ (modloader.getVersion() == null ? "Generic" : modloader.getVersion());
		}

		return loaderversions;
	}

	public void addLoadedModInfo(BiConsumer<String, Object> setDetail) {
		for (Modloader modloader : Modloader.getAllModloaders()) {
			if (modloader.supportsMods())
				setDetail.accept("Loaded " + modloader.getSimpleName().toUpperCase() + " Mods",
						modloader.getLoadedMods().length);
			if (modloader.supportsCoreMods())
				setDetail.accept("Loaded " + modloader.getSimpleName().toUpperCase() + " Coremods",
						modloader.getLoadedCoremods().length);
		}
	}

	@Override
	public IBaseMod getModInstance(IModManifest mod) {
		for (IMod md : mods) {
			if (md.getManifest().id().equals(mod.id()))
				return md;
		}

		return null;
	}

	@Override
	public IBaseMod getCoremodInstance(IModManifest mod) {
		for (ICoremod md : coremods) {
			if (md.getManifest().id().equals(mod.id()))
				return md;
		}

		return null;
	}

}
