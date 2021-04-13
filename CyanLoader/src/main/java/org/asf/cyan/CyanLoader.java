package org.asf.cyan;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.asf.cyan.api.events.IEventProvider;
import org.asf.cyan.api.events.core.EventBusFactory;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.api.modloader.information.modloader.LoadPhase;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.modloader.information.providers.IModProvider;
import org.asf.cyan.api.versioning.StringVersionProvider;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.core.SimpleModloader;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.bytecode.sources.FileClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.IClassSourceProvider;
import org.asf.cyan.fluid.bytecode.sources.LoaderClassSourceProvider;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.loader.configs.SecurityConfiguration;
import org.asf.cyan.loader.eventbus.CyanEventBridge;

import org.asf.cyan.minecraft.toolkits.mtk.FabricCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.ForgeCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftRifterToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.PaperCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.rift.SimpleRift;
import org.asf.cyan.minecraft.toolkits.mtk.rift.SimpleRiftBuilder;
import org.asf.cyan.minecraft.toolkits.mtk.rift.providers.IRiftToolchainProvider;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.mods.ICoremod;
import org.asf.cyan.mods.IMod;
import org.asf.cyan.mods.config.CyanModfileManifest;
import org.asf.cyan.mods.internal.BaseEventController;
import org.asf.cyan.mods.internal.IAcceptableComponent;
import org.asf.cyan.mods.internal.ModInfoCache;
import org.asf.cyan.security.TrustContainer;

