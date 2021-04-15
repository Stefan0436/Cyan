package org.asf.cyan.cornflower.gradle;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.function.Consumer;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGame;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGameExecutionContext;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IModloader;
import org.asf.cyan.cornflower.gradle.utilities.Log4jToGradleAppender;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;

/**
 * Cornflower Plugin ModloaderHandler Class, DO NOT USE OUTSIDE OF GRADLE
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ModloaderHandler extends CyanComponent {

	@SuppressWarnings("unchecked")
	public static void exec(Project proj) {
		Log4jToGradleAppender.logInfo();
		CornflowerCore.LOGGER.info("Loading Cornflower modloader and game dependencies...");

		ArrayList<Dependency> remoteDependencies = new ArrayList<Dependency>();
		for (Configuration conf : proj.getConfigurations()) {
			for (Dependency dep : conf.getDependencies()) {
				if (dep.getGroup() != null && !dep.getGroup().startsWith("cornflower.internal.")) {
					if (dep.getName() != null) {
						remoteDependencies.add(dep);
					}
				}
			}
		}

		// Search for modloaders
		proj.getExtensions().getExtraProperties().set("cornflowermodloaders", new ArrayList<IModloader>());
		proj.getExtensions().getExtraProperties().set("cornflowergames", new ArrayList<IGame>());

		findDeps(proj, "modloader", (dep) -> {
			String modloaderName = dep.getName();
			int api = 6;

			if (modloaderName.contains("/")) {
				api = Integer.valueOf(modloaderName.substring(modloaderName.indexOf("/") + 1));
				modloaderName = modloaderName.substring(0, modloaderName.indexOf("/"));
			}

			Class<IModloader>[] loaders = findClasses(getMainImplementation(), IModloader.class);
			for (Class<IModloader> cls : loaders) {
				if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers()))
					continue;

				try {
					IModloader modloader = cls.getConstructor().newInstance();
					if (modloader.name().equalsIgnoreCase(modloaderName)) {
						modloader = modloader.newInstance(proj, dep.getVersion(), api);
						if (modloader == null)
							continue;

						((ArrayList<IModloader>) proj.getExtensions().getExtraProperties().get("cornflowermodloaders"))
								.add(modloader);
						return;
					}
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				}
			}

			CornflowerCore.LOGGER
					.error("Cannot to find modloader: " + modloaderName + ", cornflower may not work properly.");
		});

		for (IModloader modloader : (ArrayList<IModloader>) proj.getExtensions().getExtraProperties()
				.get("cornflowermodloaders")) {
			modloader.addRepositories(proj.getRepositories());
			modloader.addDependencies(proj.getConfigurations());
		}

		findDeps(proj, "game", (dep) -> {
			Class<IGame>[] games = findClasses(getMainImplementation(), IGame.class);
			for (Class<IGame> cls : games) {
				if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers()))
					continue;

				try {
					IGame game = cls.getConstructor().newInstance();
					for (IModloader modloader : (ArrayList<IModloader>) proj.getExtensions().getExtraProperties()
							.get("cornflowermodloaders")) {
						if (game.modloader().isAssignableFrom(modloader.getClass())) {
							if (game.name().equalsIgnoreCase(dep.getName())) {
								game = game.newInstance(proj, dep.getVersion(), modloader);
								if (game == null)
									continue;

								((ArrayList<IGame>) proj.getExtensions().getExtraProperties().get("cornflowergames"))
										.add(game);
								return;
							}

						}
					}
				} catch (InstantiationException | IllegalAccessException | IllegalArgumentException
						| InvocationTargetException | NoSuchMethodException | SecurityException e) {
				}
			}

			CornflowerCore.LOGGER.error("Cannot to find game: " + dep.getName()
					+ ", make sure you have the modloader that provides it, cornflower may not work properly.");
		});

		for (IGame game : (ArrayList<IGame>) proj.getExtensions().getExtraProperties().get("cornflowergames")) {
			game.addRepositories(proj.getRepositories());
			game.addDependencies(proj.getConfigurations());

			for (IGameExecutionContext ctx : game.getContexts()) {
				IGameExecutionContext context = ctx.newInstance(proj, game.getVersion());
				File[] dirs = context.flatDirs();
				if (dirs.length != 0) {
					proj.getRepositories().flatDir((repo) -> {
						repo.dirs((Object[]) dirs);
					});
				}

				String mainJar = context.deobfuscatedJarDependency();
				if (mainJar == null)
					mainJar = context.gameJarDependency();

				proj.getDependencies().add("implementation", mainJar);
				for (String lib : context.libraries())
					proj.getDependencies().add("implementation", lib);
			}
		}

		proj.getExtensions().getExtraProperties().set("remoteDependencies", remoteDependencies);
		Log4jToGradleAppender.noLogInfo();
	}

	private static void findDeps(Project proj, String depType, Consumer<Dependency> handler) {
		for (Configuration conf : proj.getConfigurations()) {
			for (Dependency dep : new ArrayList<Dependency>(conf.getDependencies())) {
				if (dep.getGroup() != null && dep.getGroup().startsWith("cornflower.internal.")) {
					String type = dep.getGroup().substring("cornflower.internal.".length());
					if (type.equals(depType)) {
						conf.getDependencies().remove(dep);
						handler.accept(dep);
					}
				}
			}
		}
	}
}
