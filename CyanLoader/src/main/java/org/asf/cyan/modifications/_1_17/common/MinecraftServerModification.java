package org.asf.cyan.modifications._1_17.common;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Erase;
import org.asf.cyan.fluid.api.transforming.TargetClass;

/**
 * 
 * Changes the server brand to Cyan, so it knows it been modded.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@TargetClass(target = "net.minecraft.server.MinecraftServer")
public class MinecraftServerModification {

	@Erase
	public static String getServerModName() {
		return Modloader.getModloaderGameBrand();
	}

}
