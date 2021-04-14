package org.asf.cyan.cornflower.gradle.utilities.modding;

import groovy.lang.Closure;

public class PlatformClosureOwner {
	public String version = null;
	public String modloader = null;

	public void version(String version) {
		this.version = version;
	}

	public void modloader(String modloader) {
		this.modloader = modloader;
	}

	public static PlatformClosureOwner fromClosure(Closure<?> closure) {
		PlatformClosureOwner owner = new PlatformClosureOwner();
		closure.setDelegate(owner);
		closure.call();
		return owner;
	}
}
