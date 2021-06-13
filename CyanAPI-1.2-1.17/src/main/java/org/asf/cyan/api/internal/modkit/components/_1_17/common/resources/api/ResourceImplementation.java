package org.asf.cyan.api.internal.modkit.components._1_17.common.resources.api;

import java.io.InputStream;

import modkit.resources.Resource;
import modkit.resources.Resources;
import net.minecraft.resources.ResourceLocation;

public class ResourceImplementation extends Resource {

	private Resources resources;
	private String path;
	private String owner;

	public ResourceImplementation(String path, String owner, Resources resources) {
		this.path = path;
		this.owner = owner;
		this.resources = resources;
	}

	@Override
	public ResourceLocation toGameType() {
		return new ResourceLocation(owner, path);
	}

	@Override
	public String getLocation() {
		return path;
	}

	@Override
	public String getOwner() {
		return owner;
	}

	@Override
	public String readAsString() {
		return resources.getResourceAsString(this);
	}

	@Override
	public InputStream openStream() {
		return resources.getResourceAsStream(this);
	}

}
