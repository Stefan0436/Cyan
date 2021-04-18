package org.asf.cyan.cornflower.gradle.utilities.modding;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions.CornflowerMainExtension.PlatformConfiguration;
import org.asf.cyan.cornflower.gradle.tasks.CtcTask;
import org.asf.cyan.cornflower.gradle.tasks.RiftJarTask;
import org.asf.cyan.cornflower.gradle.utilities.modding.manifests.CyanModfileManifest;
import org.asf.cyan.security.TrustContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.RegularFile;
import org.gradle.api.provider.Provider;

import groovy.lang.Closure;

public class CyanModfileManifestGenerator {
	private CyanModfileManifest manifest = new CyanModfileManifest();
	private HashMap<CtcTask, String> ctcTasks = new HashMap<CtcTask, String>();

	public static CyanModfileManifest fromClosure(Closure<?> closure) {
		CyanModfileManifestGenerator owner = new CyanModfileManifestGenerator();
		closure.setDelegate(owner);
		closure.call();

		return owner.toManifest();
	}

	private CyanModfileManifest toManifest() {
		manifest.setCtcTasks(ctcTasks);
		return manifest;
	}

	public void jar(RiftJarTask[] jars) throws IOException {
		for (RiftJarTask rift : jars)
			jar(rift);
	}

	public void jar(RiftJarTask[] jars, String outputDir) throws IOException {
		for (RiftJarTask rift : jars)
			jar(rift, outputDir);
	}

	public void jar(RiftJarTask riftJar) throws IOException {
		if (riftJar.platform != null || riftJar.side != null) {
			jar(riftJar.getArchiveFile(), riftJar.platform, riftJar.side, "jars");
		} else {
			throw new RuntimeException("Cannot add RIFT jar task '" + riftJar.getName()
					+ "' to the mod manifest as it does not define a platform and/or game side, don't know what to do with it.");
		}
	}

	public void jar(RiftJarTask riftJar, String outputDir) throws IOException {
		if (riftJar.platform != null || riftJar.side != null) {
			jar(riftJar.getArchiveFile(), riftJar.platform, riftJar.side, outputDir);
		} else {
			throw new RuntimeException("Cannot add RIFT jar task '" + riftJar.getName()
					+ "' to the mod manifest as it does not define a platform and/or game side, don't know what to do with it.");
		}
	}

	public void jar(File jarfile) {
		jar(jarfile, "jars");
	}

	public void jar(File[] files) {
		for (File file : files)
			jar(file);
	}

	public void jar(RegularFile[] files) {
		for (RegularFile file : files)
			jar(file);
	}

	public void jar(Provider<RegularFile>[] files) {
		for (Provider<RegularFile> file : files)
			jar(file);
	}

	public void jar(Iterable<File> files) {
		for (File file : files)
			jar(file);
	}

	public void jar(RegularFile jarfile) {
		jar(jarfile.getAsFile());
	}

	public void jar(Provider<RegularFile> jarfile) {
		jar(jarfile.get());
	}

	public void jar(RegularFile jarfile, String outputDir) {
		jar(jarfile.getAsFile(), outputDir);
	}

	public void jar(Provider<RegularFile> jarfile, String outputDir) {
		jar(jarfile.get(), outputDir);
	}

	public void jar(RegularFile jarfile, LaunchPlatform platform) {
		jar(jarfile.getAsFile(), platform);
	}

	public void jar(Provider<RegularFile> jarfile, LaunchPlatform platform) {
		jar(jarfile.get(), platform);
	}

	public void jar(RegularFile jarfile, LaunchPlatform platform, String outputDir) {
		jar(jarfile.getAsFile(), platform, outputDir);
	}

	public void jar(Provider<RegularFile> jarfile, LaunchPlatform platform, String outputDir) {
		jar(jarfile.get(), platform, outputDir);
	}

	public void jar(RegularFile jarfile, LaunchPlatform platform, GameSide side) {
		jar(jarfile.getAsFile(), platform, side);
	}

	public void jar(Provider<RegularFile> jarfile, LaunchPlatform platform, GameSide side) {
		jar(jarfile.get(), platform, side);
	}

	public void jar(RegularFile jarfile, LaunchPlatform platform, GameSide side, String outputDir) {
		jar(jarfile.getAsFile(), platform, side, outputDir);
	}

	public void jar(Provider<RegularFile> jarfile, LaunchPlatform platform, GameSide side, String outputDir) {
		jar(jarfile.get(), platform, side, outputDir);
	}

	public void jar(File jarfile, String outputDir) {
		manifest.addJar(jarfile, null, null, outputDir);
	}

	public void jar(File jarfile, LaunchPlatform platform, String outputDir) {
		manifest.addJar(jarfile, (platform == null ? null : platform.toString()), null, outputDir);
	}

