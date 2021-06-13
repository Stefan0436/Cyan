package org.asf.cyan.api.internal.modkit.transformers._1_17.common;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import com.mojang.datafixers.DataFixerBuilder;

import modkit.events.core.DataFixerEvent;
import modkit.events.objects.core.DataFixerEventObject;

@FluidTransformer
@TargetClass(target = "net.minecraft.util.datafix.DataFixers")
public class DataFixersModification {

	@InjectAt(location = InjectLocation.TAIL)
	private static void addFixers(
			@TargetType(target = "com.mojang.datafixers.DataFixerBuilder") DataFixerBuilder builder) {
		DataFixerEvent.getInstance().dispatch(new DataFixerEventObject(builder)).getResult();
	}

}
