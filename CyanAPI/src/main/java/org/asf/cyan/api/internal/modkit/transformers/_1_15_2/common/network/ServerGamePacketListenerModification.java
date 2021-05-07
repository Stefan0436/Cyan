package org.asf.cyan.api.internal.modkit.transformers._1_15_2.common.network;

import org.asf.cyan.api.events.extended.EventObject.EventResult;
import org.asf.cyan.api.events.network.ServerPacketEvent;
import org.asf.cyan.api.events.network.ServerSideConnectedEvent;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ServerPacketEventObject;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.Constructor;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import io.netty.buffer.Unpooled;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

@FluidTransformer
@TargetClass(target = "net.minecraft.server.network.ServerGamePacketListenerImpl")
public class ServerGamePacketListenerModification {

	private String clientBrand;
	private final MinecraftServer server = null;
	public final Connection connection = null;
	public ServerPlayer player;

	@Constructor
	@InjectAt(location = InjectLocation.TAIL)
	public void init(@TargetType(target = "net.minecraft.server.MinecraftServer") MinecraftServer server,
			@TargetType(target = "net.minecraft.network.Connection") Connection connection,
			@TargetType(target = "net.minecraft.server.level.ServerPlayer") ServerPlayer player) {
		ServerSideConnectedEvent.getInstance()
				.dispatch(new ServerConnectionEventObject(connection, server, player, clientBrand)).getResult();
	}

	@InjectAt(location = InjectLocation.HEAD)
	public void handleCustomPayload(
			@TargetType(target = "net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket") ServerboundCustomPayloadPacket packet) {
		FriendlyByteBuf data = ((ServerboundCustomPayloadPacketExtension) packet).readDataCyan();
		ResourceLocation id = ((ServerboundCustomPayloadPacketExtension) packet).getIdentifierCyan();
		if (id.getPath().startsWith("cyan.")) {
			if (id.getPath().equals("cyan.requestserverbrand")) {
				connection.send(
						new ClientboundCustomPayloadPacket(ClientboundCustomPayloadPacket.BRAND,
						new FriendlyByteBuf(Unpooled.buffer()).writeUtf(server.getServerModName())));
				return;
			} else {
				if (ServerPacketEvent.getInstance().dispatch(new ServerPacketEventObject(connection, packet, player,
						clientBrand, server, id.getPath().substring(5))).getResult() == EventResult.CANCEL)
					return;
			}
		} else if (ServerboundCustomPayloadPacket.BRAND.equals(id)) {
			clientBrand = data.readUtf(32767);
			return;
		}
	}
}
