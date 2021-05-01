package org.asf.cyan.api.internal.modkit.components._1_15_2.common;

import java.io.InputStream;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.VanillaPackResources;

public class CyanPackResources extends VanillaPackResources {
	public CyanPackResources() {
		super(new String[] { "cyan" });
	}

	@Override
	protected InputStream getResourceAsStream(PackType var1, ResourceLocation var2) {
		return null;
	}

	@Override
	public boolean hasResource(PackType type, ResourceLocation location) {
		return false;
	}

	@Override
	protected InputStream getResourceAsStream(String path) {
		return null;
	}

	@Override
	public String getName() {
		return "Cyan Mods";
	}
}
