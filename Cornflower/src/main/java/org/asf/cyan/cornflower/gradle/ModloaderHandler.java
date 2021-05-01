package org.asf.cyan.cornflower.gradle;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IAPIDependency;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGame;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGameExecutionContext;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IModloader;
import org.asf.cyan.cornflower.gradle.utilities.Log4jToGradleAppender;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;

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

		ArrayList<File> remoteDependencyLst = new ArrayList<File>();
		ArrayList<File> remoteDependencySrcLst = new ArrayList<File>();
		ArrayList<String> remoteDependencies = new ArrayList<String>();
		for (Configuration conf : proj.getConfigurations()) {
			try {
				conf.setCanBeResolved(true);
			} catch (Exception e) {

			}

//			for (Dependency dep : conf.getDependencies()) {
//				if (dep.getGroup() != null && !dep.getGroup().startsWith("cornflower.internal.")) {
//					if (dep.getName() != null && !remoteDependencies.stream()
//							.anyMatch(t -> t.startsWith(dep.getGroup() + ":" + dep.getName() + ":"))) {
//						remoteDependencies.add(dep.getGroup() + ":" + dep.getName() + ":" + dep.getVersion());
//					}
//				}
//			}
		}

		// Search for modloaders
		proj.getExtensions().getExtraProperties().set("cornflowermodloaders", new ArrayList<IModloader>());
		proj.getExtensions().getExtraProperties().set("cornflowergames", new ArrayList<IGame>());
		proj.getExtensions().getExtraProperties().set("cornflowerapis", new ArrayList<IGame>());

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

		findDeps(proj, "api", (dep) -> {
			Class<IAPIDependency>[] apis = findClasses(getMainImplementation(), IAPIDependency.class);
			for (Class<IAPIDependency> cls : apis) {
				if (cls.isInterface() || Modifier.isAbstract(cls.getModifiers()))
					continue;

				try {
					IAPIDependency api = cls.getConstructor().newInstance();
					for (IModloader modloader : (ArrayList<IModloader>) proj.getExtensions().getExtraProperties()
							.get("cornflowermodloaders")) {
						if (api.modloader().isAssignableFrom(modloader.getClass())) {
							if (api.name().equalsIgnoreCase(dep.getName())) {
								api = api.newInstance(proj, dep.getVersion(), modloader);
								if (api == null)
									continue;

								((ArrayList<IAPIDependency>) proj.getExtensions().getExtraProperties()
										.get("cornflowerapis")).add(api);
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

		ArrayList<String> deps = new ArrayList<String>();
		for (IModloader modloader : (ArrayList<IModloader>) proj.getExtensions().getExtraProperties()
				.get("cornflowermodloaders")) {
			modloader.addRepositories(proj.getRepositories());

			for (String dep : modloader.addDependencies(proj.getConfigurations()))
				deps.add(dep);
		}

		for (IAPIDependency api : (ArrayList<IAPIDependency>) proj.getExtensions().getExtraProperties()
				.get("cornflowerapis")) {
			api.addRepositories(proj.getRepositories());
			api.addDependencies(proj.getConfigurations());
		}

		for (IGame game : (ArrayList<IGame>) proj.getExtensions().getExtraProperties().get("cornflowergames")) {
			game.addRepositories(proj.getRepositories());
			game.addDependencies(proj.getConfigurations());
			ArrayList<IGameExecutionContext> contexts = new ArrayList<IGameExecutionContext>();

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
				deps.add(mainJar);
				for (String lib : context.libraries()) {
					proj.getDependencies().add("implementation", lib);
					deps.add(lib);
				}
				contexts.add(context);
			}
			game.saveContexts(contexts);
		}

		try {
			Configuration conf = proj.getConfigurations().getByName("implementation");
			ResolvedConfiguration config = conf.getResolvedConfiguration();
			scanDeps(config.getFirstLevelModuleDependencies(), remoteDependencies, deps, remoteDependencyLst,
					remoteDependencySrcLst);
		} catch (Exception e) {
		}

		for (IGame game : (ArrayList<IGame>) proj.getExtensions().getExtraProperties().get("cornflowergames")) {
			game.addTasks(proj, game.getContextsList().toArray(t -> new IGameExecutionContext[t]), remoteDependencyLst,
					remoteDependencySrcLst);
		}

		proj.getExtensions().getExtraProperties().set("remoteDependencies", remoteDependencies);
		Log4jToGradleAppender.noLogInfo();
	}

	private static void scanDeps(Collection<ResolvedDependency> dependencies, ArrayList<String> remoteDependencies,
			ArrayList<String> deps, ArrayList<File> depOut, ArrayList<File> srcOut) {
		for (ResolvedDependency dep : dependencies) {
			scanDeps(dep.getChildren(), remoteDependencies, deps, depOut, srcOut);
			if (dep.getModuleGroup() != null && !dep.getModuleGroup().startsWith("cornflower.internal.")) {
				if (dep.getModuleName() != null
						&& !remoteDependencies.stream()
								.anyMatch(t -> t.startsWith(dep.getModuleGroup() + ":" + dep.getModuleName() + ":"))
						&& !deps.stream()
								.anyMatch(t -> t.startsWith(dep.getModuleGroup() + ":" + dep.getModuleName() + ":"))) {
					boolean containsNormalArtifact = false;
					for (ResolvedArtifact arti : dep.getModuleArtifacts()) {
						if (arti.getExtension().equals("jar")
								&& (arti.getClassifier() == null || arti.getClassifier().isEmpty())) {
							containsNormalArtifact = true;
							depOut.add(arti.getFile());
						} else if (arti.getClassifier() != null
								&& (arti.getExtension().equals("jar") || arti.getExtension().equals("zip"))
								&& arti.getClassifier().toLowerCase().contains("sources"))
							srcOut.add(arti.getFile());
					}
					if (containsNormalArtifact) {
						remoteDependencies
								.add(dep.getModuleGroup() + ":" + dep.getModuleName() + ":" + dep.getModuleVersion());
					}
				}
			}
		}
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
