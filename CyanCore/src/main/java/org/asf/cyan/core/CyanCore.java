package org.asf.cyan.core;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.asf.cyan.api.classloading.DynamicClassLoader;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.modloader.LoadPhase;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.FluidAgent;
import org.asf.cyan.fluid.implementation.CyanTransformer;
import org.asf.cyan.fluid.implementation.CyanTransformerMetadata;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.util.ConfigurationBuilder;

/**
 * CyanCore - Implementation of the CyanComponents system (and Cyan's core
 * bootstrap system)<br/>
 * <br/>
 * Use <code>CyanCore.addAllowedPackage(package)</code> to load packages for
 * automatic detection. (this is to prevent startup lag)
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@CYAN_COMPONENT
public class CyanCore extends CyanComponent {
	private static ArrayList<URL> addedUrls = new ArrayList<URL>();
	static ArrayList<Runnable> preLoadHooks = new ArrayList<Runnable>();
	static ArrayList<String> allowedPackages = new ArrayList<String>();
	private static ArrayList<Class<?>> additionalClasses = new ArrayList<Class<?>>();
	private static ArrayList<String> allowedAutodetectClasses = new ArrayList<String>();
	static boolean disableAgent = false;
	static boolean cornflowerSupport = false;
	private static String entryMethod = "Generic Launcher";

	/**
	 * Adds types to the list of allowed transformer providers, cannot be done after
	 * initializeComponents
	 */
	public static void addAllowedTransformerAutoDetectClass(String typeName) {
		allowedAutodetectClasses.add(typeName);
	}

	/**
	 * Register a package for loading CyanComponents. (can only be done during or
	 * before coreload)
	 * 
	 * @param packageName Package name (such as org.asf.cyan, subpackages are
	 *                    included)
	 */
	public static void addAllowedPackage(String packageName) {
		if (loadPhase.equals(LoadPhase.NOT_READY) || loadPhase.equals(LoadPhase.CORELOAD)) {
			allowedPackages.add(packageName);
		} else
			throw new IllegalStateException("CyanCore is already past CORELOAD");
	}

	/**
	 * Add a class for the findClasses method. (can only be done during or before
	 * coreload)
	 * 
	 * @param cls Class to add
	 */
	public static void addAdditionalClass(Class<?> cls) {
		if (loadPhase.equals(LoadPhase.NOT_READY) || loadPhase.equals(LoadPhase.CORELOAD))
			additionalClasses.add(cls);
		else
			throw new IllegalStateException("CyanCore is already past CORELOAD");
	}

	/**
	 * Set the name of the program/method used to start cyan, can only be set ONCE
	 * 
	 * @param method Method name
	 */
	public static void setEntryMethod(String method) {
		if (!entryMethod.equals("Generic Launcher"))
			throw new IllegalStateException("Entry method already set!");

		entryMethod = method;
	}

	static void infoLog(String msg) {
		info(msg);
	}

	static void debugLog(String msg) {
		debug(msg);
	}

	static void warnLog(String msg) {
		warn(msg);
	}

	/**
	 * Enable cornflower execution support, changes a bit of the loading sequence to
	 * allow gradle to run MTK and CyanComponents
	 */
	public static void enableCornflowerSupport() {
		cornflowerSupport = true;
	}

	/**
	 * Disables the FLUID agent<br/>
	 * <b>NOTE: You WILL need to manually load the agent to use cyan if you run
	 * this,<br/>
	 * calling this will only deactivate the Fluid.loadAgent call</b>
	 */
	public static void disableAgent() {
		disableAgent = true;
	}

	/**
	 * Register a hook that is called BEFORE all cyan components are loaded, can
	 * only be done BEFORE full initialization, ignored if added later.
	 * 
	 * @param hook Runnable to call
	 */
	public static void registerPreLoadHook(Runnable hook) {
		preLoadHooks.add(hook);
	}

	/**
	 * Track messages logged by the Cyan logger, get result by running
	 * stopTracking() from same class (changes the level of the running tracker,
	 * don't call more than once)
	 */
	public static void trackLevel(Level lv) {
		HashMap<Level, HashMap<String, Level>> mp;
		HashMap<String, Level> lst;
		if (itms.containsKey(CallTrace.traceCall())) {
			mp = itms.get(CallTrace.traceCall());
			lst = mp.values().iterator().next();
			mp.remove(mp.keySet().iterator().next());
		} else {
			mp = new HashMap<Level, HashMap<String, Level>>();
			lst = new HashMap<String, Level>();
		}

		mp.put(lv, lst);
		itms.put(CallTrace.traceCall(), mp);
	}

	/**
	 * Stop tracking and return thrown errors/warnings
	 * 
	 * @return List of error and warning log messages
	 */
	public static HashMap<String, Level> stopTracking() {
		if (!itms.containsKey(CallTrace.traceCall()))
			return null;
		else
			return itms.remove(CallTrace.traceCall()).values().iterator().next();
	}

	/**
	 * Set log to debug
	 */
	public static void setDebugLog() {
		simpleInit();
		if (LOG == null)
			initLogger();
		trace(CallTrace.traceCallName() + " set the log level to DEBUG.");
		Configurator.setLevel("CYAN", Level.DEBUG);
	}

	/**
	 * Set log to trace (prints even more than debug)
	 */
	public static void setTraceLog() {
		simpleInit();
		if (LOG == null)
			initLogger();
		trace(CallTrace.traceCallName() + " set the log level to TRACE.");
		Configurator.setLevel("CYAN", Level.TRACE);
	}

	/**
	 * Disable Cyan Logging (set to warnings only)
	 */
	public static void disableLog() {
		simpleInit();
		if (LOG == null)
			initLogger();
		trace(CallTrace.traceCallName() + " set the log level to WARN.");
		Configurator.setLevel("CYAN", Level.WARN);
	}

	/**
	 * Set Cyan Logging to errors only
	 */
	public static void disableWarnLog() {
		simpleInit();
		if (LOG == null)
			initLogger();
		trace(CallTrace.traceCallName() + " set the log level to ERROR.");
		Configurator.setLevel("CYAN", Level.ERROR);
	}

	/**
	 * Set Cyan Logging to fatal errors only
	 */
	public static void disableError() {
		simpleInit();
		if (LOG == null)
			initLogger();
		trace(CallTrace.traceCallName() + " set the log level to FATAL.");
		Configurator.setLevel("CYAN", Level.FATAL);
	}

	/**
	 * Disable Cyan Logging completely
	 */
	public static void disableAllLog() {
		simpleInit();
		if (LOG == null)
			initLogger();
		trace(CallTrace.traceCallName() + " disabled logging.");
		Configurator.setLevel("CYAN", Level.OFF);
	}

	/**
	 * Enable Cyan Logging (set level to INFO)
	 */
	public static void enableLog() {
		simpleInit();
		if (LOG == null)
			initLogger();
		trace(CallTrace.traceCallName() + " set the log level to INFO.");
		Configurator.setLevel("CYAN", Level.INFO);
	}

	@Override
	protected void setupComponents() {
		if (cyancoreInitialized)
			throw new IllegalStateException("Cyan components have already been initialized.");
		if (LOG == null)
			initLogger();
	}

	@Override
	protected void preInitAllComponents() {
		trace("OPEN FluidAPI Mappings Loader, caller: " + CallTrace.traceCallName());
		try {
			debug("Opening FLUID API...");
			Fluid.openFluidLoader();
		} catch (IllegalStateException e) {
			error("Failed to open FLUID!", e);
		}
		setPhase(LoadPhase.CORELOAD);

		trace("INITIALIZE all components, caller: " + CallTrace.traceCallName());
		trace("CREATE ConfigurationBuilder instance, caller: " + CallTrace.traceCallName());

		if (loader == null)
			initLoader();

		debug("Loading pre-load hooks...");
		trace("EXECUTE pre-load hooks, caller: " + CallTrace.traceCallName());
		for (Runnable hook : preLoadHooks) {
			hook.run();
		}

		info("Looking for more transformers...");
		for (Class<?> cls : this.findAnnotatedClassesInternal(FLUID_AUTODETECT.class)) {
			if (allowedAutodetectClasses.contains(cls.getTypeName())) {
				try {
					info("Loading " + cls.getTypeName() + "...");
					Method m = cls.getDeclaredMethod("addTransformers");
					m.setAccessible(true);
					m.invoke(null);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
						| SecurityException | NoSuchMethodException e) {
					error("Failed to add transformers, class name: " + cls.getSimpleName(), e);
				}
			}
		}
		allowedAutodetectClasses.clear();

		trace("CLOSE FluidAPI Transformer and Mappings loader, caller: " + CallTrace.traceCallName());
		Fluid.closeFluidLoader();

		if (!cornflowerSupport)
			info("Starting the FLUID agent...");
		CyanTransformer.initComponent();
		CyanTransformerMetadata.initComponent();
		if (!disableAgent && !cornflowerSupport)
			Fluid.loadAgent();
		else if (!cornflowerSupport)
			FluidAgent.initialize();
	}

	@Override
	protected void finalizeComponents() {
		trace("SECURE Core Class Loader, caller: " + CallTrace.traceCallName());
		loader.secure();
		debug("Class loader secured, preparing to start...");
	}

	@Override
	protected Class<?>[] getComponentClasses() {
		if (reflections == null) {
			initReflections();
		}
		info("Searching for Cyan Components in loaded jars...");
		trace("FIND all classes annotated with CYAN_COMPONENT, caller: " + CallTrace.traceCallName());
		Set<Class<?>> classes = reflections.getTypesAnnotatedWith(CYAN_COMPONENT.class);
		trace("LOOP through found classes, caller: " + CallTrace.traceCallName());
		return classes.toArray(t -> new Class<?>[t]);
	}

	public static void reinitReflections() {
		core.initReflections();
	}

	public void initReflections() {
		ClassLoader cl = CyanCore.class.getClassLoader();
		ConfigurationBuilder conf = ConfigurationBuilder.build(loader);
		if (cornflowerSupport) {
			conf = ConfigurationBuilder.build();
			trace("CORNFLOWER support active, using workaround loading method");
			trace("COPY config URL list, caller: " + CallTrace.traceCallName());
			Set<URL> urls = conf.getUrls();
			trace("CLEAR URL list, caller: " + CallTrace.traceCallName());
			urls.clear();
			trace("SET config URL list, caller: " + CallTrace.traceCallName());
			conf.setUrls(urls);
			trace("ADD Cyan Source Location to URL list, caller: " + CallTrace.traceCallName());
			conf.addUrls(CyanCore.class.getProtectionDomain().getCodeSource().getLocation());
			trace("ADD Cornflower Source Location to URL list, caller: " + CallTrace.traceCallName());
			try {
				conf.addUrls(Class.forName("org.asf.cyan.cornflower.gradle.CornflowerCore").getProtectionDomain()
						.getCodeSource().getLocation());
			} catch (ClassNotFoundException e1) {
			}
			try {
				conf.addUrls(Class.forName("org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit").getProtectionDomain()
						.getCodeSource().getLocation());
			} catch (ClassNotFoundException e) {
			}
			try {
				conf.addUrls(Class.forName("org.asf.cyan.fluid.implementation.CyanTransformer").getProtectionDomain()
						.getCodeSource().getLocation());
			} catch (ClassNotFoundException e) {
			}
			trace("SET setExpandSuperTypes to false, caller: " + CallTrace.traceCallName());
			conf = conf.setExpandSuperTypes(false);
		} else {
			for (Package p : cl.getDefinedPackages()) {
				String rname = p.getName().replace(".", "/");
				try {
					Enumeration<URL> roots = cl.getResources(rname);
					for (URL i : Collections.list(roots)) {
						if (i.toString().endsWith(rname)) {
							String newURL = "";
							if (i.toString().startsWith("jar:")) {
								newURL = i.toString().substring(4, i.toString().lastIndexOf("!/"));
							} else {
								newURL = i.toString().substring(0, i.toString().lastIndexOf(rname));
							}
							i = new URL(newURL);
						} else if (i.toString().startsWith("jar:")) {
							i = new URL(i.toString().substring(4, i.toString().lastIndexOf("!/")));
						}

						if (rname.startsWith("org/asf/cyan/") || rname.equals("org/asf/cyan")
								|| allowedPackages.stream().anyMatch(t -> t.equals(rname.replaceAll("/", "."))
										|| rname.replaceAll("/", ".").startsWith(t + "."))) {
							if (!conf.getUrls().contains(i)) {
								debug("Added URL for component scan. URL: " + i.toString());
								conf.addUrls(i);
							}
						}
					}
				} catch (IOException ex) {
					error("Failed to load the " + rname + " package.", ex);
				}
			}
			for (String p : extraPkgs) {
				String rname = p.replace(".", "/");
				try {
					Enumeration<URL> roots = cl.getResources(rname);
					for (URL i : Collections.list(roots)) {
						if (i.toString().endsWith(rname)) {
							String newURL = "";
							if (i.toString().startsWith("jar:")) {
								newURL = i.toString().substring(4, i.toString().lastIndexOf("!/"));
							} else {
								newURL = i.toString().substring(0, i.toString().lastIndexOf(rname));
							}
							i = new URL(newURL);
						} else if (i.toString().startsWith("jar:")) {
							i = new URL(i.toString().substring(4, i.toString().lastIndexOf("!/")));
						}

						if (rname.startsWith("org/asf/cyan/") || rname.equals("org/asf/cyan")
								|| allowedPackages.stream().anyMatch(t -> t.equals(rname.replaceAll("/", "."))
										|| rname.replaceAll("/", ".").startsWith(t + "."))) {
							if (!conf.getUrls().contains(i)) {
								debug("Added URL for component scan. URL: " + i.toString());
								conf.addUrls(i);
							}
						}
					}
				} catch (IOException ex) {
					error("Failed to load the " + rname + " package.", ex);
				}
			}
			if (openloader != null)
				conf.addUrls(openloader.getURLs());
			if (addedUrls.size() != 0) {
				for (URL u : addedUrls) {
					conf.addUrls(u);
				}
			}
			conf.addUrls(loader.getURLs());
		}

		trace("CREATE Reflections instance, caller: " + CallTrace.traceCallName());
		reflections = new Reflections(conf);
	}

	private Reflections reflections;

	private static CyanCore core;

	/**
	 * Initialize all CYAN components (required)
	 * 
	 * @throws IllegalStateException If the component is already initialized
	 */
	public static void initializeComponents() throws IllegalStateException {
		if (core == null)
			simpleInit();

		core.initializeComponentClasses();
	}

	/**
	 * Pre-initialize the Cyan Core Loader
	 */
	public static void initLoader() {
		if (loader == null) {
			loader = new DynamicClassLoader("CyanCore Internal Class Loader", CyanCore.class.getClassLoader());
			loader.setOptions(DynamicClassLoader.OPTION_PREVENT_AUTOSECURE);
			loader.setOptions(DynamicClassLoader.OPTION_ALLOW_DEFINE);
			loader.addDefaultCp();
			if (new File("vanilla-server.jar").exists())
				try {
					loader.addUrl(new File("vanilla-server.jar").toURI().toURL());
				} catch (MalformedURLException e) {
				}
		} else
			throw new IllegalStateException("CyanCore Class Loader is already registered!");
	}

	protected static void initComponent() {
		trace("INITIALIZE Main CYAN Component, caller: " + CallTrace.traceCallName());
		trace("CREATE DynamicURLClassLoaders, caller: " + CallTrace.traceCallName());
		openloader = new DynamicClassLoader("Cyan Mod Class Loader");
		openloader.setOptions(DynamicClassLoader.OPTION_PREVENT_AUTOSECURE);
		openloader.apply();
		openloader.setOptions(DynamicClassLoader.OPTION_DENY_ADD_RUNTIME);
		agentloader = loader;
		secured = false;
		cyancoreInitialized = true;
	}

	static boolean cyancoreInitialized = false;

	/**
	 * Check if the component is already initialized
	 * 
	 * @return True if the component is initialized, false otherwise
	 */
	public static boolean isInitialized() {
		return cyancoreInitialized;
	}

	static DynamicClassLoader loader = null;
	static DynamicClassLoader openloader = null;

	/**
	 * Gets the core class loader
	 * 
	 * @return Core DynamicClassLoader, try to avoid usage
	 */
	public static URLClassLoader getCoreClassLoader() {
		return loader;
	}

	/**
	 * Gets the userland class loader (for mods and such)
	 * 
	 * @return Userland DynamicClassLoader
	 */
	public static DynamicClassLoader getClassLoader() {
		return openloader;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T> Class<T>[] findClassesInternal(Class<T> interfaceOrSupertype) {
		if (reflections == null) {
			initReflections();
		}

		if (LOG == null)
			initLogger();

		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		classes.addAll(reflections.getSubTypesOf(interfaceOrSupertype));

		for (Class<?> cls : additionalClasses) {
			if (interfaceOrSupertype.isAssignableFrom(cls)
					&& !classes.stream().anyMatch(t -> t.getTypeName().equals(cls.getTypeName()))) {
				classes.add(cls);
			}
		}

		return classes.toArray(t -> new Class[t]);
	}

	@Override
	public <T> String[] findClassNamesInternal(Class<T> interfaceOrSupertype) {
		if (reflections == null) {
			initReflections();
		}

		if (LOG == null)
			initLogger();

		ArrayList<String> classes = new ArrayList<String>();
		for (String cls : reflections.getStore().getAll(SubTypesScanner.class, interfaceOrSupertype.getTypeName())) {
			classes.add(cls);
		}

		for (Class<?> cls : additionalClasses) {
			if (interfaceOrSupertype.isAssignableFrom(cls)
					&& !classes.stream().anyMatch(t -> t.equals(cls.getTypeName()))) {
				classes.add(cls.getTypeName());
			}
		}

		return classes.toArray(t -> new String[t]);
	}

	@Override
	@SuppressWarnings("unchecked")
	protected <T extends Annotation> Class<T>[] findAnnotatedClassesInternal(Class<T> annotation) {
		if (reflections == null) {
			initReflections();
		}

		if (LOG == null)
			initLogger();

		ArrayList<Class<?>> classes = new ArrayList<Class<?>>();
		classes.addAll(reflections.getTypesAnnotatedWith(annotation));

		for (Class<?> cls : additionalClasses) {
			if (cls.isAnnotationPresent(annotation)
					&& !classes.stream().anyMatch(t -> t.getTypeName().equals(cls.getTypeName()))) {
				classes.add(cls);
			}
		}

		return classes.toArray(t -> new Class[t]);
	}

	/**
	 * Start the game
	 * 
	 * @param game The game to launch
	 * @param args Arguments
	 * @throws IllegalAccessException    If starting fails
	 * @throws IllegalArgumentException  If starting fails
	 * @throws InvocationTargetException If starting fails
	 * @throws NoSuchMethodException     If starting fails
	 * @throws SecurityException         If starting fails
	 * @throws ClassNotFoundException    If starting fails
	 * @throws IOException               If closing the class loader fails
	 */
	public static void startGame(String game, String[] args)
			throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IOException {
		if (!isInitialized())
			throw new UnsupportedOperationException("Cyan is not initialized!");

		StartupWindow.WindowAppender.increaseProgress();
		Thread.currentThread().setContextClassLoader(loader);

		info("Loading CYAN information container...");
		CyanInfo.getDevStartDate();
		StartupWindow.WindowAppender.increaseProgress();

		info("Cyan launch method: " + entryMethod);
		info("Cyan launch platform: " + Modloader.getModloaderLaunchPlatform().toString());
		info("");
		String loaderStr = "";
		String modloaderStr = "";

		if (Modloader.getModloader().getChildren().length != 0) {
			loaderStr = Modloader.getModloader().getChildren()[0].getName() + "-"
					+ Modloader.getModloader().getChildren()[0].getVersion();
			modloaderStr = ", Modloader: " + loaderStr;
		}

		info("Welcome to CYAN!");
		info("Cyan version: " + Modloader.getModloaderVersion() + ", Minecraft " + CyanInfo.getMinecraftVersion()
				+ modloaderStr);
		info("Full version: " + CyanInfo.getMinecraftVersion()
				+ (loaderStr.isEmpty() ? "" : "-" + loaderStr.toLowerCase()) + "-cyan-" + CyanInfo.getCyanVersion());
		info("");
		StartupWindow.WindowAppender.increaseProgress();

		info("Loading class " + game + "...");
		Class<?> clas = loader.loadClass(game);
		StartupWindow.WindowAppender.increaseProgress();

		Method meth = clas.getMethod("main", String[].class);
		Modloader.getModloader().dispatchEvent("game.beforestart", new Object[] { game, args });

		info("Starting minecraft...");
		StartupWindow.WindowAppender.increaseProgress();
		meth.invoke(null, new Object[] { args });
	}

	private static GameSide side = null;
	private static LoadPhase loadPhase = LoadPhase.NOT_READY;

	/**
	 * Sets the current loading phase (internal use only, avoid usage)
	 * 
	 * @param phase Current phase
	 */
	public static void setPhase(LoadPhase phase) {
		loadPhase = phase;
		if (Modloader.getModloader() != null)
			Modloader.getModloader().dispatchEvent("phase.changed", phase);
		if (phase == LoadPhase.POSTINIT && !secured) {
			debug("Securing mod classloader...");
			openloader.secure();
			secured = true;
		}
	}

	private static boolean secured = false;

	/**
	 * Set the current side name (CLIENT/SERVER), can only be set ONCE
	 * 
	 * @param side Side name
	 */
	public static void setSide(String side) {
		if (CyanCore.side == null)
			CyanCore.side = GameSide.valueOf(side);
		else
			throw new IllegalStateException("This field cannot be changed after it has been assigned!");
	}

	/**
	 * Get the current side (CLIENT/SERVER)
	 * 
	 * @return Side name
	 */
	public static GameSide getSide() {
		return side;
	}

	/**
	 * Get the current loading phase
	 * 
	 * @return CyanLoadPhase object representing the loading phase
	 */
	public static LoadPhase getCurrentPhase() {
		return loadPhase;
	}

	/**
	 * Add a url to the core url class loader, can only be done from CORELOAD or
	 * before
	 * 
	 * @param url The url to add
	 */
	public static void addCoreUrl(URL url) {
		if (loadPhase.equals(LoadPhase.NOT_READY) || loadPhase.equals(LoadPhase.CORELOAD)) {
			if (loader == null)
				initLoader();
			if (core != null && core.reflections != null) {
				ConfigurationBuilder config = (ConfigurationBuilder) core.reflections.getConfiguration();
				config.addUrls(url);
			}
			loader.addUrl(url);
			addedUrls.add(url);
		} else
			throw new IllegalStateException("CyanCore is already past CORELOAD");
	}

	/**
	 * Add a url to the userland url class loader, can only be done after CyanCore
	 * has been initialized and before the POSTINIT phase has started
	 * 
	 * @param url The url to add
	 */
	public static void addUrl(URL url) {
		if (loadPhase.ge(LoadPhase.POSTINIT))
			throw new IllegalStateException("CyanCore is already past INIT");
		if (openloader == null)
			throw new IllegalStateException("Mod class loader not ready!");
		if (core != null && core.reflections != null) {
			ConfigurationBuilder config = (ConfigurationBuilder) core.reflections.getConfiguration();
			config.addUrls(url);
		}
		openloader.addUrl(url);
		addedUrls.add(url);
	}

	/**
	 * Get the entry method used to start cyan
	 * 
	 * @return Entry method name
	 */
	public static String getEntryMethod() {
		return entryMethod;
	}

	/**
	 * Simple init, no component loading, only assigns CyanCore as the main
	 * CyanComponents implementation.
	 */
	public static void simpleInit() {
		if (core != null)
			return;

		int max = 0;
		max++; // Set class loader
		max++; // Load CyanInfo
		max++; // Load game class
		max++; // Dispatch event
		max++; // Start game
		StartupWindow.WindowAppender.addMax(max);

		core = new CyanCore();
		core.assignImplementation();
	}

	public static Path[] getAddedPaths() {
		ArrayList<Path> paths = new ArrayList<Path>();
		for (URL u : addedUrls) {
			try {
				paths.add(Path.of(u.toURI()));
			} catch (URISyntaxException e) {
			}
		}
		return paths.toArray(t -> new Path[t]);
	}

	public static URL[] getAddedUrls() {
		return addedUrls.toArray(t -> new URL[t]);
	}

	private static boolean ide = false;

	public static void setIDE() {
		ide = true;
	}

	public static boolean isIdeMode() {
		return ide;
	}

	private static ArrayList<String> extraPkgs = new ArrayList<String>();

	public static void addToPackageScan(String pkg) {
		extraPkgs.add(pkg);
	}

	public static void setSupertype(String name, String superName) {
		if (core.reflections == null)
			core.initReflections();
		core.reflections.getStore().put(SubTypesScanner.class, superName.replace("/", "."), name.replace("/", "."));
	}
}
