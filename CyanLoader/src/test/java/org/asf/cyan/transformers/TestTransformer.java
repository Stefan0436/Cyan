package org.asf.cyan.transformers;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@TargetClass(target = "com.mojang.blaze3d.platform.Window")
public class TestTransformer {

	@InjectAt(location = InjectLocation.HEAD)
	public void setTitle(String title) {
		title = title + " - Tester";
	}

}
