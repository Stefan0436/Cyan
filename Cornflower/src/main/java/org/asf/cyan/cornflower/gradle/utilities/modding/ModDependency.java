package org.asf.cyan.cornflower.gradle.utilities.modding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.cornflower.gradle.Cornflower;
import org.asf.cyan.cornflower.gradle.utilities.GradleUtil;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.AbstractDependency;

public class ModDependency extends AbstractDependency {

	public static String asfServer = "https://aerialworks.ddns.net/";

	private HashMap<String, String> repos = new HashMap<String, String>();
	private String name;
	private String group;
	private String version;

	public Map<String, String> getRepositories() {
		return new HashMap<String, String>(repos);
	}

	public ModDependency(String group, String name, String version) {
		this.group = group;
		this.name = name;
		this.version = version;
	}

	public ModDependency() {
	}

	private static class DepConfig extends Configuration<DepConfig> {

		@Override
		public String filename() {
			return null;
		}

		@Override
		public String folder() {
			return null;
		}

		public HashMap<String, String> repositories = new HashMap<String, String>();
		public HashMap<String, HashMap<String, String>> artifacts = new HashMap<String, HashMap<String, String>>();
	}

	public static Collection<ModDependency> byId(String id, String version) {
		ArrayList<ModDependency> dependencies = new ArrayList<ModDependency>();

		File modCache = GradleUtil.getSharedCacheFolder(Cornflower.class, "mod-artifacts");
		File modDep = new File(modCache, id.replace(":", "/") + ".deps");

		String baseLocation = asfServer + "cyan/trust/download";
		try {
			URL u = new URL(asfServer + "cyan/security/request-server-location/" + id.replace(":", "/"));
			InputStream strm = u.openStream();
			baseLocation = new String(strm.readAllBytes()).trim();
			strm.close();
		} catch (IOException e) {
		}
		try {
			URL u = new URL(baseLocation + "/" + id.replace(".", "/").replace(":", "/") + "/mod.artifacts.deps");
			if (!modDep.getParentFile().exists())
				modDep.getParentFile().mkdirs();
			InputStream strm = u.openStream();
			FileOutputStream strmO = new FileOutputStream(modDep);
			strm.transferTo(strmO);
			strmO.close();
			strm.close();
		} catch (IOException e) {
		}
		DepConfig configuration = new DepConfig();
		try {
			configuration.readAll(new String(Files.readAllBytes(modDep.toPath())));
		} catch (IOException e) {
			throw new RuntimeException("Could not resolve mod dependencies of: " + id);
		}

		boolean first = true;
		for (String group : configuration.artifacts.keySet()) {
			for (String dep : configuration.artifacts.get(group).keySet()) {
				String ver = configuration.artifacts.get(group).get(dep);
				ModDependency dependency = byDependency(group, dep, ver.replace("%mv", version));
				if (first)
					dependency.repos.putAll(configuration.repositories);
				dependencies.add(dependency);
				first = false;
			}
		}

		return dependencies;
	}

	public static ModDependency byDependency(String group, String name, String version) {
		return new ModDependency(group, name, version);
	}

	@Override
	public String getGroup() {
		return "cornflower.internal.mod";
	}

	public String getModDepGroup() {
		return group;
	}

	public String getModDepName() {
		return name;
	}

	@Override
	public String getName() {
		return group + "__" + name;
	}

	@Override
	public String getVersion() {
		return version;
	}

	@Override
	public boolean contentEquals(Dependency dependency) {
		return dependency.getName().equals(getName()) && dependency.getGroup().equals(getGroup())
				&& dependency.getVersion().equals(getVersion());
	}

	@Override
	public Dependency copy() {
		return new ModDependency(group, name, version);
	}

}
