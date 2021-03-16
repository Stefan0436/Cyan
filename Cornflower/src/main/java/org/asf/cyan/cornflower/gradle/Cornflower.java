package org.asf.cyan.cornflower.gradle;

import java.io.File;
import java.net.MalformedURLException;

import org.asf.cyan.cornflower.gradle.utilities.ExtendedPlugin;
import org.gradle.api.Project;
import org.gradle.internal.classloader.VisitableURLClassLoader;

/**
 * Cornflower Gradle Plugin, DO NOT USE OUTSIDE OF GRADLE
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class Cornflower extends ExtendedPlugin {
	public Cornflower() throws MalformedURLException {
		System.setProperty("log4j2.configurationFile", CornflowerCore.class.getResource("/log4j2-cornflower.xml").toString());
		try {
			Class.forName("org.asf.cyan.tests.TestCommand");
			VisitableURLClassLoader urlClassLoader = (VisitableURLClassLoader) Thread.currentThread()
					.getContextClassLoader();
			for (File f : new File(System.getProperty("testLibraries")).listFiles()) {
				urlClassLoader.addURL(f.toURI().toURL());
			}
		} catch (ClassNotFoundException e) {
		}
	}

	@Override
	protected void applyPlugin(Project target) {
		CornflowerCore.load(target, getSharedCacheFolder("Cornflower-MTK"));
	}
}
