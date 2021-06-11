package org.asf.cyan.modifications._1_17.common.forge;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@PlatformOnly(LaunchPlatform.MCP)
@TargetClass(target = "cpw.mods.modlauncher.TransformingClassLoader")
public class TransformingClassLoaderModification {

	@Reflect
	protected ClassLoader getParent() {
		return null;
	}

	@InjectAt(location = InjectLocation.HEAD)
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (CyanLoader.doNotTransform(name)) {
			return getParent().loadClass(name);
		}

		return null;
	}

}
