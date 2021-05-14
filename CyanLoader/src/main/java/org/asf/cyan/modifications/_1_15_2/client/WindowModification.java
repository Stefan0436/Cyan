package org.asf.cyan.modifications._1_15_2.client;

import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

/**
 * 
 * Modifies the title of the minecraft main window
 * 
 * @author Stefan0436 - AerialWorks Software Foundation
 *
 */
@FluidTransformer
@TargetClass(target = "com.mojang.blaze3d.platform.Window")
public class WindowModification {

	private String errorSection;

	@InjectAt(location = InjectLocation.HEAD)
	public void setTitle(String title) {
		title = title + " - " + Modloader.getModloaderName() + " - Version " + Modloader.getModloaderVersion();
	}

	public String getErrorSection() {
		return errorSection;
	}

}
