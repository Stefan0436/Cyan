package org.asf.cyan;

import org.asf.cyan.api.config.Configuration;

public class RiftConfig extends Configuration<RiftConfig> {

	public String outputDir;
	public String cacheDir = ".";

	public RiftArtifact[] riftJars;

	@Override
	public String filename() {
		return "rift.properties.ccfg";
	}

	@Override
	public String folder() {
		return "";
	}

}
