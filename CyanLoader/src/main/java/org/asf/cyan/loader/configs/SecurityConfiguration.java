package org.asf.cyan.loader.configs;

import org.asf.cyan.api.config.Configuration;

public class SecurityConfiguration extends Configuration<SecurityConfiguration> {
	public SecurityConfiguration(String base) {
		super(base);
	}

	@Override
	public String filename() {
		return "security.ccfg";
	}

	@Override
	public String folder() {
		return "";
	}

	public String modSecurityService = "https://aerialworks.ddns.net/cyan/security";
}
