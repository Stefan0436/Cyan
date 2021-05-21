package org.asf.cyan.api.internal.modkit.components._1_16.client;

import java.util.function.Supplier;

import org.asf.cyan.api.events.objects.network.ClientConnectionEventObject;
import org.asf.cyan.api.events.objects.network.ServerConnectionEventObject;
import org.asf.cyan.api.internal.ClientPacketListenerExtension;
import org.asf.cyan.api.internal.IModKitComponent;
import org.asf.cyan.api.internal.modkit.components._1_16.common.network.buffer.FriendlyByteBufOutputFlow;
import org.asf.cyan.api.modloader.information.game.GameSide;
import org.asf.cyan.api.network.PacketWriter;
import org.asf.cyan.api.network.channels.AbstractPacketProcessor;
import org.asf.cyan.api.network.channels.PacketChannelContext;
import org.asf.cyan.internal.modkitimpl.channels.PacketProcessorList;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.GenericDirtMessageScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.realms.RealmsBridge;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public class PacketChannelContextClientImplementation extends PacketChannelContext implements IModKitComponent {

	private Level level;
	private Player player;

	private Connection connection;
	private String serverBrand;

	private PacketProcessorList processors = new PacketProcessorList();

	@Override
	public void initializeComponent() {
		clientImplementation = this;
	}

	@Override
	protected PacketChannelContext setupNew(ServerConnectionEventObject clientConnection) {
		return null;
	}

	@Override
	protected PacketChannelContext setupNew(ServerPlayer server) {
		return null;
	}

	@Override
	protected PacketChannelContext setupNew(Supplier<Minecraft> clientSupplier) {
		PacketChannelContextClientImplementation inst = new PacketChannelContextClientImplementation();
		Minecraft client = clientSupplier.get();
		if (client.player != null) {
			inst.level = client.player.level;
			inst.player = client.player;
		}
		if (client.getConnection() != null) {
			inst.connection = client.getConnection().getConnection();
			inst.serverBrand = ((ClientPacketListenerExtension) client.getConnection()).getServerBrand();
		}
		return inst;
	}

	@Override
	@SuppressWarnings("resource")
	protected PacketChannelContext setupNew(ClientConnectionEventObject clientConnection) {
		PacketChannelContextClientImplementation inst = new PacketChannelContextClientImplementation();
		inst.level = clientConnection.getClient().player.level;
		inst.player = clientConnection.getClient().player;
		inst.connection = clientConnection.getConnection();
		inst.serverBrand = clientConnection.getServerBrand();
		return inst;
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
		return serverBrand;
	}

	@Override
	protected GameSide getSide() {
		return GameSide.CLIENT;
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
			getConnection().send(new ServerboundCustomPayloadPacket(new ResourceLocation(channel, id), buffer));
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

	@Override
	protected void disconnectClient() {
		Minecraft client = ((ClientPacketListenerExtension) ((LocalPlayer) player).connection).cyanGetMinecraft();
		boolean wasLocal = client.isLocalServer();
		boolean wasRealms = client.isConnectedToRealms();
		client.level.disconnect();

		if (!wasLocal) {
			client.clearLevel();
		} else {
			client.clearLevel(new GenericDirtMessageScreen(new TranslatableComponent("menu.savingLevel")));
		}

		if (wasLocal) {
			client.setScreen(new TitleScreen());
		} else if (wasRealms) {
			RealmsBridge bridge = new RealmsBridge();
			bridge.switchToRealms(new TitleScreen());
		} else {
			client.setScreen(new JoinMultiplayerScreen(new TitleScreen()));
		}
	}

}
