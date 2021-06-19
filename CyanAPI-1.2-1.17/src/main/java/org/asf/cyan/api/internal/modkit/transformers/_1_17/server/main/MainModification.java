package org.asf.cyan.api.internal.modkit.transformers._1_17.server.main;

import org.asf.cyan.api.fluid.annotations.PlatformExclude;
import org.asf.cyan.api.internal.ModKitController;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@PlatformExclude(LaunchPlatform.SPIGOT)
@TargetClass(target = "net.minecraft.server.Main")
public class MainModification {
	
	@InjectAt(location = InjectLocation.HEAD)
	public static void main(String[] args) {
		new ModKitController().begin(MainModification.class.getClassLoader());
	}
	
}
