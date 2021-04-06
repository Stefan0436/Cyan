package org.asf.cyan.mods;

import java.util.ArrayList;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
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

	private String execKey = null;
	private ArrayList<String> transformers = new ArrayList<String>();
		
	@Override
	public void setup(Modloader modloader, GameSide side, CyanModfileManifest manifest) {
		super.setup(modloader, side, manifest);
		execKey = manifest.coremodComponentKey;
		addTransformerPackage(getClass().getPackageName() + ".transformers");
	}
	
	/**
	 * Adds all transformers in the given package
	 */
	protected void addTransformerPackage(String pkg) {
		
	}

	@Override
	public String[] providers() {
		return new String[] { "transformers", "coremod.init" };
	}

	@Override
	public String executionKey() {
		return execKey;
	}

	@Override
	public Object provide(String provider) {
		if (provider.equals("transformers")) {
			return transformers.toArray(t -> new String[t]);
		} else if (provider.endsWith("coremod.init")) {

		}
		return null;
	}

}
