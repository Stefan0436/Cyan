package org.asf.cyan.loader.configs;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

public class ModUpdateChannel extends Configuration<ModUpdateChannel> {

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public HashMap<String, String> versions = new HashMap<String, String>();
	public HashMap<String, String> urls = new HashMap<String, String>();

}
