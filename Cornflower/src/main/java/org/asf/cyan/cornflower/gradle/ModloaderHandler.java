package org.asf.cyan.cornflower.gradle;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.cornflower.gradle.flowerinternal.projectextensions.CornflowerMainExtension.PlatformConfiguration;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IAPIDependency;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGame;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IGameExecutionContext;
import org.asf.cyan.cornflower.gradle.flowerutil.modloaders.IModloader;
import org.asf.cyan.cornflower.gradle.tasks.EclipseLaunchGenerator;
import org.asf.cyan.cornflower.gradle.utilities.Log4jToGradleAppender;
import org.asf.cyan.cornflower.gradle.utilities.modding.IPlatformConfiguration;
import org.asf.cyan.cornflower.gradle.utilities.modding.ModDependency;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.artifacts.ResolvedConfiguration;
import org.gradle.api.artifacts.ResolvedDependency;
import org.gradle.api.artifacts.repositories.ArtifactRepository;
import org.gradle.api.artifacts.repositories.MavenArtifactRepository;

/**
 * Cornflower Plugin ModloaderHandler Class, DO NOT USE OUTSIDE OF GRADLE
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class ModloaderHandler extends CyanComponent {

	private static HashMap<String, File[]> allDeps = new HashMap<String, File[]>();
	private static HashMap<String, File[]> modDeps = new HashMap<String, File[]>();

	public static File[] getAllDependencies(Project proj) {
		return allDeps.getOrDefault(proj.getPath(), new File[0]).clone();
	}

	public static File[] getModDeps(Project proj) {
		return modDeps.getOrDefault(proj.getPath(), new File[0]).clone();
	}

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

			Class<IModloader>[] loaders = findClasses(getMainImplementation(), IModloader.class,
					ModloaderHandler.class.getClassLoader());
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
			Class<IGame>[] games = findClasses(getMainImplementation(), IGame.class,
					ModloaderHandler.class.getClassLoader());
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
			Class<IAPIDependency>[] apis = findClasses(getMainImplementation(), IAPIDependency.class,
					ModloaderHandler.class.getClassLoader());
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
		HashMap<String, String> modDeps = new HashMap<String, String>();
		HashMap<String, String> modDepsOptional = new HashMap<String, String>();
		ArrayList<String[]> modDepArtifacts = new ArrayList<String[]>();
		ArrayList<String> modDependencies = new ArrayList<String>();
		findDeps(proj, "mod", (dep) -> {
			ModDependency mod = (ModDependency) dep;
			if (!mod.isMod()) {
				mod.getRepositories().forEach((name, url) -> {
					if (proj.getRepositories().findByName(name) == null) {
						proj.getRepositories().maven(repo -> {
							repo.setName(name);
							repo.setUrl(url);
						});
					}
				});

				modDepArtifacts.add(new String[] { mod.getModDepGroup(), mod.getModDepName(), mod.getVersion() });
				addDependency(proj, mod.getModDepGroup(), mod.getModDepName(), mod.getVersion());
				modDependencies.add(mod.getModDepGroup() + ":" + mod.getModDepName() + ":" + mod.getVersion());
				deps.add(mod.getModDepGroup() + ":" + mod.getModDepName() + ":" + mod.getVersion());
			} else {
				if (proj.getExtensions().getExtraProperties().has("platforms")) {
					PlatformConfiguration config = (PlatformConfiguration) proj.getExtensions().getExtraProperties()
							.get("platforms");
					String versions = "";
					for (IPlatformConfiguration t : config.all) {
						if (t.getPlatform() == LaunchPlatform.VANILLA) {
							if (!versions.isEmpty())
								versions += "|";
							versions += Pattern.quote(t.getCommonMappingsVersion());
						}
					}
					mod.amendVersionStatement(versions);
				}
				if (mod.isOptional())
					modDepsOptional.put(mod.getModDepName(), mod.getVersionStatement());
				else
					modDeps.put(mod.getModDepName(), mod.getVersionStatement());
			}
		});
		proj.getExtensions().getExtraProperties().set("modfileDependenciesExtra", modDeps);
		proj.getExtensions().getExtraProperties().set("modfileOptionalDependenciesExtra", modDepsOptional);

		for (IModloader modloader : (ArrayList<IModloader>) proj.getExtensions().getExtraProperties()
				.get("cornflowermodloaders")) {
			modloader.addRepositories(proj.getRepositories());

			for (String dep : modloader.addDependencies(proj.getConfigurations()))
				deps.add(dep);
		}
		for (Project project : proj.getRootProject().getAllprojects()) {
			deps.add(project.getGroup() + ":" + project.getName() + ":" + project.getVersion());
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

				if (mainJar != null)
					proj.getDependencies().add("implementation", mainJar);

				if (mainJar != null)
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
			ArrayList<File> modDependencyFiles = new ArrayList<File>();
			Configuration conf = proj.getConfigurations().getByName("implementation");
			ResolvedConfiguration config = conf.getResolvedConfiguration();
			ArrayList<File> allDependencies = new ArrayList<File>();
			scanDeps(config.getFirstLevelModuleDependencies(), remoteDependencies, deps, remoteDependencyLst,
					remoteDependencySrcLst, allDependencies, modDependencies, modDependencyFiles);
			ModloaderHandler.modDeps.put(proj.getPath(), modDependencyFiles.toArray(t -> new File[t]));
			allDeps.put(proj.getPath(), allDependencies.toArray(t -> new File[t]));
		} catch (Exception e) {
		}

		proj.getExtensions().getExtraProperties().set("remoteDependencies", remoteDependencies);
		for (IGame game : (ArrayList<IGame>) proj.getExtensions().getExtraProperties().get("cornflowergames")) {
			game.addTasks(proj, game.getContextsList().toArray(t -> new IGameExecutionContext[t]), remoteDependencyLst,
					remoteDependencySrcLst);
		}

		for (String[] dep : modDepArtifacts) {
			String group = dep[0];
			String name = dep[1];
			String version = dep[2];

			for (ArtifactRepository t : proj.getRepositories()) {
				if (t instanceof MavenArtifactRepository) {
					MavenArtifactRepository repo = (MavenArtifactRepository) t;
					ArrayList<URI> uris = new ArrayList<URI>(repo.getArtifactUrls());
					uris.add(repo.getUrl());

					boolean found = false;
					for (URI u : uris) {
						for (Task tsk : proj.getTasks()) {
							if (tsk instanceof EclipseLaunchGenerator) {
								EclipseLaunchGenerator gen = (EclipseLaunchGenerator) tsk;
								if (!gen.tags.getOrDefault("cyan.debug.launch", "false").equals("true"))
									continue;

								String path = group.replace(".", "/") + "/" + name + "/" + version + "/" + name + "-"
										+ version + ".cmf";
								try {
									if (!u.toString().endsWith("/"))
										path = "/" + path;
									URL url = new URL(u.toString() + "/" + path);
									url.openStream().close();
									found = true;

									final String pathFinal = path;
									gen.doFirst(tsk2 -> {
										try {
											File outputFile = new File(gen.workingDir,
													".cyan-data/mods/" + group + "." + name + ".cmf");

											String upstreamHash = "unknown";
											String localHash = "notinstalled";
											try {
												InputStream strm = new URL(u.toString() + "/" + pathFinal + ".sha1")
														.openStream();
												upstreamHash = new String(strm.readAllBytes()).replace("\t", "  ")
														.trim();
												strm.close();
												if (upstreamHash.contains(" "))
													upstreamHash = upstreamHash.substring(0, upstreamHash.indexOf(" "));
											} catch (IOException e) {
											}

											if (outputFile.exists()) {
												try {
													localHash = sha1HEX(Files.readAllBytes(outputFile.toPath()));
												} catch (NoSuchAlgorithmException e) {
												}
											}

											if (!localHash.equals(upstreamHash)) {
												if (!outputFile.getParentFile().exists())
													outputFile.getParentFile().mkdirs();
												info("Downloading " + name + " CMF file...");
												FileOutputStream strm = new FileOutputStream(outputFile);
												InputStream in = url.openStream();
												in.transferTo(strm);
												strm.close();
												in.close();
											}
										} catch (IOException e) {
										}
									});
								} catch (IOException e) {
								}

								path = group.replace(".", "/") + "/" + name + "/" + version + "/" + name + "-" + version
										+ ".ccmf";
								try {
									if (!u.toString().endsWith("/"))
										path = "/" + path;
									URL url = new URL(u.toString() + path);
									url.openStream().close();
									found = true;

									final String pathFinal = path;
									gen.doFirst(tsk2 -> {
										try {
											File outputFile = new File(gen.workingDir,
													".cyan-data/coremods/" + group + "." + name + ".ccmf");

											String upstreamHash = "unknown";
											String localHash = "notinstalled";
											try {
												InputStream strm = new URL(u.toString() + "/" + pathFinal + ".sha1")
														.openStream();
												upstreamHash = new String(strm.readAllBytes()).replace("\t", "  ")
														.trim();
												strm.close();
												if (upstreamHash.contains(" "))
													upstreamHash = upstreamHash.substring(0, upstreamHash.indexOf(" "));
											} catch (IOException e) {
											}

											if (outputFile.exists()) {
												try {
													localHash = sha1HEX(Files.readAllBytes(outputFile.toPath()));
												} catch (NoSuchAlgorithmException e) {
												}
											}

											if (!localHash.equals(upstreamHash)) {
												if (!outputFile.getParentFile().exists())
													outputFile.getParentFile().mkdirs();
												info("Downloading " + name + " CCMF file...");
												FileOutputStream strm = new FileOutputStream(outputFile);
												InputStream in = url.openStream();
												in.transferTo(strm);
												strm.close();
												in.close();
											}
										} catch (IOException e) {
										}
									});
								} catch (IOException e) {
								}
							}
						}
						if (found)
							break;
					}
					if (found) {
						break;
					}
				}
			}
		}

		Log4jToGradleAppender.noLogInfo();
	}

	private static String sha1HEX(byte[] array) throws NoSuchAlgorithmException {
		MessageDigest digest = MessageDigest.getInstance("SHA-1");
		byte[] sha = digest.digest(array);
		StringBuilder result = new StringBuilder();
		for (byte aByte : sha) {
			result.append(String.format("%02x", aByte));
		}
		return result.toString();
	}

	private static Dependency addDependency(Project project, String group, String name, String version) {
		return project.getDependencies().add("implementation", group + ":" + name + ":" + version);
	}

	private static void scanDeps(Collection<ResolvedDependency> dependencies, ArrayList<String> remoteDependencies,
			ArrayList<String> deps, ArrayList<File> depOut, ArrayList<File> srcOut, ArrayList<File> allOut,
			ArrayList<String> modDepIDs, ArrayList<File> modOut) {
		for (ResolvedDependency dep : dependencies) {
			scanDeps(dep.getChildren(), remoteDependencies, deps, depOut, srcOut, allOut, modDepIDs, modOut);
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

				if (dep.getModuleName() != null) {
					for (ResolvedArtifact arti : dep.getModuleArtifacts()) {
						if (arti.getExtension().equals("jar")
								&& (arti.getClassifier() == null || arti.getClassifier().isEmpty())) {
							if (!allOut.contains(arti.getFile()))
								allOut.add(arti.getFile());
							if (modDepIDs.contains(
									dep.getModuleGroup() + ":" + dep.getModuleName() + ":" + dep.getModuleVersion())) {
								modDepIDs.remove(dep.getModuleGroup() + ":" + dep.getModuleName() + ":"
										+ dep.getModuleVersion());
								if (!modOut.contains(arti.getFile()))
									modOut.add(arti.getFile());
							}
						}
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
