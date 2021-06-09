package org.asf.cyan.api.internal.modkit.transformers._1_16.server.main;

import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.internal.ModKitController;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@PlatformOnly(LaunchPlatform.SPIGOT)
@TargetClass(target = "org.bukkit.craftbukkit.Main")
public class SpigotMainModification {
	
	@InjectAt(location = InjectLocation.HEAD)
	public static void main(String[] args) {
		new ModKitController().begin(SpigotMainModification.class.getClassLoader());
	}
	
}
