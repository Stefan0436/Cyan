package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.nio.file.Files;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.CyanModloader;
import org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions.CornflowerMainExtension;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGame;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGameExecutionContext;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IModloader;
import org.asf.cyan.cornflower.gradle.tasks.CmfTask;
import org.asf.cyan.cornflower.gradle.tasks.EclipseLaunchGenerator;
import org.asf.cyan.cornflower.gradle.utilities.modding.manifests.CyanModfileManifest;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.dsl.RepositoryHandler;

import groovy.lang.Closure;

public class MinecraftGameProvider implements IGame {

	private String version;
	public MinecraftVersionInfo gameVersion;

	private CyanModloader modloader;

	public MinecraftGameProvider() {
	}

	public MinecraftGameProvider(Project project, String version, CyanModloader modloader) {
		this.version = version;
		this.modloader = modloader;

		gameVersion = MinecraftVersionToolkit.getVersion(version);
		if (gameVersion == null)
			gameVersion = new MinecraftVersionInfo(version, MinecraftVersionType.UNKNOWN, null, OffsetDateTime.now());
	}

	@Override
	public String name() {
		return "minecraft";
	}

	@Override
	public IGame newInstance(Project proj, String version, IModloader modloader) {
		return new MinecraftGameProvider(proj, version, (CyanModloader) modloader);
	}

	@Override
	public Class<? extends IModloader> modloader() {
		return CyanModloader.class;
	}

	@Override
	public void addRepositories(RepositoryHandler repositories) {

	}

	@Override
	public void addDependencies(ConfigurationContainer configurations) {

	}

