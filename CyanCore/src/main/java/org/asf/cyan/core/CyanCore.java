package org.asf.cyan.core;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.asf.cyan.api.cyanloader.CyanLoadPhase;
import org.asf.cyan.api.cyanloader.CyanSide;
import org.asf.cyan.api.classloading.DynamicURLClassLoader;
import org.asf.cyan.api.common.CYAN_COMPONENT;
import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.fluid.Fluid;
import org.reflections.Reflections;
import org.reflections.util.ConfigurationBuilder;

@CYAN_COMPONENT
public class CyanCore extends CyanComponent {
	static ArrayList<Runnable> preLoadHooks = new ArrayList<Runnable>();

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
		if (itms.containsKey(KDebug.getCallerClass())) {
			mp = itms.get(KDebug.getCallerClass());
			lst = mp.values().iterator().next();
			mp.remove(mp.keySet().iterator().next());
		} else {
			mp = new HashMap<Level, HashMap<String, Level>>();
			lst = new HashMap<String, Level>();
		}

		mp.put(lv, lst);
		itms.put(KDebug.getCallerClass(), mp);
	}

	/**
	 * Stop tracking and return thrown errors/warnings
	 * 
	 * @return List of error and warning log messages
	 */
	public static HashMap<String, Level> stopTracking() {
		if (!itms.containsKey(KDebug.getCallerClass()))
			return null;
		else
			return itms.remove(KDebug.getCallerClass()).values().iterator().next();
	}

	/**
	 * Set log to debug
	 */
	public static void setDebugLog() {
		if (LOG == null)
			initLogger();
		trace(KDebug.getCallerClassName() + " set the log level to DEBUG.");
		Configurator.setLevel("CYAN", Level.DEBUG);
	}

	/**
	 * Set log to trace (prints even more than debug)
	 */
	public static void setTraceLog() {
		if (LOG == null)
			initLogger();
		trace(KDebug.getCallerClassName() + " set the log level to TRACE.");
		Configurator.setLevel("CYAN", Level.TRACE);
	}

	/**
	 * Disable Cyan Logging (set to warnings only)
	 */
	public static void disableLog() {
		if (LOG == null)
			initLogger();
		trace(KDebug.getCallerClassName() + " set the log level to WARN.");
		Configurator.setLevel("CYAN", Level.WARN);
	}

	/**
	 * Set Cyan Logging to errors only
	 */
	public static void disableWarnLog() {
		if (LOG == null)
			initLogger();
		trace(KDebug.getCallerClassName() + " set the log level to ERROR.");
		Configurator.setLevel("CYAN", Level.ERROR);
	}

	/**
	 * Set Cyan Logging to fatal errors only
	 */
	public static void disableError() {
		if (LOG == null)
			initLogger();
		trace(KDebug.getCallerClassName() + " set the log level to FATAL.");
		Configurator.setLevel("CYAN", Level.FATAL);
	}

	/**
	 * Disable Cyan Logging completely
	 */
	public static void disableAllLog() {
		if (LOG == null)
			initLogger();
		trace(KDebug.getCallerClassName() + " disabled logging.");
		Configurator.setLevel("CYAN", Level.OFF);
	}

	/**
	 * Enable Cyan Logging (set level to INFO)
	 */
	public static void enableLog() {
		if (LOG == null)
			initLogger();
		trace(KDebug.getCallerClassName() + " set the log level to INFO.");
		Configurator.setLevel("CYAN", Level.INFO);
	}

	/**
	 * Initialize all CYAN components (required)
	 * 
	 * @throws UnsupportedOperationException If the component is already initialized
	 */
	public static void initializeComponents() throws UnsupportedOperationException {
		if (cyancoreInitialized)
			throw new UnsupportedOperationException("Cyan components have already been initialized.");
		if (LOG == null)
			initLogger();
		
		phase = CyanLoadPhase.CORELOAD;
		Fluid.openFluidLoader();
		
		trace("INITIALIZE all components, caller: " + KDebug.getCallerClassName());
		trace("CREATE ConfigurationBuilder instance, caller: " + KDebug.getCallerClassName());

		ClassLoader cl = ClassLoader.getSystemClassLoader();
		if (loader == null) initLoader();

		trace("EXECUTE pre-load hooks, caller: " + KDebug.getCallerClassName());
		for (Runnable hook : preLoadHooks) {
			hook.run();
		}

		ConfigurationBuilder conf = ConfigurationBuilder.build(loader);
		for (Package p : cl.getDefinedPackages()) {
			String rname = p.getName().replace(".", "/");
			try {
				Enumeration<URL> roots = cl.getResources(rname);
				for (URL i : Collections.list(roots)) {
					conf.addUrls(i);
				}
			} catch (IOException ex) {
				error("Failed to load the " + rname + " package.", ex);
			}
		}
		for (Package p : loader.getDefinedPackages()) {
			String rname = p.getName().replace(".", "/");
			try {
				Enumeration<URL> roots = cl.getResources(rname);
				for (URL i : Collections.list(roots)) {
					conf.addUrls(i);
				}
			} catch (IOException ex) {
				error("Failed to load the " + rname + " package.", ex);
			}
		}

		trace("CREATE Reflections instance, caller: " + KDebug.getCallerClassName());
		Reflections reflections = new Reflections(conf);

		trace("FIND all classes annotated with CYAN_COMPONENT, caller: " + KDebug.getCallerClassName());
		Set<Class<?>> classes = reflections.getTypesAnnotatedWith(CYAN_COMPONENT.class);

		trace("LOOP through found classes, caller: " + KDebug.getCallerClassName());
		for (Class<?> c2 : classes) {
			trace("GET and INVOKE init method of the " + c2.getName() + " class, caller: "
					+ KDebug.getCallerClassName());
			try {
				Method m = c2.getDeclaredMethod("initComponent");
				m.setAccessible(true);
				m.invoke(null);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException
					| NoSuchMethodException e) {
				error("Failed to initialize component, class name: " + c2.getSimpleName(), e);
			}
		}
	}

	/**
	 * Pre-initialize the Cyan Core Loader
	 */
	public static void initLoader() {
		if (loader == null) loader = new DynamicURLClassLoader("CyanCore Internal Class Loader");
		else throw new IllegalStateException("CyanCore Class Loader is already registered!");
	}

	/**
	 * Initialize the component, gets called from initializeComponents()
	 */
	public static void initComponent() {
		trace("INITIALIZE Main CYAN Component, caller: " + KDebug.getCallerClassName());
		trace("CREATE DynamicURLClassLoaders, caller: " + KDebug.getCallerClassName());
		openloader = new DynamicURLClassLoader("Cyan Class Loader");
		agentloader = loader;
		cyancoreInitialized = true;
		trace("CLOSE FluidAPI Mappings Loader, caller: " + KDebug.getCallerClassName());
		Fluid.closeFluidLoader();
		phase = CyanLoadPhase.PRELOAD;
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

	static DynamicURLClassLoader loader = null;
	static DynamicURLClassLoader openloader = null;

	/**
	 * Gets the core class loader
	 * 
	 * @return Core DynamicURLClassLoader, try to avoid usage
	 */
	public static URLClassLoader getCoreClassLoader() {
		return loader;
	}

	/**
	 * Gets the userland class loader (for mods and such)
	 * 
	 * @return Userland DynamicURLClassLoader
	 */
	public static DynamicURLClassLoader getClassLoader() {
		return openloader;
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
		Fluid.loadAgent();
		Thread.currentThread().setContextClassLoader(loader);

		// TODO: Load mods
		
		info("Welcome to CYAN!");
		info("Loading CYAN information container...");
		CyanInfo.getDevStartDate();

		phase = CyanLoadPhase.INIT;
		// TODO: Initialize mods and check dependencies etc
		
		info("Loading class " + game + "...");
		Class<?> clas = loader.loadClass(game);
		
		phase = CyanLoadPhase.POSTINIT;
		// TODO: Post-initialize mods
		
		info("Starting minecraft...");
		
		phase = CyanLoadPhase.RUNTIME;
		// TODO: Call gameStart events for mods
		
		Method meth = clas.getMethod("main", String[].class);
		meth.invoke(null, new Object[] { args });
	}

	private static CyanSide side = null;
	private static CyanLoadPhase phase = CyanLoadPhase.NOT_READY;

	/**
	 * Set the current side name (CLIENT/SERVER), can only be set ONCE
	 * 
	 * @param side Side name
	 */
	public static void setSide(String side) {
		if (CyanCore.side == null) CyanCore.side = CyanSide.valueOf(side);
		else throw new IllegalStateException("This field cannot be changed after it has been assigned!");
	}

	/**
	 * Get the current side (CLIENT/SERVER)
	 * 
	 * @return Side name
	 */
	public static CyanSide getSide() {
		return side;
	}
	
	/**
	 * Get the current loading phase
	 * @return CyanLoadPhase object representing the loading phase
	 */
	public static CyanLoadPhase getCurrentPhase() {
		return phase;
	}
}
