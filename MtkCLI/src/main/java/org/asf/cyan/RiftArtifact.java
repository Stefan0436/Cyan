package org.asf.cyan;

import org.asf.cyan.api.config.Configuration;

public class RiftArtifact extends Configuration<RiftArtifact> {
	
	public String classifier;
	public String platform;
	public String loaderVersion;
	public String gameVersion;
	public String mappingsVersion;
	public String side;

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

}
