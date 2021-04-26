package org.asf.cyan.backends.downloads;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

public class UpdateInfo extends Configuration<UpdateInfo> {
	public String latestStableVersion;
	public String latestAlphaVersion;
	public String latestBetaVersion;
	public String latestPreviewVersion;

	public String[] longTermSupportVersions;
	public String[] requiredUpgrade;

	public HashMap<String, String> libraryVersions = new HashMap<String, String>();

	public HashMap<String, String> changelogs = new HashMap<String, String>();
	public HashMap<String, String> allVersions = new HashMap<String, String>();
	public HashMap<String, String> byGameVersions = new HashMap<String, String>();

	public HashMap<String, String> forgeSupport = new HashMap<String, String>();
	public HashMap<String, String> fabricSupport = new HashMap<String, String>();
	public HashMap<String, String> paperSupport = new HashMap<String, String>();

	public HashMap<String, String> paperByMappings = new HashMap<String, String>();

	public HashMap<String, String> spigotStableMappings = new HashMap<String, String>();
	public HashMap<String, String> spigotLatestMappings = new HashMap<String, String>();
	public HashMap<String, String> spigotTestingMappings = new HashMap<String, String>();

	public UpdateInfo(String content) {
		readAll(content);
	}

	public UpdateInfo() {
	}

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}
}
