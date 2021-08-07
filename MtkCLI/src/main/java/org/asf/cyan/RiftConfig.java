package org.asf.cyan;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

public class RiftConfig extends Configuration<RiftConfig> {

	public String outputDir;
	public String cacheDir = ".";

	public RiftArtifact[] riftJars;
	public HashMap<String, String> dependencies;

	@Override
	public String filename() {
		return "rift.properties.ccfg";
	}

	@Override
	public String folder() {
		return "";
	}

}
