package org.asf.cyan.mods.internal;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

public class ModInfoCache extends Configuration<ModInfoCache> {

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}
	
	public String modVersion = "";
	public String gameVersion = "";
	
	public String platform = "";
	public String platformVersion = "";
	
	public HashMap<String, String> trustContainers = new HashMap<String, String>();

}
