package org.asf.cyan.api.versioning;

import org.asf.cyan.api.modloader.information.providers.IVersionProvider;

public class StringVersionProvider implements IVersionProvider {

	private String version;
	public StringVersionProvider(String version) {
		this.version = version;
	}
	
 	@Override
	public String getModloaderVersion() {
		return version;
	}

}
