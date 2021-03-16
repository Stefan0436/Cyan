package org.asf.cyan.api.modloader.information.providers;

import org.asf.cyan.api.versioning.VersionStatus;

public interface IVersionStatusProvider extends IModloaderInfoProvider {
	public VersionStatus getModloaderVersionStatus();
}
