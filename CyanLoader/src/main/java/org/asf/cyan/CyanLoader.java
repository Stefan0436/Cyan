package org.asf.cyan;

import java.awt.GraphicsEnvironment;
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
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.events.IEventProvider;
import org.asf.cyan.api.events.core.EventBusFactory;
import org.asf.cyan.api.events.extended.IExtendedEvent;
import org.asf.cyan.api.fluid.annotations.PlatformExclude;
import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.fluid.annotations.SideOnly;
import org.asf.cyan.api.fluid.annotations.VersionRegex;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.IPostponedComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.api.modloader.information.modloader.LoadPhase;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.modloader.information.providers.IModProvider;
import org.asf.cyan.api.versioning.StringVersionProvider;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.core.SimpleModloader;
import org.asf.cyan.core.StartupWindow;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.api.ClassLoadHook;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.information.metadata.TransformerMetadata;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.internal.KickStartConfig;
import org.asf.cyan.internal.modkitimpl.util.EventUtilImpl;
import org.asf.cyan.loader.configs.SecurityConfiguration;
import org.asf.cyan.loader.eventbus.CyanEventBridge;

import org.asf.cyan.minecraft.toolkits.mtk.FabricCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.ForgeCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.PaperCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;
import org.asf.cyan.mods.AbstractMod;
import org.asf.cyan.mods.ICoremod;
import org.asf.cyan.mods.IMod;
import org.asf.cyan.mods.config.CyanModfileManifest;
import org.asf.cyan.mods.events.AttachEvent;
import org.asf.cyan.mods.events.IEventListenerContainer;
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
public class CyanLoader extends Modloader implements IModProvider, IEventListenerContainer {

	private CyanLoader() {
		mavenRepositories.put("AerialWorks", "https://aerialworks.ddns.net/maven");
		mavenRepositories.put("Maven Central", "https://repo1.maven.org/maven2");
	}

	protected static String getMarker() {
		return "MODLOADER";
	}

	// TODO: Mod thread manager

	public static void appendCyanInfo(BiConsumer<String, Object> setDetail1, BiConsumer<String, Object> setDetail2) {
		int mods = Modloader.getModloader().getLoadedCoremods().length;
		if (mods != 0) {
			setDetail1.accept("Coremods", mods);
			for (IModManifest coremod : Modloader.getModloader().getLoadedCoremods()) {
				setDetail1.accept(coremod.id(), displayMod(coremod, true));
			}
		} else
			setDetail1.accept("Mods", "No mods loaded");

		mods = Modloader.getModloader().getLoadedMods().length;
		if (mods != 0) {
			setDetail2.accept("Mods", mods);
			for (IModManifest mod : Modloader.getModloader().getLoadedMods()) {
				setDetail2.accept(mod.id(), displayMod(mod, false));
			}
		} else
			setDetail2.accept("Mods", "No mods loaded");
	}

	public static String displayMod(IModManifest mod, boolean coremod) {
		try {
			// TODO: thread manager:
			// suspended = not running queued instructions
			// in-memory = not running at all, killed and queue has been stored for when the
			// thread is re-opened
			// repeating = re-running tasks

			HashMap<String, Object> entries = new HashMap<String, Object>();
			entries.put("Version", mod.version());
			entries.put("Display Name", mod.displayName());
			entries.put("Dependencies", (mod.dependencies().length + mod.optionalDependencies().length) + " ("
					+ mod.optionalDependencies().length + " optional)");
			entries.put("Mod Threads", "0 (0 suspended, 0 in memory, 0 repeating)");

			if (coremod) {
				int transformers = 0;
				int applied = 0;
				for (String[] data : CyanLoader.transformers.keySet()) {
					if (data[1].equals(mod.id()))
						transformers++;
				}

				for (TransformerMetadata md : TransformerMetadata.getLoadedTransformers()) {
					if (md.getTransfomerOwner().equals(mod.id()))
						applied++;
				}

				entries.put("Transformers", transformers + " (" + applied + " applied)");
			}

			StringBuilder value = new StringBuilder();
			for (String name : entries.keySet()) {
				value.append("\n\t\t");
				value.append(name);
				value.append(": ");
				value.append(entries.get(name));
			}

			return value.toString();
		} catch (Exception e) {
			return "~~ ERROR ~~ " + e.getMessage();
		}
	}

	private HashMap<String, String> mavenRepositories = new HashMap<String, String>();
	private HashMap<String, Version> coremodMavenDependencies = new HashMap<String, Version>();
	private HashMap<String, Version> modMavenDependencies = new HashMap<String, Version>();

	private static String platformVersion = "";

	/**
	 * Sets the platform version (can only be done ONCE)
	 * 
	 * @param version Platform version
	 */
	public static void setPlatformVersion(String version) {
		if (!platformVersion.isEmpty())
			return;
		platformVersion = version;
	}

	private static boolean developerMode = false;
	private static SecurityConfiguration securityConf;
	private static ArrayList<TrustContainer> trust = new ArrayList<TrustContainer>();
	private static final PrintStream defaultOutputStream = System.out;
	private static ArrayList<Mapping<?>> compatibilityMappings = new ArrayList<Mapping<?>>();
	private static String[] allowedComponentPackages = new String[0];
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

	private static String[] acceptableProviders = new String[] { "transformers", "transformer-packages", "class-hooks",
			"class-hook-packages", "auto.init", "mod.id" };

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
		Configuration.setLoggers(str -> warn(str), str -> error(str));
		setupModloader(side);
		loaded = true;

		int progressMax = 0;
		progressMax++; // set dir
		progressMax++; // set dump
		progressMax++; // del backtrace
		progressMax++; // reset server connection
		progressMax++; // load version

		if (!MinecraftMappingsToolkit.areMappingsAvailable(
				new MinecraftVersionInfo(CyanInfo.getMinecraftVersion(), null, null, null), CyanCore.getSide())) {
			progressMax++; // resolve
			progressMax++; // download version
			progressMax++; // download mappings
			progressMax++; // save mappings
		}

		// Base loading
		progressMax++; // load mappings
		progressMax++; // load postponed components
		progressMax++; // load events
		progressMax++; // load coremods
		progressMax++; // finish load

		// Mod loading
		progressMax++;
		progressMax++;
		progressMax++;
		progressMax++;
		progressMax++;
		progressMax++;
		progressMax++;
		progressMax++;

		cyanDir = new File(".cyan-data");
		if (!cyanDir.exists())
			cyanDir.mkdirs();

		String cPath = cyanDir.getCanonicalPath();
		info("Starting CYAN in: " + cPath);

		StartupWindow.WindowAppender.addMax(progressMax);

		MinecraftInstallationToolkit.setMinecraftDirectory(cyanDir);
		StartupWindow.WindowAppender.increaseProgress();

		Fluid.setDumpDir(MinecraftInstallationToolkit.getMinecraftDirectory());
		StartupWindow.WindowAppender.increaseProgress();

		if (new File(cyanDir, "transformer-backtrace").exists())
			deleteDir(new File(cyanDir, "transformer-backtrace"));
		StartupWindow.WindowAppender.increaseProgress();

		MinecraftToolkit.resetServerConnectionState();
		StartupWindow.WindowAppender.increaseProgress();

		MinecraftVersionInfo mcVersion = new MinecraftVersionInfo(CyanInfo.getMinecraftVersion(), null, null, null);
		StartupWindow.WindowAppender.increaseProgress();

		if (!MinecraftMappingsToolkit.areMappingsAvailable(mcVersion, CyanCore.getSide())) {
			info("First time loading, downloading " + side.toLowerCase() + " mappings...");

			MinecraftToolkit.resolveVersions();
			StartupWindow.WindowAppender.increaseProgress();

			MinecraftVersionInfo version = MinecraftVersionToolkit.getVersion(CyanInfo.getMinecraftVersion());
			StartupWindow.WindowAppender.increaseProgress();

			MinecraftMappingsToolkit.downloadVanillaMappings(version, CyanCore.getSide());
			StartupWindow.WindowAppender.increaseProgress();

			MinecraftMappingsToolkit.saveMappingsToDisk(version, CyanCore.getSide());
			StartupWindow.WindowAppender.increaseProgress();
		}

