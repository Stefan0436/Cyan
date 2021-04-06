package org.asf.cyan.api.modloader.information.providers;

import org.asf.cyan.api.modloader.information.mods.IModManifest;

public interface IModProvider extends IModloaderInfoProvider {
	public int getAllKnownModsLength();
	public IModManifest[] getLoadedNormalMods(); // TODO: change to modholder when ready
	public IModManifest[] getLoadedCoreMods(); // TODO: change to modholder when ready
}
