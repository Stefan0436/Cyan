package org.asf.cyan.api.internal.modkit.transformers._1_16.client.main;

import org.asf.cyan.api.internal.ModKitController;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.main.Main")
public class MainModification {
	
	@InjectAt(location = InjectLocation.HEAD)
	public static void main(String[] args) {
		new ModKitController().begin(MainModification.class.getClassLoader());
	}
	
}
