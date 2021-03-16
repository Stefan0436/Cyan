package org.asf.cyan;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;

import org.asf.cyan.api.events.IEventProvider;
import org.asf.cyan.api.events.core.EventBusFactory;
import org.asf.cyan.api.modloader.IModloaderComponent;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.providers.IModProvider;
import org.asf.cyan.api.versioning.StringVersionProvider;
import org.asf.cyan.core.CyanCore;
import org.asf.cyan.core.CyanInfo;
import org.asf.cyan.core.SimpleModloader;
import org.asf.cyan.fluid.Fluid;
import org.asf.cyan.fluid.remapping.Mapping;
import org.asf.cyan.loader.eventbus.CyanEventBridge;

import org.asf.cyan.minecraft.toolkits.mtk.FabricCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.ForgeCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftInstallationToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftMappingsToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.MinecraftVersionToolkit;
import org.asf.cyan.minecraft.toolkits.mtk.PaperCompatibilityMappings;
import org.asf.cyan.minecraft.toolkits.mtk.versioninfo.MinecraftVersionInfo;

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

	private static final PrintStream defaultOutputStream = System.out;
	private static ArrayList<Mapping<?>> compatibilityMappings = new ArrayList<Mapping<?>>();
	private static Mapping<?> mappings = null;
	private static boolean vanillaMappings = true;
	private static boolean loaded = false;
	private static File cyanDir;

	public static File getCyanDataDirectory() {
		return cyanDir;
	}

	public static boolean areVanillaMappingsEnabled() {
		return vanillaMappings;
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

		// TODO: add core mod components to the cyancore so they can be presented to our
		// modloader

		CyanLoader ld = new CyanLoader();
		Modloader.addModloaderImplementation(ld);
		ld.addInformationProvider(CyanInfo.getProvider());
		ld.addInformationProvider(ld);
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
					info("Loading Cyan Coremods...");

					// TODO: Core modules loading
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
	public Object[] getLoadedNormalMods() {
		return new Object[0]; // TODO
	}

	@Override
	public Object[] getLoadedCoreMods() {
		return new Object[0]; // TODO
	}

	@Override
	public int getAllKnownModsLength() {
		return 0; // TODO
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

	@Override
	protected boolean presentComponent(Class<IModloaderComponent> component) {
		if (CyanEventBridge.class.isAssignableFrom(component)) {
			return true;
		} else if (IEventProvider.class.isAssignableFrom(component)) {
			return true;
		} else if (CyanErrorHandlers.class.isAssignableFrom(component)) {
			return true;
		}
		return false;
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
	}
}
