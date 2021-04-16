package org.asf.cyan.mods;

import java.util.ArrayList;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.fluid.api.ClassLoadHook;
import org.asf.cyan.mods.config.CyanModfileManifest;
import org.asf.cyan.mods.internal.IAcceptableComponent;

/**
 * 
 * Abstract coremod class, pre-implemented ICoremod
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class AbstractCoremod extends AbstractMod implements ICoremod, IAcceptableComponent {

	private ArrayList<String> transformerPackages = new ArrayList<String>();
	private ArrayList<String> transformers = new ArrayList<String>();

	private ArrayList<String> hookPackages = new ArrayList<String>();
	private ArrayList<ClassLoadHook> hooks = new ArrayList<ClassLoadHook>();

	private String modid = "";

	@Override
	public void setup(Modloader modloader, GameSide side, CyanModfileManifest manifest) {
		super.setup(modloader, side, manifest);
	}

	/**
	 * Adds all transformers in the given package
	 */
	protected void addTransformerPackage(String pkg) {
		transformerPackages.add(pkg);
		hookPackages.add(pkg);
	}

	/**
	 * Adds transformers
	 */
	protected void addTransformer(Class<?> transformer) {
		transformers.add(transformer.getTypeName());
	}

	/**
	 * Adds all class load hooks in the given package
	 */
	protected void addClassHookPackage(String pkg) {
		transformerPackages.add(pkg);
		hookPackages.add(pkg);
	}

	/**
	 * Adds class load hooks
	 */
	protected void addClassHook(ClassLoadHook hook) {
		hooks.add(hook);
	}

	@Override
	public String[] providers() {
		return new String[] { "transformers", "transformer-packages", "class-hooks", "class-hook-packages",
				"auto.init" };
	}

	@Override
	public Object provide(String provider) {
		if (provider.equals("transformers")) {
			return transformers.toArray(t -> new String[t]);
		} else if (provider.equals("transformer-packages")) {
			return transformerPackages.toArray(t -> new String[t]);
		} else if (provider.equals("class-hooks")) {
			return hooks.toArray(t -> new ClassLoadHook[t]);
		} else if (provider.equals("class-hook-packages")) {
			return hookPackages.toArray(t -> new String[t]);
		} else if (provider.equals("auto.init")) {
			addTransformerPackage(getClass().getPackageName() + ".transformers");
			setupCoremod();
		} else if (provider.equals("mod.id")) {
			return modid;
		}
		return null;
	}

	/**
	 * Runs before transformer registration, to allow transformers to be added
	 */
	protected abstract void setupCoremod();

	@Override
	public String[] infoRequests() {
		return new String[] { "mod.manifest" };
	}

	@Override
	public void provideInfo(Object data, String name) {
		if (name.equals("mod.manifest")) {
			modid = ((CyanModfileManifest) data).modGroup + ":" + ((CyanModfileManifest) data).modId;
		}
	}
}
