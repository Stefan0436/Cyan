package org.asf.cyan.modifications._1_15_2.server.paper;

import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;

@TargetClass(target = "net.minecraft.resources.ResourceLocation")
public class ResourceLocationMock {

	@Reflect
	public String getNamespace() {
		return null;
	}

	@Reflect
	public String getPath() {
		return null;
	}

}
