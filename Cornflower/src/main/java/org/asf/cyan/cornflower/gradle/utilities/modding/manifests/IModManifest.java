package org.asf.cyan.cornflower.gradle.utilities.modding.manifests;

import java.io.File;
import java.util.Map;

public interface IModManifest {
	public Map<File, String> getJars();
	public void addJar(File jar, String outDir, String platform, String side);
}
