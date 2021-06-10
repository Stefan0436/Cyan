package org.asf.cyan.modifications._1_17.client;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Erase;
import org.asf.cyan.fluid.api.transforming.TargetClass;

/**
 * 
 * Changes the client brand to Cyan, so it knows it has been modded.
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@TargetClass(target = "net.minecraft.client.ClientBrandRetriever")
public class BrandModification {

	@Erase
	public static String getClientModName() {
		return Modloader.getModloaderGameBrand();
	}

}
