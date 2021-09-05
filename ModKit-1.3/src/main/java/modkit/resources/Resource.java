package modkit.resources;

import java.io.InputStream;

import net.minecraft.resources.ResourceLocation;

/**
 * 
 * Resource Location Wrapper
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
public abstract class Resource {

	/**
	 * Converts this resource to its game type
	 */
	public abstract ResourceLocation toGameType();

	/**
	 * Retrieves the location string
	 */
	public abstract String getLocation();

	/**
	 * Retrieves the owner string
	 */
	public abstract String getOwner();

	/**
	 * Reads the resource to a string (null if not found)
	 */
	public abstract String readAsString();

	/**
	 * Opens the resource as stream (null if not found)
	 */
	public abstract InputStream openStream();

	@Override
	public String toString() {
		return getOwner() + ":" + toGameType();
	}

}
