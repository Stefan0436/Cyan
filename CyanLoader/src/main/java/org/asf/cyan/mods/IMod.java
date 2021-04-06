package org.asf.cyan.mods;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.modloader.information.mods.IModManifest;
import org.asf.cyan.mods.config.CyanModfileManifest;

/**
 * 
 * Mod interface, its recommended to use AbstractMod instead.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public interface IMod {
	public void setup(Modloader modloader, GameSide side, CyanModfileManifest manifest);

	public IModManifest getManifest();
}