/**
 * 
 * CyanLoader Minecraft Modloader.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CyanLoader extends Modloader implements IModProvider {

	// TODO: crash-report modification with FLUID support, also state what CYAN
	// threads are running and which have been suspended or unresponsive, list all
	// coremods and then mods (TODO: mods and mod threads)
	// TODO: Fix log, deobfuscate it
	// TODO: Per-version modifications
	// TODO: RifterToolkit
	// TODO: Coremod loading

	// TODO: Fix coremod signature problem when using RIFT

	private static boolean developerMode = false;
	private static SecurityConfiguration securityConf;
	private static ArrayList<TrustContainer> trust = new ArrayList<TrustContainer>();
	private static final PrintStream defaultOutputStream = System.out;
	private static ArrayList<Mapping<?>> compatibilityMappings = new ArrayList<Mapping<?>>();
	private static String[] allowedComponentPackages = new String[0];
	private static String[] coremoduleKeys;
	private static Mapping<?> mappings = null;
	private static boolean vanillaMappings = true;
	private static boolean loaded = false;
	private static File cyanDir;

	private HashMap<String, CyanModfileManifest> modManifests = new HashMap<String, CyanModfileManifest>();
	private HashMap<String, CyanModfileManifest> coreModManifests = new HashMap<String, CyanModfileManifest>();

	private ArrayList<IMod> mods = new ArrayList<IMod>();
	private ArrayList<ICoremod> coremods = new ArrayList<ICoremod>();

	private static HashMap<String, String[]> classesMap = new HashMap<String, String[]>();

	private HashMap<String, IAcceptableComponent> loadedComponents = new HashMap<String, IAcceptableComponent>();

	private static String[] acceptableProviders = new String[] { "transformers", "transformer-packages", "auto.init" };

	public static File getCyanDataDirectory() {
		return cyanDir;
	}

	public static boolean areVanillaMappingsEnabled() {
		return vanillaMappings;
	}

	/**
	 * Enables component whitelisting.<br/>
	 * <br/>
	 * <b>WARNING:</b> using this exposes the coremodule loading mechanism, do not
	 * use this outside of development environments!<br/>
	 * <br/>
	 * This method must be called BEFORE the modloader is prepared, only top-level
	 * wrappers can use this.
	 */
	public static void enableDeveloperMode() {
		developerMode = true;
	}

	private static void prepare(String side) throws IOException {
		setupModloader(side);
		loaded = true;

		cyanDir = new File(".cyan-data");

		if (!cyanDir.exists())
			cyanDir.mkdirs();

		String cPath = cyanDir.getCanonicalPath();
		info("Starting CYAN in: " + cPath);
		MinecraftInstallationToolkit.setMinecraftDirectory(cyanDir);

		if (new File(cyanDir, "transformer-backtrace").exists())
			deleteDir(new File(cyanDir, "transformer-backtrace"));

		MinecraftToolkit.resetServerConnectionState();

		// TODO: Mappings loading and caching (with window)
		MinecraftVersionInfo mcVersion = new MinecraftVersionInfo(CyanInfo.getMinecraftVersion(), null, null, null);
		if (!MinecraftMappingsToolkit.areMappingsAvailable(mcVersion, CyanCore.getSide())) {
			info("First time loading, downloading " + side.toLowerCase() + " mappings...");
			MinecraftToolkit.resolveVersions();
			MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(CyanInfo.getMinecraftVersion());
			MinecraftMappingsToolkit.downloadVanillaMappings(version, CyanCore.getSide());
			MinecraftMappingsToolkit.saveMappingsToDisk(version, CyanCore.getSide());
		}

		mappings = MinecraftMappingsToolkit.loadMappings(mcVersion, CyanCore.getSide());
	}

	private static boolean setup = false;

	/**
	 * Prepare CyanLoader, called automatically.
	 * 
	 * @param side Side to use
	 * @throws IOException If setting up the loader fails.
	 */
	public static void setupModloader(String side) throws IOException {
		if (setup)
			return;

		if (developerMode) {
			if (System.getProperty("coreModDebugKeys") != null)
				coremoduleKeys = System.getProperty("coreModDebugKeys").split(":");

			if (System.getProperty("authorizeDebugPackages") != null)
				allowedComponentPackages = System.getProperty("authorizeDebugPackages").split(":");

			System.err.println("");
			System.err.println("");
			System.err.println("DANGER!");
			System.err.println("Coremodule loading mechanism has been released to the command line!");
			System.err.println("Shut down the program if you are not running in a development environment!");
			System.err.println("");
			System.err.println("");
		} else {

			if (System.getProperty("coreModDebugKeys") != null
					|| System.getProperty("authorizeDebugPackages") != null) {
				System.err.println(
						"Cyan is not running in a development environment, you cannot use coreModDebugKeys and authorizeDebugPackages outside of it.");
				System.exit(-1);
			}

		}

		if (side.equals("SERVER")) {
			URL url = CyanLoader.class.getResource("/log4j2.xml");
			if (MinecraftInstallationToolkit.isIDEModeEnabled()) {
				url = CyanLoader.class.getResource("/log4j2-server-ide.xml");
			}
			if (url == null)
				url = new URL(CyanLoader.class.getProtectionDomain().getCodeSource().getLocation().toString()
						+ "/log4j2-server.xml");

			System.setProperty("log4j2.configurationFile", url.toString());
			CyanCore.disableAgent();
		}

		CyanCore.setSide(side);
		if (CyanCore.getCoreClassLoader() == null)
			CyanCore.initLoader();

		CyanCore.simpleInit();
		CyanCore.initLogger();

		if (cyanDir == null) {
			cyanDir = new File(".cyan-data");

			if (!cyanDir.exists())
				cyanDir.mkdirs();

			if (MinecraftInstallationToolkit.getMinecraftDirectory() == null) {
				MinecraftInstallationToolkit.setMinecraftDirectory(cyanDir);
			}
		}
		securityConf = new SecurityConfiguration(cyanDir.getAbsolutePath());
		securityConf.readAll();

		File coremods = new File(cyanDir, "coremods");
		File versionCoremods = new File(coremods, CyanInfo.getMinecraftVersion());

		CyanLoader ld = new CyanLoader();
		ld.addInformationProvider(CyanInfo.getProvider());
		ld.addInformationProvider(ld);
		appendImplementation(ld);

		MinecraftToolkit.resetServerConnectionState();

		if (!coremods.exists())
			coremods.mkdirs();
		if (versionCoremods.exists()) {
			ld.importCoremods(versionCoremods);
		}
		ld.importCoremods(coremods);

		File trustContainers = new File(cyanDir, "trust");
		if (!trustContainers.exists())
			trustContainers.mkdirs();
		importTrust(trustContainers);

		Modloader.addModloaderImplementation(ld);
		if (!CyanInfo.getModloaderName().isEmpty() || !CyanInfo.getModloaderVersion().isEmpty()) {
			String name = CyanInfo.getModloaderName();
			String version = CyanInfo.getModloaderVersion();
			if (CyanInfo.getModloaderName().isEmpty()) {
				name = CyanInfo.getModloaderVersion();
				version = "";
			}

			SimpleModloader modloader = new SimpleModloader(name, name, new StringVersionProvider(version));
			Modloader.addModloaderImplementation(modloader);
		}

		setup = true;
	}

	private void loadCoreMods() {
		coreModManifests.forEach((k, manifest) -> {
			loadMod(true, manifest, new ArrayList<String>());
		});
	}

	private void loadMod(boolean coremod, CyanModfileManifest manifest, ArrayList<String> loadingMods) {
		IMod[] mods;
		if (coremod) {
			mods = this.coremods.stream().map(t -> (IMod) t).toArray(t -> new IMod[t]);
		} else {
			mods = this.mods.stream().toArray(t -> new IMod[t]);
		}

		if (loadingMods.contains(manifest.modGroup + ":" + manifest.modId)) {
			fatal("Mod dependency cicle detected! Currently loading id: " + manifest.modGroup + ":" + manifest.modId);
			System.exit(-1);
		}
		loadingMods.add(manifest.modGroup + ":" + manifest.modId);

		info("Loading mod " + manifest.modGroup + ":" + manifest.modId + "... (" + manifest.displayName + ")");

		final Collection<CyanModfileManifest> allManifests;
		if (coremod) {
			allManifests = coreModManifests.values();
		} else {
			allManifests = modManifests.values();
		}

		// TODO: maven recursive dependencies (pick the highest versions)

		manifest.dependencies.forEach((id, version) -> {
			// TODO: version regex parsing

			Optional<CyanModfileManifest> optManifest = allManifests.stream()
					.filter(t -> id.equals(t.modGroup + ":" + t.modId)).findFirst();
			if (optManifest.isEmpty() && !Stream.of(mods).anyMatch(t -> t.getManifest().id().equals(id))) {
				fatal("Missing mod dependency for " + manifest.displayName + ": " + id);
				System.exit(-1);
			}

			if (!optManifest.isEmpty() && !Stream.of(mods)
					.anyMatch(t -> t.getManifest().id().equals(manifest.modGroup + ":" + manifest.modId))) {
				loadMod(coremod, optManifest.get(), loadingMods);
			}
		});

		manifest.optionalDependencies.forEach((id, version) -> {
			// TODO: version regex parsing

			Optional<CyanModfileManifest> optManifest = allManifests.stream()
					.filter(t -> id.equals(t.modGroup + ":" + t.modId)).findFirst();

			if (!optManifest.isEmpty() && !Stream.of(mods)
					.anyMatch(t -> t.getManifest().id().equals(manifest.modGroup + ":" + manifest.modId))) {
				loadMod(coremod, optManifest.get(), loadingMods);
			}
		});

		ICoremod mod = getMod(manifest.modClassPackage + "." + manifest.modClassName, true);
		if (mod == null) {
			fatal("Failed to load coremod " + manifest.modGroup + ":" + manifest.modId
					+ " as it was not accepted by the modloader!");
			System.exit(-1);
		}

		if (coremod) {
			loadCoremod(mod, manifest, classesMap.get(manifest.modGroup + ":" + manifest.modId));
		} else {
			// TODO
		}
	}

	/**
	 * Loads coremod classes (must be done during CORELOAD)
	 * 
	 * @param mod Coremod to load
	 */
	public void loadCoremod(ICoremod mod, CyanModfileManifest modManifest, String[] classes) {
		if (CyanCore.getCurrentPhase().equals(LoadPhase.NOT_READY)
				|| CyanCore.getCurrentPhase().equals(LoadPhase.CORELOAD)) {
			for (String cls : classes) {
				try {
					this.checkTrust("mod class", CyanCore.getCoreClassLoader().loadClass(cls));
				} catch (IOException | ClassNotFoundException e) {
					throw new RuntimeException(e);
				}
			}

			coremods.add(mod);
			dispatchEvent("mod.loaded", mod);
			mod.setup(getModloader(), getGameSide(), modManifest);
		} else
			throw new IllegalStateException("Already past CORELOAD");
	}

	@SuppressWarnings("unchecked")
	private <T extends IMod> T getMod(String className, boolean coremod) {
		if (coremod) {
			if (this.loadedComponents.get(className) instanceof IMod)
				return (T) this.loadedComponents.get(className);
			else
				return null;
		}

		try {
			Class<?> cls = CyanCore.getClassLoader().loadClass(className);
			if (IMod.class.isAssignableFrom(cls)) {
				return null;
			}

			Constructor<IMod> ctor = (Constructor<IMod>) cls.getConstructor();
			IMod mod = ctor.newInstance();

			return (T) mod;
		} catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException
				| IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			error("Could not instanciate mod class: " + className, e);
		}
		return null;
	}

	private void importCoremods(File coremodsDirectory) {
		// TODO: progress window on callback if needed
		for (File ccmf : coremodsDirectory.listFiles((t) -> !t.isDirectory() && t.getName().endsWith(".ccmf"))) {
			try {
				importCoremod(ccmf);
			} catch (IOException e) {
				fatal("Importing coremod failed, mod: " + ccmf.getName(), e);
				System.exit(-1);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void importCoremod(File ccmf) throws IOException {
		// TODO: error screens/windowed messages
		String ccfg = null;
		ArrayList<String> modClasses = new ArrayList<String>();

		try {
			InputStream strm = new URL("jar:" + ccmf.toURI().toURL() + "!/mod.manifest.ccfg").openStream();
			ccfg = new String(strm.readAllBytes());
			strm.close();
		} catch (IOException e) {
		}

		if (ccfg == null) {
			throw new IOException("Invalid mod file, missing manifest.");
		}

		CyanModfileManifest manifest = new CyanModfileManifest().readAll(ccfg);
		for (String k : new ArrayList<String>(manifest.jars.keySet())) {
			if (!k.startsWith("/")) {
				manifest.jars.put("/" + k, manifest.jars.get(k));
				manifest.jars.remove(k);
			}
		}

		if (manifest.gameVersionRegex != null && manifest.gameVersionMessage != null
				&& !CyanInfo.getMinecraftVersion().matches(manifest.gameVersionRegex)) {
			fatal("Incompatible game version '" + CyanInfo.getMinecraftVersion() + "', coremod " + manifest.displayName
					+ " wants " + manifest.gameVersionMessage);
			System.exit(-1);
		}

		ZipInputStream strm = new ZipInputStream(new FileInputStream(ccmf));
		boolean cacheOutOfDate = false;
		ModInfoCache info = new ModInfoCache();
		File cache = new File(cyanDir, "caches/coremods/" + manifest.modId);
		File modCache = new File(cyanDir, "caches/coremods/" + manifest.modId + "/mod.cache");

		if (!modCache.exists()) {
			cache.mkdirs();
			cacheOutOfDate = true;
		} else {
			info.readAll(Files.readString(modCache.toPath()));
			if (!info.modVersion.equals(manifest.version)) {
				cacheOutOfDate = true;
			} else if (!CyanInfo.getMinecraftVersion().equals(info.gameVersion)) {
				cacheOutOfDate = true;
			} else if (!CyanInfo.getPlatform().toString().equals(info.platform)) {
				cacheOutOfDate = true;
			} else if (!(CyanInfo.getModloaderName() + "-" + CyanInfo.getModloaderVersion().toString())
					.equals(info.platformVersion)) {
				cacheOutOfDate = true;
			}
		}

		if (cacheOutOfDate) {
			info("(Re)building coremod cache...");
			CyanLoader.deleteDir(cache);
			cache.mkdirs();
			info.gameVersion = CyanInfo.getMinecraftVersion();
			info.platform = CyanInfo.getPlatform().toString();
			info.platformVersion = CyanInfo.getModloaderName() + "-" + CyanInfo.getModloaderVersion().toString();
			info.modVersion = manifest.version;

			info("Game version: " + CyanInfo.getMinecraftVersion());
			info("Mod version: " + manifest.version);
			info("Platform: " + CyanInfo.getPlatform().toString());
			if (!CyanInfo.getModloaderName().isEmpty())
				info("Platform version: " + CyanInfo.getModloaderName() + "-"
						+ CyanInfo.getModloaderVersion().toString());

			Files.writeString(modCache.toPath(), info.toString());
		}

		ZipEntry ent = strm.getNextEntry();
		while (ent != null) {
			String path = ent.getName().replace("\\", "/");
			if (!path.startsWith("/"))
				path = "/" + path;

			if (!path.endsWith("/")) {
				if (manifest.jars.containsKey(path)) {
					File output = new File(cache, path);
					if (!output.getParentFile().exists())
						output.getParentFile().mkdirs();

					String type = manifest.jars.get(path);
					if (!output.exists()) {
						if (type.equals("rift")) {
							info("Installing RIFT mod jar: " + path + "... (this might take a while)");

							MinecraftVersionInfo version = new MinecraftVersionInfo(CyanInfo.getMinecraftVersion(),
									null, null, null);
							if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
								version = MinecraftVersionToolkit.getVersion(CyanInfo.getMinecraftVersion());
								if (version == null) {
									MinecraftToolkit.resolveVersions();
									MinecraftInstallationToolkit.clearVariables();
									version = MinecraftVersionToolkit.getVersion(CyanInfo.getMinecraftVersion());
								}
							}

							final MinecraftVersionInfo vFinal = version;
							SimpleRiftBuilder builder = new SimpleRiftBuilder();
							builder.appendRiftProvider(new IRiftToolchainProvider() {

								@Override
								public File getJar() throws IOException {
									// FIXME
									return new File(
											"C:\\Users\\stefa\\.gradle\\caches\\Cornflower\\shared\\Cornflower-MTK\\caches\\jars\\1.16.5-client-deobf.jar");
								}

								@Override
								public IClassSourceProvider<?>[] getSources() throws IOException {
									return new IClassSourceProvider<?>[] {
											new LoaderClassSourceProvider(CyanCore.getCoreClassLoader()) };
								}

								@Override
								public Mapping<?> getRiftMappings() throws IOException {
									if (CyanLoader.compatibilityMappings.size() != 0) {
										return MinecraftRifterToolkit
												.generateRiftTargets(CyanLoader.compatibilityMappings.get(0));
									} else {
										if (CyanInfo.getPlatform() == LaunchPlatform.DEOBFUSCATED) {
											return null;
										} else {
											return MinecraftRifterToolkit.generateRiftTargets(MinecraftMappingsToolkit
													.loadMappings(vFinal, getModloaderGameSide()));
										}
									}
								}

							});
							builder.setIdentifier(CyanInfo.getMinecraftVersion()
									+ (CyanInfo.getModloaderName().isEmpty() ? "" : "-" + CyanInfo.getModloaderName())
									+ (CyanInfo.getModloaderVersion().isEmpty() ? ""
											: "-" + CyanInfo.getModloaderVersion())
									+ "-" + getModloaderGameSide().toString().toLowerCase());

							info("Extracting mod jar " + path + " to temporary storage...");
							File tempOutput = new File(cache, "tmp/" + System.currentTimeMillis() + ".jar");
							if (!tempOutput.getParentFile().exists())
								tempOutput.getParentFile().mkdirs();

							FileOutputStream outputStrm = new FileOutputStream(tempOutput);
							strm.transferTo(outputStrm);
							outputStrm.close();

							info("Creating rift jar " + path + "...");
							builder.appendSources(new FileClassSourceProvider(tempOutput));
							ZipFile zip = new ZipFile(tempOutput);

							Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) zip.entries();
							while (entries.hasMoreElements()) {
								ZipEntry entry = entries.nextElement();
								String pth = entry.getName().replace("\\", "/");
								if (pth.startsWith("/"))
									pth = pth.substring(1);
								if (pth.endsWith(".class"))
									builder.addClass(pth.substring(0, pth.lastIndexOf(".class")).replaceAll("/", "."));
							}
							zip.close();

							SimpleRift rift;
							try {
								rift = builder.build();
								rift.apply();
							} catch (ClassNotFoundException | IOException e) {
								builder.close();
								throw new IOException(e);
							}

							zip = new ZipFile(tempOutput);
							entries = (Enumeration<ZipEntry>) zip.entries();
							while (entries.hasMoreElements()) {
								ZipEntry entry = entries.nextElement();
								String pth = entry.getName().replace("\\", "/");
								if (pth.startsWith("/"))
									pth = pth.substring(1);
								if (!pth.endsWith(".class")) {
									rift.addFile(pth, zip.getInputStream(entry));
								}
							}
							zip.close();

							rift.export(output);

							rift.close();
							builder.close();
							tempOutput.delete();
						} else {
							info("Installing mod jar: " + path + "...");
							FileOutputStream outputStrm = new FileOutputStream(output);
							strm.transferTo(outputStrm);
							outputStrm.close();
						}
					}

					ZipFile modJar = new ZipFile(output);
					Enumeration<? extends ZipEntry> entries = modJar.entries();
					while (entries.hasMoreElements()) {
						ZipEntry entry = entries.nextElement();
						String entryPath = entry.getName().replace("\\", "/");
						if (entryPath.startsWith("/")) {
							entryPath = entryPath.substring(1);
						}
						if (entryPath.endsWith(".class")) {
							String cls = entryPath.replace("/", ".").substring(0, entryPath.lastIndexOf(".class"));
							modClasses.add(cls);
						}
					}
					modJar.close();

					CyanCore.addCoreUrl(output.toURI().toURL());
				}
			}

			ent = strm.getNextEntry();
		}

		manifest.trustContainers.forEach((name, location) -> {
			HashMap<String, String> rewrites = new HashMap<String, String>();
			try {
				if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
					URL u = new URL(securityConf.modSecurityService + "/request-server-location/" + manifest.modGroup
							+ "/" + manifest.modId);
					InputStream in = u.openStream();
					location = new String(in.readAllBytes()).replace("\r", "").split("\n")[0];
					in.close();
					rewrites.put("location", location);
				}
			} catch (IOException ex) {
			}

			String version = "latest";
			if (name.contains("@")) {
				version = name.substring(name.lastIndexOf("@") + 1);
				name = name.substring(0, name.lastIndexOf("@"));
			} else {
				try {
					URL latestInfo = new URL(location + "/" + name.replace(".", "/") + ".latest");
					InputStream in = latestInfo.openStream();
					version = new String(in.readAllBytes()).replace("\r", "").split("\n")[0];
					in.close();
					rewrites.put("version", version);
				} catch (IOException ex) {

				}
			}

			try {
				if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
					URL u = new URL(securityConf.modSecurityService + "/request-trust-container-name/"
							+ manifest.modGroup + "/" + manifest.modId + "/" + name);
					InputStream in = u.openStream();
					name = new String(in.readAllBytes()).replace("\r", "").split("\n")[0];
					in.close();
					rewrites.put("name", name);
				}
			} catch (IOException ex) {
			}

			try {
				if (MinecraftToolkit.hasMinecraftDownloadConnection()) {
					URL u = new URL(securityConf.modSecurityService + "/request-trust-container-version/"
							+ manifest.modGroup + "/" + manifest.modId + "/" + name + "/" + version);
					InputStream in = u.openStream();
					version = new String(in.readAllBytes()).replace("\r", "").split("\n")[0];
					in.close();
					rewrites.put("version", version);
				}
			} catch (IOException ex) {
			}

			File rewriteFile = new File(cache, "rewrites.data");
			if (rewriteFile.exists()) {
				try {
					for (String line : Files.readAllLines(rewriteFile.toPath())) {
						if (!line.isEmpty()) {
							String key = line.substring(0, line.indexOf("="));
							String value = line.substring(line.indexOf("=") + 1);
							if (!rewrites.containsKey(key))
								rewrites.put(key, value);
						}
					}
				} catch (IOException e) {
				}
			}

			if (rewrites.containsKey("location"))
				location = rewrites.get("location");
			if (rewrites.containsKey("name"))
				name = rewrites.get("name");
			if (rewrites.containsKey("version"))
				version = rewrites.get("version");

			StringBuilder rewriteFileCont = new StringBuilder();
			rewrites.forEach((k, v) -> {
				rewriteFileCont.append(k).append("=").append(v).append("\n");
			});
			try {
				Files.writeString(rewriteFile.toPath(), rewriteFileCont.toString());
			} catch (IOException e1) {
			}

			boolean update = false;
			File trust = new File(new File(cyanDir, "trust"), name.replace(".", "/") + ".ctc");
			if (!trust.getParentFile().exists())
				trust.getParentFile().mkdirs();

			if (trust.exists()) {
				try {
					TrustContainer ctc = TrustContainer.importContainer(trust);
					if (!ctc.getVersion().equals(version) && !version.equals("latest")) {
						update = true;
						trust.delete();
					} else {
						try {
							URL remote = new URL(
									location + "/" + name.replace(".", "/") + "-" + version + ".ctc.sha256");

							InputStream inp = remote.openStream();
							String sha = new String(inp.readAllBytes()).replaceAll("\t", " ").replaceAll("\r", "");
							inp.close();
							if (sha.contains("\n")) {
								sha = sha.substring(0, sha.indexOf("\n"));
							}
							if (sha.contains(" ")) {
								sha = sha.substring(0, sha.indexOf(" "));
							}

							String localhash = sha256HEX(Files.readAllBytes(trust.toPath()));
							if (!localhash.equals(sha)) {
								fatal("Trust container " + name + " for coremod '" + manifest.displayName
										+ "' has been tampered with!");
								fatal("Will not start to protect the end user!");
								System.exit(-1);
							}
						} catch (IOException ex) {
						}
					}
				} catch (IOException e) {
					update = true;
					trust.delete();
				}
			} else {
				update = true;
			}

			if (update) {
				info("Downloading coremod trust container " + name + " for coremod '" + manifest.displayName + "'...");
				try {
					URL remote = new URL(location + "/" + name.replace(".", "/") + "-" + version + ".ctc");
					InputStream in = remote.openStream();
					FileOutputStream out = new FileOutputStream(trust);
					in.transferTo(out);
					out.close();
					in.close();
				} catch (IOException e) {
					fatal("Unable to download trust container " + name + " for coremod '" + manifest.displayName + "'");
					System.exit(-1);
				}
			}

			if (coreModManifests.containsKey(manifest.modGroup + "." + manifest.modId)) {
				fatal("Coremod conflict!");
				fatal("Coremod path '" + manifest.modGroup + "." + manifest.modId + "' was imported twice!");
				System.exit(-1);
			}
			coreModManifests.put(manifest.modGroup + "." + manifest.modId, manifest);
		});

		strm.close();
		classesMap.put(manifest.modGroup + ":" + manifest.modId, modClasses.toArray(t -> new String[t]));
	}

	private static String sha256HEX(byte[] array) {
		MessageDigest digest;
		try {
			digest = MessageDigest.getInstance("SHA-256");
		} catch (NoSuchAlgorithmException e) {
			return null;
		}
		byte[] sha = digest.digest(array);
		StringBuilder result = new StringBuilder();
		for (byte aByte : sha) {
			result.append(String.format("%02x", aByte));
		}
		return result.toString();
	}

	private static void importTrust(File trustContainers) {
		for (File ctc : trustContainers.listFiles((f) -> f.getName().endsWith(".ctc") && !f.isDirectory())) {
			try {
				trust.add(TrustContainer.importContainer(ctc));
			} catch (IOException e) {
				error("Trust container " + ctc.getName() + " failed to import.");
			}
		}
		for (File dir : trustContainers.listFiles((f) -> f.isDirectory())) {
			importTrust(dir);
		}
	}

	private static void deleteDir(File file) {
		for (File f : file.listFiles(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				return !arg0.isDirectory();
			}

		})) {
			f.delete();
		}
		for (File f : file.listFiles(new FileFilter() {

			@Override
			public boolean accept(File arg0) {
				return arg0.isDirectory();
			}

		})) {
			deleteDir(f);
		}
		file.delete();
	}

	/**
	 * Prepare for running Cyan Components in minecraft
	 */
	public static void initializeGame(String side) {
		try {
			if (!loaded)
				prepare(side);

			CyanCore.registerPreLoadHook(new Runnable() {

				@Override
				public void run() {

					info("Loading FLUID mappings...");

					if (vanillaMappings)
						Fluid.loadMappings(mappings);

					for (Mapping<?> cmap : compatibilityMappings)
						Fluid.loadMappings(cmap);
				}

			});
			CyanCore.registerPreLoadHook(new Runnable() {

				@Override
				public void run() {
					info("Loading FLUID class load hooks...");

					// TODO: Coremod load hooks
				}

			});
			CyanCore.registerPreLoadHook(new Runnable() {

				@Override
				public void run() {
					info("Loading FLUID transformers...");

					// TODO: Coremod fluid transformers
					try {
						switch (CyanInfo.getSide()) {
						case CLIENT:
							Fluid.registerTransformer("org.asf.cyan.modifications._1_15_2.client.MinecraftModification",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
							Fluid.registerTransformer(
									"org.asf.cyan.modifications._1_15_2.client.TitleScreenModification",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
							Fluid.registerTransformer(
									"org.asf.cyan.modifications._1_15_2.client.LoadingOverlayModification",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
							Fluid.registerTransformer("org.asf.cyan.modifications._1_15_2.client.WindowModification",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
							Fluid.registerTransformer("org.asf.cyan.modifications._1_15_2.client.BrandModification",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
						case SERVER:
							Fluid.registerTransformer(
									"org.asf.cyan.modifications._1_15_2.server.MinecraftServerGuiModification",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
							Fluid.registerTransformer(
									"org.asf.cyan.modifications._1_15_2.server.MinecraftServerGuiModification$1",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
							Fluid.registerTransformer(
									"org.asf.cyan.modifications._1_15_2.server.StatsComponentModification",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
						default:
							Fluid.registerTransformer(
									"org.asf.cyan.modifications._1_15_2.common.MinecraftServerModification",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
							Fluid.registerTransformer(
									"org.asf.cyan.modifications._1_15_2.common.CrashReportModification",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
							Fluid.registerTransformer(
									"org.asf.cyan.modifications._1_15_2.common.CrashReportCategoryModification",
									CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
							break;
						}
					} catch (IllegalStateException | ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				}

			});

			CyanCore.registerPreLoadHook(new Runnable() {

				@Override
				public void run() {
					info("Loading Coremod Transformers...");
					transformers.forEach((transformer, source) -> {
						try {
							Fluid.registerTransformer(transformer, source); // TODO: exclusion annotations
						} catch (IllegalStateException | ClassNotFoundException e) {
						}
					});

					for (Class<?> transformer : findAnnotatedClasses(getMainImplementation(), FluidTransformer.class)) { // TODO:
																															// exclusion
																															// annotations
						transformerPackages.forEach((pkg, source) -> {
							if (transformer.getPackageName().equals(pkg)
									|| transformer.getPackageName().startsWith(pkg + ".")) {
								try {
									Fluid.registerTransformer(transformer.getTypeName(), source);
								} catch (IllegalStateException | ClassNotFoundException e) {
								}
							}
						});
					}
				}
			});

			info("Starting CyanCore...");
			CyanCore.initializeComponents();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Mapping<?> getFabricCompatibilityMappings(GameSide side) {
		try {
			if (!loaded)
				prepare(side.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new FabricCompatibilityMappings(mappings, side);
	}

	public static Mapping<?> getForgeCompatibilityMappings(GameSide side, String mcpVersion) {
		try {
			if (!loaded)
				prepare(side.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new ForgeCompatibilityMappings(mappings, side, mcpVersion);
	}

	public static Mapping<?> getPaperCompatibilityMappings() {
		try {
			if (!loaded)
				prepare(GameSide.SERVER.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new PaperCompatibilityMappings(mappings);
	}

	public static void addCompatibilityMappings(Mapping<?> mappings) {
		compatibilityMappings.add(mappings);
	}

	public static void disableVanillaMappings() {
		vanillaMappings = false;
	}

	@Override
	public IModManifest[] getLoadedNormalMods() {
		return mods.stream().map(t -> t.getManifest()).toArray(t -> new IModManifest[t]);
	}

	@Override
	public IModManifest[] getLoadedCoreMods() {
		return coremods.stream().map(t -> t.getManifest()).toArray(t -> new IModManifest[t]);
	}

	@Override
	public int getAllKnownModsLength() {
		return modManifests.size() + coreModManifests.size();
	}

	/**
	 * Gets the actual System output stream, assigned before the game starts.
	 */
	public static PrintStream getSystemOutputStream() {
		return defaultOutputStream;
	}

	@Override
	protected String getImplementationName() {
		return "Cyan";
	}

	@Override
	public String getName() {
		return "CyanLoader";
	}

	@Override
	public String getSimpleName() {
		return "Cyan";
	}

	private ArrayList<String> events = new ArrayList<String>();
	private CyanEventBridge bridger = null;

	private static HashMap<String, URL> transformerPackages = new HashMap<String, URL>();
	private static HashMap<String, URL> transformers = new HashMap<String, URL>();

	@Override
	protected boolean presentComponent(Class<IModloaderComponent> component) {
		if (CyanEventBridge.class.isAssignableFrom(component)) {
			return true;
		} else if (IEventProvider.class.isAssignableFrom(component)) {
			return true;
		} else if (CyanErrorHandlers.class.isAssignableFrom(component)) {
			return true;
		} else if (BaseEventController.class.getTypeName().equals(component.getTypeName())) {
			return true;
		} else if (IAcceptableComponent.class.isAssignableFrom(component)) {
			try {
				return checkTrust("component", component);
			} catch (Exception ex) {
				fatal("Failed to authenticate component: " + component.getTypeName());
				fatal("Will not continue as it is way too risky.");
				System.exit(-1);
				return false;
			}
		}
		return false;
	}

	private boolean checkTrust(String type, Class<?> component) throws IOException {
		if (Stream.of(allowedComponentPackages).anyMatch(
				t -> t.equals(component.getPackageName()) || component.getPackageName().startsWith(t + "."))) {
			return true;
		}

		boolean found = false;
		for (TrustContainer container : trust) {
			int result = container.validateClass(component);
			if (result == 1) {
				fatal("");
				fatal("");
				fatal("----------------------- COREMOD MIGHT HAVE BEEN TAMPERED WITH! -----------------------");
				fatal("A " + type + " did not pass security checks, Cyan will shut down to protect the end-user.");
				fatal("Make sure you download content from official sources and not third-parties. If you are");
				fatal("certain the content is authentic, you might need to clear the trust container storage.");
				fatal("");
				fatal(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase() + ": "
						+ component.getTypeName());
				fatal("");
				fatal("");
				System.exit(-1);
			} else if (result == 0) {
				found = true;
				break;
			}
		}

		if (!found) {
			fatal("");
			fatal("Starting failed as a " + type + " was not present in any trust container.");
			fatal("");
			fatal("Make sure you have the component trust container installed.");
			fatal("Most components should automatically download from a trust server, if the server is");
			fatal("down, you will need to manually copy the component trust container to .cyan-data/trust.");
			fatal("");
			fatal(type.substring(0, 1).toUpperCase() + type.substring(1).toLowerCase() + ": "
					+ component.getTypeName());
			info("");
			info("");
			info("");
			info("Note for coremod developers: as it is impossible to authenticate mods from development");
			info("environments, you need to instruct the security system to allow your coremod classes.");
			info("");
			info("You will need to be running development cyan wrappers. If you are, you can use the");
			info("-DauthorizeDebugPackages=<package> argument to whitelist your component.");
			info("(use -DauthorizeDebugPackages=<package1>:<package2> for multiple)");
			info("");
			info("Note that you will need to have a valid debug key as secondary security.");
			info("A debug key needs to be assigned in the component (using a virtual manifest)");
			info("as well as on the command line. Use -");

			System.exit(-1);
		}

		return true;
	}

	@Override
	protected boolean execComponent(IModloaderComponent component) {
		if (component instanceof CyanEventBridge) {
			bridger = (CyanEventBridge) component;
			return true;
		} else if (component instanceof IEventProvider) {
			if (events.contains(((IEventProvider) component).getChannelName()))
				return false;

			events.add(((IEventProvider) component).getChannelName());

			return true;
		} else if (component instanceof CyanErrorHandlers) {
			((CyanErrorHandlers) component).attach();
			return true;
		} else if (component instanceof BaseEventController) {
			BaseEventController controller = (BaseEventController) component;
			controller.assign();
			controller.attachListenerRegistry((channel, listener) -> {
				try {
					attachEventListener(channel, listener);
				} catch (IllegalStateException e) {
					error("Failed to attach event listener " + listener.getListenerName() + " to event " + channel
							+ ", event not recognized.");
				}
			});
			return true;
		} else if (component instanceof IAcceptableComponent) {
			IAcceptableComponent cp = (IAcceptableComponent) component;

			for (String request : cp.earlyInfoRequests()) {
				if (request.equals("coremod.manifest") && cp instanceof ICoremod) {
					Optional<CyanModfileManifest> man = coreModManifests.values().stream()
							.filter(t -> cp.getClass().getTypeName().equals(t.modClassPackage + "." + t.modClassName))
							.findFirst();

					if (!man.isEmpty())
						cp.provideInfo("coremod.manifest", man.get());
				}
			}

			String key = cp.executionKey();
			if (key == null)
				return false;

			URL location = cp.getClass().getProtectionDomain().getCodeSource().getLocation();
			String pref = location.toString();
			if (pref.startsWith("jar:")) {
				pref = pref.substring("jar:".length(), pref.lastIndexOf("!/"));
			} else if (pref.contains(".class")) {
				pref = pref.substring(0, pref.lastIndexOf(cp.getClass().getTypeName().replace(".", "/") + ".class"));
			}

			String acceptedKey = "";
			if (System.getProperty("coreModDebugKeys") != null) {
				if (!Stream.of(coremoduleKeys).anyMatch(t -> t.equals(key)))
					return false;
				else {
					acceptedKey = key;
				}
			} else {
				try {
					location = cp.getClass().getProtectionDomain().getCodeSource().getLocation();

					if (!location.toString().endsWith(".class")) {
						String pref2 = location.toString();
						if ((pref2.endsWith(".jar") || pref2.endsWith(".zip")) && !pref2.startsWith("jar:")) {
							pref2 = "jar:" + pref2 + "!/";
						}
						pref2 += cp.getClass().getTypeName().replace(".", "/") + ".class";
						location = new URL(pref2);
					}

					InputStream strm = location.openStream();
					acceptedKey = sha256HEX(strm.readAllBytes());
					strm.close();
				} catch (Exception e) {
					return false;
				}
			}

			if (!key.equals(acceptedKey))
				return false;

			for (String prov : cp.providers()) {
				if (!Stream.of(acceptableProviders).anyMatch(t -> t.equals(prov))) {
					return false;
				}
			}

			for (String prov : cp.providers()) {
				if (prov.equals("auto.init")) {
					cp.provide("auto.init");
				}
			}

			for (String prov : cp.providers()) {
				if (prov.equals("transformers")) {
					String[] transformers = (String[]) cp.provide(prov);
					for (String transformer : transformers) {
						try {
							CyanLoader.transformers.put(transformer, new URL(pref));
						} catch (IllegalStateException | MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				} else if (prov.equals("transformer-packages")) {
					String[] transformerPackages = (String[]) cp.provide(prov);
					for (String transformer : transformerPackages) {
						try {
							CyanCore.addAllowedPackage(transformer);
							CyanLoader.transformerPackages.put(transformer, new URL(pref));
						} catch (IllegalStateException | MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}

			loadedComponents.put(cp.getClass().getTypeName(), cp);
			return true;
		}
		return false;
	}

	@SuppressWarnings("deprecation")
	protected EventBusFactory<?> createEventBusFactory() {
		if (bridger == null) {
			error("createEventBusFactory was called in CyanLoader, but no CyanEventBridge was presented yet!");
			return null;
		}
		return bridger.getNewFactory();
	}

	@Override
	protected boolean acceptsAnonymousComponent() {
		return true;
	}

	@Override
	protected void postRegister() {
		for (String event : events) {
			try {
				createEventChannel(event);
			} catch (IllegalStateException e) {
			}
		}

		createEventChannel("mod.loaded");
		loadCoreMods();
		BaseEventController.work();

	}
}
