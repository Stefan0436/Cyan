package org.asf.cyan.api.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.stream.Stream;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.apache.logging.log4j.spi.AbstractLogger;
import org.apache.logging.log4j.spi.ExtendedLogger;

/**
 * 
 * CyanComponents, a runtime component detection system.<br />
 * <br />
 * <b>Warning:</b> CyanComponents needs a implementation to run.<br />
 * See implementations such as CyanCore for more info.<br/>
 * <br/>
 * For automatic detection, the {@link CYAN_COMPONENT @CYAN_COMPONENT}
 * annotations is required.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class CyanComponent {
	protected static HashMap<Class<?>, HashMap<Level, HashMap<String, Level>>> itms = new HashMap<Class<?>, HashMap<Level, HashMap<String, Level>>>();
	protected static ExtendedLogger LOG;

	/**
	 * 
	 * Stacktrace-based detection of a method caller
	 * 
	 * @author Stefan0436 - AerialWorks Software Foundation
	 *
	 */
	protected static class CallTrace {

		/**
		 * Retrieves the caller class of the method calling this function.
		 */
		public static Class<?> traceCall() {
			return traceCall(2);
		}

		/**
		 * Retrieves the caller class of the method calling this function.
		 * 
		 * @param componentDepth The amount of CyanComponents needed in the stack before
		 *                       one is actually returned. (other classes are
		 *                       immediately returned)
		 */
		public static Class<?> traceCall(int componentDepth) {
			StackTraceElement[] stack = Thread.currentThread().getStackTrace();

			int currentDepth = 0;
			for (StackTraceElement element : stack) {
				if (!element.getClassName().equals(Thread.class.getTypeName())
						&& !element.getClassName().equals(CallTrace.class.getTypeName())) {
					Class<?> cls;
					try {
						cls = Class.forName(element.getClassName());
					} catch (ClassNotFoundException e) {
						continue;
					}

					if (CyanComponent.class.isAssignableFrom(cls) && currentDepth != componentDepth) {
						currentDepth++;
						continue;
					} else {
						return cls;
					}
				}
			}

			return traceCall(1);
		}

		/**
		 * Retrieves the caller class simple name of the method calling this function.
		 */
		public static String traceCallName() {
			return traceCall().getSimpleName();
		}

		/**
		 * Retrieves the caller class simple name of the method calling this function.
		 * 
		 * @param componentDepth The amount of CyanComponents needed in the stack before
		 *                       one is actually returned. (other classes are
		 *                       immediately returned)
		 */
		public static String traceCallName(int componentDepth) {
			return traceCall(componentDepth).getSimpleName();
		}

	}

	// Credits to Denys Seguret on SO: https://stackoverflow.com/a/11306854, (ascii
	// issues, had to change the name here, sorry)
	// modified a bit of the code
	/**
	 * @deprecated Use {@link CallTrace CallTrace} instead.
	 */
	@Deprecated
	protected static class KDebug {
		@Deprecated
		public static String getCallerClassName() {
			StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
			int depth = 0;
			for (int i = 1; i < stElements.length; i++) {
				StackTraceElement ste = stElements[i];
				if (!ste.getClassName().equals(KDebug.class.getTypeName())
						&& (!ste.getClassName().equals(CyanComponent.class.getTypeName()))
						&& ste.getClassName().indexOf("java.lang.Thread") != 0) {
					try {
						Class<?> cls = Class.forName(ste.getClassName());
						if (CyanComponent.class.isAssignableFrom(cls) && depth != 2) {
							depth++;
							continue;
						}
						return cls.getSimpleName();
					} catch (ClassNotFoundException e) {
					}
				}
			}
			return null;
		}

		@Deprecated
		static String getClassName() {
			StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
			for (int i = 1; i < stElements.length; i++) {
				StackTraceElement ste = stElements[i];
				if (!ste.getClassName().equals(KDebug.class.getTypeName())
						&& (!ste.getClassName().equals(CyanComponent.class.getTypeName()))
						&& ste.getClassName().indexOf("java.lang.Thread") != 0) {
					try {
						Class<?> cls = Class.forName(ste.getClassName());
						return cls.getSimpleName();
					} catch (ClassNotFoundException e) {
					}
				}
			}
			return null;
		}

		@Deprecated
		public static Class<?> getCallerClass() {
			return getCallerClass(2);
		}

		@Deprecated
		public static Class<?> getCallerClass(int maxdepth) {
			StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
			int depth = 0;
			for (int i = 1; i < stElements.length; i++) {
				StackTraceElement ste = stElements[i];
				if (!ste.getClassName().equals(KDebug.class.getTypeName())
						&& (!ste.getClassName().equals(CyanComponent.class.getTypeName()))
						&& ste.getClassName().indexOf("java.lang.Thread") != 0) {
					try {
						Class<?> cls = Class.forName(ste.getClassName());
						if (CyanComponent.class.isAssignableFrom(cls) && depth != maxdepth) {
							depth++;
							continue;
						}
						return cls;
					} catch (ClassNotFoundException e) {
					}
				}
			}
			return null;
		}

		@Deprecated
		static Class<?> getCallClass() {
			StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
			for (int i = 1; i < stElements.length; i++) {
				StackTraceElement ste = stElements[i];
				if (!ste.getClassName().equals(KDebug.class.getTypeName())
						&& (!ste.getClassName().equals(CyanComponent.class.getTypeName()))
						&& ste.getClassName().indexOf("java.lang.Thread") != 0) {
					try {
						Class<?> cls = Class.forName(ste.getClassName());
						return cls;
					} catch (ClassNotFoundException e) {
					}
				}
			}
			return null;
		}
	}

	static void logtrc(String message, Level level) {
		if (LOG == null)
			initLogger();

		String markerName = CallTrace.traceCallName(2);
		if (Stream.of(CallTrace.traceCall(2).getDeclaredMethods()).anyMatch(t -> t.getName() == "getMarker")) {
			Method meth = Stream.of(CallTrace.traceCall(2).getDeclaredMethods()).filter(t -> t.getName() == "getMarker")
					.findFirst().get();
			try {
				meth.setAccessible(true);
				markerName = (String) meth.invoke(null);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			}
		}
		if (markerName.startsWith("Cyan") && !markerName.substring("Cyan".length()).equals("Component")) {
			markerName = markerName.substring("Cyan".length());
		}
		if (markerName.startsWith("Minecraft") && !markerName.substring("Minecraft".length()).equals("Toolkit")) {
			markerName = markerName.substring("Minecraft".length());
		}
		markerName = markerName.replaceAll("Component", "");
		markerName = markerName.replaceAll("Toolkit", "");
		markerName = markerName.toUpperCase();
		Marker m = MarkerManager.getMarker(markerName);
		for (Class<?> key : itms.keySet()) {
			Level lv2 = level;
			HashMap<Level, HashMap<String, Level>> tracker = itms.get(key);
			Level lv = tracker.keySet().iterator().next();
			if (LOG.isEnabled(lv2) && lv.intLevel() >= lv2.intLevel()) {
				HashMap<String, Level> msgs = tracker.get(lv);
				msgs.put(message, lv2);
				tracker.put(lv, msgs);
				itms.put(key, tracker);
			}
		}
		LOG.trace(m, message);
	}

	static void logwm(String message, Level level, Throwable data) {
		if (LOG == null)
			initLogger();
		trace("SEND " + level.toString() + " message from class " + CallTrace.traceCallName(2) + ", message: " + message);
		trace("PARSE marker of the " + CallTrace.traceCallName(2) + " class");
		String markerName = CallTrace.traceCallName(2);
		if (Stream.of(CallTrace.traceCall(2).getDeclaredMethods()).anyMatch(t -> t.getName() == "getMarker")) {
			trace("OVERRIDE marker name by " + CallTrace.traceCallName(2) + ", getMarker method was found");
			Method meth = Stream.of(CallTrace.traceCall(2).getDeclaredMethods()).filter(t -> t.getName() == "getMarker")
					.findFirst().get();
			try {
				meth.setAccessible(true);
				markerName = (String) meth.invoke(null);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			}
		}
		if (markerName.startsWith("Cyan") && !markerName.substring("Cyan".length()).equals("Component")) {
			trace(CallTrace.traceCallName(2) + " is a CYAN component, stripping 'Cyan' from marker name");
			markerName = markerName.substring("Cyan".length());
		}
		if (markerName.startsWith("Minecraft") && !markerName.substring("Minecraft".length()).equals("Toolkit")) {
			markerName = markerName.substring("Minecraft".length());
		}
		trace("REMOVE all 'Component' and 'Toolkit' strings in " + markerName);
		markerName = markerName.replaceAll("Component", "");
		markerName = markerName.replaceAll("Toolkit", "");
		trace("CONVERT " + markerName + " to uppercase");
		markerName = markerName.toUpperCase();
		trace("GET marker " + markerName);
		Marker m = MarkerManager.getMarker(markerName);
		trace("PRINT " + level.toString() + " message, message: " + message);
		for (Class<?> key : itms.keySet()) {
			Level lv2 = level;
			HashMap<Level, HashMap<String, Level>> tracker = itms.get(key);
			Level lv = tracker.keySet().iterator().next();
			if (LOG.isEnabled(lv2) && lv.intLevel() >= lv2.intLevel()) {
				HashMap<String, Level> msgs = tracker.get(lv);
				msgs.put(message, lv2);
				tracker.put(lv, msgs);
				itms.put(key, tracker);
			}
		}
		LOG.logIfEnabled(AbstractLogger.class.getTypeName(), level, m, message, data);
	}

	/**
	 * Print component trace debug message
	 * 
	 * @param message The message
	 */
	protected static void trace(String message) {
		logtrc(message, Level.TRACE);
	}

	/**
	 * Print component debug message
	 * 
	 * @param message The message
	 */
	protected static void debug(String message) {
		logwm(message, Level.DEBUG, null);
	}

	/**
	 * Print component info message
	 * 
	 * @param message The message
	 */
	protected static void info(String message) {
		logwm(message, Level.INFO, null);
	}

	/**
	 * Print component warning message
	 * 
	 * @param message The message
	 */
	protected static void warn(String message) {
		logwm(message, Level.WARN, null);
	}

	/**
	 * Print component warning message
	 * 
	 * @param message The message
	 * @param t       The throwable
	 */
	protected static void warn(String message, Throwable t) {
		logwm(message, Level.WARN, t);
	}

	/**
	 * Print component error message
	 * 
	 * @param message The message
	 */
	protected static void error(String message) {
		logwm(message, Level.ERROR, null);
	}

	/**
	 * Print component error message
	 * 
	 * @param message The message
	 * @param t       The throwable
	 */
	protected static void error(String message, Throwable t) {
		logwm(message, Level.ERROR, t);
	}

	/**
	 * Print component fatal error message
	 * 
	 * @param message The message
	 */
	protected static void fatal(String message) {
		logwm(message, Level.FATAL, null);
	}

	/**
	 * Print component fatal error message
	 * 
	 * @param message The message
	 * @param t       The throwable
	 */
	protected static void fatal(String message, Throwable t) {
		logwm(message, Level.FATAL, t);
	}

	protected static void initLogger() throws UnsupportedOperationException {
		if (LOG == null) {
			LOG = (ExtendedLogger) LogManager.getLogger("CYAN");
		} else {
			throw new UnsupportedOperationException("Logger already initialized.");
		}
	}

	protected static ClassLoader agentloader;

	public static ClassLoader getAgentClassLoader() {
		return agentloader;
	}

	/**
	 * Gets the component classes, only use this if trying to implement the
	 * component API without CyanCore, this will not return anything if not
	 * overridden. (Override only)
	 */
	protected Class<?>[] getComponentClasses() {
		return new Class<?>[0];
	}

	/**
	 * Prepares CyanComponents for loading, only use this if trying to implement the
	 * component API without CyanCore. Setup logging and class loaders, jar loading
	 * etc... (Override only)
	 */
	protected void setupComponents() {
		throw new IllegalStateException("Not an implementation for the CyanComponents intialization system.");
	}

	/**
	 * Called after setupComponetnts (Override only)
	 */
	protected void preInitAllComponents() {
		throw new IllegalStateException("Not an implementation for the CyanComponents intialization system.");
	}

	/**
	 * Initializes components, only use this if trying to implement the component
	 * API without CyanCore. Make sure the classes have been annotated.
	 */
	protected void initializeComponentClasses() {
		assignImplementation();

		setupComponents();
		debug("Preparing to load components...");
		preInitAllComponents();
		debug("Finding components...");
		Class<?>[] components = getComponentClasses();
		debug("Starting components implementation '" + CallTrace.traceCall(2).getSimpleName() + "'...");

		trace("LOOP through found classes, caller: " + CallTrace.traceCallName());
		for (Class<?> c2 : components) {
			debug("Loading CyanComponent " + c2.getTypeName() + "...");
			trace("GET and INVOKE init method of the " + c2.getName() + " class, caller: "
					+ CallTrace.traceCallName());
			try {
				Method m = c2.getDeclaredMethod("initComponent");
				m.setAccessible(true);
				m.invoke(null);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | SecurityException
					| NoSuchMethodException e) {
				error("Failed to initialize component, class name: " + c2.getSimpleName(), e);
			}
		}
		debug("Finalizing...");
		finalizeComponents();
	}

	protected void assignImplementation() {
		if (componentImplementation == null)
			componentImplementation = this;
	}

	/**
	 * Finalizes the component system, only use this if trying to implement the
	 * component API without CyanCore. (Override only)
	 */
	protected void finalizeComponents() {
		throw new IllegalStateException("Not an implementation for the CyanComponents intialization system.");
	}

	private static CyanComponent componentImplementation;

	protected static CyanComponent getMainImplementation() {
		return componentImplementation;
	}

	/**
	 * Called in some cases to find other classes of a certain supertype or
	 * interface. (Override only)
	 */
	@SuppressWarnings("unchecked")
	protected <T> Class<T>[] findClassesInternal(Class<T> interfaceOrSupertype) {
		return (Class<T>[]) new Class<?>[0];
	}

	/**
	 * Finds classes of a certain supertype or interface. Needs an implementation to
	 * work.
	 * 
	 * @param implementation       Implementation to use.
	 * @param interfaceOrSupertype Class supertype or interface.
	 * @return Array of matching classes.
	 */
	protected static <T> Class<T>[] findClasses(CyanComponent implementation, Class<T> interfaceOrSupertype) {
		return implementation.findClassesInternal(interfaceOrSupertype);
	}

}
