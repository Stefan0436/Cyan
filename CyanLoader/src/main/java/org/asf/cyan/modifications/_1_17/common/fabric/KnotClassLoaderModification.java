package org.asf.cyan.modifications._1_17.common.fabric;

import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.Reflect;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@PlatformOnly(LaunchPlatform.INTERMEDIARY)
@TargetClass(target = "net.fabricmc.loader.launch.knot.KnotClassLoader")
public class KnotClassLoaderModification {

	@Reflect
	protected ClassLoader getParent() {
		return null;
	}

	@InjectAt(location = InjectLocation.HEAD)
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (name.equals("com.mojang.util.QueueLogAppender") || name.startsWith("org.apache.logging."))
			return ClassLoader.getSystemClassLoader().loadClass(name);
		
		return null;
	}
	
}
