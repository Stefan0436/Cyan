package org.asf.cyan;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

public class RiftConfig extends Configuration<RiftConfig> {

	public String outputDir;
	public String cacheDir = ".";

	public RiftArtifact[] riftJars = new RiftArtifact[0];
	public HashMap<String, String> dependencies = new HashMap<String, String>();

	@Override
	public String filename() {
		return "rift.properties.ccfg";
	}

	@Override
	public String folder() {
		return "";
	}

}
