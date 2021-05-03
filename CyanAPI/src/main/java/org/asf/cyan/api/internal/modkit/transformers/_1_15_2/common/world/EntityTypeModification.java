package org.asf.cyan.api.internal.modkit.transformers._1_15_2.common.world;

import org.asf.cyan.api.events.entities.EntityRegistryEvent;
import org.asf.cyan.api.events.objects.entities.EntityRegistryEventObject;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.entity.EntityType")
public class EntityTypeModification {

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	public static void clinit() {
		EntityRegistryEventObject cyanEntities = new EntityRegistryEventObject();
		EntityRegistryEvent.getInstance().dispatch(cyanEntities).getResult();
	}

}