		mappings = MinecraftMappingsToolkit.loadMappings(mcVersion, CyanCore.getSide());
		StartupWindow.WindowAppender.increaseProgress();
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
			if (System.getProperty("authorizeDebugPackages") != null) {
				System.err.println(
						"Cyan is not running in a development environment, you cannot use authorizeDebugPackages outside of it.");
				System.exit(-1);
			}
		}

		if (side.equals("SERVER")) {
			URL url = CyanLoader.class.getResource("/log4j2-server.xml");
			if (MinecraftInstallationToolkit.isIDEModeEnabled()) {
				url = CyanLoader.class.getResource("/log4j2-server-ide.xml");
			}
			System.setProperty("log4j2.configurationFile", url.toString());
			System.setProperty("log4j.configurationFile", url.toString());
			CyanCore.disableAgent();
		}

		CyanCore.setSide(side);
		if (CyanCore.getCoreClassLoader() == null)
			CyanCore.initLoader();

		CyanCore.simpleInit();
		CyanCore.initLogger();

		if (side.equals("CLIENT") || !GraphicsEnvironment.isHeadless())
			StartupWindow.WindowAppender.showWindow();

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

		if (System.getProperty("debugModfileManifests") != null) {
			String[] files = null;

			if (System.getProperty("os.name").toLowerCase().contains("win")
					&& !System.getProperty("os.name").toLowerCase().contains("darwin"))
				files = System.getProperty("debugModfileManifests").split(";");
			else
				files = System.getProperty("debugModfileManifests").split(":");

			for (String file : files) {
				if (file.startsWith("CM//") && developerMode == true) {
					CyanModfileManifest mod = new CyanModfileManifest();
					mod.readAll(new String(Files.readAllBytes(new File(file.substring(4)).toPath())));
					ld.coreModManifests.put(mod.modGroup + ":" + mod.modId, mod);
					ld.coreModManifests.put(mod.modClassPackage + "." + mod.modClassName, mod);
					CyanCore.addAllowedPackage(mod.modClassPackage);
					classesMap.put(mod.modGroup + ":" + mod.modId,
							new String[] { mod.modClassPackage + "." + mod.modClassName });
					try {
						Class<?> cls = Class.forName(mod.modClassPackage + "." + mod.modClassName);
						String base = cls.getProtectionDomain().getCodeSource().getLocation().toString();
						if (base.toString().startsWith("jar:"))
							base = base.substring(0, base.lastIndexOf("!"));
						else if (base.endsWith("/" + cls.getTypeName().replace(".", "/") + ".class")) {
							base = base.substring(0,
									base.length() - ("/" + cls.getTypeName().replace(".", "/") + ".class").length());
						}
						if (!base.startsWith("jar:") && (base.endsWith(".jar") || base.endsWith(".zip")))
							base = "jar:" + base + "!/";
						mod.source = base;
						CyanCore.addAdditionalClass(cls);
					} catch (ClassNotFoundException e) {
						throw new RuntimeException(e);
					}
				} else if (file.startsWith("M//")) {
					CyanModfileManifest mod = new CyanModfileManifest();
					mod.readAll(new String(Files.readAllBytes(new File(file.substring(3)).toPath())));
					ld.modManifests.put(mod.modClassPackage + "." + mod.modClassName, mod);
					ld.modManifests.put(mod.modGroup + ":" + mod.modId, mod);
					CyanCore.addAllowedPackage(mod.modClassPackage);
				}
			}
		}
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

	private void loadCoreMods(ClassLoader loader) {
		coreModManifests.forEach((k, manifest) -> {
			if (k.contains(":") && !manifest.loaded)
				loadMod(true, manifest, new ArrayList<String>(), loader);
		});
	}

	private void loadMod(boolean coremod, CyanModfileManifest manifest, ArrayList<String> loadingMods,
			ClassLoader loader) {
		IMod[] mods = new IMod[this.mods.size() + coremods.size()];

		int i = 0;
		for (IMod mod : this.mods)
			mods[i++] = mod;
		for (IMod mod : coremods)
			mods[i++] = mod;

		if (loadingMods.contains(manifest.modGroup + ":" + manifest.modId)) {
			fatal("Mod dependency cicle detected! Currently loading id: " + manifest.modGroup + ":" + manifest.modId);
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}
		loadingMods.add(manifest.modGroup + ":" + manifest.modId);

		debug("Preparing to load mod " + manifest.modGroup + ":" + manifest.modId + "... (" + manifest.displayName
				+ ")");

		final Collection<CyanModfileManifest> allManifests;
		if (coremod) {
			allManifests = coreModManifests.values();
		} else {
			allManifests = modManifests.values();
		}

		manifest.mavenRepositories.forEach((name, url) -> {
			mavenRepositories.putIfAbsent(name, url);
		});

		manifest.dependencies.forEach((id, version) -> {
			Optional<CyanModfileManifest> optManifest = allManifests.stream()
					.filter(t -> id.equals(t.modGroup + ":" + t.modId)).findFirst();
			if (optManifest.isEmpty() && !Stream.of(mods).anyMatch(t -> t.getManifest().id().equals(id))) {
				fatal("Missing mod dependency for " + manifest.displayName + ": " + id);
				StartupWindow.WindowAppender.fatalError();
				System.exit(-1);
			}
			String cVersion = (optManifest.isEmpty()
					? Stream.of(mods).filter(t -> t.getManifest().id().equals(id)).findFirst().get().getManifest()
							.version().toString()
					: manifest.version);

			Version ver = Version.fromString(cVersion);
			checkDependencyVersion(version, ver, "Missing mod dependency for " + manifest.displayName + ": " + id);

			if (!optManifest.isEmpty() && !Stream.of(mods).anyMatch(
					t -> t.getManifest().id().equals(optManifest.get().modGroup + ":" + optManifest.get().modId))) {
				loadMod(coremod, optManifest.get(), loadingMods, loader);
			}
		});

		manifest.optionalDependencies.forEach((id, version) -> {
			Optional<CyanModfileManifest> optManifest = allManifests.stream()
					.filter(t -> id.equals(t.modGroup + ":" + t.modId)).findFirst();

			if (!optManifest.isEmpty() && !Stream.of(mods).anyMatch(
					t -> t.getManifest().id().equals(optManifest.get().modGroup + ":" + optManifest.get().modId))) {

				String cVersion = (optManifest.isEmpty()
						? Stream.of(mods).filter(t -> t.getManifest().id().equals(id)).findFirst().get().getManifest()
								.version().toString()
						: manifest.version);

				Version ver = Version.fromString(cVersion);
				checkDependencyVersion(version, ver, "Missing mod dependency for " + manifest.displayName + ": " + id);

				loadMod(coremod, optManifest.get(), loadingMods, loader);
			}
		});

		IMod mod = getMod(manifest.modClassPackage + "." + manifest.modClassName, coremod, loader);
		if (mod == null) {
			if (coremod) {
				fatal("Failed to load coremod " + manifest.modGroup + ":" + manifest.modId
						+ " as it was not accepted by the modloader!");
				StartupWindow.WindowAppender.fatalError();
			} else {
				fatal("Failed to load mod " + manifest.modGroup + ":" + manifest.modId + ", unknown error!");
				StartupWindow.WindowAppender.fatalError();
			}
			System.exit(-1);
		}

		if (coremod) {
			loadCoremod((ICoremod) mod, manifest, classesMap.get(manifest.modGroup + ":" + manifest.modId));
		} else {
			loadMod(mod, manifest);
		}
	}

	public static boolean validateCheckString(String checkString, Version version) {
		return validateCheckString(checkString, version, "", true) == null;
	}

	public static String validateCheckString(String check, Version version, String message, boolean brief) {
		String error = "";
		for (String checkVersion : check.split(" | ")) {
			error = null;
			for (String checkStr : checkVersion.split(" & ")) {
				checkStr = checkStr.trim();
				if (checkStr.startsWith("~=")) {
					String regex = checkStr.substring(2);
					if (regex.startsWith(" "))
						regex = regex.substring(1);
					if (!version.toString().matches(regex)) {
						if (!brief)
							error = message + " (incompatible version installed)";
						else
							error = message + " (incompatible)";
						break;
					}
				} else if (checkStr.startsWith(">=")) {
					String str = checkStr.substring(2);
					if (str.startsWith(" "))
						str = str.substring(1);

					Version min = Version.fromString(str);

					if (!version.isGreaterOrEqualTo(min)) {
						if (!brief)
							error = message + " (outdated version installed)";
						else
							error = message + " (>= " + str + ")";
						break;
					}
				} else if (checkStr.startsWith("<=")) {
					String str = checkStr.substring(2);
					if (str.startsWith(" "))
						str = str.substring(1);

					Version min = Version.fromString(str);

					if (!version.isLessOrEqualTo(min)) {
						if (!brief)
							error = message + " (incompatible newer version installed)";
						else
							error = message + " (<= " + str + ")";
						break;
					}
				} else if (checkStr.startsWith(">")) {
					String str = checkStr.substring(1);
					if (str.startsWith(" "))
						str = str.substring(1);

					Version min = Version.fromString(str);

					if (!version.isGreaterThan(min)) {
						if (!brief)
							error = message + " (outdated version installed)";
						else
							error = message + " (" + str + "+)";
						break;
					}
				} else if (checkStr.startsWith("<")) {
					String str = checkStr.substring(1);
					if (str.startsWith(" "))
						str = str.substring(1);

					Version min = Version.fromString(str);

					if (!version.isLessThan(min)) {
						if (!brief)
							error = message + " (incompatible newer version installed)";
						else
							error = message + " (Pre-" + str + ")";
						break;
					}
				} else if (checkStr.startsWith("!=")) {
					if (version.isEqualTo(Version.fromString(checkStr.substring(2).trim()))) {
						if (!brief)
							error = message + " (incompatible version installed)";
						else
							error = message + " (" + checkStr.substring(2).trim() + ")";
						break;
					}
				} else {
					if (!version.isEqualTo(Version.fromString(checkStr.trim()))) {
						if (!brief)
							error = message + " (incompatible version installed)";
						else
							error = message + " (" + checkStr.trim() + ")";
						break;
					}
				}
			}
			if (error == null)
				break;
		}
		return error;
	}

	private void checkDependencyVersion(String check, Version version, String message) {
		String error = validateCheckString(check, version, message, false);
		if (error != null) {
			fatal(error);
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}
	}

	/**
	 * Loads mod classes (must be done during CORELOAD)
	 * 
	 * @param mod         Mod to load
	 * @param modManifest Mod manifest
	 */
	public void loadMod(IMod mod, CyanModfileManifest modManifest) {
		if (CyanCore.getCurrentPhase().equals(LoadPhase.CORELOAD)) {
			mods.add(mod);
			dispatchEvent("mod.loaded", mod);

			for (URL url : CyanCore.getAddedUrls()) {
				String base = url.toString();
				if (!base.startsWith("jar:") && (base.endsWith(".jar") || base.endsWith(".zip")))
					base = "jar:" + base + "!";
				try {
					URL newURL = new URL(base + "/" + mod.getClass().getTypeName().replace(".", "/") + ".class");
					newURL.openStream().close();
					modManifest.source = base;
				} catch (Exception e) {
				}
			}
			if (modManifest.source == null && mod.getClass().getProtectionDomain() != null
					&& mod.getClass().getProtectionDomain().getCodeSource() != null
					&& mod.getClass().getProtectionDomain().getCodeSource().getLocation() != null) {
				String base = mod.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
				if (base.toString().startsWith("jar:"))
					base = base.substring(0, base.lastIndexOf("!"));
				else if (base.endsWith("/" + mod.getClass().getTypeName().replace(".", "/") + ".class")) {
					base = base.substring(0,
							base.length() - ("/" + mod.getClass().getTypeName().replace(".", "/") + ".class").length());
				}
				if (!base.startsWith("jar:") && (base.endsWith(".jar") || base.endsWith(".zip")))
					base = "jar:" + base + "!";
				modManifest.source = base;
			} else if (modManifest.source == null)
				throw new RuntimeException(
						"Could not determine mod source, mod: " + modManifest.modGroup + ":" + modManifest.modId);

			info("Loading mod " + modManifest.modGroup + ":" + modManifest.modId + "... (" + modManifest.displayName
					+ ")");

			mod.setup(getModloader(), getGameSide(), modManifest);

			modManifest.loaded = true;
			modManifests.put(mod.getManifest().id(), modManifest);
			modManifests.put(mod.getClass().getTypeName(), modManifest);

			if (mod instanceof AbstractMod) {
				modManifest.dependencies.forEach((id, ver) -> {
					Optional<? extends IMod> depmod = coremods.stream().filter(t -> t.getManifest().id().equals(id))
							.findFirst();
					if (!depmod.isPresent())
						depmod = mods.stream().filter(t -> t.getManifest().id().equals(id)).findFirst();

					if (depmod.isPresent() && depmod.get() instanceof AbstractMod) {
						((AbstractMod) mod).enableModSupport((AbstractMod) depmod.get());
					}
				});
				modManifest.optionalDependencies.forEach((id, ver) -> {
					Optional<? extends IMod> depmod = coremods.stream().filter(t -> t.getManifest().id().equals(id))
							.findFirst();
					if (!depmod.isPresent())
						depmod = mods.stream().filter(t -> t.getManifest().id().equals(id)).findFirst();

					if (depmod.isPresent() && depmod.get() instanceof AbstractMod) {
						((AbstractMod) mod).enableModSupport((AbstractMod) depmod.get());
					}
				});
			}
		} else
			throw new IllegalStateException("Already past CORELOAD");
	}

	/**
	 * Loads coremod classes (must be done during CORELOAD)
	 * 
	 * @param mod         Coremod to load
	 * @param modManifest Mod manifest
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

			for (URL url : CyanCore.getAddedUrls()) {
				String base = url.toString();
				if (!base.startsWith("jar:") && (base.endsWith(".jar") || base.endsWith(".zip")))
					base = "jar:" + base + "!";
				try {
					URL newURL = new URL(base + "/" + mod.getClass().getTypeName().replace(".", "/") + ".class");
					newURL.openStream().close();
					modManifest.source = base;
				} catch (Exception e) {
				}
			}
			if (modManifest.source == null && mod.getClass().getProtectionDomain() != null
					&& mod.getClass().getProtectionDomain().getCodeSource() != null
					&& mod.getClass().getProtectionDomain().getCodeSource().getLocation() != null) {
				String base = mod.getClass().getProtectionDomain().getCodeSource().getLocation().toString();
				if (base.toString().startsWith("jar:"))
					base = base.substring(0, base.lastIndexOf("!"));
				else if (base.endsWith("/" + mod.getClass().getTypeName().replace(".", "/") + ".class")) {
					base = base.substring(0,
							base.length() - ("/" + mod.getClass().getTypeName().replace(".", "/") + ".class").length());
				}
				if (!base.startsWith("jar:") && (base.endsWith(".jar") || base.endsWith(".zip")))
					base = "jar:" + base + "!";
				modManifest.source = base;
			} else if (modManifest.source == null)
				throw new RuntimeException(
						"Could not determine mod source, mod: " + modManifest.modGroup + ":" + modManifest.modId);

			coremods.add(mod);
			dispatchEvent("mod.loaded", mod);

			info("Loading coremod " + modManifest.modGroup + ":" + modManifest.modId + "... (" + modManifest.displayName
					+ ")");

			mod.setup(getModloader(), getGameSide(), modManifest);
			modManifest.loaded = true;
			coreModManifests.put(mod.getManifest().id(), modManifest);
			coreModManifests.put(mod.getClass().getTypeName(), modManifest);

			if (mod instanceof AbstractMod) {
				modManifest.dependencies.forEach((id, ver) -> {
					Optional<? extends IMod> depmod = coremods.stream().filter(t -> t.getManifest().id().equals(id))
							.findFirst();
					if (!depmod.isPresent())
						depmod = mods.stream().filter(t -> t.getManifest().id().equals(id)).findFirst();

					if (depmod.isPresent() && depmod.get() instanceof AbstractMod) {
						((AbstractMod) mod).enableModSupport((AbstractMod) depmod.get());
					}
				});
				modManifest.optionalDependencies.forEach((id, ver) -> {
					Optional<? extends IMod> depmod = coremods.stream().filter(t -> t.getManifest().id().equals(id))
							.findFirst();
					if (!depmod.isPresent())
						depmod = mods.stream().filter(t -> t.getManifest().id().equals(id)).findFirst();

					if (depmod.isPresent() && depmod.get() instanceof AbstractMod) {
						((AbstractMod) mod).enableModSupport((AbstractMod) depmod.get());
					}
				});
			}
		} else
			throw new IllegalStateException("Already past CORELOAD");
	}

	@SuppressWarnings("unchecked")
	private <T extends IMod> T getMod(String className, boolean coremod, ClassLoader loader) {
		if (coremod) {
			if (this.loadedComponents.get(className) instanceof IMod)
				return (T) this.loadedComponents.get(className);
			else
				return null;
		}

		try {
			Class<?> cls;
			try {
				cls = loader.loadClass(className);
			} catch (ClassNotFoundException e) {
				try {
					cls = CyanCore.getCoreClassLoader().loadClass(className);
				} catch (ClassNotFoundException e2) {
					try {
						cls = CyanCore.getClassLoader().loadClass(className);
					} catch (ClassNotFoundException e3) {
						cls = getClass().getClassLoader().loadClass(className);
					}
				}
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
		for (File ccmf : coremodsDirectory.listFiles((t) -> !t.isDirectory() && t.getName().endsWith(".ccmf"))) {
			try {
				importCoremod(ccmf);
			} catch (IOException e) {
				fatal("Importing coremod failed, mod: " + ccmf.getName());
				StartupWindow.WindowAppender.fatalError();
				fatal("Exception was thrown in the mod loading process.", e);
				System.exit(-1);
			}
		}
	}

	private void importCoremod(File ccmf) throws IOException {
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

		info("Importing coremod " + manifest.modGroup + ":" + manifest.modId + "...");

		if (manifest.gameVersionRegex != null && manifest.gameVersionMessage != null
				&& !CyanInfo.getMinecraftVersion().matches(manifest.gameVersionRegex)) {
			fatal("Incompatible game version '" + CyanInfo.getMinecraftVersion() + "', coremod " + manifest.displayName
					+ " wants " + manifest.gameVersionMessage);
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}

		if (CyanInfo.getPlatform() != LaunchPlatform.DEOBFUSCATED && CyanInfo.getPlatform() != LaunchPlatform.VANILLA
				&& CyanInfo.getPlatform() != LaunchPlatform.UNKNOWN) {
			if (!manifest.platforms.containsKey(CyanInfo.getPlatform().toString())) {
				boolean first = true;
				StringBuilder platforms = new StringBuilder();
				for (String platform : manifest.platforms.keySet()) {
					if (!first)
						platforms.append(", ");
					first = false;
					platforms.append(platform);
				}
				fatal("Incompatible platform '" + CyanInfo.getPlatform() + "', coremod " + manifest.displayName
						+ " only supports the following platforms: " + platforms);
				StartupWindow.WindowAppender.fatalError();
				System.exit(-1);
			}

			String platformVersion = manifest.platforms.get(CyanInfo.getPlatform().toString());
			String cVersion = CyanLoader.platformVersion;

			if (cVersion == null)
				cVersion = CyanInfo.getModloaderVersion();

			this.checkDependencyVersion(platformVersion, Version.fromString(cVersion), "Incompatible platform '"
					+ CyanInfo.getPlatform() + "' for coremod '" + manifest.displayName + "'");
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

		manifest.mavenDependencies.forEach((group, item) -> {
			item.forEach((name, version) -> {
				if (!coremodMavenDependencies.containsKey(group + ":" + name) || Version.fromString(version)
						.isGreaterThan(coremodMavenDependencies.get(group + ":" + name))) {
					coremodMavenDependencies.put(group + ":" + name, Version.fromString(version));
				}
			});
		});

		info("Loading mod jars...");
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

					if (!output.exists()) {
						boolean allow = false;

						for (String type : manifest.jars.get(path).split(" & ")) {
							type = type.trim();

							if (type.startsWith("platform:")) {
								String platform = type.substring("platform:".length());
								if (CyanInfo.getPlatform().toString().equalsIgnoreCase(platform)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase()
											.endsWith("-" + platform.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + platform.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("gameversion:")) {
								String gameversion = type.substring("gameversion:".length());
								if (CyanInfo.getMinecraftVersion().toString().equalsIgnoreCase(gameversion)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase().endsWith(
											"-" + gameversion.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + gameversion.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("side:")) {
								String side = type.substring("side:".length());
								if (CyanInfo.getSide().toString().equalsIgnoreCase(side)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase()
											.endsWith("-" + side.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0, path.toLowerCase().indexOf("-" + side.toLowerCase()))
												+ "." + extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("loaderversion:")) {
								String loaderversion = type.substring("loaderversion:".length());
								if (CyanInfo.getCyanVersion().toString().equalsIgnoreCase(loaderversion)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase().endsWith(
											"-" + loaderversion.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + loaderversion.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("mappingsversion:")) {
								String version = type.substring("mappingsversion:".length());
								boolean found = false;
								for (Mapping<?> mapping : Fluid.getMappings()) {
									if (mapping.mappingsVersion != null && mapping.mappingsVersion.equals(version)) {
										found = true;
										break;
									}
								}
								if (found)
									allow = true;
							} else if (type.equals("any")) {
								allow = true;
							}
						}

						if (allow) {
							info("Installing mod jar: " + path + "...");
							FileOutputStream outputStrm = new FileOutputStream(output);
							strm.transferTo(outputStrm);
							outputStrm.close();
						} else {
							ent = strm.getNextEntry();
							continue;
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
					if (!ctc.getVersion().equals(version) && !version.equals("latest")
							&& version.matches("^[0-9]+-[0-9]+$")) {
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
								StartupWindow.WindowAppender
										.fatalError("Trust container has been tampered with!\nPlease check the log!");
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
					StartupWindow.WindowAppender.fatalError();
					System.exit(-1);
				}
			}
		});

		if (coreModManifests.containsKey(manifest.modGroup + ":" + manifest.modId)) {
			fatal("Coremod conflict!");
			fatal("Coremod path '" + manifest.modGroup + ":" + manifest.modId + "' was imported twice!");
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}

		CyanCore.addAllowedPackage(manifest.modClassPackage);
		coreModManifests.put(manifest.modGroup + ":" + manifest.modId, manifest);
		coreModManifests.put(manifest.modClassPackage + "." + manifest.modClassName, manifest);

		strm.close();
		classesMap.put(manifest.modGroup + ":" + manifest.modId, modClasses.toArray(t -> new String[t]));
	}

	private void importMods(File modsDirectory) {
		for (File cmf : modsDirectory.listFiles((t) -> !t.isDirectory() && t.getName().endsWith(".cmf"))) {
			try {
				importMod(cmf);
			} catch (IOException e) {
				fatal("Importing mod failed, mod: " + cmf.getName());
				StartupWindow.WindowAppender.fatalError();
				fatal("Exception was thrown in the mod loading process.", e);
				System.exit(-1);
			}
		}
	}

	@Override
	protected ClassLoader getComponentClassLoader() {
		return CyanCore.getCoreClassLoader();
	}

	private void importMod(File cmf) throws IOException {
		String ccfg = null;
		try {
			InputStream strm = new URL("jar:" + cmf.toURI().toURL() + "!/mod.manifest.ccfg").openStream();
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

		info("Importing mod " + manifest.modGroup + ":" + manifest.modId + "...");

		if (manifest.gameVersionRegex != null && manifest.gameVersionMessage != null
				&& !CyanInfo.getMinecraftVersion().matches(manifest.gameVersionRegex)) {
			fatal("Incompatible game version '" + CyanInfo.getMinecraftVersion() + "', mod " + manifest.displayName
					+ " wants " + manifest.gameVersionMessage);
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}

		if (CyanInfo.getPlatform() != LaunchPlatform.DEOBFUSCATED && CyanInfo.getPlatform() != LaunchPlatform.VANILLA
				&& CyanInfo.getPlatform() != LaunchPlatform.UNKNOWN) {
			if (!manifest.platforms.containsKey(CyanInfo.getPlatform().toString())) {
				boolean first = true;
				StringBuilder platforms = new StringBuilder();
				for (String platform : manifest.platforms.keySet()) {
					if (!first)
						platforms.append(", ");
					first = false;
					platforms.append(platform);
				}
				fatal("Incompatible platform '" + CyanInfo.getPlatform() + "', mod " + manifest.displayName
						+ " only supports the following platforms: " + platforms);
				StartupWindow.WindowAppender.fatalError();
				System.exit(-1);
			}

			String platformVersion = manifest.platforms.get(CyanInfo.getPlatform().toString());
			String cVersion = CyanLoader.platformVersion;

			if (cVersion == null)
				cVersion = CyanInfo.getModloaderVersion();

			this.checkDependencyVersion(platformVersion, Version.fromString(cVersion),
					"Incompatible platform '" + CyanInfo.getPlatform() + "' for mod '" + manifest.displayName + "'");
		}

		ZipInputStream strm = new ZipInputStream(new FileInputStream(cmf));
		boolean cacheOutOfDate = false;
		ModInfoCache info = new ModInfoCache();
		File cache = new File(cyanDir, "caches/mods/" + manifest.modId);
		File modCache = new File(cyanDir, "caches/mods/" + manifest.modId + "/mod.cache");

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
			info("(Re)building mod cache...");
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

		manifest.mavenDependencies.forEach((group, item) -> {
			item.forEach((name, version) -> {
				if (!modMavenDependencies.containsKey(group + ":" + name)
						|| Version.fromString(version).isGreaterThan(modMavenDependencies.get(group + ":" + name))) {
					modMavenDependencies.put(group + ":" + name, Version.fromString(version));
				}
			});
		});

		info("Loading mod jars...");
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

					if (!output.exists()) {
						boolean allow = false;

						for (String type : manifest.jars.get(path).split(" & ")) {
							type = type.trim();

							if (type.startsWith("platform:")) {
								String platform = type.substring("platform:".length());
								if (CyanInfo.getPlatform().toString().equalsIgnoreCase(platform)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase()
											.endsWith("-" + platform.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + platform.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("gameversion:")) {
								String gameversion = type.substring("gameversion:".length());
								if (CyanInfo.getMinecraftVersion().toString().equalsIgnoreCase(gameversion)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase().endsWith(
											"-" + gameversion.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + gameversion.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("side:")) {
								String side = type.substring("side:".length());
								if (CyanInfo.getSide().toString().equalsIgnoreCase(side)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase()
											.endsWith("-" + side.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0, path.toLowerCase().indexOf("-" + side.toLowerCase()))
												+ "." + extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("loaderversion:")) {
								String loaderversion = type.substring("loaderversion:".length());
								if (CyanInfo.getCyanVersion().toString().equalsIgnoreCase(loaderversion)) {
									String extension = path.substring(path.lastIndexOf(".") + 1);
									if (path.toLowerCase().endsWith(
											"-" + loaderversion.toLowerCase() + "." + extension.toLowerCase())) {
										path = path.substring(0,
												path.toLowerCase().indexOf("-" + loaderversion.toLowerCase())) + "."
												+ extension;

										output = new File(cache, path);
									}
									allow = true;
								} else {
									allow = false;
									break;
								}
							} else if (type.startsWith("mappingsversion:")) {
								String version = type.substring("mappingsversion:".length());
								boolean found = false;
								for (Mapping<?> mapping : Fluid.getMappings()) {
									if (mapping.mappingsVersion != null && mapping.mappingsVersion.equals(version)) {
										found = true;
										break;
									}
								}
								if (found)
									allow = true;
							} else if (type.equals("any")) {
								allow = true;
							}
						}

						if (allow) {
							info("Installing mod jar: " + path + "...");
							FileOutputStream outputStrm = new FileOutputStream(output);
							strm.transferTo(outputStrm);
							outputStrm.close();
						} else {
							ent = strm.getNextEntry();
							continue;
						}
					}
					CyanCore.addUrl(output.toURI().toURL());
				}
			}

			ent = strm.getNextEntry();
		}

		if (modManifests.containsKey(manifest.modGroup + ":" + manifest.modId)) {
			fatal("Mod conflict!");
			fatal("Mod path '" + manifest.modGroup + ":" + manifest.modId + "' was imported twice!");
			StartupWindow.WindowAppender.fatalError();
			System.exit(-1);
		}

		CyanCore.addAllowedPackage(manifest.modClassPackage);
		modManifests.put(manifest.modGroup + ":" + manifest.modId, manifest);
		modManifests.put(manifest.modClassPackage + "." + manifest.modClassName, manifest);

		strm.close();
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
			Modloader.getModloader(CyanLoader.class).startCore();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static Mapping<?> getFabricCompatibilityMappings(GameSide side, String mappingsVersion) {
		try {
			if (!loaded)
				prepare(side.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new FabricCompatibilityMappings(mappings, side, mappingsVersion);
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

	public static Mapping<?> getPaperCompatibilityMappings(String mappingsVersion) {
		try {
			if (!loaded)
				prepare(GameSide.SERVER.toString());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new PaperCompatibilityMappings(mappings, mappingsVersion);
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
		return (int) modManifests.keySet().stream().filter(t -> t.contains(":")).count()
				+ (int) coreModManifests.keySet().stream().filter(t -> t.contains(":")).count();
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
	private ArrayList<IExtendedEvent<?>> extEvents = new ArrayList<IExtendedEvent<?>>();
	private CyanEventBridge bridger = null;

	private static ArrayList<String> classHookPackages = new ArrayList<String>();
	private static ArrayList<ClassLoadHook> classHooks = new ArrayList<ClassLoadHook>();

	private static HashMap<String[], URL> transformerPackages = new HashMap<String[], URL>();
	private static HashMap<String[], URL> transformers = new HashMap<String[], URL>();

	@Override
	protected boolean presentComponent(Class<IModloaderComponent> component) {
		if (CyanEventBridge.class.isAssignableFrom(component)) {
			return true;
		} else if (IEventProvider.class.isAssignableFrom(component) || IExtendedEvent.class.isAssignableFrom(component)
				|| IPostponedComponent.class.isAssignableFrom(component)) {
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
				StartupWindow.WindowAppender.fatalError("Failed to authenticate component: " + component.getTypeName());
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
				StartupWindow.WindowAppender.fatalError("A coremod might havev been tampered with!\nPlease check log!");
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
			StartupWindow.WindowAppender.fatalError("Unauthorized component.\nPlease check log.");
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
		} else if (component instanceof IExtendedEvent) {
			if (events.contains(((IExtendedEvent<?>) component).channelName()))
				return false;

			IExtendedEvent<?> event = (IExtendedEvent<?>) component;
			extEvents.add(event);
			events.add(event.channelName());

			return true;
		} else if (component instanceof IPostponedComponent) {
			((IPostponedComponent) component).initComponent();
			return true;
		}
		if (component instanceof CyanErrorHandlers) {
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
			}, eventType -> {
				for (IExtendedEvent<?> ev : extEvents) {
					if (eventType.isAssignableFrom(ev.getClass()))
						return ev.channelName();
				}

				return null;
			}, eventType -> {
				for (IExtendedEvent<?> ev : extEvents) {
					if (eventType.isAssignableFrom(ev.getClass()))
						return ev.requiresSynchronizedListeners();
				}

				return false;
			});
			return true;
		} else if (component instanceof IAcceptableComponent) {
			IAcceptableComponent cp = (IAcceptableComponent) component;

			URL location = cp.getClass().getProtectionDomain().getCodeSource().getLocation();
			String pref = location.toString();
			if (pref.startsWith("jar:")) {
				pref = pref.substring("jar:".length(), pref.lastIndexOf("!/"));
			} else if (pref.contains(".class")) {
				pref = pref.substring(0, pref.lastIndexOf(cp.getClass().getTypeName().replace(".", "/") + ".class"));
			}

			for (String prov : cp.providers()) {
				if (!Stream.of(acceptableProviders).anyMatch(t -> t.equals(prov))) {
					return false;
				}
			}

			for (String rq : cp.infoRequests()) {
				if (rq.equals("mod.manifest")) {
					cp.provideInfo(coreModManifests.get(cp.getClass().getTypeName()), rq);
				}
			}

			String modid = cp.getClass().getSimpleName();
			for (String prov : cp.providers()) {
				if (prov.equals("auto.init")) {
					cp.provide("auto.init");
				} else if (prov.equals("mod.id")) {
					modid = cp.provide(prov).toString();
				}
			}

			for (String prov : cp.providers()) {
				if (prov.equals("transformers")) {
					String[] transformers = (String[]) cp.provide(prov);
					for (String transformer : transformers) {
						try {
							CyanLoader.transformers.put(new String[] { transformer, modid }, new URL(pref));
						} catch (IllegalStateException | MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				} else if (prov.equals("transformer-packages")) {
					String[] transformerPackages = (String[]) cp.provide(prov);
					for (String transformer : transformerPackages) {
						try {
							CyanCore.addAllowedPackage(transformer);
							CyanLoader.transformerPackages.put(new String[] { transformer, modid }, new URL(pref));
						} catch (IllegalStateException | MalformedURLException e) {
							throw new RuntimeException(e);
						}
					}
				} else if (prov.equals("class-hook-packages")) {
					String[] classHookPackages = (String[]) cp.provide(prov);
					for (String hook : classHookPackages) {
						try {
							CyanCore.addAllowedPackage(hook);
							CyanLoader.classHookPackages.add(hook);
						} catch (IllegalStateException e) {
							throw new RuntimeException(e);
						}
					}
				} else if (prov.equals("class-hooks")) {
					ClassLoadHook[] classHooks = (ClassLoadHook[]) cp.provide(prov);
					for (ClassLoadHook hook : classHooks) {
						try {
							CyanLoader.classHooks.add(hook);
						} catch (IllegalStateException e) {
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
	protected boolean postLoadOnlyComponent(Class<IModloaderComponent> component) {
		if (IExtendedEvent.class.isAssignableFrom(component) || IPostponedComponent.class.isAssignableFrom(component)) {
			return true;
		}
		return false;
	}

	private void startCore() {
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
				for (ClassLoadHook hook : classHooks) {
					Class<?> cls = hook.getClass();
					if (cls.isAnnotationPresent(SideOnly.class)
							&& cls.getAnnotation(SideOnly.class).value() != getModloaderGameSide()) {
						continue;
					} else if (cls.isAnnotationPresent(PlatformOnly.class)
							&& cls.getAnnotation(PlatformOnly.class).value() != getModloaderLaunchPlatform()) {
						continue;
					} else if (cls.isAnnotationPresent(PlatformExclude.class)
							&& cls.getAnnotation(PlatformOnly.class).value() == getModloaderLaunchPlatform()) {
						continue;
					} else if (cls.isAnnotationPresent(VersionRegex.class)
							&& !cls.getAnnotation(VersionRegex.class).modloaderVersion()
							&& !getModloaderGameVersion().matches(cls.getAnnotation(VersionRegex.class).value())) {
						continue;
					} else if (cls.isAnnotationPresent(VersionRegex.class)
							&& cls.getAnnotation(VersionRegex.class).modloaderVersion() && !getModloaderVersion()
									.toString().matches(cls.getAnnotation(VersionRegex.class).value())) {
						continue;
					}

					Fluid.registerHook(hook);
				}
				for (Class<?> hook : findClasses(getMainImplementation(), ClassLoadHook.class,
						getClass().getClassLoader())) {
					if (hook.isAnnotationPresent(SideOnly.class)
							&& hook.getAnnotation(SideOnly.class).value() != getModloaderGameSide()) {
						continue;
					} else if (hook.isAnnotationPresent(PlatformOnly.class)
							&& hook.getAnnotation(PlatformOnly.class).value() != getModloaderLaunchPlatform()) {
						continue;
					} else if (hook.isAnnotationPresent(PlatformExclude.class)
							&& hook.getAnnotation(PlatformExclude.class).value() == getModloaderLaunchPlatform()) {
						continue;
					} else if (hook.isAnnotationPresent(VersionRegex.class)
							&& !hook.getAnnotation(VersionRegex.class).modloaderVersion()
							&& !getModloaderGameVersion().matches(hook.getAnnotation(VersionRegex.class).value())) {
						continue;
					} else if (hook.isAnnotationPresent(VersionRegex.class)
							&& hook.getAnnotation(VersionRegex.class).modloaderVersion() && !getModloaderVersion()
									.toString().matches(hook.getAnnotation(VersionRegex.class).value())) {
						continue;
					}

					classHookPackages.forEach((pkg) -> {
						if (hook.getPackageName().equals(pkg) || hook.getPackageName().startsWith(pkg + ".")) {
							try {
								Fluid.registerHook((ClassLoadHook) hook.getConstructor().newInstance());
							} catch (IllegalStateException | InstantiationException | IllegalAccessException
									| IllegalArgumentException | InvocationTargetException | NoSuchMethodException
									| SecurityException e) {
							}
						}
					});
				}
			}

		});
		CyanCore.registerPreLoadHook(new Runnable() {
			@Override
			public void run() {
				info("Loading FLUID transformers...");
				Version minecraft = Version.fromString(CyanInfo.getMinecraftVersion());
				Version last = Version.fromString("0.0.0");

				String selectedPackage = "";
				Class<?>[] classes = findAnnotatedClasses(getMainImplementation(), FluidTransformer.class);
				for (Class<?> cls : classes) {
					String pkg = cls.getPackageName();
					if (!pkg.startsWith("org.asf.cyan.modifications."))
						continue;

					if (pkg.contains(".common")) {
						pkg = pkg.substring(0, pkg.lastIndexOf(".common"));
					} else if (pkg.contains(".client")) {
						pkg = pkg.substring(0, pkg.lastIndexOf(".client"));
					} else if (pkg.contains(".server")) {
						pkg = pkg.substring(0, pkg.lastIndexOf(".server"));
					}

					String version = pkg.substring(pkg.lastIndexOf(".") + 1).substring(1).replace("_", ".");
					if (Version.fromString(version).isGreaterThan(minecraft)
							|| Version.fromString(version).isLessThan(last))
						continue;

					selectedPackage = pkg;
					last = Version.fromString(version);
				}

				try {
					for (Class<?> transformer : classes) {
						String pkg = transformer.getPackageName();
						if (!pkg.startsWith("org.asf.cyan.modifications.")
								|| (!pkg.equals(selectedPackage) && !pkg.startsWith(selectedPackage + ".")))
							continue;

						if (pkg.contains(".client") && CyanInfo.getSide() != GameSide.CLIENT) {
							continue;
						} else if (pkg.contains(".server") && CyanInfo.getSide() != GameSide.SERVER) {
							continue;
						}

						if (transformer.isAnnotationPresent(SideOnly.class)
								&& transformer.getAnnotation(SideOnly.class).value() != getModloaderGameSide()) {
							continue;
						} else if (transformer.isAnnotationPresent(PlatformOnly.class) && transformer
								.getAnnotation(PlatformOnly.class).value() != getModloaderLaunchPlatform()) {
							continue;
						} else if (transformer.isAnnotationPresent(PlatformExclude.class) && transformer
								.getAnnotation(PlatformExclude.class).value() == getModloaderLaunchPlatform()) {
							continue;
						} else if (transformer.isAnnotationPresent(VersionRegex.class)
								&& !transformer.getAnnotation(VersionRegex.class).modloaderVersion()
								&& !getModloaderGameVersion()
										.matches(transformer.getAnnotation(VersionRegex.class).value())) {
							continue;
						} else if (transformer.isAnnotationPresent(VersionRegex.class)
								&& transformer.getAnnotation(VersionRegex.class).modloaderVersion()
								&& !getModloaderVersion().toString()
										.matches(transformer.getAnnotation(VersionRegex.class).value())) {
							continue;
						}

						Fluid.registerTransformer(transformer.getTypeName(),
								CyanLoader.class.getProtectionDomain().getCodeSource().getLocation());
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
				for (String[] transformerInfo : transformers.keySet()) {
					String transformer = transformerInfo[0];
					String owner = transformerInfo[1];
					URL source = transformers.get(transformerInfo);
					try {
						Class<?> cls = Class.forName(transformer, false, CyanCore.getCoreClassLoader());
						if (cls.isAnnotationPresent(SideOnly.class)
								&& cls.getAnnotation(SideOnly.class).value() != getModloaderGameSide()) {
							continue;
						} else if (cls.isAnnotationPresent(PlatformOnly.class)
								&& cls.getAnnotation(PlatformOnly.class).value() != getModloaderLaunchPlatform()) {
							continue;
						} else if (cls.isAnnotationPresent(PlatformExclude.class)
								&& cls.getAnnotation(PlatformExclude.class).value() == getModloaderLaunchPlatform()) {
							continue;
						} else if (cls.isAnnotationPresent(VersionRegex.class)
								&& !cls.getAnnotation(VersionRegex.class).modloaderVersion()
								&& !getModloaderGameVersion().matches(cls.getAnnotation(VersionRegex.class).value())) {
							continue;
						} else if (cls.isAnnotationPresent(VersionRegex.class)
								&& cls.getAnnotation(VersionRegex.class).modloaderVersion() && !getModloaderVersion()
										.toString().matches(cls.getAnnotation(VersionRegex.class).value())) {
							continue;
						}

						Fluid.registerTransformer(transformer, owner, source);
					} catch (IllegalStateException | ClassNotFoundException e) {
					}
				}

				for (Class<?> transformer : findAnnotatedClasses(getMainImplementation(), FluidTransformer.class)) {
					if (transformer.isAnnotationPresent(SideOnly.class)
							&& transformer.getAnnotation(SideOnly.class).value() != getModloaderGameSide()) {
						continue;
					} else if (transformer.isAnnotationPresent(PlatformOnly.class)
							&& transformer.getAnnotation(PlatformOnly.class).value() != getModloaderLaunchPlatform()) {
						continue;
					} else if (transformer.isAnnotationPresent(PlatformExclude.class) && transformer
							.getAnnotation(PlatformExclude.class).value() == getModloaderLaunchPlatform()) {
						continue;
					} else if (transformer.isAnnotationPresent(VersionRegex.class)
							&& !transformer.getAnnotation(VersionRegex.class).modloaderVersion()
							&& !getModloaderGameVersion()
									.matches(transformer.getAnnotation(VersionRegex.class).value())) {
						continue;
					} else if (transformer.isAnnotationPresent(VersionRegex.class)
							&& transformer.getAnnotation(VersionRegex.class).modloaderVersion()
							&& !getModloaderVersion().toString()
									.matches(transformer.getAnnotation(VersionRegex.class).value())) {
						continue;
					}

					transformerPackages.forEach((pkgInfo, source) -> {
						String pkg = pkgInfo[0];
						String owner = pkgInfo[1];
						if (transformer.getPackageName().equals(pkg)
								|| transformer.getPackageName().startsWith(pkg + ".")) {
							try {
								transformers.put(new String[] { transformer.getTypeName(), owner }, source);
								Fluid.registerTransformer(transformer.getTypeName(), owner, source);
							} catch (IllegalStateException | ClassNotFoundException e) {
							}
						}
					});
				}
			}
		});

		EventUtilImpl.init();
		BaseEventController.addEventContainer(this);

		try {
			registerPath(getClass());
		} catch (MalformedURLException e) {
		}

		createEventChannel("mods.aftermodloader");
		createEventChannel("mods.prestartgame");
		createEventChannel("mod.loaded");
		createEventChannel("mods.all.loaded");
		createEventChannel("modloader.register.path");
		createEventChannel("mods.load.regular.start");

		BaseEventController.work();

		info("Starting CyanCore...");
		CyanCore.addAllowedTransformerAutoDetectClass("org.asf.cyan.api.internal.CyanAPIComponent");
		CyanCore.initializeComponents();

		info("Downloading coremod maven dependencies...");
		downloadMavenDependencies(coremodMavenDependencies);

		File mods = new File(cyanDir, "mods");
		File versionMods = new File(mods, CyanInfo.getMinecraftVersion());
		StartupWindow.WindowAppender.increaseProgress();

		info("Scanning CMF mod files...");
		if (!mods.exists())
			mods.mkdirs();
		StartupWindow.WindowAppender.increaseProgress();

		if (versionMods.exists()) {
			info("Importing version-specific mods...");
			importMods(versionMods);
		}
		StartupWindow.WindowAppender.increaseProgress();

		info("Downloading mod maven dependencies...");
		downloadMavenDependencies(modMavenDependencies);
		StartupWindow.WindowAppender.increaseProgress();

		info("Importing regular mods...");
		importMods(mods);
		StartupWindow.WindowAppender.increaseProgress();
	}

	private static ArrayList<Path> paths = new ArrayList<Path>();

	@AttachEvent(value = "modloader.register.path", synchronize = true)
	public void registerPath(Class<?> owner) throws MalformedURLException {
		URL u = owner.getProtectionDomain().getCodeSource().getLocation();
		if (u.toString().startsWith("jar:"))
			u = new URL(u.toString().substring(4, u.toString().lastIndexOf("!/")));
		else if (u.toString().endsWith(owner.getTypeName().replace(".", "/") + ".class")) {
			u = new URL(u.toString().substring(0,
					u.toString().lastIndexOf(owner.getTypeName().replace(".", "/") + ".class")));
		}
		try {
			paths.add(Path.of(u.toURI()));
		} catch (URISyntaxException e) {
		}
	}

	private boolean gameLoad = false;

	@AttachEvent(value = "mods.prestartgame", synchronize = true)
	private void beforeGame(ClassLoader loader) throws ClassNotFoundException {
		if (gameLoad)
			return;
		gameLoad = true;
		info("Finishing bootstrap... Loading postponed components...");
		loadPostponedComponents(loader);
		StartupWindow.WindowAppender.increaseProgress();

		info("Loading final events...");
		loadEvents();
		StartupWindow.WindowAppender.increaseProgress();

		info("Loading coremods...");
		loadCoreMods(loader);
		StartupWindow.WindowAppender.increaseProgress();

		dispatchEvent("mods.aftermodloader", loader);
		BaseEventController.work();
		StartupWindow.WindowAppender.increaseProgress();
	}

	@Override
	protected void postRegister() {
		loadEvents();
	}

	private void loadEvents() {
		for (String event : events) {
			try {
				Optional<IExtendedEvent<?>> optEvent = extEvents.stream().filter(t -> t.channelName().equals(event))
						.findFirst();
				if (optEvent.isPresent()) {
					IExtendedEvent<?> extEvent = optEvent.get();

					extEvent.afterInstantiation();
					extEvent.assign(createEventChannel(event));
				} else {
					createEventChannel(event);
				}
			} catch (IllegalStateException e) {
			}
		}
		events.clear();
	}

	private void downloadMavenDependencies(HashMap<String, Version> mavenDependencies) {
		mavenDependencies.forEach((dep, version) -> {
			String group = dep.split(":")[0];
			String name = dep.split(":")[1];

			String path = group.replace(".", "/") + "/" + name + "/" + version.toString() + "/" + name + "-"
					+ version.toString() + ".jar";
			File libFile = new File("libraries/" + path);
			if (!libFile.getParentFile().exists())
				libFile.getParentFile().mkdirs();

			if (!libFile.exists()) {
				boolean downloaded = false;
				for (String repo : mavenRepositories.keySet()) {
					String url = mavenRepositories.get(repo);
					info("Trying to download " + group + ":" + name + " from " + repo + " (" + url + ")...");
					try {
						URL u = new URL(url + "/" + path);
						debug("Full URL: " + u);

						InputStream strm = u.openStream();
						FileOutputStream libOut = new FileOutputStream(libFile);
						strm.transferTo(libOut);
						strm.close();
						libOut.close();
						downloaded = true;
						info("Done.");
						break;
					} catch (IOException e) {
					}
				}
				if (!downloaded) {
					fatal("Could not download dependency " + group + ":" + name + " from ANY maven repository!");
					StartupWindow.WindowAppender.fatalError();
					System.exit(1);
				}
			}

			try {
				CyanCore.addCoreUrl(libFile.toURI().toURL());
			} catch (MalformedURLException e) {
				fatal("Could not load dependency " + group + ":" + name);
				StartupWindow.WindowAppender.fatalError();
				fatal("Exception was thrown during dependency download.", e);
				System.exit(1);
			}
		});
	}

	public IMod[] getAllModInstances() {
		ArrayList<IMod> mods = new ArrayList<IMod>();
		mods.addAll(this.mods);
		mods.addAll(this.coremods);
		return mods.toArray(t -> new IMod[t]);
	}

	private boolean loadedMods = false;

	@AttachEvent(value = "mods.load.regular.start", synchronize = true)
	private void loadMods(ClassLoader loader) throws IOException {
		if (loadedMods)
			return;
		loadedMods = true;

		info("Loading regular mods...");
		loadModClasses(loader);
		StartupWindow.WindowAppender.increaseProgress();

		info("Looking for the KickStart CYAN Installer...");
		String dir = System.getenv("APPDATA");
		if (dir == null)
			dir = System.getProperty("user.home");
		File installs = new File(dir, ".kickstart-installer.ccfg");
		if (installs.exists()) {
			info("Adding current installation to manifest...");
			KickStartConfig conf = new KickStartConfig();
			conf.readAll(new String(Files.readAllBytes(installs.toPath())));
			ArrayList<KickStartConfig.KickStartInstallation> configs = new ArrayList<KickStartConfig.KickStartInstallation>();
			for (KickStartConfig.KickStartInstallation install : conf.installations) {
				if (new File(install.cyanData).exists() && !install.cyanData.equals(cyanDir.getCanonicalPath()))
					configs.add(install);
			}
			KickStartConfig.KickStartInstallation install = new KickStartConfig.KickStartInstallation();
			install.side = Modloader.getModloaderGameSide();
			install.cyanData = cyanDir.getCanonicalPath();
			install.gameVersion = Modloader.getModloaderGameVersion();
			install.platformVersion = platformVersion;
			install.platform = Modloader.getModloaderLaunchPlatform().toString();
			install.loaderVersion = getVersion().toString();
			configs.add(install);
			conf.installations = configs.toArray(t -> new KickStartConfig.KickStartInstallation[t]);
			Files.write(installs.toPath(), conf.toString().getBytes());
		}
		StartupWindow.WindowAppender.increaseProgress();

		dispatchEvent("mods.all.loaded");
		dispatchEvent("mods.all.loaded", loader);
		StartupWindow.WindowAppender.increaseProgress();
	}

	private void loadModClasses(ClassLoader loader) {
		modManifests.forEach((k, manifest) -> {
			if (k.contains(":") && !manifest.loaded)
				loadMod(false, manifest, new ArrayList<String>(), loader);
		});
	}

	/**
	 * Retrieves a mod instance by its class
	 * 
	 * @param modClass Mod class
	 * @return Mod instance or null
	 */
	@SuppressWarnings("unchecked")
	public <T extends IBaseMod> T getModByClass(Class<T> modClass) {
		for (IMod md : mods) {
			if (md.getClass().isAssignableFrom(modClass))
				return (T) md;
		}
		for (IMod md : coremods) {
			if (md.getClass().isAssignableFrom(modClass))
				return (T) md;
		}
		return null;
	}

	public static void addCyanPaths(ArrayList<Path> paths) {
		for (Path pth : CyanCore.getAddedPaths()) {
			paths.add(pth);
		}
		paths.addAll(CyanLoader.paths);
	}

	public static void crash() {
		CyanLoader.getModloader(CyanLoader.class);
	}

	static String[] coremodTypes = null;
	static String[] cyanClasses = new String[] { "org.asf.cyan.CyanLoader",
			"org.asf.cyan.mods.events.IEventListenerContainer", "org.asf.cyan.mods.internal.BaseEventController",
			"org.asf.cyan.mods.IMod", "org.asf.cyan.mods.ICoreMod", "org.asf.cyan.mods.AbstractCoremod",
			"org.asf.cyan.mods.AbstractMod", "org.asf.cyan.mods.IBaseMod", "org.asf.cyan.core.CyanCore",
			"org.asf.cyan.api.modloader.Modloader", "org.asf.cyan.api.common.CyanComponent",
			"org.asf.cyan.api.config.Configuration", "org.asf.cyan.api.util.EventUtil",
			"org.asf.cyan.api.util.ContainerConditions", "org.asf.cyan.api.internal.CyanAPIComponent",
			"org.asf.cyan.mods.config.CyanModfileManifest" };

	public static boolean doNotTransform(String name) {
		if (coremodTypes == null) {
			coremodTypes = CyanLoader.getModloader(CyanLoader.class).coremods.stream()
					.map(t -> t.getClass().getTypeName()).toArray(t -> new String[t]);
		}

		return Stream.of(cyanClasses).anyMatch(t -> t.equals(name))
				|| Stream.of(coremodTypes).anyMatch(t -> t.equals(name));
	}

	public static void setCallTraceClassLoader(ClassLoader loader) {
		CallTrace.setCallTraceClassLoader(loader);
	}

	private static ArrayList<String> locations = null;

	private static CyanLoader cyanModloader = null;

	public static InputStream getFabricClassStream(String name) throws MalformedURLException {
		if (doNotTransform(name))
			return null;

		if (cyanModloader == null)
			cyanModloader = CyanLoader.getModloader(CyanLoader.class);

		if (locations == null) {
			locations = new ArrayList<String>();
			for (Path pth : CyanCore.getAddedPaths()) {
				String path = pth.toString();
				if (path.endsWith(".jar") || path.endsWith(".zip"))
					path = "jar:" + pth.toUri().toURL() + "!/";
				if (!path.endsWith("/"))
					path += "/";
				locations.add(path);
			}
			for (Path pth : paths) {
				String path = pth.toString();
				if (path.endsWith(".jar") || path.endsWith(".zip"))
					path = "jar:" + pth.toUri().toURL() + "!/";
				if (!path.endsWith("/"))
					path += "/";
				locations.add(path);
			}
		}

		for (String loc : locations) {
			try {
				URL u = new URL(loc + name.replace(".", "/") + ".class");
				InputStream strm = u.openStream();
				return strm;
			} catch (IOException e) {
			}
		}

		return null;
	}

	public static String getModSourceBase(Class<? extends IMod> modType) {
		String name = modType.getTypeName();

		if (cyanModloader == null)
			cyanModloader = CyanLoader.getModloader(CyanLoader.class);

		if (cyanModloader.modManifests.containsKey(name))
			return cyanModloader.modManifests.get(name).source;
		else if (cyanModloader.coreModManifests.containsKey(name))
			return cyanModloader.coreModManifests.get(name).source;

		return null;
	}

}
