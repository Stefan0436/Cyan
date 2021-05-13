package org.asf.cyan;

import java.io.IOException;
import java.io.InputStream;

import org.asf.cyan.api.config.Configuration;

public class ProjectConfig extends Configuration<ProjectConfig> {
	
	public ProjectConfig() throws IOException {
		InputStream strm = getClass().getClassLoader().getResource("project.ccfg").openStream();
		readAll(new String(strm.readAllBytes()));
		strm.close();
	}

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}
	
	public String name;
	public String version;

}
