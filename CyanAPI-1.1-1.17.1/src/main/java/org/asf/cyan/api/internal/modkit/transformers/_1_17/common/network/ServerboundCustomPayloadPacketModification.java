package org.asf.cyan.api.internal.modkit.transformers._1_17.common.network;

import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.TargetClass;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

@FluidTransformer
@TargetClass(target = "net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket")
public class ServerboundCustomPayloadPacketModification implements ServerboundCustomPayloadPacketExtension {

	private FriendlyByteBuf data;
	private ResourceLocation identifier;

	@Override
	public FriendlyByteBuf readDataCyan() {
		return data;
	}

	@Override
	public ResourceLocation getIdentifierCyan() {
		return identifier;
	}

}
