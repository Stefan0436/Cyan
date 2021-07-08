package org.asf.cyan.api.internal.modkit.transformers._1_17.common.network;

import org.asf.cyan.api.internal.ServerGamePacketListenerExtension;
import org.asf.cyan.api.internal.modkit.components._1_17.common.network.NetworkHooks;
import org.asf.cyan.fluid.api.FluidTransformer;
import org.asf.cyan.fluid.api.transforming.InjectAt;
import org.asf.cyan.fluid.api.transforming.TargetClass;
import org.asf.cyan.fluid.api.transforming.TargetType;
import org.asf.cyan.fluid.api.transforming.enums.InjectLocation;

import modkit.events.network.PlayerLogoutEvent;
import modkit.events.objects.network.PlayerLogoutEventObject;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

@FluidTransformer
@TargetClass(target = "net.minecraft.server.network.ServerGamePacketListenerImpl")
public class ServerGamePacketListenerModification implements ServerGamePacketListenerExtension {

	private String clientBrand;
	private final MinecraftServer server = null;
	public final Connection connection = null;
	public ServerPlayer player;

	public boolean connectedCyan = false;

	@InjectAt(location = InjectLocation.HEAD)
	public void handleCustomPayload(
			@TargetType(target = "net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket") ServerboundCustomPayloadPacket packet) {
		if (NetworkHooks.handlePayload(packet, server, connection, player, clientBrand, this::setBrand,
				this::setConnectedCyan)) {
			return;
		}

		return;
	}

	public void setConnectedCyan(boolean state) {
		connectedCyan = state;
	}

	@InjectAt(location = InjectLocation.TAIL)
	public void onDisconnect(@TargetType(target = "net.minecraft.network.chat.Component") Component message) {
		if (connectedCyan)
			PlayerLogoutEvent.getInstance()
					.dispatch(new PlayerLogoutEventObject(connection, server, player, clientBrand, message))
					.getResult();
	}

	private void setBrand(String brand) {
		clientBrand = brand;
	}

	@Override
	public String cyanClientBrand() {
		return clientBrand;
	}
}
