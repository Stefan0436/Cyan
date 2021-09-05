package modkit.config;

import java.io.IOException;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.modloader.information.mods.IBaseMod;

/**
 * 
 * Mod Configuration -- simple mod configuration abstract
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 * @param <T> Self
 */
public abstract class ModConfiguration<T extends ModConfiguration<T, T2>, T2 extends IBaseMod>
		extends Configuration<T> {

	private T2 mod;

	/**
	 * Instantiates the configuration and reads it (automated)
	 * 
	 * @param instance Mod instance
	 * @throws IOException If reading fails
	 */
	public ModConfiguration(T2 instance) throws IOException {
		mod = instance;
		assignFile(ConfigManager.implementation.getMainDir());
	}

	@Override
	public String filename() {
		if (mod == null)
			return null;
		return "mod.ccfg";
	}

	@Override
	public String folder() {
		if (mod == null)
			return null;
		return "config/" + mod.getManifest().id().replace(":", "/");
	}

}
