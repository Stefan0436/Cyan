package org.asf.cyan.cornflower.gradle.utilities.modding;

import org.asf.cyan.cornflower.gradle.utilities.modding.manifests.CyanModfileManifest;

import groovy.lang.Closure;

public class CyanModfileManifestGenerator {
	private CyanModfileManifest manifest = new CyanModfileManifest();
	
	public static CyanModfileManifest fromClosure(Closure<?> closure) {
		CyanModfileManifestGenerator owner = new CyanModfileManifestGenerator();
		closure.setDelegate(owner);
		closure.call();
		return owner.toManifest();
	}

	private CyanModfileManifest toManifest() {		
		return manifest;
	}
	
	public void modid(String id) {
		manifest.modId = id;
	}
	
	public void modgroup(String group) {
		manifest.modGroup = group;
	}
}
