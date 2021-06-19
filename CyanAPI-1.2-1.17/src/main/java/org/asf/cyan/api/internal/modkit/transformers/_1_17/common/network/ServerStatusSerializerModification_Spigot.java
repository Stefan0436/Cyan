package org.asf.cyan.api.internal.modkit.transformers._1_17.common.network;

import java.lang.reflect.Type;

import org.asf.cyan.CyanLoader;
import org.asf.cyan.api.modloader.Modloader;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;
import org.asf.cyan.api.fluid.annotations.PlatformOnly;
import org.asf.cyan.api.modloader.information.game.LaunchPlatform;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;

import modkit.protocol.transformers.handshake.VersionStatusTransformer;
import net.minecraft.network.protocol.status.ServerStatus;

@FluidTransformer
@PlatformOnly(LaunchPlatform.SPIGOT)
@TargetClass(target = "net.minecraft.network.protocol.status.ServerStatus$Serializer")
public class ServerStatusSerializerModification_Spigot {

	@InjectAt(location = InjectLocation.TAIL)
	@TargetType(target = "com.google.gson.JsonElement")
	public void serialize(Object var1, Type var2, JsonSerializationContext var3, @LocalVariable JsonObject data) {
		if (Modloader.getModloader(CyanLoader.class).isRootModloader())
			VersionStatusTransformer.applySerializeMethodTransformer(data);
	}

	@InjectAt(location = InjectLocation.TAIL)
	@TargetType(target = "java.lang.Object")
	public void deserialize(JsonElement var1, Type var2, JsonDeserializationContext var3,
			@LocalVariable JsonObject data,
			@LocalVariable @TargetType(target = "net.minecraft.network.protocol.status.ServerStatus") ServerStatus output) {
		((ServerStatusInterface) output).setJson(data);
	}

}
