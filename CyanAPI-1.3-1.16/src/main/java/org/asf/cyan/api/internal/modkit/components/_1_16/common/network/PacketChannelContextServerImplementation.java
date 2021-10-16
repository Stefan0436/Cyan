package org.asf.cyan.api.internal.modkit.components._1_16.common.network;

import java.util.Arrays;
import java.util.function.Supplier;

import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.ServerGamePacketListenerExtension;
import org.asf.cyan.api.internal.modkit.components._1_16.common.network.buffer.FriendlyByteBufOutputFlow;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.internal.modkitimpl.channels.PacketProcessorList;

import io.netty.buffer.Unpooled;
import modkit.events.objects.network.ClientConnectionEventObject;
import modkit.events.objects.network.ServerConnectionEventObject;
import modkit.network.ByteFlow;
import modkit.network.PacketWriter;
import modkit.network.channels.AbstractPacketProcessor;
import modkit.network.channels.PacketChannel;
import modkit.network.channels.PacketChannelContext;
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
	protected void sendPacket(String channel, String id, PacketWriter writer, boolean allowSplit) {
		FriendlyByteBuf buffer = null;
		if (writer == null) {
			buffer = new FriendlyByteBuf(Unpooled.buffer(0));
		} else {
			if (writer.getOutput() instanceof FriendlyByteBufOutputFlow)
				buffer = ((FriendlyByteBufOutputFlow) writer.getOutput()).toBuffer();
			if (allowSplit && buffer.readableBytes() > 32767) {
				NetworkHooks.splitSendClient(channel + ":" + id,
						Arrays.copyOfRange(buffer.array(), 0, buffer.writerIndex()), getConnection(),
						System.currentTimeMillis(), player.tickCount);
				return;
			}
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
	protected boolean runProcessor(PacketChannel channel, String id, ByteFlow flow, PrepareCall prepare,
			ProcessCall process) {
		for (AbstractPacketProcessor proc : processors) {
			if (proc.regexId() && id.matches(proc.id()) && prepare.call(channel, proc)) {
				if (process.call(channel, proc, id, flow))
					return true;
			} else if (proc.id().equalsIgnoreCase(id) && prepare.call(channel, proc)) {
				if (process.call(channel, proc, id, flow))
					return true;
			}
		}
		return false;
	}

}