	public void jar(File jarfile, LaunchPlatform platform, GameSide side, String outputDir) {
		manifest.addJar(jarfile, (platform == null ? null : platform.toString()),
				(side == null ? null : side.toString()), outputDir);
	}

	public void jar(File jarfile, LaunchPlatform platform) {
		manifest.addJar(jarfile, (platform == null ? null : platform.toString()), null, "jars");
	}

	public void jar(File jarfile, LaunchPlatform platform, GameSide side) {
		manifest.addJar(jarfile, (platform == null ? null : platform.toString()),
				(side == null ? null : side.toString()), "jars");
	}

	public void trust_container(CtcTask task, String remote) throws IOException {
		ctcTasks.put(task, remote);
	}

	public void trust_container(File trust, String remote) throws IOException {
		TrustContainer cont = TrustContainer.importContainer(trust);
		trust_container(cont.getContainerName(), cont.getVersion(), remote);
	}

	public void trust_container(String name, String version, String remote) {
		trust_container(name + "@" + version, remote);
	}

	public void trust_container(String name, String remote) {
		manifest.trustContainers.put(name, remote);
	}

	public void mod_version(String version) {
		manifest.version = version;
	}

	public void mod_class(String name) {
		manifest.modClassName = name;
	}

	public void mod_package(String pkg) {
		manifest.modClassPackage = pkg;
	}

	public void modid(String id) {
		manifest.modId = id;
	}

	public void modgroup(String group) {
		manifest.modGroup = group;
	}

	public void game_version(String version, String message) {
		manifest.gameVersionRegex = version;
		manifest.gameVersionMessage = message;
	}

	public void display_name(String name) {
		manifest.displayName = name;
	}

	public void dependency(String mod, String versionCheckString) {
		manifest.dependencies.put(mod, versionCheckString);
	}

	public void optional_dependency(String mod, String versionCheckString) {
		manifest.optionalDependencies.put(mod, versionCheckString);
	}

	public void maven_dependency(String group, String name, String version) {
		HashMap<String, String> deps = manifest.mavenDependencies.get(group);
		if (deps == null) {
			deps = new HashMap<String, String>();
			manifest.mavenDependencies.put(group, deps);
		}

		deps.put(name, version);
	}

	public void maven_dependency(Dependency dep) {
		HashMap<String, String> deps = manifest.mavenDependencies.get(dep.getGroup());
		if (deps == null) {
			deps = new HashMap<String, String>();
			manifest.mavenDependencies.put(dep.getGroup(), deps);
		}

		deps.put(dep.getName(), dep.getVersion());
	}

	public void maven_dependency(Iterable<String> dependencies) {
		for (String dep : dependencies)
			maven_dependency(dep);
	}

	public void maven_dependency(Dependency[] dependencies) {
		for (Dependency dep : dependencies)
			maven_dependency(dep);
	}

	public void maven_dependency(String dependency) {
		if (!dependency.matches(".*:.*:.*"))
			throw new IllegalArgumentException(
					"Invalid maven dependency format: " + dependency + ", expected group:name:version");

		String[] info = dependency.split(":");
		maven_dependency(info[0], info[1], info[2]);
	}

	public void maven_dependency(String[] dependencies) {
		for (String dep : dependencies)
			maven_dependency(dep);
	}

	public void repository(String name, String url) {
		manifest.mavenRepositories.put(name, url);
	}

	public void repository(String name, URL url) {
		manifest.mavenRepositories.put(name, url.toString());
	}

	public void description(String description) {
		if (!description.startsWith("\n"))
			description = "\n" + description;
		if (!description.endsWith("\n"))
			description += "\n";

		manifest.fallbackDescription = description;
	}

	public void description(String key, String fallback) {
		if (!fallback.startsWith("\n"))
			fallback = "\n" + fallback;
		if (!fallback.endsWith("\n"))
			fallback += "\n";

		manifest.descriptionLanguageKey = key;
		manifest.fallbackDescription = fallback;
	}

	public void platform(LaunchPlatform platform, String version) {
		manifest.platforms.put(platform.toString().toUpperCase(), version);
	}

	public void platform(IPlatformConfiguration platform) {
		manifest.platforms.put(platform.getPlatform().toString().toUpperCase(), platform.getCommonMappingsVersion());
	}

	public void platform(IPlatformConfiguration[] platforms) {
		for (IPlatformConfiguration platform : platforms)
			platform(platform);
	}

	public void platform(Iterable<IPlatformConfiguration> platforms) {
		for (IPlatformConfiguration platform : platforms)
			platform(platform);
	}

	public void platform(PlatformConfiguration platforms) {
		for (IPlatformConfiguration platform : platforms.all)
			platform(platform);
	}

}