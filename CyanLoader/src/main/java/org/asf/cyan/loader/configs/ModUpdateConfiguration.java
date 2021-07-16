package org.asf.cyan.loader.configs;

import java.io.File;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.annotations.Comment;

@Comment("")
@Comment("Mod update configuration, controls the update settings of this mod.")
@Comment("")
public class ModUpdateConfiguration extends Configuration<ModUpdateConfiguration> {

	public ModUpdateConfiguration(File file) {
		super(file.getAbsolutePath());
	}

	@Override
	public String filename() {
		return "update.ccfg";
	}

	@Override
	public String folder() {
		return "";
	}

	@Comment("True to keep this mod up-to-date, false otherwise")
	public boolean updates = true;

	@Comment("The update channel to use (most mods have stable, latest and testing)")
	public String channel = "@default";

	@Comment("The update server url to use (advanced)")
	public String server = "@default";

}
