package org.asf.cyan.api.modloader.information.providers;

public interface IVersionChangelogProvider extends IModloaderInfoProvider {
	public String getCurrentVersionChangelog();
	public String getUpdateVersionChangelog();
}
