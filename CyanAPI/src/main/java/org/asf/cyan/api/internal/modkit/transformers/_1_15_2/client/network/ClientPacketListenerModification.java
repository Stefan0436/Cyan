package org.asf.cyan.api.internal.modkit.transformers._1_15_2.client.network;

import org.asf.cyan.api.events.extended.EventObject.EventResult;
import org.asf.cyan.api.events.network.ClientPacketEvent;
import org.asf.cyan.api.events.network.ClientSideLoginEvent;
import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ClientPacketEventObject;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.LocalVariable;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;

@FluidTransformer
@TargetClass(target = "net.minecraft.client.multiplayer.ClientPacketListener")
public class ClientPacketListenerModification implements ClientPacketListenerExtension {

	private String serverBrand;
	private Minecraft minecraft;

	private final Connection connection = null;

	public String getBrand() {
		return serverBrand;
	}

	@InjectAt(location = InjectLocation.TAIL, offset = 1)
	public void handleLogin(
			@TargetType(target = "net.minecraft.network.protocol.game.ClientboundLoginPacket") ClientboundLoginPacket packet) {
		connection.send(new ServerboundCustomPayloadPacket(new ResourceLocation("cyan.requestserverbrand"),
				new FriendlyByteBuf(Unpooled.buffer(0))));

		for (int i = 0; i < 500; i++) {
			if (serverBrand != null)
				break;
			try {
				Thread.sleep(1);
			} catch (InterruptedException e) {
				break;
			}
		}

		ClientSideLoginEvent.getInstance().dispatch(new ClientConnectionEventObject(connection, minecraft, serverBrand))
				.getResult();
	}

	@InjectAt(location = InjectLocation.HEAD, targetCall = "equals(java.lang.Object)", targetOwner = "net.minecraft.resources.ResourceLocation")
	public void handleCustomPayload(
			@TargetType(target = "net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket") ClientboundCustomPayloadPacket packet,
			@LocalVariable ResourceLocation type, @LocalVariable FriendlyByteBuf buffer) {
		if (ClientboundCustomPayloadPacket.BRAND.equals(type)) {
			minecraft.player.setServerBrand(buffer.readUtf(32767));
			serverBrand = minecraft.player.getServerBrand();
			return;
		} else if (type.getPath().startsWith("cyan.")) {
			if (serverBrand == null)
				serverBrand = "Cyan";

			if (ClientPacketEvent.getInstance().dispatch(new ClientPacketEventObject(connection, packet, serverBrand,
					minecraft, type.getPath().substring(5))).getResult() == EventResult.CANCEL)
				return;
		}
	}

}
