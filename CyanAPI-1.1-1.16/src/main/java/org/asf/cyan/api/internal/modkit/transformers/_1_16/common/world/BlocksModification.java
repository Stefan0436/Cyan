package org.asf.cyan.api.internal.modkit.transformers._1_16.common.world;

import org.asf.cyan.api.events.ingame.blocks.BlockRegistryEvent;
import org.asf.cyan.api.events.objects.ingame.blocks.BlockRegistryEventObject;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.level.block.Blocks")
public class BlocksModification {

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL, targetCall = "register(java.lang.String,net.minecraft.world.level.block.Block)")
	public static void clinit() {
		BlockRegistryEventObject cyanBlocks = new BlockRegistryEventObject();
		BlockRegistryEvent.getInstance().dispatch(cyanBlocks).getResult();
		BlockRegistryEvent.getInstance().registerBlocks(cyanBlocks);
	}

}
