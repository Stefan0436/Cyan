package org.asf.cyan.api.modloader.information.providers;

import org.asf.cyan.api.versioning.Version;

public interface IVersionProvider extends IModloaderInfoProvider {
	public Version getModloaderVersion();
}
