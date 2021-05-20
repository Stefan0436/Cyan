package org.asf.cyan;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;

public class ProjectConfig extends Configuration<ProjectConfig> {
	
	public static class DefaultContentProvider extends ContentProvider {

		@Override
		public String provide() throws IOException {
			InputStream strm = getClass().getClassLoader().getResource("project.ccfg").openStream();
			String content = new String(strm.readAllBytes());
			strm.close();
			return content;
		}

	}
	public static ContentProvider contentProvider = new DefaultContentProvider();
	
	public static abstract class ContentProvider {
		public abstract String provide() throws IOException;
	}

	public ProjectConfig() throws IOException {
		
	}

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	public String profileName;
	public String profileIcon;
	
	public String wrapper;
	public String manifest;

	public String id;
	public String inheritsFrom;
	public String game;

	public String name;
	public String version;

	public String mappings;
	public String platform;

	public String loaderVersion;
	public String loader;

	public String serverMain;
	public String clientMain;
	public String serverOutput;
	public String[] bootLibs = new String[0];
	public String[] loadFirst = new String[0];
	public String[] fatServer = new String[0];

	public HashMap<String, String> jarManifest = new HashMap<String, String>();

	public HashMap<String, String> repositories = new HashMap<String, String>();
	public HashMap<String, HashMap<String, String>> artifactModifications = new HashMap<String, HashMap<String, String>>();

}
