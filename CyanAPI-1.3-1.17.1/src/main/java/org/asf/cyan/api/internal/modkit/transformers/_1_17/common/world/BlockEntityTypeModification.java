package org.asf.cyan.api.internal.modkit.transformers._1_17.common.world;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.ingame.blocks.BlockEntityRegistryEvent;
import modkit.events.objects.ingame.blocks.BlockEntityRegistryEventObject;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.level.block.entity.BlockEntityType")
public class BlockEntityTypeModification {

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	public static void clinit() {
		BlockEntityRegistryEventObject cyanEntities = new BlockEntityRegistryEventObject();
		BlockEntityRegistryEvent.getInstance().dispatch(cyanEntities).getResult();
		BlockEntityRegistryEvent.getInstance().registerEntities(cyanEntities);
	}

}
