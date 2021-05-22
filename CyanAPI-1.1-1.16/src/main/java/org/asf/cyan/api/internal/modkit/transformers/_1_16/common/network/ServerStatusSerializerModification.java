package org.asf.cyan.api.internal.modkit.transformers._1_16.common.network;

import java.lang.reflect.Type;

import org.asf.cyan.api.protocol.transformers.handshake.VersionStatusTransformer;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import net.minecraft.network.protocol.status.ServerStatus;

@FluidTransformer
@TargetClass(target = "net.minecraft.network.protocol.status.ServerStatus$Serializer")
public class ServerStatusSerializerModification {

	@InjectAt(location = InjectLocation.TAIL)
	@TargetType(target = "com.google.gson.JsonElement")
	public void serialize(@TargetType(target = "net.minecraft.network.protocol.status.ServerStatus") ServerStatus var1,
			Type var2, JsonSerializationContext var3, @LocalVariable JsonObject data) {
		VersionStatusTransformer.applySerializeMethodTransformer(data);
	}

	@InjectAt(location = InjectLocation.TAIL)
	@TargetType(target = "net.minecraft.network.protocol.status.ServerStatus")
	public void deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3,
			@LocalVariable JsonObject data,
			@LocalVariable @TargetType(target = "net.minecraft.network.protocol.status.ServerStatus") ServerStatus output) {
		((ServerStatusInterface) output).setJson(data);
	}

}
