package org.asf.cyan.api.modloader;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Stream;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.core.EventBus;
import org.asf.cyan.api.events.core.EventBusFactory;
import org.asf.cyan.api.events.core.IEventListener;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.api.modloader.information.modloader.LoadPhase;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.modloader.information.providers.IGameProvider;
import org.asf.cyan.api.modloader.information.providers.ILaunchPlatformProvider;
import org.asf.cyan.api.modloader.information.providers.IModProvider;
import org.asf.cyan.api.modloader.information.providers.IModloaderInfoProvider;
import org.asf.cyan.api.modloader.information.providers.IPhaseProvider;
import org.asf.cyan.api.modloader.information.providers.IVersionProvider;
import org.asf.cyan.api.modloader.information.providers.IVersionChangelogProvider;
import org.asf.cyan.api.modloader.information.providers.IVersionStatusProvider;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.api.versioning.VersionStatus;

/**
 * 
 * Modloader information system.<br />
 * <b>Warning:</b> this class needs one or more implementations to work.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class Modloader extends CyanComponent {
	protected Modloader() {
	}

	private ArrayList<IModloaderInfoProvider> informationProviders = new ArrayList<IModloaderInfoProvider>();

	private static Modloader selectedImplementation;
	private Modloader nextImplementation;

	private static EventBus sharedEventBus;

	private boolean manualAppend = false;
	private EventBus modloaderEventBus;

	private String brandCache = null;

	private EventBusFactory<?> eventBusFactory;
	private EventBusFactory<?> sharedEventBusFactory;
	private IVersionStatusProvider statusProvider;
	private IVersionProvider versionProvider;
	private IModProvider modProvider;
	private IVersionChangelogProvider changelogProvider;
	private ILaunchPlatformProvider platformProvider;
	private IGameProvider gameProvider;
	private IPhaseProvider phaseProvider;

	private boolean noStatusProvider = false;
	private boolean noVersionProvider = false;
	private boolean noModProvider = false;
	private boolean noChangelogProvider = false;
	private boolean noPlatformProvider = false;
	private boolean noGameProvider = false;
	private boolean noPhaseProvider = false;

	private ArrayList<String> postLoadComponents = new ArrayList<String>();

	@SuppressWarnings("unchecked")
	protected void loadPostponedComponents(ClassLoader... loaders) throws ClassNotFoundException {
		for (String typeName : postLoadComponents) {
			Constructor<IModloaderComponent> ctor;
			IModloaderComponent component;
			try {
				Class<IModloaderComponent> componentCls = null;
				ClassNotFoundException e = null;
				for (ClassLoader loader : loaders) {
					try {
						componentCls = (Class<IModloaderComponent>) loader.loadClass(typeName);
						e = null;
						break;
					} catch (ClassNotFoundException | NoClassDefFoundError ex) {
						if (e == null && ex instanceof ClassNotFoundException)
							e = (ClassNotFoundException) ex;
					}
				}
				if (e != null)
					throw e;
				if (!presentComponent(componentCls)) {
					continue;
				}
				ctor = componentCls.getDeclaredConstructor();
				ctor.setAccessible(true);
				component = ctor.newInstance();
			} catch (IllegalAccessException | NoSuchMethodException | SecurityException | InstantiationException
					| IllegalArgumentException | InvocationTargetException ex) {
				error("Could not execute component " + typeName + " though it was accepted by the modloader", ex);
				continue;
			}
			if (execComponent(component))
				debug(getName() + " accepted the " + typeName + " component.");
			else
				debug(getName() + " rejected the " + typeName + " component.");
		}
		postLoadComponents.clear();
	}

	/**
	 * Present a component to the modloader, intended to be overridden.
	 * 
	 * @param component Component to present.
	 * @return True if the component is supported, false otherwise
	 */
	protected boolean presentComponent(Class<IModloaderComponent> component) {
		return false;
	}

	/**
	 * Checks if a modloader component needs to be loaded later
	 * 
	 * @param component Component to check.
	 * @return True if the component needs to be loaded later, false otherwise
	 */
	protected boolean postLoadOnlyComponent(Class<IModloaderComponent> component) {
		return false;
	}

	/**
	 * Checks if the modloader supports anonymous (any=true) components. False by
	 * default.
	 * 
	 * @return True if anonymous components are allowed, false otherwise
	 */
	protected boolean acceptsAnonymousComponent() {
		return false;
	}

	/**
	 * Executes accepted components, intended to be overridden.
	 * 
	 * @param component Component to run.
	 * @return True if the component is supported, false otherwise
	 */
	protected boolean execComponent(IModloaderComponent component) {
		return false;
	}

	/**
	 * Creates an event bus factory of this modloader (if any)
	 */
	protected EventBusFactory<?> createEventBusFactory() {
		return null;
	}

	/**
	 * Gets the event bus factory of this modloader (if any)
	 */
	protected EventBusFactory<?> getEventBusFactory() {
		return eventBusFactory;
	}

	/**
	 * Gets the shared event bus factory of this modloader (if any)
	 */
	protected EventBusFactory<?> getSharedEventBusFactory() {
		return sharedEventBusFactory;
	}

	/**
	 * Retrieves the main modloader.
	 */
	public static Modloader getModloader() {
		return selectedImplementation;
	}

	/**
	 * Retrieves the side the game is running on.
	 */
	public static GameSide getModloaderGameSide() {
		return selectedImplementation.getGameSide();
	}

	/**
	 * Retrieves the version of the modlaoder.
	 */
	public static Version getModloaderVersion() {
		return selectedImplementation.getVersion();
	}

	/**
	 * Retrieves the game name of the modloader.
	 */
	public static String getModloaderGameName() {
		return selectedImplementation.getGameName();
	}

	/**
	 * Retrieves the modloader name.
	 */
	public static String getModloaderName() {
		return selectedImplementation.getName();
	}

	/**
	 * Retrieves the phase of the main modloader.
	 */
	public static LoadPhase getLoadingPhase() {
		return selectedImplementation.getPhase();
	}

	/**
	 * Retrieves the version status of the modlaoder.
	 */
	public static VersionStatus getModloaderVersionStatus() {
		return selectedImplementation.getVersionStatus();
	}

	/**
	 * Retrieves the launch platform the game is running through.
	 */
	public static LaunchPlatform getModloaderLaunchPlatform() {
		return selectedImplementation.getLaunchPlatform();
	}

	/**
	 * Retrieves the brand of the modloader for the game.
	 */
	public static String getModloaderGameBrand() {
		return selectedImplementation.getGameBrand();
	}

	/**
	 * Gets the game version given by the first modloader.
	 */
	public static String getModloaderGameVersion() {
		return selectedImplementation.getGameVersion();
	}

	/**
	 * Retrieve a specific modloader.
	 * 
	 * @param name Modloader name.
	 * @return The modloader instance, null if it was not found.
	 */
	public static Modloader getModloader(String name) {
		Modloader impl = selectedImplementation;
		while (impl != null) {
			if (impl.getName().equalsIgnoreCase(name))
				return impl;
			impl = impl.getNextImplementation();
		}
		while (impl != null) {
			if (impl.getSimpleName().equalsIgnoreCase(name))
				return impl;
			impl = impl.getNextImplementation();
		}
		return null;
	}

	/**
	 * Retrieve a specific modloader.
	 * 
	 * @param <T>            Modloader type
	 * @param modloaderClass Modloader class
	 * @return The modloader instance, null if it was not found.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Modloader> T getModloader(Class<T> modloaderClass) {
		Modloader impl = selectedImplementation;
		while (impl != null) {
			if (modloaderClass.isAssignableFrom(impl.getClass()))
				return (T) impl;
			impl = impl.getNextImplementation();
		}
		return null;
	}

	/**
	 * Retrieves the child modloaders of the main running modloader.
	 */
	public static Modloader[] getAllModloaders() {
		ArrayList<Modloader> loaders = new ArrayList<Modloader>();
		if (getModloader() == null)
			return new Modloader[0];
		loaders.add(getModloader());
		for (Modloader l : getModloader().getChildren()) {
			loaders.add(l);
		}
		return loaders.toArray(t -> new Modloader[t]);
	}

	/**
	 * Get all loaded mods of all modloaders (including coremods)
	 */
	public static IModManifest[] getAllMods() {
		ArrayList<IModManifest> mods = new ArrayList<IModManifest>();
		for (Modloader loader : getAllModloaders()) {
			mods.addAll(Arrays.asList(loader.getLoadedMods()));
			mods.addAll(Arrays.asList(loader.getLoadedCoremods()));
		}
		return mods.toArray(t -> new IModManifest[t]);
	}

	/**
	 * Retrieves the next implementation of the Modloader abstract.
	 */
	public Modloader getNextImplementation() {
		return nextImplementation;
	}

	/**
	 * Adds modloaders to the implementation index.
	 * 
	 * @param modloader Modloader to add.
	 */
	@SuppressWarnings("unchecked")
	protected static void addModloaderImplementation(Modloader modloader) {
		debug("Assigning modloader implementation... Implementation name: " + modloader.getImplementationName());

		if (getModloader(modloader.getName()) != null && !getModloader(modloader.getName()).manualAppend) {
			throw new RuntimeException("Modloader conflict! Duplicate modloader detected: " + modloader.getName()
					+ " found in both " + modloader.getClass().getTypeName() + " and "
					+ getModloader(modloader.getName()).getClass().getTypeName());
		} else if (getModloader(modloader.getName()) == null) {
			appendImplementation(modloader);
		}

		getModloader(modloader.getName()).manualAppend = false;
		debug("Added modloader: " + modloader.getName());
		debug("Searching for IModloaderComponent implementations...");

		String[] components = findClassNames(getMainImplementation(), IModloaderComponent.class);
		String[] postPonedComponents = findClassNames(getMainImplementation(), IPostponedComponent.class);

		for (String typeName : components) {
			if (Stream.of(postPonedComponents).anyMatch(t -> t.equals(typeName))) {
				modloader.postLoadComponents.add(typeName);
				continue;
			}
			Class<IModloaderComponent> componentCls;
			try {
				componentCls = (Class<IModloaderComponent>) modloader.getComponentClassLoader().loadClass(typeName);
			} catch (ClassNotFoundException e2) {
				continue;
			}
			if (!componentCls.isAnnotationPresent(TargetModloader.class)
					|| (!componentCls.getAnnotation(TargetModloader.class).value().getTypeName()
							.equals(modloader.getClass().getTypeName())
							&& !(modloader.acceptsAnonymousComponent()
									&& componentCls.getAnnotation(TargetModloader.class).any()))
					|| Modifier.isAbstract(componentCls.getModifiers()) || componentCls.isInterface()) {
				continue;
			}
			if (modloader.presentComponent(componentCls)) {
				if (!modloader.postLoadOnlyComponent(componentCls)) {
					Constructor<IModloaderComponent> ctor;
					IModloaderComponent component;
					try {
						ctor = componentCls.getDeclaredConstructor();
						ctor.setAccessible(true);
						component = ctor.newInstance();
					} catch (IllegalAccessException | NoSuchMethodException | SecurityException | InstantiationException
							| IllegalArgumentException | InvocationTargetException ex) {
						error("Could not execute component " + componentCls.getTypeName()
								+ " though it was accepted by the modloader", ex);
						continue;
					}
					if (modloader.execComponent(component))
						debug(modloader.getName() + " accepted the " + componentCls.getTypeName() + " component.");
					else
						debug(modloader.getName() + " rejected the " + componentCls.getTypeName() + " component.");
				} else {
					modloader.postLoadComponents.add(componentCls.getTypeName());
				}
			} else {
				debug(modloader.getName() + " rejected the " + componentCls.getTypeName() + " component.");
			}
		}

		debug("Trying to assign the event bus...");
		modloader.eventBusFactory = modloader.createEventBusFactory();
		modloader.sharedEventBusFactory = modloader.createEventBusFactory();
		if (modloader.eventBusFactory != null) {
			debug("Assigning event bus... Using factory class: " + modloader.eventBusFactory.getClass().getTypeName());

			if (sharedEventBus == null) {
				sharedEventBus = modloader.sharedEventBusFactory.createBus("modloaders.shared");
			}

			modloader.modloaderEventBus = modloader.eventBusFactory
					.createBus("modloader." + modloader.getName().toLowerCase() + ".events");
		}

		modloader.postRegister();
	}

	protected ClassLoader getComponentClassLoader() {
		return getClass().getClassLoader();
	}

	protected static void appendImplementation(Modloader modloader) {
		modloader.manualAppend = true;
		if (selectedImplementation != null) {
			Modloader loader = selectedImplementation;
			while (loader.nextImplementation != null)
				loader = loader.getNextImplementation();

			loader.nextImplementation = modloader;
		} else {
			selectedImplementation = modloader;
		}
	}

	protected EventBus getEventChannel(String name) {
		return getBusRecursive(name, CallTrace.traceCallName());
	}

	private EventBus getBusRecursive(String name, String caller) {
		if (modloaderEventBus == null) {
			if (getNextImplementation() == null)
				throw new IllegalStateException("Event channel " + name + " could not be found, caller: " + caller);

			return getNextImplementation().getBusRecursive(name, caller);
		}

		name = name.toLowerCase();
		if (getBus(modloaderEventBus.getChannel() + "." + name) == null) {
			if (getNextImplementation() == null)
				throw new IllegalStateException("Event channel " + name + " could not be found, caller: " + caller);

			return getNextImplementation().getBusRecursive(name, caller);
		}

		return getBus(modloaderEventBus.getChannel() + "." + name);
	}

	protected EventBus createEventChannel(String name) {
		name = name.toLowerCase();
		if (getBus(modloaderEventBus.getChannel() + "." + name) != null)
			throw new IllegalStateException("Event channel " + name + " already registered. Modloader: " + getName());

		debug("Creating modloader event channel " + modloaderEventBus.getChannel() + "." + name + "...");
		return eventBusFactory.createBus(modloaderEventBus.getChannel() + "." + name);
	}

	protected EventBus getSharedEventChannel(String name) {
		name = name.toLowerCase();

		if (getSharedBus(name) == null)
			throw new IllegalStateException(
					"Shared event channel " + name + " could not be found, caller: " + CallTrace.traceCallName());

		return getSharedBus(name);
	}

	protected EventBus createSharedEventChannel(String name) {
		name = name.toLowerCase();

		if (getBus(name) != null)
			throw new IllegalStateException(
					"Shared event channel " + name + " already registered. Modloader: " + getName());

		debug("Creating shared event channel " + name + "...");
		return sharedEventBusFactory.createBus(name);
	}

	protected EventBus getBus(String channel) {
		EventBus bus = modloaderEventBus;
		while (bus != null) {
			if (bus.getChannel().equals(channel)) {
				return bus;
			}

			bus = eventBusFactory.getChildBus(bus);
		}
		return null;
	}

	protected EventBus getSharedBus(String channel) {
		EventBus bus = sharedEventBus;
		while (bus != null) {
			if (bus.getChannel().equals(channel)) {
				return bus;
			}

			bus = eventBusFactory.getChildBus(bus);
		}
		return null;
	}

	/**
	 * Gets all provider instances for this modloader by a provider class.
	 * 
	 * @param <T>           Provider type
	 * @param providerClass Provider class
	 * @return Array of providers.
	 */
	@SuppressWarnings("unchecked")
	protected <T extends IModloaderInfoProvider> T[] getProviders(Class<T> providerClass) {
		ArrayList<T> providers = new ArrayList<T>();

		for (IModloaderInfoProvider provider : informationProviders) {
			if (providerClass.isAssignableFrom(provider.getClass())) {
				providers.add((T) provider);
			}
		}

		return providers.toArray(t -> (T[]) Array.newInstance(providerClass, t));
	}

	/**
	 * Adds information providers to the modloader.
	 */
	protected void addInformationProvider(IModloaderInfoProvider provider) {
		debug("Adding information provider " + provider.getClass().getTypeName() + " to modloader " + getName()
				+ "...");
		informationProviders.add(provider);
	}

	/**
	 * Retrieves the child modloaders of this modloader.
	 */
	public Modloader[] getChildren() {
		ArrayList<Modloader> modloaders = new ArrayList<Modloader>();
		Modloader impl = this.getNextImplementation();
		while (impl != null) {
			modloaders.add(impl);
			impl = impl.getNextImplementation();
		}
		impl = this.getNextImplementation();
		while (impl != null) {
			recurseModloaders(modloaders, impl);
			impl = impl.getNextImplementation();
		}
		return modloaders.toArray(t -> new Modloader[t]);
	}

	private void recurseModloaders(ArrayList<Modloader> loaders, Modloader loader) {
		for (Modloader child : loader.getChildren()) {
			if (!loaders.stream().anyMatch(t -> t.getName().equals(child.getName()))) {
				loaders.add(child);
				recurseModloaders(loaders, child);
			}
		}
		if (!loaders.stream().anyMatch(t -> t.getName().equals(loader.getName()))) {
			loaders.add(loader);
		}
	}

	/**
	 * Retrieves the implementation name.
	 */
	protected abstract String getImplementationName();

	/**
	 * Retrieves the simple loader name.
	 */
	public abstract String getSimpleName();

	/**
	 * Retrieves the name of the modloader.
	 */
	public abstract String getName();

	/**
	 * Retrieves the version of the modloader. (if present)
	 */
	public Version getVersion() {
		if (versionProvider == null && !noVersionProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IVersionProvider) {
					versionProvider = (IVersionProvider) info;
					return versionProvider.getLoaderVersion();
				}
			}
			noVersionProvider = true;
		} else if (!noVersionProvider) {
			return versionProvider.getLoaderVersion();
		}

		return Version.fromString("none");
	}

	/**
	 * Retrieves the version status of the modloader. (if present)
	 */
	public VersionStatus getVersionStatus() {
		if (statusProvider == null && !noStatusProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IVersionStatusProvider) {
					statusProvider = (IVersionStatusProvider) info;
					return statusProvider.getModloaderVersionStatus();
				}
			}
			noStatusProvider = true;
		} else if (!noStatusProvider) {
			return statusProvider.getModloaderVersionStatus();
		}

		return VersionStatus.UNKNOWN;
	}

	/**
	 * Retrieves the game side of the modloader.
	 */
	public GameSide getGameSide() {
		if (gameProvider == null && !noGameProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IGameProvider) {
					gameProvider = (IGameProvider) info;
					return gameProvider.getGameSide();
				}
			}
			noGameProvider = true;
		} else if (!noGameProvider) {
			return gameProvider.getGameSide();
		}

		if (getNextImplementation() == null)
			return null;

		return getNextImplementation().getGameSide();
	}

	/**
	 * Retrieves the game version of the modloader.
	 */
	public String getGameVersion() {
		if (gameProvider == null && !noGameProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IGameProvider) {
					gameProvider = (IGameProvider) info;
					return gameProvider.getGameVersion();
				}
			}
			noGameProvider = true;
		} else if (!noGameProvider) {
			return gameProvider.getGameVersion();
		}

		if (getNextImplementation() == null)
			return null;

		return getNextImplementation().getGameVersion();
	}

	/**
	 * Retrieves the game name of the modloader.
	 */
	public String getGameName() {
		if (gameProvider == null && !noGameProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IGameProvider) {
					gameProvider = (IGameProvider) info;
					return gameProvider.getGameName();
				}
			}
			noGameProvider = true;
		} else if (!noGameProvider) {
			return gameProvider.getGameName();
		}

		if (getNextImplementation() == null)
			return null;

		return getNextImplementation().getGameName();
	}

	/**
	 * Retrieves the launch platform of the modloader.
	 */
	public LaunchPlatform getLaunchPlatform() {
		if (platformProvider == null && !noPlatformProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof ILaunchPlatformProvider) {
					platformProvider = (ILaunchPlatformProvider) info;
					return platformProvider.getPlatform();
				}
			}
			noPlatformProvider = true;
		} else if (!noPlatformProvider) {
			return platformProvider.getPlatform();
		}

		if (getNextImplementation() == null)
			return null;

		return getNextImplementation().getLaunchPlatform();
	}

	/**
	 * Retrieves the current changelog of the modloader.
	 */
	public String getVersionChangelog() {
		if (changelogProvider == null && !noChangelogProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IVersionChangelogProvider) {
					changelogProvider = (IVersionChangelogProvider) info;
					return changelogProvider.getCurrentVersionChangelog();
				}
			}
			noChangelogProvider = true;
		} else if (!noChangelogProvider) {
			return changelogProvider.getCurrentVersionChangelog();
		}

		return "";
	}

	/**
	 * Retrieves the changelog of the newer version.
	 */
	public String getUpdateVersionChangelog() {
		if (changelogProvider == null && !noChangelogProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IVersionChangelogProvider) {
					changelogProvider = (IVersionChangelogProvider) info;
					return changelogProvider.getUpdateVersionChangelog();
				}
			}
			noChangelogProvider = true;
		} else if (!noChangelogProvider) {
			return changelogProvider.getUpdateVersionChangelog();
		}

		return "";
	}

	private boolean noModSupport = false;
	private boolean noCoreModSupport = false;

	public boolean supportsMods() {
		getLoadedMods();
		return !noModSupport;
	}

	public boolean supportsCoreMods() {
		getLoadedCoremods();
		return !noCoreModSupport;
	}

	/**
	 * Retrieves the mods registered for loading by this modloader.
	 */
	public IModManifest[] getKnownMods() {
		if (modProvider == null && !noModProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IModProvider) {
					modProvider = (IModProvider) info;
					return modProvider.getKnownNormalMods();
				}
			}
			noModProvider = true;
		} else if (!noModProvider) {
			return modProvider.getKnownNormalMods();
		}

		noModSupport = true;
		return new IModManifest[0];
	}

	/**
	 * Retrieves the mods loaded by this modloader.
	 */
	public IModManifest[] getLoadedMods() {
		if (modProvider == null && !noModProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IModProvider) {
					modProvider = (IModProvider) info;
					return modProvider.getLoadedNormalMods();
				}
			}
			noModProvider = true;
		} else if (!noModProvider) {
			return modProvider.getLoadedNormalMods();
		}

		noModSupport = true;
		return new IModManifest[0];
	}

	/**
	 * Retrieves the core mods registered for loading by this modloader.
	 */
	public IModManifest[] getKnownCoremods() {
		if (modProvider == null && !noModProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IModProvider) {
					modProvider = (IModProvider) info;
					return modProvider.getKnownCoreMods();
				}
			}
			noModProvider = true;
		} else if (!noModProvider) {
			return modProvider.getKnownCoreMods();
		}

		noCoreModSupport = true;
		return new IModManifest[0];
	}

	/**
	 * Retrieves the core mods loaded by this modloader.
	 */
	public IModManifest[] getLoadedCoremods() {
		if (modProvider == null && !noModProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IModProvider) {
					modProvider = (IModProvider) info;
					return modProvider.getLoadedCoreMods();
				}
			}
			noModProvider = true;
		} else if (!noModProvider) {
			return modProvider.getLoadedCoreMods();
		}

		noCoreModSupport = true;
		return new IModManifest[0];
	}

	/**
	 * Retrieves loaded mods by their id
	 * 
	 * @param id Mod id
	 * @return Mod manifest or null
	 */
	public IModManifest getLoadedMod(String id) {
		for (IModManifest mod : getLoadedMods()) {
			if (mod.id().equalsIgnoreCase(id)) {
				return mod;
			}
		}

		return null;
	}

	/**
	 * Retrieves loaded coremods by their id
	 * 
	 * @param id Coremod id
	 * @return Coremod manifest or null
	 */
	public IModManifest getLoadedCoremod(String id) {
		for (IModManifest mod : getLoadedCoremods()) {
			if (mod.id().equalsIgnoreCase(id)) {
				return mod;
			}
		}

		return null;
	}

	private String tsCache = null;

	@Override
	public String toString() {
		if (tsCache != null)
			return tsCache;

		tsCache = getName() + (getVersion() == null ? "" : " " + getVersion());
		return tsCache;
	}

	/**
	 * Retrieves the game brand of this modloader.
	 */
	public String getGameBrand() {
		if (brandCache != null)
			return brandCache;

		String brand = getSimpleName();

		if (getNextImplementation() == null) {
			brandCache = brand;
			return brand;
		}

		brand += getNextImplementation().getGameBrand();

		brandCache = brand;
		return brandCache;
	}

	/**
	 * Gets the amount of known mods for this modloader.
	 */
	public int getKnownModsCount() {
		if (modProvider == null && !noModProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IModProvider) {
					modProvider = (IModProvider) info;
					return modProvider.getAllKnownModsLength();
				}
			}
			noModProvider = true;
		} else if (!noModProvider) {
			return modProvider.getAllKnownModsLength();
		}

		return getLoadedMods().length + getLoadedCoremods().length;
	}

	/**
	 * Retrieves the phase of the modloader.
	 */
	public LoadPhase getPhase() {
		if (phaseProvider == null && !noPhaseProvider) {
			for (IModloaderInfoProvider info : informationProviders) {
				if (info instanceof IPhaseProvider) {
					phaseProvider = (IPhaseProvider) info;
					return phaseProvider.getPhase();
				}
			}
			noPhaseProvider = true;
		} else if (!noPhaseProvider) {
			return phaseProvider.getPhase();
		}

		if (getNextImplementation() == null)
			return LoadPhase.NOT_READY;

		return getNextImplementation().getPhase();
	}

	/**
	 * Dispatches an event from the modloader event bus.<br />
	 * <b>Warning:</b> by default, events are not supported and need to be
	 * implemented.
	 * 
	 * @param event  Event name
	 * @param params Event parameters
	 */
	public void dispatchEvent(String event, Object... params) {
		if (modloaderEventBus == null)
			throw new IllegalStateException("The " + getName() + " modloader does not support events!");

		getEventChannel(event).dispatch(params);
	}

	/**
	 * Dispatches an event from the shared modloader event bus.<br />
	 * <b>Warning:</b> by default, events are not supported and need to be
	 * implemented by at least one modloader.
	 * 
	 * @param event  Event name
	 * @param params Event parameters
	 */
	public void dispatchSharedEvent(String event, Object... params) {
		if (sharedEventBus == null)
			throw new IllegalStateException("Not a single installed modloader supports events!");

		getSharedEventChannel(event).dispatch(params);
	}

	/**
	 * Attaches an event listener.<br />
	 * <b>Warning:</b> by default, events are not supported and need to be
	 * implemented.
	 * 
	 * @param event    Event name
	 * @param listener Event listener
	 */
	public void attachEventListener(String event, IEventListener listener) {
		if (modloaderEventBus == null)
			throw new IllegalStateException("The " + getName() + " modloader does not support events!");

		debug("Attaching event listener " + listener.getListenerName() + " to event " + event + "...");
		getEventChannel(event).attachListener(listener);
	}

	/**
	 * Attaches an event listener to the shared bus.<br />
	 * <b>Warning:</b> by default, events are not supported and need to be
	 * implemented by at least one modloader.
	 * 
	 * @param event    Event name
	 * @param listener Event listener
	 */
	public void attachSharedEventListener(String event, IEventListener listener) {
		if (sharedEventBus == null)
			throw new IllegalStateException("Not a single installed modloader supports events!");

		debug("Attaching event listener " + listener.getListenerName() + " to shared event " + event + "...");
		getSharedEventChannel(event).attachListener(listener);
	}

	/**
	 * Post-register is called after modloader assignment, it does nothing by
	 * default but can be used to define events.
	 */
	protected void postRegister() {
	}

	/**
	 * Retrieves a mod instance by its class
	 * 
	 * @param modClass Mod class
	 * @return Mod instance or null
	 */
	public <T extends IBaseMod> T getModByClass(Class<T> modClass) {
		return null;
	}

	/**
	 * Retrieves the modloader owning the given mod id
	 * 
	 * @param id Mod id
	 * @return Modloader instance or null
	 */
	public static Modloader getModOwner(String id) {
		for (Modloader loader : Modloader.getAllModloaders()) {
			for (IModManifest man : loader.getLoadedMods()) {
				if (man.id().equals(id))
					return loader;
			}
			for (IModManifest man : loader.getLoadedCoremods()) {
				if (man.id().equals(id))
					return loader;
			}
		}
		return null;
	}

	/**
	 * Retrieves the modloader owning the given mod
	 * 
	 * @param mod Mod instance
	 * @return Modloader instance or null
	 */
	public static <T extends IBaseMod> Modloader getModOwner(T mod) {
		return getModOwner(mod.getManifest().id());
	}

	/**
	 * Retrieves the modloader owning the given mod
	 * 
	 * @param mod Mod manifest
	 * @return Modloader instance or null
	 */
	public static Modloader getModOwner(IModManifest mod) {
		return getModOwner(mod.id());
	}

}
