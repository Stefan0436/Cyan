package org.asf.cyan.cornflower.gradle.utilities;

import java.io.Serializable;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.Core;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.Layout;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginAttribute;
import org.apache.logging.log4j.core.config.plugins.PluginElement;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.gradle.api.logging.LogLevel;

@Plugin(name = "Log4jToGradleAppender", category = Core.CATEGORY_NAME, elementType = Appender.ELEMENT_TYPE, printObject = true)
public class Log4jToGradleAppender extends AbstractAppender {
	org.gradle.api.logging.Logger gradleLogger;
	static boolean logInfo = false;
	static org.gradle.api.logging.Logger mainGradleLogger = null;

	/**
	 * Turns on logging of info in lifecycle log
	 */
	public static void logInfo() {
		logInfo = true;
	}

	/**
	 * Turns off logging of info in lifecycle log
	 */
	public static void noLogInfo() {
		logInfo = false;
	}

	public static void setGradleLogger(org.gradle.api.logging.Logger gradleLogger) {
		mainGradleLogger = gradleLogger;
	}

	protected Log4jToGradleAppender(String name, Filter filter, Layout<? extends Serializable> layout,
			boolean ignoreExceptions, Property[] properties, org.gradle.api.logging.Logger gradleLogger) {
		super(name, filter, layout, ignoreExceptions, properties);
		this.gradleLogger = gradleLogger;
	}

	@Override
	public void append(LogEvent event) {
		String msg = new String(this.getLayout().toByteArray(event));
		if (msg.endsWith(System.lineSeparator())) msg = msg.substring(0, msg.lastIndexOf(System.lineSeparator()));

		if (event.getLevel().equals(Level.DEBUG)) gradleLogger.log(LogLevel.DEBUG, msg);
		else if (event.getLevel().equals(Level.WARN)) gradleLogger.log(LogLevel.WARN, msg);
		else if (event.getLevel().equals(Level.INFO) && !logInfo) gradleLogger.log(LogLevel.INFO, msg);
		else if (event.getLevel().equals(Level.INFO) && logInfo) gradleLogger.log(LogLevel.LIFECYCLE, msg);
		else if (event.getLevel().equals(Level.FATAL) || event.getLevel().equals(Level.ERROR)) gradleLogger.log(LogLevel.ERROR, msg);
	}

	@SuppressWarnings("unchecked")
	@PluginFactory
	public static Log4jToGradleAppender createAppender(@PluginAttribute("name") String name,
            @PluginAttribute("ignoreExceptions") boolean ignoreExceptions,
            @SuppressWarnings("rawtypes") @PluginElement("Layout") Layout layout,
            @PluginElement("Filters") Filter filter) {
		return new Log4jToGradleAppender(name, filter, layout, ignoreExceptions, new Property[0], mainGradleLogger);
	}
}
