package org.asf.cyan.api.internal.modkit.components._1_16.common.network.packets.channels;

import java.util.function.Supplier;

import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.ServerGamePacketListenerExtension;
import org.asf.cyan.api.internal.modkit.components._1_16.common.network.packets.buffer.FriendlyByteBufOutputFlow;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.network.PacketWriter;
import org.asf.cyan.api.network.channels.AbstractPacketProcessor;
import org.asf.cyan.api.network.channels.PacketChannelContext;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PacketChannelContextServerImplementation extends PacketChannelContext implements IModKitComponent {

	private Level level;
	private Player player;

	private Connection connection;
	private String clientBrand;

	private PacketProcessorList processors = new PacketProcessorList();

	@Override
	public void initializeComponent() {
		serverImplementation = this;
	}

	@Override
	protected PacketChannelContext setupNew(ServerConnectionEventObject serverConnection) {
		PacketChannelContextServerImplementation inst = new PacketChannelContextServerImplementation();
		inst.level = serverConnection.getPlayer().level;
		inst.player = serverConnection.getPlayer();
		inst.connection = serverConnection.getConnection();
		inst.clientBrand = serverConnection.getClientBrand();
		return inst;
	}

	@Override
	protected PacketChannelContext setupNew(ServerPlayer server) {
		PacketChannelContextServerImplementation inst = new PacketChannelContextServerImplementation();
		inst.level = server.level;
		inst.player = server;
		inst.connection = server.connection.getConnection();
		inst.clientBrand = ((ServerGamePacketListenerExtension) server.connection).cyanClientBrand();
		return inst;
	}

	@Override
	protected PacketChannelContext setupNew(Supplier<Minecraft> clientSupplier) {
		return null;
	}

	@Override
	protected PacketChannelContext setupNew(ClientConnectionEventObject clientConnection) {
		return null;
	}

	@Override
	protected Connection getConnection() {
		return connection;
	}

	@Override
	protected Player getPlayer() {
		return player;
	}

	@Override
	protected String getRemoteBrand() {
		return clientBrand;
	}

	@Override
	protected GameSide getSide() {
		return GameSide.SERVER;
	}

	@Override
	protected Level getLevel() {
		return level;
	}

	@Override
	protected void sendPacket(String channel, String id, PacketWriter writer) {
		FriendlyByteBuf buffer = null;
		if (writer == null) {
			buffer = new FriendlyByteBuf(Unpooled.buffer(0));
		} else {
			if (writer.getOutput() instanceof FriendlyByteBufOutputFlow)
				buffer = ((FriendlyByteBufOutputFlow) writer.getOutput()).toBuffer();
		}
		if (buffer != null) {
			getConnection().send(new ClientboundCustomPayloadPacket(new ResourceLocation(channel, id), buffer));
		}
	}

	@Override
	protected PacketWriter newPacketWriter() {
		return PacketWriter.create(new FriendlyByteBufOutputFlow());
	}

	@Override
	protected <T extends AbstractPacketProcessor> void registerProcessor(Class<T> processor) {
		processors.add(processor);
	}

	@Override
	protected AbstractPacketProcessor getProcessor(String id) {
		for (AbstractPacketProcessor proc : processors) {
			if (proc.regexId() && id.matches(proc.id())) {
				return proc;
			} else if (proc.id().equalsIgnoreCase(id)) {
				return proc;
			}
		}
		return null;
	}

}
