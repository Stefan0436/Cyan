package org.asf.cyan.api.internal.modkit.transformers._1_16.common.network;

import java.lang.reflect.Type;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.internal.modkitimpl.util.HandshakeUtils;

import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.network.protocol.status.ServerStatus.Version;

@FluidTransformer
@TargetClass(target = "net.minecraft.network.protocol.status.ServerStatus$Serializer")
public class ServerStatusModification {

	@InjectAt(location = InjectLocation.TAIL)
	@TargetType(target = "com.google.gson.JsonElement")
	public void serialize(
			@TargetType(target = "net.minecraft.network.protocol.status.ServerStatus$Version") Version var1, Type var2,
			JsonSerializationContext var3, @LocalVariable JsonObject data) {
		HandshakeUtils.getImpl().onSerializeJson(data);
	}

}
