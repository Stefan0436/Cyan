package org.asf.cyan.loader.configs;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

public class ModUpdateChannelConfig extends Configuration<ModUpdateChannelConfig> {

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public HashMap<String, String> channels = new HashMap<String, String>();

}
