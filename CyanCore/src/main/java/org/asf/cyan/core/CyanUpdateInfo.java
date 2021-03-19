package org.asf.cyan.core;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

/**
 * 
 * Cyan update information holder.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CyanUpdateInfo extends Configuration<CyanUpdateInfo> {
	public String latestStableVersion;
	public String latestAlphaVersion;
	public String latestBetaVersion;
	public String latestPreviewVersion;
	
	public String[] longTermSupportVersions;
	public String[] requiredUpgrade;
	
	public HashMap<String, String> changelogs = new HashMap<String, String>();
	public HashMap<String, String> allVersions = new HashMap<String, String>();
	public HashMap<String, String> byGameVersions = new HashMap<String, String>();
	
	public HashMap<String, String> forgeSupport = new HashMap<String, String>();
	public HashMap<String, String> fabricSupport = new HashMap<String, String>();
	public HashMap<String, String> paperSupport = new HashMap<String, String>();
	
	public CyanUpdateInfo(String content) {
		readAll(content);
	}
	
	public CyanUpdateInfo() {		
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
