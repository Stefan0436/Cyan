package org.asf.cyan.api.internal.modkit.transformers._1_16.common.world;

import org.asf.cyan.api.events.ingame.materials.MaterialCreationEvent;
import org.asf.cyan.api.events.objects.ingame.materials.MaterialCreationEventObject;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.level.material.Material")
public class MaterialModification {

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	public static void clinit() {
		MaterialCreationEventObject cyanMaterials = new MaterialCreationEventObject();
		MaterialCreationEvent.getInstance().dispatch(cyanMaterials).getResult();
		MaterialCreationEvent.getInstance().registerMaterials(cyanMaterials);
	}

}
