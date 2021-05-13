package org.asf.cyan.modifications._1_15_2.common.forge;

import java.util.stream.Stream;

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

	private String[] cyanClasses;

	@InjectAt(location = InjectLocation.HEAD)
	protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		if (cyanClasses == null) {
			cyanClasses = new String[] {
				"org.asf.cyan.CyanLoader",
				"org.asf.cyan.mods.events.IEventListenerContainer",
				"org.asf.cyan.mods.internal.BaseEventController",
				"org.asf.cyan.mods.IMod",
				"org.asf.cyan.mods.ICoreMod",
				"org.asf.cyan.mods.AbstractCoremod",
				"org.asf.cyan.mods.AbstractMod",
				"org.asf.cyan.mods.IBaseMod",
				"org.asf.cyan.core.CyanCore",
				"org.asf.cyan.api.modloader.Modloader",
				"org.asf.cyan.api.common.CyanComponent",
				"org.asf.cyan.api.config.Configuration",
				"org.asf.cyan.api.util.EventUtil",
				"org.asf.cyan.api.util.ContainerConditions",
				"org.asf.cyan.api.internal.CyanAPIComponent"
			};
		}
		if (Stream.of(cyanClasses).anyMatch(t -> t.equals(name)) || CyanLoader.noLoadClassForge(name)) {
			return getParent().loadClass(name);
		}

		return null;
	}

}
