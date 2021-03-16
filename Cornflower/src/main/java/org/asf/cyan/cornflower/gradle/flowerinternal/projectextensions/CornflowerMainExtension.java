package org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions;

import org.asf.cyan.cornflower.gradle.utilities.IProjectExtension;

public class CornflowerMainExtension implements IProjectExtension {
	public static final Class<org.asf.cyan.cornflower.classpath.util.SourceType> SourceType = org.asf.cyan.cornflower.classpath.util.SourceType.class;
	public static final Class<org.asf.cyan.cornflower.classpath.util.EntryType> EntryType = org.asf.cyan.cornflower.classpath.util.EntryType.class;
	public static final Class<org.asf.cyan.cornflower.classpath.util.PathPriority> PathPriority = org.asf.cyan.cornflower.classpath.util.PathPriority.class;
	
	public static final Class<?> EclipseLaunchGenerator = org.asf.cyan.cornflower.gradle.tasks.EclipseLaunchGenerator.class;
}
