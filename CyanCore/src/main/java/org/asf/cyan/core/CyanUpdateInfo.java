package org.asf.cyan.core;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

class CyanUpdateInfo extends Configuration<CyanUpdateInfo> {
	public String latestStableVersion;
	public String latestAlphaVersion;
	public String latestBetaVersion;
	public String latestPreviewVersion;
	
	public String[] longTermSupportVersions;
	public String[] requiredUpgrade;
	
	public HashMap<String, String> changelogs = new HashMap<String, String>();
	public HashMap<String, String> allVersions = new HashMap<String, String>();
	
	public CyanUpdateInfo(String content) {
		readAll(content);
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
