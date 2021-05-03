package org.asf.cyan.api.internal.modkit.components._1_15_2.common.resources;

import java.util.function.Supplier;

import net.minecraft.server.packs.PackResources;

public class CyanPackSupplier implements Supplier<PackResources> {

	private CyanPackResources pack;
	public CyanPackSupplier(CyanPackResources pack) {
		this.pack = pack;
	}
	
	@Override
	public PackResources get() {
		return pack;
	}

}
