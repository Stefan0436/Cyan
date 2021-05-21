package org.asf.cyan.api.internal.modkit.transformers._1_16.common.items;

import org.asf.cyan.api.events.ingame.items.ItemRegistryEvent;
import org.asf.cyan.api.events.objects.ingame.items.ItemRegistryEventObject;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

@FluidTransformer
@TargetClass(target = "net.minecraft.world.item.Items")
public class ItemsModification {

	@Constructor(clinit = true)
	@InjectAt(location = InjectLocation.TAIL)
	public static void clinit() {
		ItemRegistryEventObject cyanItems = new ItemRegistryEventObject();
		ItemRegistryEvent.getInstance().dispatch(cyanItems).getResult();
		ItemRegistryEvent.getInstance().registerItems(cyanItems);
	}

}
