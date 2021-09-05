package org.asf.cyan.api.internal.modkit.transformers._1_17.client.keys;

import java.util.Map;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import com.mojang.blaze3d.platform.InputConstants.Type;

import modkit.events.keys.KeyBindingCategoryRegistryEvent;
import modkit.events.objects.keys.KeyBindingCategoryRegistryEventObject;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.KeyMapping")
public class KeyMappingModification {

	private static final Map<String, Integer> CATEGORY_SORT_ORDER = null;
	private static boolean cyanAddedCategories = false;

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	public void ctor(String var1, @TargetType(target = "com.mojang.blaze3d.platform.InputConstants$Type") Type var2,
			int var3, String var4) {
		if (!cyanAddedCategories) {
			KeyBindingCategoryRegistryEventObject cyanCategories = new KeyBindingCategoryRegistryEventObject();
			KeyBindingCategoryRegistryEvent.getInstance().dispatch(cyanCategories).getResult();

			Map<String, Integer> moddedCyanCategories = cyanCategories.getCategories();
			moddedCyanCategories.forEach((id, pos) -> {
				CATEGORY_SORT_ORDER.putIfAbsent(id, pos);
			});
			cyanAddedCategories = true;
		}
	}

}
