package org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions;

import org.asf.cyan.cornflower.gradle.utilities.IProjectExtension;

public class CornflowerMainExtension implements IProjectExtension {
	public static final Class<org.asf.cyan.cornflower.classpath.util.SourceType> SourceType = org.asf.cyan.cornflower.classpath.util.SourceType.class;
	public static final Class<org.asf.cyan.cornflower.classpath.util.EntryType> EntryType = org.asf.cyan.cornflower.classpath.util.EntryType.class;
	public static final Class<org.asf.cyan.cornflower.classpath.util.PathPriority> PathPriority = org.asf.cyan.cornflower.classpath.util.PathPriority.class;

	public static final Class<?> EclipseLaunchGenerator = org.asf.cyan.cornflower.gradle.tasks.EclipseLaunchGenerator.class;
	public static final Class<?> CtcUtil = org.asf.cyan.cornflower.gradle.tasks.CtcTask.class;

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
}
