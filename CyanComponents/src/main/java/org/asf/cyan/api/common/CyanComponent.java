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

public abstract class CyanComponent {
	protected static HashMap<Class<?>, HashMap<Level, HashMap<String, Level>>> itms = new HashMap<Class<?>, HashMap<Level, HashMap<String, Level>>>();
	protected static ExtendedLogger LOG;

	// Credits to Denys Séguret on SO: https://stackoverflow.com/a/11306854,
	// modified a bit of the code
	protected static class KDebug {
		public static String getCallerClassName() {
			StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
			for (int i = 1; i < stElements.length; i++) {
				StackTraceElement ste = stElements[i];
				if (!ste.getClassName().equals(KDebug.class.getTypeName())
						&& (!ste.getClassName().equals(CyanComponent.class.getTypeName()))
						&& ste.getClassName().indexOf("java.lang.Thread") != 0) {
					try {
						return Class.forName(ste.getClassName()).getSimpleName();
					} catch (ClassNotFoundException e) {
					}
				}
			}
			return null;
		}

		public static Class<?> getCallerClass() {
			StackTraceElement[] stElements = Thread.currentThread().getStackTrace();
			for (int i = 1; i < stElements.length; i++) {
				StackTraceElement ste = stElements[i];
				if (!ste.getClassName().equals(KDebug.class.getTypeName())
						&& (!ste.getClassName().equals(CyanComponent.class.getTypeName()))
						&& ste.getClassName().indexOf("java.lang.Thread") != 0) {
					try {
						return Class.forName(ste.getClassName());
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

		String markerName = KDebug.getCallerClassName();
		if (Stream.of(KDebug.getCallerClass().getDeclaredMethods()).anyMatch(t -> t.getName() == "getMarker")) {
			Method meth = Stream.of(KDebug.getCallerClass().getDeclaredMethods())
					.filter(t -> t.getName() == "getMarker").findFirst().get();
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
		trace("SEND " + level.toString() + " message from class " + KDebug.getCallerClassName() + ", message: "
				+ message);
		trace("PARSE marker of the " + KDebug.getCallerClassName() + " class");
		String markerName = KDebug.getCallerClassName();
		if (Stream.of(KDebug.getCallerClass().getDeclaredMethods()).anyMatch(t -> t.getName() == "getMarker")) {
			trace("OVERRIDE marker name by " + KDebug.getCallerClassName() + ", getMarker method was found");
			Method meth = Stream.of(KDebug.getCallerClass().getDeclaredMethods())
					.filter(t -> t.getName() == "getMarker").findFirst().get();
			try {
				meth.setAccessible(true);
				markerName = (String) meth.invoke(null);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			}
		}
		if (markerName.startsWith("Cyan") && !markerName.substring("Cyan".length()).equals("Component")) {
			trace(KDebug.getCallerClassName() + " is a CYAN component, stripping 'Cyan' from marker name");
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
}
