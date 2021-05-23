package org.asf.cyan.api.internal.modkit.transformers._1_16.common.world;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.ingame.entities.EntityRegistryEvent;
import modkit.events.objects.ingame.entities.EntityRegistryEventObject;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.entity.EntityType")
public class EntityTypeModification {

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	public static void clinit() {
		EntityRegistryEventObject cyanEntities = new EntityRegistryEventObject();
		EntityRegistryEvent.getInstance().dispatch(cyanEntities).getResult();
		EntityRegistryEvent.getInstance().registerEntities(cyanEntities);
	}

}
