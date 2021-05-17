package org.asf.cyan.cornflower.gradle.utilities.modding.manifests;

import java.util.Map;

import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

public interface IModManifest {
	public Map<Provider<RegularFile>, String> getJars();
	public void addJar(Provider<RegularFile> jar, String outDir, String platform, String side, String loaderVersion, String gameVersion, String mappingsVersion);
}
