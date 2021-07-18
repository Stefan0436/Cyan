package org.asf.cyan.api.versioning;

import org.asf.cyan.api.modloader.information.providers.IVersionProvider;

public class StringVersionProvider implements IVersionProvider {

	private Version version;
	public StringVersionProvider(String version) {
		this.version = Version.fromString(version);
	}
	
 	@Override
	public Version getLoaderVersion() {
		return version;
	}

}
