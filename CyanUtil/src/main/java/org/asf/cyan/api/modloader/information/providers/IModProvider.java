package org.asf.cyan.api.modloader.information.providers;

public interface IModProvider extends IModloaderInfoProvider {
	public int getAllKnownModsLength();
	public Object[] getLoadedNormalMods(); // TODO: change to modholder when ready
	public Object[] getLoadedCoreMods(); // TODO: change to modholder when ready
}
