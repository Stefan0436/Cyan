package org.asf.cyan.api.modloader.information.providers;

import org.asf.cyan.api.modloader.information.mods.IModManifest;

public interface IModProvider extends IModloaderInfoProvider {
	public int getAllKnownModsLength();
	public IModManifest[] getLoadedNormalMods();
	public IModManifest[] getLoadedCoreMods();
	
	public default IModManifest[] getKnownCoreMods() {
		return getLoadedCoreMods();
	}
	
	public default IModManifest[] getKnownNormalMods() {
		return getLoadedNormalMods();
	}
}
