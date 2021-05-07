package org.asf.cyan.api.internal.modkit.transformers._1_15_2.common.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public interface ServerboundCustomPayloadPacketExtension {
	public FriendlyByteBuf readDataCyan();
	public ResourceLocation getIdentifierCyan();
}
