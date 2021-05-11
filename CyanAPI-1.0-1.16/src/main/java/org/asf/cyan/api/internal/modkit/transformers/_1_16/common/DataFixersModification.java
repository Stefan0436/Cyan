package org.asf.cyan.api.internal.modkit.transformers._1_16.common;

import org.asf.cyan.api.events.core.DataFixerEvent;
import org.asf.cyan.api.events.objects.core.DataFixerEventObject;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import com.mojang.datafixers.DataFixerBuilder;

@FluidTransformer
@TargetClass(target = "net.minecraft.util.datafix.DataFixers")
public class DataFixersModification {

	@InjectAt(location = InjectLocation.TAIL)
	private static void addFixers(
			@TargetType(target = "com.mojang.datafixers.DataFixerBuilder") DataFixerBuilder builder) {
		DataFixerEvent.getInstance().dispatch(new DataFixerEventObject(builder)).getResult();
	}

}
