package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.core.CyanUpdateInfo;
import org.asf.cyan.cornflower.gradle.Cornflower;
import org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions.CornflowerMainExtension;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IModloader;
import org.asf.cyan.cornflower.gradle.utilities.GradleUtil;
import org.gradle.api.Project;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;

public class CyanModloader implements IModloader {

	public static final String infoPathTemplate = "/org/asf/cyan/CyanVersionHolder/%version%/CyanVersionHolder-%version%-versions.ccfg";

	public HashMap<String, String> libraries = new HashMap<String, String>();

	private String version;
	private Project project;
	private int api;

	public static final int BaseModding = 1 << 1;
	public static final int CoreMods = 1 << 2;
	public static final int FLUID = 1 << 3;
	public static final int CyanCore = 1 << 4;
	public static final int MTK = 1 << 5;
	public static final int ClassTrust = 1 << 6;
	public static final int FullCyanLoader = 1 << 7;

	private boolean hasAPI(int api) {
		return (api & this.api) == api;
	}

	public CyanModloader() {
	}

	public CyanModloader(Project proj, String version, int api) {
		project = proj;
		boolean latest = false;
		if (version.equals("latest")) {
			try {
				StringBuilder conf = new StringBuilder();
				URL u = new URL(CornflowerMainExtension.AerialWorksMaven + CyanInfo.infoPath);
				Scanner sc = new Scanner(u.openStream());
				while (sc.hasNext())
					conf.append(sc.nextLine() + System.lineSeparator());
				sc.close();

				CyanUpdateInfo versions = new CyanUpdateInfo(conf.toString());
				version = versions.latestStableVersion;
				if (version.isEmpty())
					version = versions.latestPreviewVersion;
				if (version.isEmpty())
					version = versions.latestBetaVersion;
				if (version.isEmpty())
					version = versions.latestAlphaVersion;
			} catch (IOException e) {
				latest = true;
			}
		}

		this.api = api;

		File infoDir = GradleUtil.getCacheFolder(Cornflower.class, project, "cyanloader-modloader-info");
		File versionFile = new File(infoDir, "version.info");
		if (versionFile.exists() && latest) {
			try {
				version = new String(Files.readAllBytes(versionFile.toPath()));
			} catch (IOException e) {
			}
		} else if (latest) {
			throw new RuntimeException("Failed to resolve latest cyan version");
		}

		try {
			Files.write(versionFile.toPath(), version.getBytes());
		} catch (IOException e) {
		}

		File manifest = new File(infoDir, "mainfest-" + version + ".ccfg");
		String config = "";
		try {
			URL u = new URL(CornflowerMainExtension.AerialWorksMaven + infoPathTemplate.replace("%version%", version));
			StringBuilder conf = new StringBuilder();
			Scanner sc = new Scanner(u.openStream());
			while (sc.hasNext())
				conf.append(sc.nextLine() + System.lineSeparator());
			sc.close();
			config = conf.toString();
		} catch (IOException e) {
			if (manifest.exists()) {
				try {
					config = new String(Files.readAllBytes(manifest.toPath()));
				} catch (IOException e1) {
					throw new RuntimeException(
							"Failed to resolve cyan library information for version '" + version + "'");
				}
			} else {
				throw new RuntimeException("Failed to resolve cyan library information for version '" + version + "'");
			}
		}
		try {
			Files.write(manifest.toPath(), config.getBytes());
		} catch (IOException e) {
		}

		libraries = new CyanUpdateInfo(config).libraryVersions;
		this.version = version;
	}

	@Override
	public String name() {
		return "Cyan";
	}

	@Override
	public String fullName() {
		return "CyanLoader";
	}

	@Override
	public IModloader newInstance(Project project, String version, int api) {
		return new CyanModloader(project, version, api);
	}

	@Override
	public void addRepositories(RepositoryHandler repositories) {
		repositories.mavenCentral();

		repositories.maven((repo) -> {
			repo.setName("AerialWorks");
			repo.setUrl(CornflowerMainExtension.AerialWorksMaven);
		});
	}

	@Override
	public String[] addDependencies(ConfigurationContainer configurations) {
		ArrayList<String> deps = new ArrayList<String>();
		addDependency("org.asf.cyan", "CCFG", libraries.get("CCFG"), deps);
		addDependency("org.asf.cyan", "CyanComponents", libraries.get("CyanComponents"), deps);
		addDependency("org.asf.cyan", "CyanUtil", libraries.get("CyanUtil"), deps);

		if (hasAPI(FullCyanLoader))
			addDependency("org.asf.cyan", "CyanLoader", libraries.get("CyanLoader"), deps);

		if (hasAPI(BaseModding) && !hasAPI(FullCyanLoader))
			addDependency("org.asf.cyan", "CyanModding", libraries.get("CyanLoader"), deps);
		if (hasAPI(CoreMods) && !hasAPI(FullCyanLoader))
			addDependency("org.asf.cyan", "CyanCoreModding", libraries.get("CyanLoader"), deps);
		if (hasAPI(FLUID))
			addDependency("org.asf.cyan", "Fluid", libraries.get("Fluid"), deps);
		if (hasAPI(CyanCore))
			addDependency("org.asf.cyan", "CyanCore", libraries.get("CyanCore"), deps);

		if (hasAPI(MTK))
			addDependency("org.asf.cyan", "MTK", libraries.get("MTK"), deps);
		if (hasAPI(ClassTrust))
			addDependency("org.asf.cyan", "ClassTrust", libraries.get("ClassTrust"), deps);

		return deps.toArray(new String[0]);
	}

	private void addDependency(String group, String name, String version, ArrayList<String> deps) {
		project.getDependencies().add("implementation", group + ":" + name + ":" + version);
		deps.add(group + ":" + name + ":" + version);
	}

	public String getVersion() {
		return version;
	}

}
