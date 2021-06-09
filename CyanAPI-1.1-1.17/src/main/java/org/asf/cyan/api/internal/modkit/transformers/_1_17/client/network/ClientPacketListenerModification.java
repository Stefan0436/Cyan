package org.asf.cyan.api.internal.modkit.transformers._1_17.client.network;

import org.asf.cyan.api.internal.ClientPacketListenerExtension;
import org.asf.cyan.api.internal.modkit.components._1_17.common.network.NetworkHooks;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.ingame.entities.EntityRegistryEvent;
import modkit.events.ingame.entities.EntityRegistryEvent.EntityTypeEntry;
import modkit.events.network.ClientSideLoginEvent;
import modkit.events.objects.network.ClientConnectionEventObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.multiplayer.ClientPacketListener")
public class ClientPacketListenerModification implements ClientPacketListenerExtension {

	private String serverBrand;
	private Minecraft minecraft;
	private ClientLevel level;

	private final Connection connection = null;

	public String getServerBrand() {
		return serverBrand;
	}

	@InjectAt(location = InjectLocation.TAIL)
	public void handleLogin(
			@TargetType(target = "net.minecraft.network.protocol.game.ClientboundLoginPacket") ClientboundLoginPacket packet) {
		ClientSideLoginEvent.getInstance().dispatch(new ClientConnectionEventObject(connection, minecraft, serverBrand))
				.getResult();
	}

	@InjectAt(location = InjectLocation.HEAD)
	public void handleAddEntity(
			@TargetType(target = "net.minecraft.network.protocol.game.ClientboundAddEntityPacket") ClientboundAddEntityPacket packet) {
		EntityTypeEntry moddedCyanEntityType = EntityRegistryEvent.getInstance().findEntity(packet.getType());
		if (moddedCyanEntityType != null) {
			Entity entity = moddedCyanEntityType.getConstructor().apply(moddedCyanEntityType.type, level);
			entity.setId(packet.getId());
			entity.moveTo(packet.getX(), packet.getY(), packet.getZ());
			entity.setXRot((packet.getxRot() * 360) / 256f); // credits to mojang
			entity.setYRot((packet.getyRot() * 360) / 256f); // credits to mojang
			entity.setUUID(packet.getUUID());
			level.putNonPlayerEntity(entity.getId(), entity);
		}

		return;
	}

	@InjectAt(location = InjectLocation.HEAD, targetCall = "equals(java.lang.Object)", targetOwner = "net.minecraft.resources.ResourceLocation")
	public void handleCustomPayload(
			@TargetType(target = "net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket") ClientboundCustomPayloadPacket packet,
			@LocalVariable ResourceLocation type, @LocalVariable FriendlyByteBuf buffer) {
		if (NetworkHooks.handlePayload(packet, type, buffer, minecraft, this::setBrand, serverBrand, connection)) {
			return;
		}

		return;
	}

	private void setBrand(String brand) {
		serverBrand = brand;
	}

	@Override
	public Minecraft cyanGetMinecraft() {
		return minecraft;
	}

}
