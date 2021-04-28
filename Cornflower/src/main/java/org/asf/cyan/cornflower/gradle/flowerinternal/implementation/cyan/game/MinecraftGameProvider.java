package org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.game;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;

import org.asf.cyan.cornflower.gradle.flowerinternal.implementation.cyan.CyanModloader;
import org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions.CornflowerMainExtension;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGame;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGameExecutionContext;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IModloader;
import org.asf.cyan.cornflower.gradle.tasks.EclipseLaunchGenerator;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionType;
import org.gradle.api.Project;
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
	public void addTasks(Project project, IGameExecutionContext[] contexts, ArrayList<File> dependencies,
			ArrayList<File> sourceLookup) {
		EclipseLaunchGenerator launchGen = (EclipseLaunchGenerator) project.getTasks()
				.getByName("createEclipseLaunches");
		launchGen.disable = true;
		int i = 1;
		for (IGameExecutionContext side : contexts) {
			String name = "createSidedLaunch" + i++;

			if (side instanceof ILaunchProvider) {
				name = ((ILaunchProvider) side).launchName();
			}

			project.task(Map.of("type", CornflowerMainExtension.EclipseLaunchGenerator), name,
					new LaunchClosure(project, (tsk) -> {
						tsk.name("Launch " + side.name() + " (Deobfuscated)");
						tsk.workingDir(new File(project.getRootDir(), "run/" + side.name()));
						tsk.classpath(project);
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

	public class LaunchClosure extends Closure<EclipseLaunchGenerator> {

		private static final long serialVersionUID = 1L;

		private Project project;
		private Consumer<EclipseLaunchGenerator> configure;

		public LaunchClosure(Project owner, Consumer<EclipseLaunchGenerator> configure) {
			super(owner);
			this.project = owner;
			this.configure = configure;
		}

		@Override
		public EclipseLaunchGenerator call() {
			EclipseLaunchGenerator tsk = (EclipseLaunchGenerator) getDelegate();
			configure.accept(tsk);
			project.getTasks().getByName("createEclipseLaunches").finalizedBy(tsk);
			project.getTasks().getByName("createEclipseLaunches").dependsOn("eclipse");
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
