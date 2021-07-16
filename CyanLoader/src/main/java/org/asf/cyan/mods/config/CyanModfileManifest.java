package org.asf.cyan.mods.config;

import java.util.HashMap;

import org.asf.cyan.api.config.Configuration;
import org.asf.cyan.api.config.annotations.Exclude;

/**
 * 
 * Cyan Modfile manifest.ccfg container.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public class CyanModfileManifest extends Configuration<CyanModfileManifest> {

	@Override
	public String filename() {
		return null;
	}

	@Override
	public String folder() {
		return null;
	}

	@Exclude
	public String source = null;

	@Exclude
	public boolean loaded;

	public String modClassName = null;
	public String modClassPackage = null;

	public String modGroup = null;
	public String modId = null;
	public String displayName = null;

	public String version = null;
	public String gameVersionRegex = null;
	public String gameVersionMessage = null;

	public String descriptionLanguageKey = null;
	public String fallbackDescription = null;
	public String updateserver = null;

	public HashMap<String, String> jars = new HashMap<String, String>();

	public HashMap<String, String> dependencies = new HashMap<String, String>();
	public HashMap<String, String> optionalDependencies = new HashMap<String, String>();

	public HashMap<String, String> incompatibilities = new HashMap<String, String>();

	public HashMap<String, String> mavenRepositories = new HashMap<String, String>();
	public HashMap<String, HashMap<String, String>> mavenDependencies = new HashMap<String, HashMap<String, String>>();

	public HashMap<String, String> trustContainers = new HashMap<String, String>();
	public HashMap<String, String> platforms = new HashMap<String, String>();

}
