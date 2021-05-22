package org.asf.cyan.mods;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.stream.Stream;

import org.asf.cyan.api.common.CyanComponent;
import org.asf.cyan.api.events.core.ISynchronizedEventListener;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.api.versioning.Version;
import org.asf.cyan.mods.config.CyanModfileManifest;
import org.asf.cyan.api.protocol.handshake.HandshakeRule;
import org.asf.cyan.mods.dependencies.ModProvider;
import org.asf.cyan.mods.dependencies.ModSupportHandler;
import org.asf.cyan.mods.events.IEventListenerContainer;
import org.asf.cyan.mods.internal.BaseEventController;
import org.asf.cyan.mods.internal.CyanModManifest;

/**
 * 
 * Abstract mod, pre-implemented IMod with various features
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class AbstractMod extends CyanComponent implements IMod, IEventListenerContainer {

	/**
	 * Retrieves the instance of the given mod
	 * 
	 * @param <T>      Mod type
	 * @param modClass Mod class
	 */
	@SuppressWarnings("unchecked")
	public static <T extends AbstractMod> T getInstance(Class<T> modClass) {
		if (modClass.getTypeName().equals(AbstractMod.class.getTypeName()))
			return null;

		for (Modloader loader : Modloader.getAllModloaders()) {
			for (IModManifest manifest : loader.getLoadedMods()) {
				if (manifest instanceof CyanModManifest) {
					AbstractMod md = ((CyanModManifest) manifest).getInstance();
					if (modClass.isAssignableFrom(md.getClass())) {
						return (T) md;
					}
				}
			}
		}

		return null;
	}

	private CyanModManifest manifest;
	private Modloader owner;
	private GameSide side;

	private String id = null;
	private String dsp = null;
	private String desc = null;
	private String group = null;

	private String descKey = null;
	private String fallbackDesc = null;

	private AbstractMod[] deps;
	private String[] optdeps;

	private Version version;

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param key   Rule key
	 * @param value Rule check string
	 */
	protected void addHandshakeRule(String key, String value) {
		addHandshakeRule(GameSide.SERVER, key, value);
		addHandshakeRule(GameSide.CLIENT, key, value);
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param side  Rule side
	 * @param key   Rule key
	 * @param value Rule check string
	 */
	protected void addHandshakeRule(GameSide side, String key, String value) {
		HandshakeRule.registerRule(new HandshakeRule(side, key, value));
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param side  Rule side
	 * @param key   Rule key
	 * @param value Rule version
	 */
	protected void addHandshakeRule(GameSide side, String key, Version value) {
		addHandshakeRule(side, key, value.toString());
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param side  Rule side
	 * @param mod   Mod to use as key
	 * @param value Rule version
	 */
	protected void addHandshakeRule(GameSide side, IModManifest mod, Version value) {
		addHandshakeRule(side, mod.id(), value.toString());
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param mod   Mod to use as key
	 * @param value Rule version
	 */
	protected void addHandshakeRule(IModManifest mod, Version value) {
		addHandshakeRule(mod.id(), value.toString());
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param side Rule side
	 * @param mod  Mod to use as key and value
	 */
	protected void addHandshakeRule(GameSide side, IModManifest mod) {
		addHandshakeRule(side, mod.id(), mod.version());
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param mod Mod to use as key and value
	 */
	protected void addHandshakeRule(IModManifest mod) {
		addHandshakeRule(mod.id(), mod.version());
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param side  Rule side
	 * @param mod   Mod to use as key
	 * @param value Rule version
	 */
	protected void addHandshakeRule(GameSide side, AbstractMod mod, Version value) {
		addHandshakeRule(side, mod.getManifest().id(), value.toString());
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param mod   Mod to use as key
	 * @param value Rule version
	 */
	protected void addHandshakeRule(AbstractMod mod, Version value) {
		addHandshakeRule(mod.getManifest().id(), value.toString());
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param side Rule side
	 * @param mod  Mod to use as key and value
	 */
	protected void addHandshakeRule(GameSide side, AbstractMod mod) {
		addHandshakeRule(side, mod.getManifest().id(), mod.getManifest().version());
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param mod Mod to use as key and value
	 */
	protected void addHandshakeRule(AbstractMod mod) {
		addHandshakeRule(mod.getManifest().id(), mod.getManifest().version());
	}

	/**
	 * Registers a mod server/client handshake rule
	 * 
	 * @param key   Rule key
	 * @param value Rule version
	 */
	protected void addHandshakeRule(String key, Version value) {
		addHandshakeRule(key, value.toString());
	}

	@Override
	public String getDescriptionLanguageKey() {
		return descKey;
	}

	@Override
	public void setLanguageBasedDescription(String desc) {
		if (!desc.startsWith("$")) // prevent any processing code
			this.desc = desc;
	}

	@Override
	public void setDefaultDescription() {
		this.desc = fallbackDesc;
	}

	@Override
	public void setup(Modloader modloader, GameSide side, CyanModfileManifest manifest) {
		this.owner = modloader;
		this.side = side;

		id = manifest.modId;
		dsp = manifest.displayName;
		desc = manifest.fallbackDescription;
		group = manifest.modGroup;
		descKey = manifest.descriptionLanguageKey;
		if (manifest.fallbackDescription != null)
			fallbackDesc = manifest.fallbackDescription.trim();
		if (descKey != null && descKey.isEmpty())
			descKey = null;
		version = Version.fromString(manifest.version);

		ArrayList<AbstractMod> mods = new ArrayList<AbstractMod>();
		manifest.dependencies.forEach((id, versionRegex) -> {
			mods.add(getModInstance(id));
		});
		deps = mods.toArray(t -> new AbstractMod[t]);

		mods.clear();
		optdeps = manifest.optionalDependencies.keySet().toArray(t -> new String[t]);

		modloader.attachEventListener("mod.loaded", new ISynchronizedEventListener() {

			@Override
			public String getListenerName() {
				return getId() + " Mod Support Listener";
			}

			@Override
			public void received(Object... params) {
				if (params.length == 1 && params[0] instanceof AbstractMod) {
					enableModSupport((AbstractMod) params[0]);
				}
			}

		});

		addEventListenerContainer(this);
	}

	/**
	 * Runs support methods (if any) for the given mod that recently loaded
	 * 
	 * @param <T> Mod type
	 * @param mod Mod instance
	 */
	public <T extends AbstractMod> void enableModSupport(T mod) {
		if (!Stream.of(deps).anyMatch(t -> t.getId().equals(mod.getId()))
				&& !Stream.of(optdeps).anyMatch(t -> t.equals(mod.getId()))) {
			return;
		}

		for (Method mth : getClass().getDeclaredMethods()) {
			if (mth.isAnnotationPresent(ModSupportHandler.class)
					&& mth.getAnnotation(ModSupportHandler.class).value().equals(mod.getManifest().id())) {
				mth.setAccessible(true);
				if (mth.getParameterCount() == 1) {
					if (mth.getParameters()[0].getType().isAssignableFrom(mod.getClass())) {
						try {
							mth.invoke(this, mod);
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new RuntimeException(e);
						}
					} else if (ModProvider.class.isAssignableFrom(mth.getParameters()[0].getType())) {
						try {
							mth.invoke(this, new ModProvider<IMod>() {

								@Override
								public IMod get() {
									return mod;
								}

							});
						} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
							throw new RuntimeException(e);
						}
					}
				}
			}
		}
	}

	/**
	 * Adds event containers
	 */
	public void addEventListenerContainer(IEventListenerContainer container) {
		BaseEventController.addEventContainer(container);
	}

	/**
	 * Retrieves the mod instance of the given class
	 * 
	 * @param <T> Mod type
	 * @param mod Mod class
	 * @return Mod instance or null.
	 */
	@SuppressWarnings("unchecked")
	protected <T extends AbstractMod> T getModInstance(Class<T> mod) {

		for (IModManifest manifest : getModloader().getLoadedMods()) {
			if (manifest instanceof CyanModManifest) {
				AbstractMod md = ((CyanModManifest) manifest).getInstance();
				if (mod.isAssignableFrom(md.getClass())) {
					return (T) md;
				}
			}
		}

		for (IModManifest manifest : getModloader().getLoadedCoremods()) {
			if (manifest instanceof CyanModManifest) {
				AbstractMod md = ((CyanModManifest) manifest).getInstance();
				if (mod.isAssignableFrom(md.getClass())) {
					return (T) md;
				}
			}
		}

		return null;
	}

	/**
	 * Retrieves the mod instance of the given id
	 * 
	 * @param <T> Mod type
	 * @param id  Mod id (group:modid)
	 * @return Mod instance or null.
	 */
	@SuppressWarnings("unchecked")
	protected <T extends AbstractMod> T getModInstance(String id) {

		for (IModManifest manifest : getModloader().getLoadedMods()) {
			if (manifest instanceof CyanModManifest) {
				AbstractMod md = ((CyanModManifest) manifest).getInstance();
				if (manifest.id().equalsIgnoreCase(id)) {
					return (T) md;
				}
			}
		}

		for (IModManifest manifest : getModloader().getLoadedCoremods()) {
			if (manifest instanceof CyanModManifest) {
				AbstractMod md = ((CyanModManifest) manifest).getInstance();
				if (manifest.id().equalsIgnoreCase(id)) {
					return (T) md;
				}
			}
		}

		return null;
	}

	/**
	 * Checks if the given mod id is loaded.
	 * 
	 * @param id Mod id (group:modid)
	 * @return True if loaded, false otherwise
	 */
	protected boolean isModLoaded(String id) {
		if (getModloader().getLoadedMod(id) != null)
			return true;
		if (getModloader().getLoadedCoremod(id) != null)
			return true;

		return false;
	}

	/**
	 * Retrieves this mod's id (group:modid)
	 */
	public String getId() {
		return group + ":" + id;
	}

	/**
	 * Retrieves this mod's display name
	 */
	public String getDisplayName() {
		return dsp;
	}

	/**
	 * Retrieves this mod's localized description (localized on language init)
	 */
	public String getDescription() {
		return desc;
	}

	/**
	 * Retrieves this mod's version
	 */
	public Version getVersion() {
		return version;
	}

	/**
	 * Retrieves the mod dependencies
	 */
	public AbstractMod[] getDependencies() {
		return deps;
	}

	/**
	 * Retrieves the optional mod dependencies
	 */
	public String[] getOptionalDependencies() {
		return optdeps;
	}

	/**
	 * Retrieves the owning modloader
	 */
	protected Modloader getModloader() {
		return owner;
	}

	/**
	 * Retrieves the owning game side
	 */
	protected GameSide getSide() {
		return side;
	}

	@Override
	public IModManifest getManifest() {
		if (manifest == null) {
			manifest = new CyanModManifest(this);
		}
		return manifest;
	}
}
