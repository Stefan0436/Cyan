package org.asf.cyan.mods.internal;

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

}