	@Override
	public IGameExecutionContext[] getContexts() {
		return new IGameExecutionContext[] { new ClientGame(modloader), new ServerGame(modloader) };
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addTasks(Project project, IGameExecutionContext[] contexts, ArrayList<File> dependencies,
			ArrayList<File> sourceLookup) {
		EclipseLaunchGenerator launchGen = (EclipseLaunchGenerator) project.getTasks()
				.getByName("createEclipseLaunches");
		launchGen.disable = true;

		final Configuration<?> manifest;
		final boolean coremod;

		Class<Configuration> cls = null;
		try {
			cls = (Class<Configuration>) Class
					.forName("org.asf.cyan.cornflower.gradle.utilities.modding.manifests.CyanModfileManifest");
		} catch (ClassNotFoundException e) {
		}

		if (cls != null) {
			Object[] info = processCmf(project, cls);
			coremod = (boolean) info[0];
			manifest = (Configuration<?>) info[1];
		} else {
			manifest = null;
			coremod = false;
		}

		int i = 1;
		if (manifest != null) {
			cyanRunManifestTask(project, manifest, coremod);
		}
		for (IGameExecutionContext side : contexts) {
			String name = "createSidedLaunch" + i++;

			if (side instanceof ILaunchProvider) {
				name = ((ILaunchProvider) side).launchName();
			}

			project.task(Map.of("type", CornflowerMainExtension.EclipseLaunchGenerator), name,
					new ConfigureClosure<EclipseLaunchGenerator>(project, (tsk) -> {
						if (manifest != null) {
							cmfSupportLaunch(project, tsk, manifest, coremod);
						}

						tsk.name("Launch " + side.name() + " (Deobfuscated)");
						tsk.workingDir(new File(project.getRootDir(), "run/" + side.name()));

						tsk.tags.put("cyan.debug.launch", "true");
						tsk.sourceLookup(project);
						tsk.classpath(dependencies);
						tsk.sourceLookup(sourceLookup);
						tsk.main(side.mainClass());

						URL location = getClass().getProtectionDomain().getCodeSource().getLocation();
						try {
							tsk.classpath(new File(location.toURI()));
							tsk.sourceLookup(new File(location.toURI()));
						} catch (URISyntaxException e) {
							throw new RuntimeException(e);
						}

						if (side instanceof ILaunchProvider) {
							tsk.classpath(((ILaunchProvider) side).libraryJars());
							tsk.sourceLookup(((ILaunchProvider) side).libraryJars());

							tsk.classpath(((ILaunchProvider) side).mainJar());
							tsk.sourceLookup(((ILaunchProvider) side).mainJar());
						}

						tsk.jvm(side.jvm());
						tsk.argument(side.commandline());
					}));
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private Object[] processCmf(Project project, Class<? extends Configuration> cls) {
		Configuration manifest = null;
		boolean coremod = false;
		if (project.getTasks().findByName("cmf") != null && project.getTasks().findByName("cmf") instanceof CmfTask) {
			CmfTask cmfTsk = (CmfTask) project.getTasks().findByName("cmf");
			if (cmfTsk.getArchiveExtension().getOrNull() != null
					&& cmfTsk.getArchiveExtension().getOrNull().equals("ccmf")) {
				coremod = true;
			} else
				coremod = false;

			if (cls != null) {
				manifest = cmfTsk.getManifest(cls);
			}
		}

		return new Object[] { coremod, manifest };
	}

	private void cyanRunManifestTask(Project project, Configuration<?> man, boolean coremod) {
		CyanModfileManifest manifest = (CyanModfileManifest) man;

		EclipseLaunchGenerator launchGen = (EclipseLaunchGenerator) project.getTasks()
				.getByName("createEclipseLaunches");
		launchGen.dependsOn(project.task(Map.of(), "runManifest", new ConfigureClosure<Task>(project, (tsk) -> {
			CyanModfileManifest config = new CyanModfileManifest().readAll(manifest.toString(false));

			config.platforms.clear();
			config.gameVersionRegex = null;
			config.gameVersionMessage = null;
			config.mavenDependencies.clear();
			config.mavenRepositories.clear();
			config.trustContainers.clear();
			config.jars.clear();

			tsk.doLast((tsk2) -> {
				File dest = new File(project.getBuildDir(), "/run/mod.manifest.ccfg");
				if (!dest.getParentFile().exists())
					dest.getParentFile().mkdirs();

				try {
					Files.writeString(dest.toPath(), config.toString());
				} catch (IOException e) {
				}
			});
		})));
	}

	private void cmfSupportLaunch(Project proj, EclipseLaunchGenerator tsk, Configuration<?> man, boolean coremod) {
		CyanModfileManifest manifest = (CyanModfileManifest) man;
		if (coremod) {
			tsk.jvm("-DauthorizeDebugPackages=" + manifest.modClassPackage);
			tsk.jvm("-DebugModfileManifests=CM//"
					+ new File(proj.getBuildDir(), "run/mod.manifest.ccfg").getAbsolutePath());
		} else {
			tsk.jvm("-DebugModfileManifests=M//"
					+ new File(proj.getBuildDir(), "run/mod.manifest.ccfg").getAbsolutePath());
			tsk.jvm("-Dcyan.load.classpath=${project_loc:" + proj.getName() + "}/bin/main" + File.pathSeparator
					+ "${project_loc:" + proj.getName() + "}/bin/test");
		}
	}

	public class ConfigureClosure<T> extends Closure<T> {

		private static final long serialVersionUID = 1L;

		private Project project;
		private Consumer<T> configure;

		public ConfigureClosure(Project owner, Consumer<T> configure) {
			super(owner);
			this.project = owner;
			this.configure = configure;
		}

		@Override
		@SuppressWarnings("unchecked")
		public T call() {
			T tsk = (T) getDelegate();
			configure.accept(tsk);
			if (tsk instanceof EclipseLaunchGenerator) {
				project.getTasks().getByName("createEclipseLaunches").finalizedBy(tsk);
				project.getTasks().getByName("createEclipseLaunches").dependsOn("eclipse");
			}
			return tsk;
		}

	}

	private ArrayList<IGameExecutionContext> contexts;

	@Override
	public void saveContexts(ArrayList<IGameExecutionContext> contexts) {
		this.contexts = contexts;
	}

	@Override
	public ArrayList<IGameExecutionContext> getContextsList() {
		return contexts;
	}

}
