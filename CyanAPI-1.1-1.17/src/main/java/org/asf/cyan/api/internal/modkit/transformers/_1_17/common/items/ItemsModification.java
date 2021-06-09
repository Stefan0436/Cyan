package org.asf.cyan.api.internal.modkit.transformers._1_17.common.items;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.ingame.items.ItemRegistryEvent;
import modkit.events.objects.ingame.items.ItemRegistryEventObject;

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
