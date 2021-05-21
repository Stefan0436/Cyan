package org.asf.cyan.api.resources;

import java.io.InputStream;

import org.asf.cyan.api.modloader.information.mods.IBaseMod;
import org.asf.cyan.api.modloader.information.mods.IModManifest;

/**
 * 
 * Mod Resource Provider
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class Resources {

	protected static Resources implementation;

	protected abstract void setDefaultOwner(IModManifest mod);

	protected abstract void setDefaultOwner(String owner);

	protected abstract Resources newInstance();

	/**
	 * Retrieves a new resources container for a mod
	 * 
	 * @param mod Mod instance
	 * @return Resources instance
	 */
	public static Resources getFor(IModManifest mod) {
		Resources impl = implementation.newInstance();
		impl.setDefaultOwner(mod);
		return impl;
	}

	/**
	 * Retrieves a new resources container for a mod
	 * 
	 * @param mod Mod instance
	 * @return Resources instance
	 */
	public static Resources getFor(IBaseMod mod) {
		Resources impl = implementation.newInstance();
		impl.setDefaultOwner(mod.getManifest());
		return impl;
	}

	/**
	 * Retrieves a new resources container for a name
	 * 
	 * @param owner Owner name
	 * @return Resources instance
	 */
	public static Resources getFor(String owner) {
		Resources impl = implementation.newInstance();
		impl.setDefaultOwner(owner);
		return impl;
	}

	/**
	 * Retrieves a resource by location
	 * 
	 * @param location Resource location path
	 * @return Resource instance
	 */
	public abstract Resource getResource(String location);

	/**
	 * Retrieves a resource by location and owner
	 * 
	 * @param owner    Resource owner identifier
	 * @param location Resource location path
	 * @return Resource instance
	 */
	public abstract Resource getResource(String owner, String location);

	/**
	 * Retrieves the default owner name
	 */
	public abstract String getDefaultOwner();

	/**
	 * Retrieves the owning mod
	 */
	public abstract IModManifest getMod();

	/**
	 * Retrieves resources as stream
	 * 
	 * @param resource Resource to retrieve
	 * @return Resource input stream or null if not found
	 */
	public abstract InputStream getResourceAsStream(Resource resource);

	/**
	 * Reads resources into a string
	 * 
	 * @param resource Resource to retrieve
	 * @return Resource content or null if not found
	 */
	public abstract String getResourceAsString(Resource resource);
}
